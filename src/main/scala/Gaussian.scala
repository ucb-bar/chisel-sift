package SIFT

import Chisel._

class Gaussian(params: SSEParams) extends Module{
  val io = new Bundle{
    val in = Decoupled(UInt(width=params.it.dwidth)).flip
    val out = Decoupled(UInt(width=params.it.dwidth))
  }
  
  val row_fir = Module(new SymmetricFIR(params, 1, params.it.width))
  row_fir.io.in <> io.in

  val col_fir = Module(new SymmetricFIR(params, params.it.width, params.it.height))
  col_fir.io.in <> row_fir.io.out
  io.out <> col_fir.io.out
}
