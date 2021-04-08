package baseband

import chisel3._
import chisel3.experimental.BundleLiterals.AddBundleLiteralConstructor
import chisel3.util._

import ee290cdma._
import modem._

class TXChainControllerCommand(val addrBits: Int, val maxReadSize: Int) extends Bundle {
  val addr = UInt(addrBits.W)
  val totalBytes = UInt(log2Ceil(maxReadSize).W)
}

class TXChainControllerControlIO(addrBits: Int, maxReadSize: Int) extends Bundle {
  val cmd = Flipped(Decoupled(new TXChainControllerCommand(addrBits, maxReadSize)))
  val done = Output(Bool())
}

class TXChainController(params: BLEBasebandModemParams) extends Module {
  val io = IO(new Bundle {
    val interrupt = new Bundle {
      val error = Output(Bool())
      val txFinish = Output(Bool())
    }
    val assemblerControl = Flipped(new AssemblerControlIO)
    val modemTXControl = Flipped(new GFSKTXControlIO(params))
    val  dma = new Bundle {
      val readReq = Decoupled(new EE290CDMAReaderReq(params.paddrBits, params.maxReadSize))
      val readResp = Flipped(Decoupled(new EE290CDMAReaderResp(params.maxReadSize)))
    }
    val constants = Input(new BasebandConstants)
    val control = new TXChainControllerControlIO(params.paddrBits, params.maxReadSize)
  })

  val s_idle :: s_working :: Nil = Enum(2)
  val state = RegInit(s_idle)

  val done = RegInit(false.B)
  val cmd = Reg(new TXChainControllerCommand(params.paddrBits, params.maxReadSize))

  val assemblerReqValid = RegInit(false.B)
  val assemblerDone = RegInit(false.B)

  val modemTXReqValid = RegInit(false.B)
  val modemTXDone = RegInit(false.B)

  val dmaReqValid = RegInit(false.B)
  val dmaRespReady = RegInit(false.B)
  val dmaReadResp = Reg(new EE290CDMAReaderResp(params.maxReadSize))

  val error = RegInit(false.B)
  val txFinish = RegInit(false.B)

  // Control IO
  io.control.cmd.ready := io.assemblerControl.in.ready & io.dma.readReq.ready & state === s_idle
  io.control.done := done

  // Assembler IO
  io.assemblerControl.in.valid := assemblerReqValid
  io.assemblerControl.in.bits.pduLength := cmd.totalBytes - 2.U
  io.assemblerControl.in.bits.aa := io.constants.accessAddress

  // TX IO
  io.modemTXControl.in.valid := modemTXReqValid
  io.modemTXControl.in.bits.totalBytes := cmd.totalBytes - 2.U

  // DMA IO
  io.dma.readReq.valid := dmaReqValid
  io.dma.readReq.bits.addr := cmd.addr
  io.dma.readReq.bits.totalBytes := cmd.totalBytes

  io.dma.readResp.ready := dmaRespReady

  // Interrupt
  io.interrupt.error := error
  io.interrupt.txFinish := txFinish

  // Set interrupts to be one cycle pulses
  when(error) {
    error := false.B
  }

  when(txFinish) {
    txFinish := false.B
  }

  // Main FSM
  switch(state) {
    is(s_idle) {
      done := false.B

      when(io.control.cmd.fire()) {
        when(io.control.cmd.bits.totalBytes > 1.U & io.control.cmd.bits.totalBytes < 258.U) {
          cmd := io.control.cmd.bits

          dmaReqValid := true.B
          assemblerReqValid := true.B
          modemTXReqValid := true.B

          state := s_working
        }.otherwise {
          // TODO: Invalid PDU width exception
          error := true.B
        }
      }
    }
    is(s_working) {
      when(io.dma.readReq.fire()) {
        dmaReqValid := false.B
        dmaRespReady := true.B
      }

      when(io.assemblerControl.in.fire()) {
        assemblerReqValid := false.B
      }

      when(io.modemTXControl.in.fire()) {
        modemTXReqValid := false.B
      }

      when(io.dma.readResp.fire()) {
        dmaReadResp := io.dma.readResp.bits
        dmaRespReady := false.B
      }

      when(io.assemblerControl.out.done) {
        assemblerDone := true.B
      }

      when(io.modemTXControl.out.done) {
        modemTXDone := true.B
      }

      when(assemblerDone && modemTXDone) {
        when(dmaReadResp.bytesRead === cmd.totalBytes) { // DMA should complete before our packet is done sending, so the resp should match our cmd
          // TODO: Place finish message in reg
          txFinish := true.B
          // Fire done
          done := true.B

          // Confirm that all regs get reset to false
          assemblerReqValid := false.B
          assemblerDone := false.B
          modemTXReqValid := false.B
          modemTXDone := false.B
          dmaReqValid := false.B
          dmaRespReady := false.B

          state := s_idle
        }.otherwise {
          // TODO: Place error for didn't fetch enough DMA data into error reg
          error := true.B
        }
      }
    }
  }
}

class RXChainControllerCommand(val addrBits: Int) extends Bundle {
  val command = UInt(2.W)
  val addr = UInt(addrBits.W)
}

class RXChainControllerControlIO(addrBits: Int) extends Bundle {
  val cmd = Flipped(Decoupled(new RXChainControllerCommand(addrBits)))
  val baseAddr = Valid(UInt(addrBits.W))
  val done = Output(Bool())
}

class RXChainController(params: BLEBasebandModemParams) extends Module {
  val io = IO(new Bundle {
    val interrupt = new Bundle {
      val error = Output(Bool())
      val rxStart = Output(Bool())
      val rxFinish = Output(Bool())
    }
    val disassemblerControl = Flipped(new DisassemblerControlIO)
    val modemRXControl = new Bundle {
      val enable = Output(Bool())
    }
    val constants = Input(new BasebandConstants)
    val control = new RXChainControllerControlIO(params.paddrBits)
  })

  val s_idle :: s_working :: Nil = Enum(2)
  val state = RegInit(s_idle)

  val done = RegInit(false.B)
  val cmd = Reg(new RXChainControllerCommand(params.paddrBits))

  val baseAddrValid = RegInit(false.B)

  val disassemblerReqValid = RegInit(false.B)
  val disassemblerDone = RegInit(false.B)
  val disassemblerLength = RegInit(0.U(8.W))
  val disassemblerBusy = RegInit(false.B)

  val modemRXEnable = RegInit(false.B)

  val error = RegInit(false.B)
  val rxStart = RegInit(false.B)
  val rxFinish = RegInit(false.B)

  def gotoIdle(): Unit = {
    // Signal done
    done := true.B

    // Confirm that all regs get reset to false
    modemRXEnable := false.B
    disassemblerBusy := false.B
    disassemblerReqValid := false.B
    disassemblerDone := false.B

    state := s_idle
  }

  // Control IO
  io.control.cmd.ready := io.disassemblerControl.in.ready & !disassemblerBusy
  io.control.done := done
  io.control.baseAddr.valid := baseAddrValid
  io.control.baseAddr.bits := cmd.addr

  // Disassembler IO
  io.disassemblerControl.in.valid := disassemblerReqValid
  io.disassemblerControl.in.bits.command := cmd.command
  io.disassemblerControl.in.bits.aa := io.constants.accessAddress

  // Modem IO
  io.modemRXControl.enable := modemRXEnable

  // Interrupt IO
  io.interrupt.error := error
  io.interrupt.rxStart := rxStart
  io.interrupt.rxFinish := rxFinish

  // Set interrupts to be one cycle pulses
  when(error) {
    error := false.B
  }

  when(rxStart) {
    rxStart := false.B
  }

  when(rxFinish) {
    rxFinish := false.B
  }

  // Main FSM
  switch(state) {
    is(s_idle) {
      done := false.B
      disassemblerLength := 0.U
      disassemblerReqValid := false.B
      modemRXEnable := false.B

      when(io.control.cmd.fire() & (io.control.cmd.bits.command === PDAControlInputCommands.START_CMD |
        io.control.cmd.bits.command === PDAControlInputCommands.DEBUG_CMD)) {
        when(io.control.cmd.bits.addr(1,0) === 0.U) {
          cmd := io.control.cmd.bits
          baseAddrValid := true.B

          state := s_working
        }.otherwise {
          // TODO: Place invalid rx addr in error reg
          error := true.B
          gotoIdle()
        }
      }
    }
    is(s_working) {
      when(io.control.baseAddr.fire()) {
        baseAddrValid := false.B

        disassemblerReqValid := true.B
        when (cmd.command === PDAControlInputCommands.START_CMD) { // Don't enable modem in debug mode
          modemRXEnable := true.B
        }
      }

      when(io.disassemblerControl.in.fire()) {
        disassemblerReqValid := false.B
      }

      when(io.disassemblerControl.out.busy) { // Point of no return for this command
        rxStart := true.B
        disassemblerBusy := true.B
      }.elsewhen(io.control.cmd.fire() & io.control.cmd.bits.command === PDAControlInputCommands.EXIT_CMD) {
        // TODO: Send finish message with length = 0
        rxFinish := true.B

        disassemblerReqValid := true.B
        cmd := io.control.cmd.bits

        // Confirm that all other regs get reset to false
        disassemblerBusy := false.B
        disassemblerDone := false.B

        state := s_idle
      }

      when(io.disassemblerControl.out.done) {
        modemRXEnable := false.B
        disassemblerDone := true.B
        disassemblerBusy := false.B
        disassemblerLength := io.disassemblerControl.out.length

        when (io.disassemblerControl.out.flag_aa | io.disassemblerControl.out.flag_crc) {
          // TODO: Exception
          error := true.B
        }.otherwise {
          // TODO: Send disassembler length in finish message
          rxFinish := true.B
        }

        gotoIdle()
      }
    }
  }
}

class Controller(params: BLEBasebandModemParams, beatBytes: Int) extends Module {
  val io = IO(new Bundle {
    val interrupt = new Bundle {
      val error = Output(Bool())
      val txFinish = Output(Bool())
      val rxStart = Output(Bool())
      val rxFinish = Output(Bool())
    }
    val analog = new Bundle {
      val offChipMode = new Bundle {
        val rx = Output(Bool())
        val tx = Output(Bool())
      }
      val pllD = Output(UInt(11.W))
      val enable = new Bundle {
        val rx = Output(UInt(5.W))
      }
    }
    val basebandControl = Flipped(new BasebandControlIO(params.paddrBits))
    val cmd = Flipped(Decoupled(new BLEBasebandModemCommand))
    val constants = Output(new BasebandConstants)
    val  dma = new Bundle {
      val readReq = Decoupled(new EE290CDMAReaderReq(params.paddrBits, params.maxReadSize))
      val readResp = Flipped(Decoupled(new EE290CDMAReaderResp(params.maxReadSize)))
    }
    val modemControl = Flipped(new GFSKModemControlIO(params))
  })

  val constants = RegInit(new BasebandConstants, WireInit(new BasebandConstants().Lit(
    _.crcSeed -> "x555555".U,
    _.channelIndex -> "b010011".U,
    _.accessAddress -> "x8E89BED6".U
  )))

  io.constants := constants
  io.modemControl.rx <> DontCare

  val s_idle :: s_tx :: s_rx :: s_debug :: s_interrupt :: Nil = Enum(5)

  val state = RegInit(s_idle)

  // TX Controller
  val txController = Module(new TXChainController(params))
  io.basebandControl.assembler <> txController.io.assemblerControl
  io.dma <> txController.io.dma
  io.modemControl.tx <> txController.io.modemTXControl

  txController.io.constants := constants

  val txControllerCmdValid = RegInit(false.B)
  val txControllerCmd = Reg(new TXChainControllerCommand(params.paddrBits, params.maxReadSize))
  txController.io.control.cmd.valid := txControllerCmdValid
  txController.io.control.cmd.bits := txControllerCmd

  val txControllerDone = RegInit(false.B)

  // RX Controller
  val rxController = Module(new RXChainController(params))
  io.basebandControl.disassembler <> rxController.io.disassemblerControl
  io.basebandControl.baseAddr <> rxController.io.control.baseAddr

  rxController.io.constants := constants

  val rxControllerCmdValid = RegInit(false.B)
  val rxControllerCmd = Reg(new RXChainControllerCommand(params.paddrBits))
  rxController.io.control.cmd.valid := rxControllerCmdValid
  rxController.io.control.cmd.bits := rxControllerCmd

  val rxControllerDone = RegInit(false.B)

  // Loopback Mask
  val loopbackMask = RegInit(0.U(4.W))
  io.basebandControl.loopback := loopbackMask(1,0).asBools()

  // Analog IO
  io.analog.pllD := 1200.U + constants.channelIndex + (state === s_tx).asUInt()
  io.analog.enable.rx := Mux(state === s_rx | state === s_debug, (scala.math.pow(2, io.analog.enable.rx.getWidth) - 1).toInt.asUInt, 0.U)
  io.analog.offChipMode.rx := state === s_rx
  io.analog.offChipMode.tx := state === s_tx

  // Interrupt
  io.interrupt.error := txController.io.interrupt.error | rxController.io.interrupt.error
  io.interrupt.txFinish := txController.io.interrupt.txFinish
  io.interrupt.rxStart := rxController.io.interrupt.rxStart
  io.interrupt.rxFinish := rxController.io.interrupt.rxFinish

  // Command wires
  io.cmd.ready := state === s_idle

  switch(state) {
    is (s_idle) {
      when (io.cmd.fire) {
        switch (io.cmd.bits.inst.primaryInst) {
          is (BasebandISA.CONFIG_CMD) { // Don't need to waste a cycle to setup config
            switch (io.cmd.bits.inst.secondaryInst) {
              is (BasebandISA.CONFIG_CRC_SEED) {
                constants.crcSeed := io.cmd.bits.additionalData(23, 0)
              }
              is (BasebandISA.CONFIG_ACCESS_ADDRESS) {
                constants.accessAddress := io.cmd.bits.additionalData
              }
              is (BasebandISA.CONFIG_CHANNEL_INDEX) {
                constants.channelIndex := io.cmd.bits.additionalData(5, 0)
              }
            }
          }
          is (BasebandISA.SEND_CMD) {
            txControllerCmdValid := true.B
            txControllerCmd.totalBytes := io.cmd.bits.inst.data
            txControllerCmd.addr := io.cmd.bits.additionalData
            state := s_tx
          }
          is (BasebandISA.RECEIVE_START_CMD) {
            rxControllerCmdValid := true.B
            rxControllerCmd.command := PDAControlInputCommands.START_CMD
            rxControllerCmd.addr := io.cmd.bits.additionalData
            state := s_rx
          }
          is (BasebandISA.DEBUG_CMD) {
            loopbackMask := io.cmd.bits.inst.secondaryInst

            txControllerCmdValid := true.B
            txControllerCmd.totalBytes := io.cmd.bits.inst.data
            txControllerCmd.addr := io.cmd.bits.additionalData

            rxControllerCmdValid := true.B
            rxControllerCmd.command := PDAControlInputCommands.DEBUG_CMD
            rxControllerCmd.addr := (io.cmd.bits.additionalData + io.cmd.bits.inst.data + beatBytes.U) & (~((beatBytes-1).U(params.paddrBits.W))).asUInt
            state := s_debug
          }
        }
      }
    }
    is (s_tx) {
      when (txController.io.control.cmd.fire()) {
        txControllerCmdValid := false.B
      }

      when (txController.io.control.done) {
        state := s_idle
      }
    }
    is (s_rx) { // TODO: Need to peek into valid commands and if they are RX, or exit allow for return to idle when not busy
      when (rxController.io.control.cmd.fire()) {
        rxControllerCmdValid := false.B
      }

      when (rxController.io.control.done) {
        state := s_idle
      }
    }
    is (s_debug) {
      when (rxController.io.control.cmd.fire()) {
        rxControllerCmdValid := false.B
      }

      when (txController.io.control.cmd.fire()) {
        txControllerCmdValid := false.B
      }

      when (txController.io.control.done) {
        txControllerDone := true.B
      }

      when (rxController.io.control.done) {
        rxControllerDone := true.B
      }

      when (rxControllerDone & txControllerDone) {
        loopbackMask := 0.U

        txControllerDone := false.B
        rxControllerDone := false.B

        state := s_idle
      }
    }
    is (s_interrupt) {
      rxControllerCmd.command := PDAControlInputCommands.EXIT_CMD
    }
  }
}