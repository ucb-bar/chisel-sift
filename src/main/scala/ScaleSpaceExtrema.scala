package SIFT

import Chisel._
import Node._
import scala.collection.mutable.HashMap

case class ImageType(width:UInt, height:UInt, dwidth: Int = 8)

class Pixel extends Bundle {
  val r = UInt(width = 8)
  val g = UInt(width = 8)
  val b = UInt(width = 8)
}

class Coord(it: ImageType) extends Bundle {
  val col = UInt(OUTPUT,width=it.width.getWidth)
  val row = UInt(OUTPUT,width=it.height.getWidth)
  override def clone: this.type = {
    new Coord(new ImageType(UInt(width=col.getWidth),
      UInt(width=row.getWidth))).asInstanceOf[this.type];
  }
}

class Counter(max: UInt) extends Module {
  val io = new Bundle {
    val reset = Bool(INPUT)
    val en = Bool(INPUT)
    val count = UInt(OUTPUT, max.getWidth)
    val top = Bool(OUTPUT)
  }

  val x = Reg(init=UInt(0, max.getWidth))
  
  io.count := x
  io.top := x === max

  when (io.en) {x := Mux(io.top, UInt(0), x + UInt(1))}
  when (io.reset) {x := UInt(0)}
}

class ImageCounter(it: ImageType) extends Module {
  val io = new Bundle {
    val reset = Bool(INPUT)
    val en = Bool(INPUT)
    val out = new Coord(it).asOutput
    val top = Bool(OUTPUT)
  }
  
  val col_counter = Module(new Counter(it.width-UInt(1)))
  val row_counter = Module(new Counter(it.height-UInt(1)))

  col_counter.io.reset := io.reset
  row_counter.io.reset := io.reset

  col_counter.io.en := io.en
  row_counter.io.en := io.en & col_counter.io.top
  
  io.out.col := col_counter.io.count
  io.out.row := row_counter.io.count

  io.out.top := col_counter.io.top & row_counter.io.top
}

class ScaleSpaceExtrema(it: ImageType) extends Module {
  val io = new Bundle {
    val reset = Bool(INPUT)
    val in = Valid(UInt(width=it.dwidth)).asInput
    val img = Valid(UInt(width=it.dwidth)).asOutput
    val coord = Valid(new Coord(it)).asOutput
  }

  val ic = Module(new ImageCounter(it))
  ic.io.reset := io.reset
  ic.io.en := io.in.valid

  // Test image output
  io.img.valid := io.in.valid
  io.img.bits := (io.in.bits >> UInt(1)) & UInt(0x7F7F7F)

  // Coordinate output
  io.coord.valid := io.in.valid & (ic.io.out.row > ic.io.out.col) & (io.in.bits < UInt(128))
  io.coord.bits <> ic.io.out
}

class ScaleSpaceExtremaTests(c: ScaleSpaceExtrema, val infilename: String,
  val imgfilename: String, val coordfilename: String) 
  extends Tester(c, Array(c.io)) {
  
  defTests {
    val svars = new HashMap[Node, Node]()
    val ovars = new HashMap[Node, Node]()

    val inPic = Image(infilename)
    val imgPic = Image(inPic.w, inPic.h, inPic.d)
    val coordPic = Image(inPic.w, inPic.h, inPic.d)
    val n_byte = inPic.d/8

    svars(c.io.reset) = Bool(true)
    
    step(svars, ovars, false)
    
    svars(c.io.reset) = Bool(false)
    svars(c.io.in.valid) = Bool(true)
    
    for (i <- 0 until inPic.data.length/n_byte) {
      var pixel = 0
      for (j <- 0 until n_byte) {
        pixel = pixel << 8
        val rin = inPic.data(3*i+j)
        val  in = if (rin < 0) 256 + rin else rin
        pixel += in
      }
      
      svars(c.io.in.bits) = Bits(pixel)
      step(svars, ovars, false)
      
      // Write debug image out
      val imgpix = ovars(c.io.img.bits).litValue()
      for (j <- 0 until n_byte) {
        imgPic.data(3*i+j) = ((imgpix >> (8*j)) & 0xFF).toByte
      }

      // Color pixel red if outputting valid coord, grey otherwise
      val coord = ovars(c.io.coord.valid).litValue()
      val coordpix = if (coord.testBit(0)) 0xFF0000 else 0x808080
      for (j <- 0 until n_byte) {
        coordPic.data(3*i+j) = ((coordpix >> (8*j)) & 0xFF).toByte
      }
    }

    imgPic.write(imgfilename)
    coordPic.write(coordfilename)

    true
  }
}

