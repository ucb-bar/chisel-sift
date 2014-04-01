package SIFT

import Chisel._

class UpSampler(params: SSEParams) extends Module{
  val io = new Bundle{
    val in = Decoupled(UInt(width=params.it.dwidth)).flip
    val out = Decoupled(UInt(width=params.it.dwidth))
  }

  val buf = Mem(UInt(width=params.it.dwidth), params.it.width)

  val out_col = Module(new Counter(params.it.width*2-1))
  val out_idx = out_col.io.count >> UInt(1)

  val in_idx = Module(new Counter(params.it.width-1))
  
  val maybe_full = Reg(init = Bool(false))
  val ptr_match = in_idx.io.count === out_idx
  
  when (io.in.fire() != (out_col.io.count(0) & io.out.fire())) {
    maybe_full := io.in.fire()
  }

  val empty = Bool()
  val full = Bool()

  val s_normal :: s_wait :: s_pass :: Nil = Enum(UInt(),3)
  val state = Reg(init = s_normal)
  
  val state_out = UInt()
  state_out := state

  empty := Bool(false)
  full := Bool(true)

  switch(state_out) {
    is(s_normal) {
      empty := ptr_match & !maybe_full & !out_col.io.count(0)
      full := ptr_match & maybe_full
      
      when (in_idx.io.top & io.in.fire()) {
        state := s_wait
      }
    }
    
    // Row has been read, wait for output to complete row
    is(s_wait) {
      empty := Bool(false)
      full := Bool(true)

      when (out_col.io.top & io.out.fire()) {
        state := s_pass
      }
    }
    
    // Ensure output pointer passes input pointer to duplicate row
    is(s_pass) {
      empty := Bool(false)
      full := Bool(true)

      when(out_col.io.count(0) & io.out.fire()) {
        state := s_normal
      }
    }
  }

  /*val row_in_done = Reg(init = Bool(false))
  val row_out_done = Reg(init = Bool(true))
 
  val duplicate_row = row_in_done && !row_out_done

  val empty = Mux(duplicate_row, Bool(false), ptr_match && !maybe_full)
  
  val full = Mux(duplicate_row, Bool(true), ptr_match && maybe_full)*/


  /*when(out_col.io.top && io.out.fire()) {
    row_out_done := ~row_out_done
  }

  when(io.in.fire()) {
    when(in_idx.io.top) {
      row_in_done := Bool(true)
    } .otherwise {
      row_in_done := Bool(false)
    }
  }*/

  out_col.io.en := io.out.fire()

  in_idx.io.en := io.in.fire()

  io.in.ready := !full
  io.out.valid := !empty

  io.out.bits := buf(out_idx)
  when(io.in.fire()) {
    buf(in_idx.io.count) := io.in.bits
  }
}
