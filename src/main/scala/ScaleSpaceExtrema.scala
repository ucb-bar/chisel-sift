package SIFT

import Chisel._
import Node._
import scala.collection.mutable.HashMap

class ScaleSpaceExtrema(it: ImageType, n_oct: Int = 2) extends Module {
  val io = new Bundle {
    val reset = Bool(INPUT)
    val img_in = Valid(UInt(width=it.dwidth)).asInput
    val img_out = Valid(UInt(width=it.dwidth)).asOutput
    val coord = Valid(new Coord(it)).asOutput
  }

  val oct = Range(0, n_oct).map(i => Module(new Octave(it.subsample(i))))

  for (i <- 1 until n_oct) {
    oct(i).io.reset := io.reset
    oct(i).io.img_in <> oct(i-1).io.next_img_out
    oct(i).io.img_out <> oct(i-1).io.next_img_in
  }
  
  if (it.dwidth == 8)
    oct(0).io.img_in.bits := io.img_in.bits
  else {
    // Approximate (r+b+g)/3 as (r+b+g)*(16 + 4 + 1)/64
    // Also offset by 4 to allow for colorspace mapping
    val sum = UInt(width = 10)
    val div = UInt(width = 14)
    sum := io.img_in.bits(23,16) + io.img_in.bits(15,8) + io.img_in.bits(7,0)
    div := UInt(16)*sum + UInt(4)*sum + sum
    oct(0).io.img_in.bits := div(13,6) + UInt(4)
  }
  oct(0).io.img_in.valid := io.img_in.valid
  
  if (it.dwidth == 8)
    io.img_out.bits := oct(n_oct-1).io.img_out.bits
  else 
    io.img_out.bits := Fill(3,oct(n_oct-1).io.img_out.bits)
  io.img_out.valid := oct(n_oct-1).io.img_out.valid
  

  /*val ic = Module(new ImageCounter(it))
  ic.io.reset := io.reset
  ic.io.en := io.in.valid

  // Test image output
  io.img.valid := io.in.valid
  io.img.bits := (io.in.bits >> UInt(1)) & UInt(0x7F7F7F)

  // Coordinate output
  io.coord.valid := io.in.valid & (ic.io.out.row > ic.io.out.col) & 
    (io.in.bits < UInt(128))
  
  io.coord.bits <> ic.io.out*/
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

