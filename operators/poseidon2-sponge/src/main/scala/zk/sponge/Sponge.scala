/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
 * Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
 * File: operators/poseidon2-sponge/src/main/scala/zk/sponge/Sponge.scala
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
import zk.poseidon2._

case class SpongeConfig(contexts: Int = 16, maxElements: Int = 4096) {
  require(Set(16, 32).contains(contexts))
  require(maxElements >= 8 && maxElements <= 1048576)
  val indexBits = log2Ceil(contexts)
}
class TaskRecord extends Bundle {
  val tag = UInt(32.W); val mode = UInt(2.W); val elementCount = UInt(32.W)
}
class InputRecord extends Bundle {
  val tag = UInt(32.W); val words = Vec(8, UInt(64.W))
  val count = UInt(4.W); val last = Bool()
}
class DigestRecord extends Bundle {
  val tag = UInt(32.W); val words = Vec(4, UInt(64.W))
  val count = UInt(4.W); val last = Bool()
}
class CompletionRecord extends Bundle {
  val tag = UInt(32.W); val status = UInt(2.W); val elementCount = UInt(32.W)
}

// 独立IP有ENV之外的用户；保留完整公开接口，禁止测试常量跨边界特化产品。
class GoldilocksPoseidon2Sponge(val cfg: SpongeConfig) extends Module with Public with RequireSyncReset {
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val task = Flipped(Decoupled(new TaskRecord))
    val input = Flipped(Decoupled(new InputRecord))
    val digest = Decoupled(new DigestRecord)
    val completion = Decoupled(new CompletionRecord)
    val busy = Output(Bool())
  })
  // 寄存容量决定外部ready；不允许tag/valid/外部ready组合直通。
  val sampledEnable=RegNext(io.enable,false.B)
  // 入口FIFO中的任务也预留上下文；否则未分配tag的数据会堵塞已有任务。
  val taskCredits=RegInit(cfg.contexts.U(log2Ceil(cfg.contexts+1).W))
  when(io.task.fire =/= io.completion.fire) {
    taskCredits:=Mux(io.task.fire,taskCredits-1.U,taskCredits+1.U)
  }
  val tq=Module(new Queue(new TaskRecord,2,pipe=false,flow=false))
  tq.io.enq.valid:=io.task.valid && sampledEnable && taskCredits=/=0.U
  tq.io.enq.bits:=io.task.bits
  io.task.ready:=tq.io.enq.ready && sampledEnable && taskCredits=/=0.U
  val task=tq.io.deq
  val iq=Module(new Queue(new InputRecord,2,pipe=false,flow=false))
  iq.io.enq<>io.input
  val input=iq.io.deq
  val oq=Module(new Queue(new DigestRecord,2,pipe=false,flow=false))
  io.digest<>oq.io.deq
  val digest=oq.io.enq
  val cq=Module(new Queue(new CompletionRecord,2,pipe=false,flow=false))
  io.completion<>cq.io.deq
  val completion=cq.io.enq
  val Idle = 0.U(3.W); val Absorb = 1.U(3.W); val Ready = 2.U(3.W)
  val Wait = 3.U(3.W); val Digest = 4.U(3.W); val Complete = 5.U(3.W); val Drain = 6.U(3.W)
  val Retired = 7.U(3.W)
  val phase = RegInit(VecInit(Seq.fill(cfg.contexts)(Idle)))
  val state = Reg(Vec(cfg.contexts, Vec(12, UInt(64.W))))
  val tag = Reg(Vec(cfg.contexts, UInt(32.W)))
  val mode = Reg(Vec(cfg.contexts, UInt(2.W)))
  val length = Reg(Vec(cfg.contexts, UInt(32.W)))
  val remaining = Reg(Vec(cfg.contexts, UInt(32.W)))
  val status = Reg(Vec(cfg.contexts, UInt(2.W)))
  val finalBlock = Reg(Vec(cfg.contexts, Bool()))
  val free = VecInit(phase.map(_ === Idle))
  val allocate = PriorityEncoder(free)
  task.ready := free.asUInt.orR
  io.busy := phase.map(_ =/= Idle).reduce(_ || _) || tq.io.count=/=0.U ||
    iq.io.count=/=0.U || oq.io.count=/=0.U || cq.io.count=/=0.U
  when(task.fire) {
    for(c <- 0 until cfg.contexts) {
      assert(phase(c) === Idle || tag(c) =/= task.bits.tag, "in-flight tags must be unique")
    }
    val a = allocate
    tag(a) := task.bits.tag; mode(a) := task.bits.mode
    length(a) := task.bits.elementCount; remaining(a) := task.bits.elementCount
    status(a) := 0.U; finalBlock(a) := false.B
    state(a) := VecInit(Seq.fill(12)(0.U(64.W)))
    val bad = task.bits.mode > 1.U || task.bits.elementCount > cfg.maxElements.U ||
      (task.bits.mode === 1.U && task.bits.elementCount =/= 8.U)
    when(bad) { status(a) := 1.U; phase(a) := Complete }
    .elsewhen(task.bits.elementCount === 0.U) {
      state(a)(0) := 1.U; finalBlock(a) := true.B; phase(a) := Ready
    }.otherwise { phase(a) := Absorb }
  }
  val matches = VecInit((0 until cfg.contexts).map(c =>
    (phase(c) === Absorb || phase(c) === Drain) && tag(c) === input.bits.tag))
  val inputSlot = PriorityEncoder(matches)
  input.ready := matches.asUInt.orR
  val r = remaining(inputSlot)
  val count = Mux(r >= 8.U, 8.U(4.W), r(3,0))
  val last = r <= 8.U
  val badFrame = input.bits.count =/= count || input.bits.last =/= last
  val badField = (0 until 8).map(i => i.U < count && input.bits.words(i) >= Parameters.P.U(64.W)).reduce(_ || _)
  when(input.fire) {
    val a = inputSlot
    remaining(a) := r - count
    when(phase(a) === Drain) { when(last) { phase(a) := Complete } }
    .elsewhen(badFrame || badField) {
      status(a) := Mux(badFrame, 2.U, 3.U)
      phase(a) := Mux(last, Complete, Drain)
    }.otherwise {
      for(i <- 0 until 8) {
        state(a)(i) := Mux(i.U < count, input.bits.words(i),
          Mux(i.U === count, 1.U(64.W), 0.U(64.W)))
      }
      when(last && count === 8.U && mode(a) === 0.U) {
        state(a)(8) := Field.add(state(a)(8), 1.U(64.W))
      }
      finalBlock(a) := last; phase(a) := Ready
    }
  }
  val core = Module(new GoldilocksPoseidon2(PoseidonConfig(cfg.contexts)))
  core.io.enable := true.B
  val reqSlot = RegInit(0.U(cfg.indexBits.W))
  core.io.request.valid := phase(reqSlot) === Ready
  core.io.request.bits.tag := reqSlot
  core.io.request.bits.words := state(reqSlot)
  when(!(core.io.request.valid && !core.io.request.ready)) { reqSlot := reqSlot + 1.U }
  when(core.io.request.fire) { phase(reqSlot) := Wait }
  val rq=Module(new Queue(new NativeState,2,pipe=false,flow=false))
  rq.io.enq<>core.io.response
  val returned=rq.io.deq
  returned.ready := true.B
  val returnSlot = returned.bits.tag(cfg.indexBits-1,0)
  when(returned.fire) {
    assert(returned.bits.tag < cfg.contexts.U && phase(returnSlot) === Wait)
    state(returnSlot) := returned.bits.words
    phase(returnSlot) := Mux(finalBlock(returnSlot), Digest, Absorb)
  }
  val outSlot = RegInit(0.U(cfg.indexBits.W))
  digest.valid := phase(outSlot) === Digest
  digest.bits.tag := tag(outSlot)
  digest.bits.words := VecInit((0 until 4).map(i => state(outSlot)(i)))
  digest.bits.count := 4.U; digest.bits.last := true.B
  when(!(digest.valid && !digest.ready)) { outSlot := outSlot + 1.U }
  // completion不能早于外部digest握手；仅入队还不算交付。
  when(digest.fire) { phase(outSlot) := Complete }
  val doneSlot = RegInit(0.U(cfg.indexBits.W))
  val digestPending=RegInit(VecInit(Seq.fill(cfg.contexts)(false.B)))
  when(digest.fire) { digestPending(outSlot):=true.B }
  when(io.digest.fire) {
    for(c<-0 until cfg.contexts) { when(digestPending(c) && tag(c)===io.digest.bits.tag) { digestPending(c):=false.B } }
  }
  completion.valid := phase(doneSlot) === Complete && !digestPending(doneSlot)
  completion.bits.tag := tag(doneSlot); completion.bits.status := status(doneSlot)
  completion.bits.elementCount := length(doneSlot)
  when(!(completion.valid && !completion.ready)) { doneSlot := doneSlot + 1.U }
  // 槽在外部completion握手后再释放，防止相同tag复用时还留有排队完成。
  when(completion.fire) { phase(doneSlot):=Retired }
  when(io.completion.fire) {
    for(c<-0 until cfg.contexts) { when(phase(c)===Retired && tag(c)===io.completion.bits.tag) { phase(c):=Idle } }
  }
  chisel3.layer.block(chisel3.layers.Verification) {
    assert(taskCredits<=cfg.contexts.U)
    val admitted=RegInit(0.U(log2Ceil(cfg.contexts+1).W))
    when(io.task.fire =/= io.completion.fire) {
      admitted:=Mux(io.task.fire,admitted+1.U,admitted-1.U)
    }
    assert((admitted+&taskCredits)===cfg.contexts.U,"admission credit conservation")
    when(io.completion.fire) { assert(admitted=/=0.U) }
    val traceCycle = RegInit(0.U(64.W)); traceCycle := traceCycle + 1.U
    when(core.io.response.fire) {
      val observedSlot=core.io.response.bits.tag(cfg.indexBits-1,0)
      printf(p"PERM_OUT cycle=${traceCycle} tag=${tag(observedSlot)} context=${observedSlot} final=${finalBlock(observedSlot)}\n")
    }
  }
}
