package SIFT

import Chisel._

class ScaleSpaceExtrema(it: ImageType, n_oct: Int = 1) extends Module {
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
  
  // For now octaves only operate on grayscale byte images
  val gray_it = new ImageType(it.width, it.height, 8)

  val oct = Range(0, n_oct).map(
    i => Module(new Octave(gray_it.subsample(i), i))
  )

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
    /*val sum = (Cat(UInt("h00"), io.img_in.bits(23,16)) + 
      io.img_in.bits(15,8) + io.img_in.bits(7,0))
    val div = (UInt(16)*sum) + (UInt(4)*sum) + sum
    oct(0).io.img_in.bits := (div >> UInt(6)) + UInt(4)*/
    oct(0).io.img_in.bits := io.img_in.bits(7,0)
  }

  oct(0).io.img_in.valid := io.img_in.valid

  oct(0).io.img_out.ready := Bool(true)
  
  if (it.dwidth == 8)
    io.img_out.bits := oct(0).io.img_out.bits
  else 
    io.img_out.bits := Fill(3,oct(0).io.img_out.bits)
  
  io.img_out.valid := oct(0).io.img_out.valid
}

class ScaleSpaceExtremaTests(c: ScaleSpaceExtrema, val infilename: String,
  val imgfilename: String, val coordfilename: String) 
  extends Tester(c, false) {

  val inPic = Image(infilename)
  val imgPic = Image(inPic.w, inPic.h, inPic.d)
  val coordPic = Image(inPic.w, inPic.h, inPic.d)
  val n_byte = inPic.d/8
  val n_pixel = inPic.w * inPic.h

  // Select debug image stream
  poke(c.io.select.bits, 0x02)
  poke(c.io.select.valid, 1)
  step(1)
  poke(c.io.img_in.valid, 0)
  step(1)

  println("w=" + inPic.w + " h=" + inPic.h + " d=" + inPic.d)
  println("out length=" + imgPic.data.length)
  
  var in_idx = 0
  var out_idx = 0
  var timeout = 0
  
  var triplet = 0
  var pixel = 0

  while ((timeout < 3*inPic.w) && 
    (in_idx < n_pixel || out_idx < n_pixel)) {
    
    if (in_idx < n_pixel) {
      triplet = 0
      for (j <- 0 until n_byte) {
        pixel = inPic.data(3*in_idx+j)
        if (pixel < 0) pixel += 256
        triplet += pixel << (8*j)
      }
      
      poke(c.io.img_in.bits, triplet)
      poke(c.io.img_in.valid, 1)

      in_idx += 1
      timeout = 0
    } else {
      poke(c.io.img_in.valid, 0)
    }

    step(1)
    timeout += 1

    if(out_idx < n_pixel && peek(c.io.img_out.valid)==1) {
      // Write debug image out
      val out_triplet = peek(c.io.img_out.bits)

      for (j <- 0 until n_byte) {
        imgPic.data((3*out_idx)+j) = ((out_triplet >> (8*j)) & 0xFF).toByte
      }

      // Color pixel red if outputting valid coord, grey otherwise
      val coordpix = if (peek(c.io.coord.valid)==1) 0xFF0000 else 0x808080  
      for (j <- 0 until n_byte) {
        coordPic.data(3*out_idx+j) = ((coordpix >> (8*j)) & 0xFF).toByte
      }

      out_idx += 1
      timeout = 0
    }
  }
  
  poke(c.io.img_in.valid,0)
  
  step(10)
  
  println("InIdx: " + in_idx + ", OutIdx: " + out_idx + ", Cycle: " + timeout)
  imgPic.write(imgfilename)
  coordPic.write(coordfilename)

  ok = true
}

