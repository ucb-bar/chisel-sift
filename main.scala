package SIFT

import Chisel._

object SIFT {
  def main(args: Array[String]): Unit = {
    val tutArgs = args.slice(1, args.length)
    val res =
    
    args(0) match {
      case "ScaleSpaceExtrema" =>
        chiselMainTest(tutArgs,
          () => Module(new ScaleSpaceExtrema(ImageType("../data/in.im24")))){
            c => new ScaleSpaceExtremaTests(c,"../data/in.im24","../data/out.im24")
      }
    }
  }
}
