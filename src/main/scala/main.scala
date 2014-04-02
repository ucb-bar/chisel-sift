package SIFT

import Chisel._

object SIFT {
  def main(args: Array[String]): Unit = {
    val tutArgs = args.slice(1, args.length)
    val res =
    
    args(0) match {
      case "ScaleSpaceExtrema" => {
        val params = SSEParams(
          it = ImageType("data/in.im24"),
          n_oct = 1
        )

        val ftp = FileTesterParams(
          "data/control.csv",
          "data/in.im24",
          "data/out.im24",
          "data/coord.im24"
        )

        chiselMainTest(tutArgs, () => Module(new ScaleSpaceExtrema(params)))
          {c => new SSEFileTester(c, ftp)}
      }

      case "Debug" => {
        val params = SSEParams(
          it = ImageType("data/count.im8"),
          n_oct = 1,
          coeff = StdCoeff.CenterKernel
        )

        val ftp = FileTesterParams(
          "data/debug.csv",
          "data/count.im8",
          "data/debug.im8",
          "data/debug_coord.im24"
        )

        chiselMainTest(tutArgs, () => Module(new ScaleSpaceExtrema(params)))
          {c => new SSEFileTester(c, ftp)}
      }

      // Only for generating verilog to hook in to Zedboard Xillydemo system
      case "Zedboard" => {
        val params = SSEParams(
          it = new ImageType(640,480,24),
          n_oct = 1
        )

        chiselMain(tutArgs, () => Module(new ScaleSpaceExtrema(params)))
      }
    }
  }
}
