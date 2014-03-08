package SIFT

import Chisel._

class UpSampler(it: ImageType) extends Module{
  val io = new Bundle{
    val in = Decoupled(UInt(width=it.dwidth)).flip
    val out = Decoupled(UInt(width=it.dwidth))
  }

  val buf = Mem(UInt(width=it.dwidth),it.width)

  val out_col = Module(new Counter(it.width*2-1))
  val out_idx = out_col.io.count >> UInt(1)

  val in_idx = Module(new Counter(it.width-1))
 
  val maybe_full = Reg(init = Bool(false))
  val row_in_done = Reg(init = Bool(false))
  val row_out_done = Reg(init = Bool(true))

  val ptr_match = in_idx.io.count === out_idx
  
  val duplicate_row = row_in_done && !row_out_done

  val empty = Mux(duplicate_row, Bool(false), ptr_match && !maybe_full)
  
  val full = Mux(duplicate_row, Bool(true), ptr_match && maybe_full)

  io.out.bits := buf(out_idx)

  when(io.in.fire() != io.out.fire()) {
    maybe_full := io.in.fire()
  }

  when(out_col.io.top && io.out.fire()) {
    row_out_done := ~row_out_done
  }

  when(io.in.fire()) {
    when(in_idx.io.top) {
      row_in_done := Bool(true)
    } .otherwise {
      row_in_done := Bool(false)
    }
  }

  out_col.io.en := io.out.fire()

  in_idx.io.en := io.in.fire()

  io.in.ready := !full || (duplicate_row && out_col.io.top)
  io.out.valid := !empty

  when(io.in.fire()) {
    buf(in_idx.io.count) := io.in.bits
  }
}
