package SIFT

import Chisel._
import Node._
import scala.collection.mutable.HashMap

case class ImageType(width:UInt, height:UInt) 

/*{
  def this(filename: String) = {
    val img = Image(filename)
    this(UInt(img.w), UInt(img.h))
  }
}*/

class Pixel extends Bundle {
  val r = UInt(width = 8)
  val g = UInt(width = 8)
  val b = UInt(width = 8)
}

class Coord(it: ImageType) extends Bundle {
  val col = UInt(OUTPUT,width=it.width.getWidth)
  val row = UInt(OUTPUT,width=it.height.getWidth)
  override def clone: this.type = {
    new Coord(new ImageType(UInt(width=col.getWidth),UInt(width=row.getWidth))).asInstanceOf[this.type];
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
    val valid = Bool(INPUT)
    val out = new Coord(it).asOutput
  }
  
  val col_counter = Module(new Counter(it.width-UInt(1)))
  val row_counter = Module(new Counter(it.height-UInt(1)))

  col_counter.io.reset := io.reset
  row_counter.io.reset := io.reset

  col_counter.io.en := io.valid
  row_counter.io.en := io.valid & col_counter.io.top
  
  io.out.col := col_counter.io.count
  io.out.row := row_counter.io.count
}

/*object ExtremaDetector {
  def apply(p: Pixel) = {(p.r + p.g + p.b) > UInt(383))
  def apply(p: UInt) = {p > UInt(127)}
}*/

class ScaleSpaceExtrema(it: ImageType) extends Module {
  val io = new Bundle {
    val reset = Bool(INPUT)
    val in = Valid(UInt(width=8)).asInput
    val out = Valid(new Coord(it)).asOutput
  }

  val ic = Module(new ImageCounter(it))
  ic.io.reset := io.reset
  ic.io.valid := io.in.valid

  io.out.valid := io.in.valid & (ic.io.out.row > ic.io.out.col)// & (io.in.bits < UInt(128))
  io.out.bits <> ic.io.out
}

class ScaleSpaceExtremaTests(c: ScaleSpaceExtrema, val infilename: String, val outfilename: String) extends Tester(c, Array(c.io)) {
  defTests {
    val svars = new HashMap[Node, Node]()
    val ovars = new HashMap[Node, Node]()

    val inPic  = Image(infilename)
    val outPic = Image(inPic.w, inPic.h, inPic.d)

    svars(c.io.reset) = Bool(true)
    
    step(svars, ovars, false)
    
    svars(c.io.reset) = Bool(false)
    svars(c.io.in.valid) = Bool(true)
    
    for (i <- 0 until inPic.data.length) {
      val rin = inPic.data(i)
      val  in = if (rin < 0) 256 + rin else rin
      svars(c.io.in.bits) = Bits(in)
      step(svars, ovars, false)
      
      val out = ovars(c.io.out.valid).litValue()
      val pix = if (out.testBit(0)) 200 else 100
      outPic.data(i) = pix.toByte
    }
    outPic.write(outfilename)
    true
  }
}

