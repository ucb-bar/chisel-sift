package SIFT

import Chisel._

class DelayDiff(it: ImageType, n_tap: Int = 5) extends Module{
  val io = new Bundle{
    val a = Valid(UInt(width=it.dwidth)).flip
    val b = Valid(UInt(width=it.dwidth)).flip
    val out = Valid(UInt(width=it.dwidth))
  }
  
  val mid_tap = (n_tap+1)/2

  io.out.valid := io.b.valid
  io.out.bits := ShiftRegister(io.a.bits,mid_tap*it.width) - io.b.bits
}
