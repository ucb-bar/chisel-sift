package SIFT

import Chisel._

class DelayDiff(params: SSEParams) extends Module{
  val io = new Bundle{
    val a = Decoupled(UInt(width=params.it.dwidth)).flip
    val b = Decoupled(UInt(width=params.it.dwidth)).flip
    val out = Decoupled(UInt(width=params.it.dwidth))
  }
  
  val mid_tap = (params.n_tap+1)/2

  val q = Module(new Queue(UInt(width=params.it.dwidth), mid_tap*params.it.width))

  q.io.enq <> io.a

  q.io.deq.ready := io.b.valid & io.out.ready

  io.b.ready := io.out.ready & q.io.deq.valid
  
  io.out.valid := q.io.deq.valid & io.b.valid
  
  io.out.bits := q.io.deq.bits - io.b.bits
}
