package SIFT

import Chisel._

class Gaussian(it: ImageType, val n_tap: Int = 5, debug: Boolean = false) extends Module{
  val io = new Bundle{
    val in = Decoupled(UInt(width=it.dwidth)).flip
    val out = Decoupled(UInt(width=it.dwidth))
  }
  
  val kernel = if (debug) StdCoeff.CenterKernel else StdCoeff.GaussKernel

  val row_fir = Module(new SymmetricFIR(1, it.width, n_tap, coeff = kernel))
  row_fir.io.in <> io.in

  val col_fir = Module(new SymmetricFIR(it.width, it.height, n_tap, coeff = kernel))
  col_fir.io.in <> row_fir.io.out
  io.out <> col_fir.io.out
}
