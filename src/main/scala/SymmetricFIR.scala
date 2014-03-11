package SIFT

import Chisel._

object StdCoeff {
  val GaussKernel = List(UInt(6,8), UInt(58,8), UInt(128,8))
  val CenterKernel = List(UInt(0,8), UInt(0,8), UInt(255,8))
  val UnityKernel = List(UInt(1,8), UInt(1,8), UInt(1,8))
  val AvgKernel = List(UInt(85,8), UInt(85,8), UInt(85,8))
}

/*  delay: delay between individual elements
  * line: number of elements in a line
  * n_tap: number of taps in a filter
  * dwidth: bit width of elements
  * coeff: coefficients of symmetric FIR filter, listed from outer to center
  */
class SymmetricFIR(delay: Int, line: Int, n_tap: Int, dwidth : Int = 8,
  coeff: List[UInt] = StdCoeff.CenterKernel) extends Module{
  val io = new Bundle {
    val in = Decoupled(UInt(width = dwidth)).flip
    val out = Decoupled(UInt(width = dwidth))
  }

  // Delay from shifters to adder input
  val mul_delay = 1

  // Delay from adder tree. Determines number of retiming registers
  val sum_delay = 1

  // Count (zero index - 1) of middle tap (number of pathways)
  val mid_tap = (n_tap+1)/2
  
  // Delay from first to middle tap
  val tap_delay = (mid_tap-1) * delay
  
  val advance_taps = Bool()

  val taps_valid = Bool()

  // Count inputs until pipeline is primed
  val in_counter = Module(new Counter(UInt(delay*line-1)))
  in_counter.io.en := io.in.valid

  // Counter for line to disable taps when at edge
  val tap_counter = Module(new Counter(UInt(delay*line-1)))
  tap_counter.io.en := taps_valid

  // prime is true when filling the first few elements of the pipeline
  val prime = (tap_counter.io.count === UInt(0) & 
    in_counter.io.count < UInt(tap_delay))
  
  io.in.ready := prime || advance_taps

  taps_valid := Mux(prime, Bool(false), advance_taps)

  // flush is true when we have received all elements of a line but they 
  // have not been pushed out (and a new line hasn't started)
  val flush =  in_counter.io.count === UInt(0) & tap_counter.io.count != UInt(0)

  advance_taps := io.out.ready && (io.in.valid || flush)

  def tap_delays[T <: Data](x: T, n: Int): List[T] = {
    if(n <= 1) 
      List(x) 
    else 
      x :: tap_delays(ShiftRegister(x, delay, advance_taps), n-1)
  }
  
  // Element-wise multiplication of coeff and delay elements
  val mul_out = (coeff, tap_delays(io.in.bits, mid_tap)).zipped.map( _ * _ ) 
  
  // Add multiplier retiming registers
  val mul_out_d = Vec.fill(mid_tap) {UInt(width = 2*dwidth)}
  for (i <- 0 until mid_tap) {
    mul_out_d(i) := ShiftRegister(mul_out(i), mul_delay, advance_taps)
  }
  
  val mul_valid = ShiftRegister(taps_valid, mul_delay, advance_taps)

  // Collect all terms to sum
  val terms = Vec.fill(n_tap) { UInt(width=2*dwidth) }
  terms(mid_tap-1) := mul_out_d(mid_tap-1)

  for (tap_idx <- 0 until mid_tap - 1) {
    // Low-side muxes
    terms(tap_idx) := Mux(
      ShiftRegister(
        tap_counter.io.count > UInt(tap_idx*delay),
        mul_delay, advance_taps), 
      mul_out_d(tap_idx), UInt(0))

    // High-side muxes and delays
    terms(n_tap-tap_idx-1) := Mux(
      ShiftRegister(
        tap_counter.io.count < UInt((line-tap_idx-1)*delay),
        mul_delay, advance_taps),
      ShiftRegister(
        mul_out_d(tap_idx),
        (n_tap-(2*tap_idx)-1)*delay,
        advance_taps),
      UInt(0))
  }

  val sum = terms.reduceRight( _ + _ )
  io.out.bits := ShiftRegister(sum(15,8), sum_delay, advance_taps)
  
  val sum_valid = ShiftRegister(mul_valid, sum_delay, advance_taps)
  io.out.valid := sum_valid
}
