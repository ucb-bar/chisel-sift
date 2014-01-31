package SIFT

import Chisel._

object SIFT {
  def main(args: Array[String]): Unit = {
    val tutArgs = args.slice(1, args.length)
    val res =
    
    args(0) match {
      case "ScaleSpaceExtrema" =>
        val img = Image("data/in.im24")

        chiselMainTest(tutArgs,
          () => Module(new ScaleSpaceExtrema(
            new ImageType(UInt(img.w), UInt(img.h), img.d)))) {
            c => new ScaleSpaceExtremaTests(c, "data/in.im24", "data/out.im24")
      }
    }
  }
}
