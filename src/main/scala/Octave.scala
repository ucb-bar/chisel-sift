package SIFT

import Chisel._

/*  it: provides width, height, and depth info on img
  * index: the index of this gaussian
  * n_ext: number of possible extrema output from this octave
  * next_tap: what point in the gaussian stream to send to the next octave
  */
class Octave(it: ImageType, index: Int, n_ext: Int = 2, next_tap: Int = 2)
  extends Module {
  
  val io = new Bundle {
    val img_in = Decoupled(UInt(width=it.dwidth)).flip
    val coord = Valid(new Coord(it))
    
    // Debug image selection and output
    val select = UInt(INPUT,width=8)
    val img_out = Decoupled(UInt(width=it.dwidth))

    // Chain output and input
    val next_img_in = Decoupled(UInt(width=it.dwidth)).flip
    val next_img_out = Decoupled(UInt(width=it.dwidth))
  }

  // Downsampler
  val ds = Module(new DownSampler(it))
  
  // Default connection to input image
  ds.io.in.bits := io.img_in.bits
  ds.io.in.valid := io.img_in.valid
  io.img_in.ready := ds.io.in.ready

  val it_div_2 = it.subsample()

  // Upsampler
  val us = Module(new UpSampler(it_div_2))

  // Chain of gaussian blurs
  val n_gauss = n_ext + 3
  val gauss = Range(0, n_gauss).map(i => Module(new Gaussian(it_div_2)))
  
  // Default
  gauss(0).io.in.bits := ds.io.out.bits
  gauss(0).io.in.valid := ds.io.out.valid
  ds.io.out.ready := gauss(0).io.in.ready

  for(i <- 0 until n_gauss-1) {
    gauss(i+1).io.in.bits := gauss(i).io.out.bits
    gauss(i+1).io.in.valid := gauss(i).io.out.valid
    gauss(i).io.out.ready := gauss(i+1).io.out.ready
  }

  // Make sure last ready is wired
  gauss(n_gauss-1).io.out.ready := Bool(true)

  // Wire downstream octave image to selected gaussian tap
  /*io.next_img_out <> gauss(next_tap).io.out
  io.next_img_out.bits := gauss(next_tap).io.out.bits
  io.next_img_out.valid := gauss(next_tap).io.out.valid*/

  // Take difference of gaussian pairs
  /*val n_diff = n_ext + 2
  val diff = Range(0, n_diff).map(
    i => Module(new DelayDiff(it_div_2,gauss(i).n_tap)))
  for (i <- 0 until n_diff) {
    diff(i).io.a <> gauss(i).io.out
    diff(i).io.b <> gauss(i+1).io.out
  }*/

  // Debug image output stream selection
  // When our index is the active source, select an internal stream
  when(io.select(7,4) === UInt(index)) {
    // 0 is bypasses down/upsample, helps debug tap select
    when(io.select(3,0) === UInt(0)) {
      io.img_out.valid := io.img_in.valid
      io.img_out.bits := io.img_in.bits
      io.img_in.ready := io.img_out.ready
    // Otherwise use output from upsampler
    } .otherwise {
      io.img_out.valid := us.io.out.valid
      io.img_out.bits := us.io.out.bits
      us.io.out.ready := io.img_out.ready
    }

    // Default input to upsampler is downsampler, includes select = 1
    us.io.in.valid := ds.io.out.valid
    us.io.in.bits := ds.io.out.bits
    ds.io.out.ready := us.io.in.ready

    switch(io.select(3,0)) {
      for (i <- 0 until n_gauss) {
        is(UInt(i+2)) {
          ds.io.out.ready := gauss(0).io.in.ready

          us.io.in.bits := gauss(i).io.out.bits
          us.io.in.valid := gauss(i).io.out.valid
          gauss(i).io.out.ready := us.io.in.ready
        }
      }
    }

  // Otherwise select stream from next octave and upsample it
  } .otherwise {
    io.img_out.valid := us.io.out.valid
    io.img_out.bits := us.io.out.bits
    us.io.out.ready := io.img_out.ready
    
    us.io.in.valid := io.next_img_in.valid
    us.io.in.bits := io.next_img_in.bits
    io.next_img_in.ready := us.io.in.ready
  }
}
