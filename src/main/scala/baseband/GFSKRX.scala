package baseband

import chisel3._
import chisel3.util._
import freechips.rocketchip.config.Parameters

class AnalogRXIO extends Bundle {
  val i = Input(UInt(5.W))
  val q = Input(UInt(5.W))
}

class GFSKRX()(implicit p: Parameters) extends Module {
  val io = IO(new Bundle {
    val analog = new Bundle {
      val rx = new AnalogRXIO
    }
    val digital = new Bundle {
      val out = Flipped(Decoupled(UInt(1.W)))
    }
  })
}