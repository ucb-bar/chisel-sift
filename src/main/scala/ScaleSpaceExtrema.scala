package SIFT

import Chisel._

import scala.io.Source
import java.io.File

case class SSEParams(
  it: ImageType, 
  n_oct: Int = 2,
  n_ext: Int = 2,
  n_tap: Int = 5,
  next_tap: Int = 2,
  coeff: List[UInt] = StdCoeff.GaussKernel,
  mul_delay: Int = 1,
  sum_delay: Int = 1
)

class ScaleSpaceExtrema(val params: SSEParams) extends Module {

  val io = new Bundle {
    val img_in = Decoupled(UInt(width=params.it.dwidth)).flip
    val coord = Valid(new Coord(params.it))
    
    val select = Decoupled(UInt(width=8)).flip
    val img_out = Decoupled(UInt(width=params.it.dwidth))
  }
  
  // Count pixels output and input
  val out_count = Module(new ImageCounter(params.it))
  io.coord.bits := out_count.io.out
  out_count.io.en := io.img_out.fire()

  val in_count = Module(new ImageCounter(params.it))
  in_count.io.en := io.img_in.fire()
  
  // Allow changing source when stream is not in process
  //val select_ready = Reg(init = Bool(true))
  io.select.ready := (
    !io.img_in.valid &
    (in_count.io.out.row === UInt(0)) &
    (in_count.io.out.col === UInt(0)) &
    (out_count.io.out.row === UInt(0)) &
    (out_count.io.out.col === UInt(0)))

  // Just generate a pattern on valid for now
  io.coord.valid := out_count.io.out.col > out_count.io.out.row

  // Latch output image source when allowed
  val select_r = Reg(init = UInt(0,8))
  when (io.select.fire()) {
    select_r := io.select.bits
  }
  
  // For now octaves only operate on grayscale byte images
  val gray_it = new ImageType(params.it.width, params.it.height, 8)
  
  // Create sequence of octaves
  val oct = Range(0, params.n_oct).map(
    i => Module(new Octave(params.copy(it = gray_it.subsample(i)), i))
  )

  // Wire downstream octave ports to "next" image ports of previous octave
  for (i <- 0 until params.n_oct) {
    oct(i).io.select := select_r
    
    if (i != 0) {
      oct(i).io.img_in <> oct(i-1).io.next_img_out
      oct(i).io.img_out <> oct(i-1).io.next_img_in
    }
  }
  
  // Always assert last octave ready to avoid stalling pipeline
  oct(params.n_oct-1).io.next_img_out.ready := Bool(true)
  
  // Wire input image to first octave main image input
  io.img_in.ready := oct(0).io.img_in.ready
  oct(0).io.img_in.valid := io.img_in.valid
  
  if (params.it.dwidth == 24) {
    // Approximate (r+b+g)/3 as (r+b+g)*(16 + 4 + 1)/64
    // Also offset by 4 to allow for colorspace mapping
    val sum = (Cat(UInt("h00"), io.img_in.bits(23,16)) + 
      io.img_in.bits(15,8) + io.img_in.bits(7,0))
    val div = (UInt(16)*sum) + (UInt(4)*sum) + sum
    oct(0).io.img_in.bits := (div >> UInt(6)) + UInt(4)
  } else {
    oct(0).io.img_in.bits := io.img_in.bits
  }

  // Wire output image to first octave main image output
  oct(0).io.img_out.ready := io.img_out.ready
  io.img_out.valid := oct(0).io.img_out.valid
  
  if (params.it.dwidth == 24)
    io.img_out.bits := Fill(3,oct(0).io.img_out.bits)
  else 
    io.img_out.bits := oct(0).io.img_out.bits
}

case class FileTestParams(
  ctrl: String, 
  img_in: String,
  img_out: String,
  coord: String
)

class SSEFileTest(c: ScaleSpaceExtrema, ftp: FileTestParams) 
  extends Tester(c, false) {

  var ctrl = List(0x00)

  try {
    val ctrl_file = Source.fromFile(ftp.ctrl)
    ctrl = ctrl_file.getLines().map(_.split(",")).flatten.map(_.toInt).toList
    ctrl_file.close()
  } catch {
    case _ : Throwable => {
      println("File \"" + ftp.ctrl + "\" not found")
    }
  }
  
  println("Ctrl Inputs: " + ctrl)
  val n_ctrl = ctrl.length
  
  val inPic = Image(ftp.img_in)
  val imgPic = Image(inPic.w, inPic.h, inPic.d)
  val coordPic = Image(inPic.w, inPic.h, 24)
  val n_byte = inPic.d/8
  val n_pixel = inPic.w * inPic.h
  
  println("w=" + inPic.w + " h=" + inPic.h + " d=" + inPic.d)

  var ctrl_idx = 0
  var in_idx = n_pixel
  var out_idx = n_pixel

  var timeout = 0
  
  var triplet = 0
  var pixel = 0
  
  poke(c.io.img_in.valid, 0)
  poke(c.io.select.valid, 0)
  step(1)

  while ((timeout < 1000) && (ctrl_idx < ctrl.length) ||
    ((ctrl_idx == ctrl.length) && (in_idx < n_pixel || out_idx < n_pixel))) {
    
    if (ctrl_idx < ctrl.length) {
      poke(c.io.select.bits, ctrl(ctrl_idx))
      poke(c.io.select.valid, 1)
    } else {
      poke(c.io.select.valid, 0)
    }
    
    if (in_idx < n_pixel) {
      triplet = 0
      for (j <- 0 until n_byte) {
        pixel = inPic.data(n_byte*in_idx+j)
        if (pixel < 0) pixel += 256
        triplet += pixel << (8*j)
      }
      
      poke(c.io.img_in.bits, triplet)
      poke(c.io.img_in.valid, 1)
      
    } else {
      poke(c.io.img_in.valid, 0)
    }

    poke(c.io.img_out.ready, 1)
    step(1)
    
    if((in_idx == n_pixel) && (out_idx == n_pixel) && (peek(c.io.select.ready)==1)) {
      println("Submitting image " + ctrl_idx)
      
      if (ctrl_idx < n_ctrl) {
        // Reset design to clear all pipeline state
        reset()
        step(1)
        in_idx = 0
        out_idx = 0
        timeout = 0
      }
    } else {
      if ((in_idx < n_pixel) && (peek(c.io.img_in.ready)==1)) {
        in_idx += 1
        timeout = 0
      } else {
        timeout += 1
      }
    }

    if((ctrl_idx < n_ctrl) && (out_idx < n_pixel && peek(c.io.img_out.valid)==1)) {
      // Write debug image out
      val out_triplet = peek(c.io.img_out.bits)

      for (j <- 0 until n_byte) {
        imgPic.data((n_byte*out_idx)+j) = ((out_triplet >> (8*j)) & 0xFF).toByte
      }

      if (out_idx == n_pixel - 1) {
        val img_list = ftp.img_out.split("\\.").toList
        val (img_base, img_ext) = img_list.splitAt(img_list.length-1)
        val img_name = img_base(0) + ctrl_idx + "." + img_ext(0)
        println("Writing " + img_name)
        imgPic.write(img_name)
        
        ctrl_idx += 1
      }
      
      // Color pixel red if outputting valid coord, grey otherwise
      val coordpix = if (peek(c.io.coord.valid)==1) 0xFF0000 else 0x808080  
      for (j <- 0 until 3) {
        coordPic.data(3*out_idx+j) = ((coordpix >> (8*j)) & 0xFF).toByte
      }

      out_idx += 1
      timeout = 0
    }
  }
  
  poke(c.io.img_in.valid,0)
  
  step(10)
  
  println("InIdx: " + in_idx + ", OutIdx: " + out_idx + ", Cycle: " + timeout)
  coordPic.write(ftp.coord)

  ok = true
}

