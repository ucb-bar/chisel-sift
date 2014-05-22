package SIFT

import Chisel._

/*  it: provides width, height, and depth info on img
  * index: the index of this gaussian
  * n_ext: number of possible extrema output from this octave
  * next_tap: what point in the gaussian stream to send to the next octave
  */
class OctaveIO(params: SSEParams) extends Bundle {
  val img_in = Decoupled(UInt(width=params.it.dwidth)).flip
  val coord = Valid(new Coord(params.it))
  
  // Debug image selection and output
  val select = UInt(INPUT,width=8)
  val img_out = Decoupled(UInt(width=params.it.dwidth))

  // Chain output and input
  val next_img_in = Decoupled(UInt(width=params.it.dwidth)).flip
  val next_img_out = Decoupled(UInt(width=params.it.dwidth))
}

class Octave(params: SSEParams, index: Int) extends Module {
  
  val io = new OctaveIO(params)

  // Default values?
  io.coord.valid := Bool(false)
  io.img_in.ready := Bool(true)
  io.img_out.valid := Bool(false)
  io.next_img_in.ready := Bool(true)
  io.next_img_out.valid := Bool(false)

  // Downsampler
  val ds = Module(new DownSampler(params))
  
  // Default connection to input image
  ds.io.in.bits := io.img_in.bits
  ds.io.in.valid := io.img_in.valid
  io.img_in.ready := ds.io.in.ready

  val params_div_2 = params.copy(it = params.it.subsample())

  // Upsampler
  val us = Module(new UpSampler(params_div_2))

  // Chain of gaussian blurs
  val n_gauss = params.n_ext + 3
  val gauss = Range(0, n_gauss).map(i => Module(new Gaussian(params_div_2)))
  
  // Delayed Difference modules
  val n_diff = params.n_ext + 2
  val diff = Range(0, n_diff).map(i => Module(new DelayDiff(params_div_2)))
  
  // Wire Gaussian chain and difference modules
  gauss(0).io.in.bits := ds.io.out.bits
  gauss(0).io.in.valid := ds.io.out.valid
  ds.io.out.ready := gauss(0).io.in.ready

  for(i <- 0 until n_gauss) {
    
    // Gaussian chain
    if(i < n_gauss - 1) {
      gauss(i+1).io.in.bits := gauss(i).io.out.bits
      gauss(i+1).io.in.valid := (
        gauss(i).io.out.valid &
        (if(i < n_diff) diff(i).io.a.ready else Bool(true)) & 
        (if(i > 0) diff(i-1).io.b.ready else Bool(true))
      )
    }
    
    // Difference between adjacent gaussians
    if(i < n_diff) {
      diff(i).io.a.bits := gauss(i).io.out.bits
      diff(i).io.a.valid := (
        gauss(i).io.out.valid &
        gauss(i+1).io.in.ready & 
        (if(i > 0) diff(i-1).io.b.ready else Bool(true))
      )
      
      diff(i).io.b.bits := gauss(i+1).io.out.bits
      diff(i).io.b.valid := (
        gauss(i+1).io.out.valid &
        (if(i+2 < n_gauss) gauss(i+2).io.in.ready else Bool(true)) &
        (if(i+1 < n_diff) diff(i+1).io.a.ready else Bool(true))
      )

      diff(i).io.out.ready := Bool(true)
    }

    // Sources should wait on all sinks (if they exist)
    gauss(i).io.out.ready := (
      (if(i+1 < n_gauss) gauss(i+1).io.in.ready else Bool(true)) & 
      (if(i < n_diff) diff(i).io.a.ready else Bool(true)) & 
      (if(i > 0) diff(i-1).io.b.ready else Bool(true)) &
      (if(i==params.next_tap) io.next_img_out.ready else Bool(true))
    )
  }

  // Wire downstream octave image to selected gaussian tap
  io.next_img_out.bits := gauss(params.next_tap).io.out.bits

  // Will this create combinational loops?
  io.next_img_out.valid := gauss(params.next_tap).io.out.fire()

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
        is(UInt(2+i)) {
          ds.io.out.ready := gauss(0).io.in.ready

          us.io.in.bits := gauss(i).io.out.bits
          us.io.in.valid := gauss(i).io.out.valid
          gauss(i).io.out.ready := us.io.in.ready
        }
      }

      for (i <- 0 until n_diff) {
        is(UInt(2+n_gauss+i)) {
          ds.io.out.ready := gauss(0).io.in.ready

          us.io.in.bits := diff(i).io.out.bits
          us.io.in.valid := diff(i).io.out.valid
          diff(i).io.out.ready := us.io.in.ready
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
