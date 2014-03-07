package SIFT

import Chisel._
import Node._
import scala.collection.mutable.HashMap

class ScaleSpaceExtrema(it: ImageType, n_oct: Int = 2) extends Module {
  val io = new Bundle {
    val img_in = Valid(UInt(width=it.dwidth)).flip
    val coord = Valid(new Coord(it))
    
    val select = Decoupled(UInt(width=8)).flip
    val img_out = Valid(UInt(width=it.dwidth))
  }

  // Count pixels output
  val ic = Module(new ImageCounter(it))
  io.coord.bits := ic.io.out
  ic.io.en := io.img_out.fire()

  // Allow changing source when stream is not in process
  val select_ready = Reg(init = Bool(true))
  io.select.ready := select_ready
  when(io.img_out.fire() & ic.io.top) {
    select_ready := Bool(true)
  }
  when(io.img_in.valid) {
    select_ready := Bool(false)
  }

  // Just generate a pattern on valid for now
  io.coord.valid := ic.io.out.col > ic.io.out.row

  // Latch output image source when allowed
  val select_r = Reg(init = UInt(0,8))
  when (io.select.fire()) {
    select_r := io.select.bits
  }

  val oct = Range(0, n_oct).map(i => Module(new Octave(it.subsample(i),i)))

  for (i <- 0 until n_oct) {
    oct(i).io.select := select_r
    
    if (i != 0) {
      oct(i).io.img_in <> oct(i-1).io.next_img_out
      oct(i).io.img_out <> oct(i-1).io.next_img_in
    }
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

  oct(0).io.img_out.ready := Bool(true)
  
  if (it.dwidth == 8)
    io.img_out.bits := oct(n_oct-1).io.img_out.bits
  else 
    io.img_out.bits := Fill(3,oct(n_oct-1).io.img_out.bits)
  
  io.img_out.valid := oct(n_oct-1).io.img_out.valid
}

class ScaleSpaceExtremaTests(c: ScaleSpaceExtrema, val infilename: String,
  val imgfilename: String, val coordfilename: String) 
  //extends Tester(c, Array(c.io)) {
  extends Tester(c, false) {

  //defTests {
  //  val svars = new HashMap[Node, Node]()
  //  val ovars = new HashMap[Node, Node]()

    val inPic = Image(infilename)
    val imgPic = Image(inPic.w, inPic.h, inPic.d)
    val coordPic = Image(inPic.w, inPic.h, inPic.d)
    val n_byte = inPic.d/8

    //svars(c.io.in.valid) = Bool(true)

    // Select debug image stream
    poke(c.io.select.bits, 0)
    poke(c.io.select.valid, 1)
    step(1)

    for (i <- 0 until inPic.data.length/n_byte) {
      var pixel = 0
      for (j <- 0 until n_byte) {
        pixel = pixel << 8
        val rin = inPic.data(3*i+j)
        val  in = if (rin < 0) 256 + rin else rin
        pixel += in
      }
      
      //svars(c.io.in.bits) = Bits(pixel)
      poke(c.io.img_in.bits, pixel)
      poke(c.io.img_in.valid, 1)
      
      //step(svars, ovars, false)
      step(1)

      // Write debug image out
      //val imgpix = ovars(c.io.img.bits).litValue()
      val imgpix = peek(c.io.img_out.bits)

      for (j <- 0 until n_byte) {
        imgPic.data(3*i+j) = ((imgpix >> (8*j)) & 0xFF).toByte
      }

      // Color pixel red if outputting valid coord, grey otherwise
      //val coord = ovars(c.io.coord.valid).litValue()
      //val coordpix = if (coord.testBit(0)) 0xFF0000 else 0x808080
      val coordpix = if (peek(c.io.coord.valid)==1) 0xFF0000 else 0x808080
      
      for (j <- 0 until n_byte) {
        coordPic.data(3*i+j) = ((coordpix >> (8*j)) & 0xFF).toByte
      }
    }

    imgPic.write(imgfilename)
    coordPic.write(coordfilename)

  //  true
  ok = true
  //}
}

