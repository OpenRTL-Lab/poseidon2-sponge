/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
 * Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
 * File: operators/poseidon2-sponge/src/main/scala/zk/sponge/Environment.scala
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

package zk.sponge

import chisel3._
import chisel3.util._
import chisel3.util.experimental.loadMemoryFromFileInline

/** 离线C ROM驱动所有DUT通道；C++只能操作时钟、复位和本ENV控制口。 */
class SpongeSimTop(cfg: SpongeConfig, directory: String, rows: Seq[Seq[Int]], inputDepth: Int) extends Module {
  val tasks = rows.size
  require(tasks >= 2 && isPow2(tasks))
  val tb = log2Ceil(tasks)
  val io = IO(new Bundle {
    val trigger = Input(Bool()); val mode = Input(UInt(3.W))
    val done = Output(Bool()); val pass = Output(Bool())
    val errors = Output(UInt(32.W)); val checked = Output(UInt(32.W)); val cycles = Output(UInt(64.W))
  })
  val running = RegInit(false.B); val cycle = RegInit(0.U(64.W))
  val issued = RegInit(0.U(32.W)); val completed = RegInit(0.U(32.W))
  val errors = RegInit(0.U(32.W)); val checked = RegInit(0.U(32.W))
  val active = RegInit(VecInit(Seq.fill(tasks)(false.B)))
  val seen = RegInit(VecInit(Seq.fill(tasks)(false.B)))
  val sent = RegInit(VecInit(Seq.fill(tasks)(0.U(32.W))))
  val accepted = Reg(Vec(tasks, UInt(64.W)))
  val lengths = VecInit(rows.map(_(1).U(32.W)))
  val modes = VecInit(rows.map(_(0).U(2.W)))
  val statuses = VecInit(rows.map(_(2).U(2.W)))
  val faults = VecInit(rows.map(_(3).U(2.W)))
  val offsets = VecInit(rows.map(_(4).U(32.W)))
  val input = Mem(inputDepth, UInt(64.W)); val expected = Mem(tasks*4, UInt(64.W))
  loadMemoryFromFileInline(input, directory+"/input.hex")
  loadMemoryFromFileInline(expected, directory+"/expected.hex")
  val resetDone = RegInit(false.B)
  val abort = running && io.mode === 2.U && cycle === 1000.U && !resetDone
  val dut = withReset(reset.asBool || abort) { Module(new GoldilocksPoseidon2Sponge(cfg)) }
  dut.io.enable := !(io.mode === 1.U && cycle(5,0) < 3.U)
  val a = issued(tb-1,0)
  // 一旦valid置位，输入payload不受停顿模式影响。
  dut.io.task.valid := running && issued < tasks.U && !abort &&
    !(io.mode === 4.U && issued =/= completed)
  dut.io.task.bits.tag := issued; dut.io.task.bits.mode := modes(a)
  dut.io.task.bits.elementCount := lengths(a)
  val dataPointer = RegInit(0.U(tb.W))
  val dataLocked = RegInit(false.B)
  val lockedSlot = Reg(UInt(tb.W))
  val eligible = VecInit((0 until tasks).map(t => active(t) && statuses(t) =/= 1.U && sent(t) < lengths(t)))
  // 理想生产者组合跳过空槽；已拉高valid的阻塞拍锁定，不能靠偷看DUT内部状态抢占。
  val candidate = Mux(eligible(dataPointer), dataPointer, PriorityEncoder(eligible))
  val dataSlot = Mux(dataLocked, lockedSlot, candidate)
  val remaining = lengths(dataSlot) - sent(dataSlot)
  val count = Mux(remaining >= 8.U, 8.U(4.W), remaining(3,0))
  dut.io.input.valid := running && active(dataSlot) && statuses(dataSlot) =/= 1.U &&
    sent(dataSlot) < lengths(dataSlot) && !abort
  dut.io.input.bits.tag := dataSlot
  dut.io.input.bits.count := Mux(faults(dataSlot) === 1.U && sent(dataSlot) === 0.U, 0.U, count)
  dut.io.input.bits.last := Mux(faults(dataSlot) === 2.U && sent(dataSlot) === 0.U, true.B, remaining <= 8.U)
  for(i <- 0 until 8) dut.io.input.bits.words(i) := input(offsets(dataSlot) + sent(dataSlot) + i.U)
  when(dut.io.input.valid && !dut.io.input.ready) {
    dataLocked := true.B; lockedSlot := dataSlot
  }.otherwise {
    dataLocked := false.B
    when(dut.io.input.fire) { dataPointer := dataSlot + 1.U }
  }
  dut.io.digest.ready := !(io.mode === 1.U && cycle(5,0) < 21.U)
  dut.io.completion.ready := !(io.mode === 1.U && cycle(4,0) < 13.U)
  when(io.trigger) { running := true.B }
  when(running) { cycle := cycle + 1.U }
  when(abort) {
    resetDone := true.B; issued := 0.U; completed := 0.U; errors := 0.U; checked := 0.U
    active.foreach(_ := false.B); seen.foreach(_ := false.B); sent.foreach(_ := 0.U)
    dataPointer := 0.U; dataLocked := false.B
    printf(p"RESET cycle=${cycle}\n")
  }.otherwise {
    when(dut.io.task.fire) {
      active(a) := true.B; accepted(a) := cycle; issued := issued + 1.U
      printf(p"A tag=${issued} cycle=${cycle}\n")
    }
    when(dut.io.input.fire) {
      when(sent(dataSlot) === 0.U) { printf(p"F tag=${dataSlot} cycle=${cycle}\n") }
      sent(dataSlot) := sent(dataSlot) + count
    }
    when(dut.io.digest.fire) {
      val t = dut.io.digest.bits.tag(tb-1,0)
      assert(dut.io.digest.bits.tag < tasks.U && active(t) && !seen(t) && statuses(t) === 0.U)
      assert(dut.io.digest.bits.count === 4.U && dut.io.digest.bits.last)
      seen(t) := true.B
      val bad = (0 until 4).map(i => dut.io.digest.bits.words(i) =/=
        (expected(t*4.U+i.U) ^ Mux(io.mode === 3.U && t === 0.U && i.U === 0.U, 1.U, 0.U)))
      errors := errors + PopCount(bad); checked := checked + 4.U
      printf(p"O tag=${t} cycle=${cycle} errors=${PopCount(bad)}\n")
    }
    when(dut.io.completion.fire) {
      val t = dut.io.completion.bits.tag(tb-1,0)
      assert(dut.io.completion.bits.tag < tasks.U && active(t))
      assert(dut.io.completion.bits.status === statuses(t) && dut.io.completion.bits.elementCount === lengths(t))
      assert(Mux(statuses(t) === 0.U, seen(t), !seen(t)))
      active(t) := false.B; completed := completed + 1.U
      printf(p"C tag=${t} cycle=${cycle} latency=${cycle-accepted(t)} status=${dut.io.completion.bits.status}\n")
      when(completed === (tasks-1).U) { running := false.B }
    }
  }
  for(channel <- Seq(dut.io.task, dut.io.input, dut.io.digest, dut.io.completion)) {
    val held = RegNext(channel.valid && !channel.ready && !abort, false.B)
    val previous = RegNext(channel.bits.asUInt)
    when(held && !abort) { assert(channel.valid && channel.bits.asUInt === previous, "ready/valid payload must hold") }
  }
  io.done := completed === tasks.U || cycle > 200000000.U
  io.pass := completed === tasks.U && errors === 0.U
  io.errors := errors; io.checked := checked; io.cycles := cycle
}
