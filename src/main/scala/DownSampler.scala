package SIFT

import Chisel._

class DownSampler(it: ImageType) extends Module {
  val io = new Bundle {
    val in = Decoupled(UInt(width=it.dwidth)).flip
    val out = Decoupled(UInt(width=it.dwidth))
  }

  io.in.ready := io.out.ready

  io.out.bits := io.in.bits

  val col_counter = Module(new Counter(it.width-1))
  col_counter.io.en := io.in.fire()

  val row_active = Reg(init = Bool(true))
  when(io.in.fire() & col_counter.io.top) {
    row_active := ~row_active
  }

  io.out.valid := ~col_counter.io.count(0) & row_active & io.in.valid
}
