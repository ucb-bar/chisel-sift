package SIFT

import Chisel._

case class ImageType(width:UInt, height:UInt, dwidth: Int = 8) {
  def subsample(factor: Int = 1) : ImageType = {
    new ImageType(width >> UInt(factor), height >> UInt(factor), dwidth >> factor)
  }
}

class Pixel extends Bundle {
  val r = UInt(width = 8)
  val g = UInt(width = 8)
  val b = UInt(width = 8)
}

class Coord(it: ImageType) extends Bundle {
  val col = UInt(OUTPUT,width=it.width.getWidth)
  val row = UInt(OUTPUT,width=it.height.getWidth)
  override def clone: this.type = {
    new Coord(new ImageType(UInt(width=col.getWidth),
      UInt(width=row.getWidth))).asInstanceOf[this.type];
  }
}

class Counter(max: UInt) extends Module {
  val io = new Bundle {
    val reset = Bool(INPUT)
    val en = Bool(INPUT)
    val count = UInt(OUTPUT, max.getWidth)
    val top = Bool(OUTPUT)
  }

  val x = Reg(init=UInt(0, max.getWidth))

  io.count := x
  io.top := x === max

  when (io.en) {x := Mux(io.top, UInt(0), x + UInt(1))}
  when (io.reset) {x := UInt(0)}
}

class ImageCounter(it: ImageType) extends Module {
  val io = new Bundle {
    val reset = Bool(INPUT)
    val en = Bool(INPUT)
    val out = new Coord(it).asOutput
    val top = Bool(OUTPUT)
  }

  val col_counter = Module(new Counter(it.width-UInt(1)))
  val row_counter = Module(new Counter(it.height-UInt(1)))

  col_counter.io.reset := io.reset
  row_counter.io.reset := io.reset

  col_counter.io.en := io.en
  row_counter.io.en := io.en & col_counter.io.top

  io.out.col := col_counter.io.count
  io.out.row := row_counter.io.count

  io.top := col_counter.io.top & row_counter.io.top
}
