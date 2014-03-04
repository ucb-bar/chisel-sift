package SIFT

import Chisel._

class DownSampler(it: ImageType) extends Module {
  val io = new Bundle {
    val in = Valid(UInt(width=it.dwidth)).flip
    val out = Valid(UInt(width=it.dwidth))
  }

  io.out.bits := io.in.bits

  val col_counter = Module(new Counter(it.width-1))
  col_counter.io.en := io.in.valid

  val row_active = Reg(resetVal = Bool(true))
  when(io.in.valid & col_counter.io.top) {
    row_active := ~row_active
  }
  when(io.reset) {
    row_active := Bool(true)
  }

  io.out.valid := ~col_counter.io.count(0) & row_active & io.in.valid
}
