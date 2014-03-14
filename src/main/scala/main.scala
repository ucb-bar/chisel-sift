package SIFT

import Chisel._

object SIFT {
  def main(args: Array[String]): Unit = {
    val tutArgs = args.slice(1, args.length)
    val res =
    
    args(0) match {
      case "ScaleSpaceExtrema" => {
        val img = Image("data/in.im24")

        chiselMainTest(tutArgs,
          () => Module(new ScaleSpaceExtrema(
            new ImageType(img.w, img.h, img.d), n_oct=1))) {
              c => new ScaleSpaceExtremaTests(c, "data/control.csv",
                "data/in.im24","data/out.im24","data/coord.im24")
            }
      }

      case "Debug" => {
        val img = Image("data/count.im8")

        chiselMainTest(tutArgs,
          () => Module(new ScaleSpaceExtrema(
            new ImageType(img.w, img.h, img.d), n_oct=1, debug=true))) {
              c => new ScaleSpaceExtremaTests(c, "data/debug.csv",
                "data/count.im8","data/debug.im8","data/debug_coord.im24")
            }
      }
    }
  }
}
