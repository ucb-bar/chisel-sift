package SIFT

import Chisel._

class Colorspace(params: SSEParams) extends Module {
  val io = new Bundle {
    val select = UInt(INPUT, width=8)
    val in = UInt(INPUT, width=8)
    val out = UInt(OUTPUT, width=24)
  }
  
  def ramp(gray: Int) = {
    if (gray < 128) {
      if (gray < 16) {
        gray * 16
      } else {
        255
      }
    } else {
      0
    }
  }

  val cmap_red = Vec(Range(0,256).map(x => UInt(ramp(x),width=8)))
  val cmap_blue = Vec(Range(0,256).map(x => UInt(ramp(255-x),width=8)))

  when(io.select >= UInt(params.n_ext+5)) {
    io.out := Cat(cmap_red(io.in), UInt(0,width=8), cmap_blue(io.in))
  } .otherwise {
    io.out := Fill(3,io.in)
  }
}
