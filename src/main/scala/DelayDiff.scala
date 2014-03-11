package SIFT

import Chisel._

class DelayDiff(it: ImageType, n_tap: Int = 5) extends Module{
  val io = new Bundle{
    val a = Decoupled(UInt(width=it.dwidth)).flip
    val b = Decoupled(UInt(width=it.dwidth)).flip
    val out = Decoupled(UInt(width=it.dwidth))
  }
  
  io.a.ready := io.out.ready
  io.b.ready := io.out.ready

  val mid_tap = (n_tap+1)/2

  val shifter = ShiftRegister(
    Cat(io.a.valid,io.a.bits),
    (mid_tap-1)*it.width, io.out.valid)
  
  io.out.valid := shifter(it.dwidth) & io.b.valid & io.out.ready

  io.out.bits := shifter(it.dwidth-1,0) - io.b.bits
}
