package SIFT

import Chisel._

import scala.math._

object StdCoeff {
  def GaussKernel(p: SSEParams) = {
    val n_coeff = (p.n_tap/2)-1
    val idx = Range(0,n_coeff)
    val coeff = idx.map(x => exp(-0.5 * pow(x-p.n_tap,2)/pow(p.sigma,2)))
    val scale = 2*coeff.sum + 1
    val scaled_coeff = coeff.map(x => round(256*(x/scale)))
    val out_coeff = scaled_coeff ++ List(256-2*scaled_coeff.sum)
    out_coeff
  }
  val GaussKernel = List(UInt(6,8), UInt(58,8), UInt(128,8))
  val CenterKernel = List(UInt(0,8), UInt(0,8), UInt(256,9))
  val UnityKernel = List(UInt(1,8), UInt(1,8), UInt(1,8))
  val AvgKernel = List(UInt(85,8), UInt(85,8), UInt(85,8))
}

/*  delay: delay between individual elements
  * line: number of elements in a line
  * n_tap: number of taps in a filter
  * dwidth: bit width of elements
  * coeff: coefficients of symmetric FIR filter, listed from outer to center
  * mul_delay: delay from shifters to adder input
  * sum_delay: delay from adder tree. Determines number of retiming registers
  */
class SymmetricFIR(params: SSEParams, delay: Int, line: Int) extends Module{

  val io = new Bundle {
    val in = Decoupled(UInt(width = params.it.dwidth)).flip
    val out = Decoupled(UInt(width = params.it.dwidth))
  }

  // Count (zero index - 1) of middle tap (number of pathways)
  val mid_tap = (params.n_tap+1)/2

  // Delay from first input to terms arriving at adder
  val term_delay = (mid_tap-1) * delay + params.mul_delay
  
  // Delay from first element in to first element out
  val total_delay = term_delay + params.sum_delay
  
  val advance = Bool()

  // Count inputs until pipeline is primed
  val in_counter = Module(new Counter(UInt(delay*line-1)))
  in_counter.io.en := io.in.fire()

  // Counter for line to disable terms when at edges
  val term_en = Bool() 
  
  val term_counter = Module(new Counter(UInt(delay*line-1)))
  term_counter.io.en := term_en & advance
 
  term_en := Mux(
    term_counter.io.count === UInt(0),
    (in_counter.io.count === UInt(term_delay)),
    Bool(true))

  // Count outputs
  val out_counter = Module(new Counter(UInt(delay*line-1)))
  out_counter.io.en := io.out.fire()

  // State machine governing pipeline behavior
  val s_prime :: s_pipe :: s_flush :: Nil = Enum(UInt(),3)
  
  val state = Reg(init = s_prime)

  // Default to s_prime behavior
  io.in.ready := Bool(true)
  io.out.valid := Bool(false)
  advance := io.in.valid

  switch (state) {
    is (s_prime) {
      io.in.ready := Bool(true)
      io.out.valid := Bool(false)
      advance := io.in.valid

      when ((in_counter.io.count === UInt (total_delay-1)) & advance) {
        state := s_pipe
      }
    }
    is (s_pipe) {
      io.in.ready := io.out.ready
      io.out.valid := io.in.valid
      advance := io.in.valid & io.out.ready
      
      when (in_counter.io.count < UInt(total_delay-1) & out_counter.io.top & advance) {
        state := s_prime
      } .elsewhen (in_counter.io.top & advance) {
        state := s_flush
      }
    }
    is (s_flush) {
      io.in.ready := io.out.ready
      io.out.valid := Bool(true)
      advance := io.out.ready

      when (out_counter.io.top & advance) {
        state := s_prime
      } .elsewhen (io.in.valid) {
        state := s_pipe
      }
    }
  }
  
  // Create tapped delay line of inputs
  val mul_in = Vec(TapDelayLineEn(io.in.bits, delay, advance, mid_tap))
  
  // Element-wise multiplication of coeff and delay elements
  //val coeff = StdCoeff.GaussKernel(params)
  val coeff = params.coeff
  //println("Coeff:"  + coeff)
  val mul_out = (coeff, mul_in).zipped.map( _ * _ )
  
  // Insert multiplier retiming registers
  val mul_out_d = Vec(mul_out.map(ShiftRegisterEn(_, params.mul_delay, advance)))

  // Collect all terms to sum
  val terms = Vec.fill(params.n_tap) { UInt(width=2*params.it.dwidth) }
  terms(mid_tap-1) := mul_out_d(mid_tap-1)
 
  // Disable terms at edges of lines
  val term_enable = Vec.fill(params.n_tap) { Bool() }
  term_enable(mid_tap-1) := Bool(true)

  for (tap_idx <- 0 until mid_tap - 1) {
    // n_blocked is the number of terms that should be disabled
    val n_blocked = mid_tap-tap_idx-1

    // High-side muxes and delays
    term_enable(tap_idx) := (
      term_counter.io.count >= UInt(n_blocked*delay))

    terms(tap_idx) := Mux(
      term_enable(tap_idx),
      ShiftRegisterEn(
        mul_out_d(tap_idx),
        (params.n_tap-(2*tap_idx)-1)*delay,
        advance),
      UInt(0))

    // Low-side muxes
    term_enable(params.n_tap-tap_idx-1) := (
      term_counter.io.count < UInt((line-n_blocked)*delay))

    terms(params.n_tap-tap_idx-1) := Mux(
      term_enable(params.n_tap-tap_idx-1),
      mul_out_d(tap_idx),
      UInt(0))
  }

  val sum = terms.reduceRight( _ + _ )
  val sum_d = ShiftRegisterEn(sum(15,8), params.sum_delay, advance) 
  io.out.bits := sum_d
}
