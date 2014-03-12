package SIFT

import Chisel._

class Gaussian(it: ImageType, val n_tap: Int = 5) extends Module{
  val io = new Bundle{
    val in = Decoupled(UInt(width=it.dwidth)).flip
    val out = Decoupled(UInt(width=it.dwidth))
  }

  /*val row_fir = Module(new SymmetricFIR(1, it.width, n_tap))
  row_fir.io.in <> io.in
  io.out <> row_fir.io.out*/

  val col_fir = Module(new SymmetricFIR(it.width, it.height, n_tap))
  //col_fir.io.in <> row_fir.io.out
  col_fir.io.in <> io.in
  io.out <> col_fir.io.out
}
