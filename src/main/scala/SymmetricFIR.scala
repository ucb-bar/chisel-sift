package SIFT

import Chisel._

/*  delay: delay between individual elements
  * line: number of elements in a line
  * n_tap: number of taps in a filter
  * dwidth: bit width of elements
  * coeff: coefficients of symmetric FIR filter, listed from outer to center
  */
class SymmetricFIR(delay: Int, line: Int, n_tap: Int, dwidth : Int = 8,
  coeff: List[UInt] = List(UInt(6,8), UInt(58,8), UInt(128,8))) extends Module{
  val io = new Bundle {
    val in = Valid(UInt(width = dwidth)).flip
    val out = Valid(UInt(width = dwidth))
  }

  // Delay from shifters to adder input
  val mul_delay = 0

  // Delay from adder tree. Determines number of retiming registers
  val sum_delay = 0

  // Total delay from in to out
  val prime_delay = 2*delay + mul_delay + sum_delay
  
  // Count inputs until pipeline is primed
  val prime_counter = Module(new Counter(UInt(prime_delay)))
  prime_counter.io.en := io.in.valid & ~prime_counter.io.top
  io.out.valid := prime_counter.io.top

  // Counter for line to disable taps when at edge
  val line_counter = Module(new Counter(UInt(line-1)))
  line_counter.io.en := io.in.valid &
    prime_counter.io.count > UInt(2*delay + mul_delay - 1)

  // Count (zero index - 1) of middle tap (number of pathways)
  val mid_tap = (n_tap+1)/2

  def tap_delays[T <: Data](x: T, n: Int): List[T] = {
    if(n <= 1) 
      List(x) 
    else 
      x :: tap_delays(ShiftRegister(x, delay, io.in.valid), n-1)
  }
  
  // Element-wise multiplication of coeff and delay elements
  val mul_out = (coeff, tap_delays(io.in.bits, mid_tap)).zipped.map( _ * _ )

  // Collect all terms to sum
  val terms = Vec.fill(n_tap) { UInt(width=2*dwidth) }
  terms(mid_tap - 1) := mul_out(mid_tap - 1)
  
  for (tap_idx <- 0 until mid_tap - 1) {
    // Low-side paths
    terms(tap_idx) := Mux(line_counter.io.count > UInt(tap_idx), mul_out(tap_idx), UInt(0))

    // High-side paths (include delays)
    terms(n_tap-tap_idx-1) := Mux(line_counter.io.count < UInt(line-tap_idx-1), ShiftRegister(mul_out(tap_idx), (n_tap-(2*tap_idx)-1)*delay, io.in.valid), UInt(0))
  }

  val sum = UInt(width = 2*dwidth)
  sum := terms.reduceRight( _ + _ )
  io.out.bits := sum(15,8)
}
