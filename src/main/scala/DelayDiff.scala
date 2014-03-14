package SIFT

import Chisel._

class DelayDiff(it: ImageType, n_tap: Int = 5) extends Module{
  val io = new Bundle{
    val a = Decoupled(UInt(width=it.dwidth)).flip
    val b = Decoupled(UInt(width=it.dwidth)).flip
    val out = Decoupled(UInt(width=it.dwidth))
  }
  
  val mid_tap = (n_tap+1)/2

  val q = Module(new Queue(UInt(width=it.dwidth), (mid_tap-1)*it.width + mid_tap))

  q.io.enq <> io.a

  q.io.deq.ready := io.b.valid & io.out.ready

  io.b.ready := io.out.ready & q.io.deq.valid
  
  io.out.valid := q.io.deq.valid & io.b.valid
  
  io.out.bits := q.io.deq.bits - io.b.bits
}
