package SIFT

import Chisel._

class UpSampler(it: ImageType) extends Module{
  val io = new Bundle{
    val in = Decoupled(UInt(width=it.dwidth)).flip
    val out = Decoupled(UInt(width=it.dwidth))
  }

  val buf = Mem(UInt(width=it.dwidth),it.width)

  val out_col = new Counter(it.width*2-1)
  val out_idx = out_count.io.count >> UInt(1)
  

  val in_idx = new Counter(it.width-1)
  
  
  val empty = ptr_match && !maybe_full

  val ptr_match = in_idx === out_idx
  

  val row_in_done = Reg(init = Bool(false))
  val row_out_done = Reg(init = Bool(false))

  when(row_out_done & out_col.io.top) {
    row_in_done := Bool(false)
  }
  when(in_idx.io.top) {
    row_in_done := Bool(true)
  }

  when(io.in.valid) {
    empty := Bool(false)
  }

  col.io.en := io.out.fire()

  io.out.valid := !empty && (out_idx < in_idx || row_done)

  io.out.bits := buf(out_col>>)

  when(io.in.fire()) {
    buf(in_col) := io.in.bits
  }
}
