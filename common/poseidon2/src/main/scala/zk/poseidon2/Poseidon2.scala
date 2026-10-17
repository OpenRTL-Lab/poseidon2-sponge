/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
 * Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
 * File: common/poseidon2/src/main/scala/zk/poseidon2/Poseidon2.scala
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

package zk.poseidon2

import chisel3._
import chisel3.util._

case class PoseidonConfig(contexts: Int = 16) {
  require(Set(16, 32).contains(contexts))
  val indexBits = log2Ceil(contexts)
}
class NativeState extends Bundle {
  val tag = UInt(32.W)
  val words = Vec(12, UInt(64.W))
}
object Field {
  private def p = Parameters.P.U(65.W)
  def add(a: UInt, b: UInt): UInt = {
    val s = Prefix.add(a,b,65)
    val corrected = Prefix.add(s(63,0),"hffffffff".U(65.W),65)
    Mux(s(64) || corrected(64),corrected(63,0),s(63,0))
  }
  def neg(a: UInt): UInt = Mux(a === 0.U, 0.U(64.W), (p - a)(63, 0))
  def half(a: UInt): UInt = {
    val s = Mux(a(0), Cat(0.U(1.W), a) + p, Cat(0.U(1.W), a))
    s(64, 1)
  }
  def sum(xs: Seq[UInt]): UInt =
    if (xs.size == 1) xs.head else sum(xs.grouped(2).map(x =>
      if(x.size == 2) add(x(0),x(1)) else x.head).toSeq)
  def mat4(x: Seq[UInt]): Seq[UInt] = {
    val a = add(x(0),x(1)); val b = add(x(2),x(3))
    val s = add(a,b); val u = add(s,x(1)); val v = add(s,x(3))
    Seq(add(u,a),add(u,add(x(2),x(2))),add(v,b),add(v,add(x(0),x(0))))
  }
  def diagonal(x: Seq[UInt]): Seq[UInt] = Seq(
    neg(add(x(0),x(0))),x(1),add(x(2),x(2)),half(x(3)),
    add(add(x(4),x(4)),x(4)),add(add(x(5),x(5)),add(x(5),x(5))),
    neg(half(x(6))),neg(add(add(x(7),x(7)),x(7))),
    neg(add(add(x(8),x(8)),add(x(8),x(8)))),
    half(half(x(9))),neg(half(half(x(10)))),half(half(half(x(11)))))
}

/** 每层规范模加真实两拍，所有叉路显式对齐；不把深组合归约包进一拍。 */
object LinearPipe {
  def delay(x: UInt,n: Int): UInt = if(n==0) x else ShiftRegister(x,n)
  def add(a: UInt,b: UInt): UInt = {
    val s = RegNext(Prefix.add(a,b,65))
    val corrected = Prefix.add(s(63,0),"hffffffff".U(65.W),65)
    RegNext(Mux(s(64) || corrected(64),corrected(63,0),s(63,0)))
  }
  def half(a: UInt): UInt = {
    val s = RegNext(Prefix.add(a,Parameters.P.U(65.W),65))
    val original = RegNext(a)
    RegNext(Mux(original(0),s(64,1),Cat(0.U(1.W),original(63,1))))
  }
  def neg(a: UInt): UInt = {
    val s = RegNext(Prefix.subtract(Parameters.P.U(65.W),a,65))
    val zero = RegNext(a === 0.U)
    RegNext(Mux(zero,0.U(64.W),s(63,0)))
  }
  def sum(xs: Seq[UInt]): UInt = {
    if(xs.size==1) xs.head else sum(xs.grouped(2).map(x =>
      if(x.size==2) add(x(0),x(1)) else delay(x.head,2)).toSeq)
  }
  def mat4(x: Seq[UInt]): Seq[UInt] = {
    val a = add(x(0),x(1));val b = add(x(2),x(3))
    val d0 = add(x(0),x(0));val d2 = add(x(2),x(2))
    val s = add(a,b)
    val u = add(s,delay(x(1),4));val v = add(s,delay(x(3),4))
    Seq(add(u,delay(a,4)),add(u,delay(d2,4)),add(v,delay(b,4)),add(v,delay(d0,4)))
  }
  def extB(x: Seq[UInt]): Seq[UInt] = {
    val sums = (0 until 4).map(k => sum((0 until 3).map(j => x(4*j+k))))
    (0 until 12).map(i => add(delay(x(i),4),sums(i%4)))
  }
  def diagonal(x: Seq[UInt]): Seq[UInt] = {
    val d0=add(x(0),x(0)); val d2=add(x(2),x(2));val h3=half(x(3))
    val triple4=add(add(x(4),x(4)),delay(x(4),2))
    val double5=add(x(5),x(5));val quad5=add(double5,double5)
    val h6=half(x(6));val triple7=add(add(x(7),x(7)),delay(x(7),2))
    val double8=add(x(8),x(8));val quad8=add(double8,double8)
    val hh9=half(half(x(9)));val hh10=half(half(x(10)))
    Seq(delay(neg(d0),2),delay(x(1),6),delay(d2,4),delay(h3,4),
      delay(triple4,2),delay(quad5,2),delay(neg(h6),2),neg(triple7),neg(quad8),
      delay(hh9,2),neg(hh10),half(half(half(x(11)))))
  }
}

class GoldilocksPoseidon2(val cfg: PoseidonConfig) extends Module {
  val io = IO(new Bundle {
    val enable = Input(Bool())
    val request = Flipped(Decoupled(new NativeState))
    val response = Decoupled(new NativeState)
    val busy = Output(Bool())
  })
  val Idle = 0.U(4.W); val ExtA = 1.U(4.W); val ExtB = 2.U(4.W)
  val Square = 3.U(4.W); val Cube = 4.U(4.W); val Fourth = 5.U(4.W)
  val Seventh = 6.U(4.W); val IntA = 7.U(4.W); val IntB = 8.U(4.W)
  val Done = 9.U(4.W)
  val slot = RegInit(0.U(cfg.indexBits.W))
  val phase = RegInit(VecInit(Seq.fill(cfg.contexts)(Idle)))
  val states = Reg(Vec(cfg.contexts, Vec(12, UInt(64.W))))
  val tags = Reg(Vec(cfg.contexts, UInt(32.W)))
  val rounds = Reg(Vec(cfg.contexts, UInt(5.W)))
  val word = Reg(Vec(cfg.contexts, UInt(4.W)))
  val initial = Reg(Vec(cfg.contexts, Bool()))
  val base = Reg(Vec(cfg.contexts, UInt(64.W)))
  val x2 = Reg(Vec(cfg.contexts, UInt(64.W)))
  val x3 = Reg(Vec(cfg.contexts, UInt(64.W)))
  val x4 = Reg(Vec(cfg.contexts, UInt(64.W)))
  val total = Reg(Vec(cfg.contexts, UInt(64.W)))
  val st = states(slot)
  val ph = phase(slot)
  val full = rounds(slot) < 4.U || rounds(slot) >= 26.U
  val rc = VecInit(Parameters.RC.map(row => VecInit(row.map(_.U(64.W)))))
  // 选择独立寄存；准备模加两拍，操作数选取一拍。反馈15<16，不改变轮转相位数。
  val readState = RegNext(st)
  val readWord = RegNext(word(slot));val readRound = RegNext(rounds(slot))
  val readPhase = RegNext(ph)
  val readBase = RegNext(base(slot));val readX2 = RegNext(x2(slot))
  val readX3 = RegNext(x3(slot));val readX4 = RegNext(x4(slot))
  val z = LinearPipe.add(readState(readWord),rc(readRound)(readWord))
  val preparePhase = ShiftRegister(readPhase,2)
  val mul = Module(new PoseidonGoldilocksMul)
  val issue = ph >= Square && ph <= Seventh
  mul.io.a := RegNext(MuxLookup(preparePhase,0.U)(Seq(Square -> z,
    Cube -> LinearPipe.delay(readX2,2),Fourth -> LinearPipe.delay(readX2,2),
    Seventh -> LinearPipe.delay(readX3,2))))
  mul.io.b := RegNext(MuxLookup(preparePhase,0.U)(Seq(Square -> z,
    Cube -> LinearPipe.delay(readBase,2),Fourth -> LinearPipe.delay(readX2,2),
    Seventh -> LinearPipe.delay(readX4,2))))
  val feedback = PoseidonGoldilocksMul.Latency+4
  require(feedback < cfg.contexts)
  val mulIssue = ShiftRegister(issue,4,false.B,true.B)
  val resultValid = ShiftRegister(issue,feedback,false.B,true.B)
  val resultSlot = ShiftRegister(slot,feedback)
  val resultWord = ShiftRegister(word(slot),feedback)
  val resultPhase = ShiftRegister(ph,feedback)
  val baseSlot = ShiftRegister(slot,3)
  val baseValid = ShiftRegister(ph===Square,3,false.B,true.B)
  io.request.ready := io.enable && ph === Idle
  io.response.valid := ph === Done
  io.response.bits.tag := tags(slot)
  io.response.bits.words := st
  io.busy := phase.map(_ =/= Idle).reduce(_ || _)
  // 背压期间锁定响应及tag，其他在途乘法仍完成写回。
  when(!(io.response.valid && !io.response.ready)) { slot := slot + 1.U }
  val m4 = readState.toSeq.grouped(4).flatMap(LinearPipe.mat4).toSeq
  val extB = LinearPipe.extB(readState.toSeq)
  val intSum = LinearPipe.sum(readState.toSeq)
  val diag = LinearPipe.diagonal(readState.toSeq)
  val intB = diag.map(x => LinearPipe.add(LinearPipe.delay(RegNext(total(slot)),6),x))
  val m4Slot=ShiftRegister(slot,9);val m4Valid=ShiftRegister(ph===ExtA,9,false.B,true.B)
  val ebSlot=ShiftRegister(slot,7);val ebValid=ShiftRegister(ph===ExtB,7,false.B,true.B)
  val iaSlot=ShiftRegister(slot,9);val iaValid=ShiftRegister(ph===IntA,9,false.B,true.B)
  val ibSlot=ShiftRegister(slot,9);val ibValid=ShiftRegister(ph===IntB,9,false.B,true.B)
  def advance(c: Int): Unit = {
    when(rounds(c) === 29.U) { phase(c) := Done }
    .otherwise { rounds(c) := rounds(c) + 1.U; word(c) := 0.U; phase(c) := Square }
  }
  for(c <- 0 until cfg.contexts) {
    when(slot === c.U) {
      switch(ph) {
        is(Idle) { when(io.request.fire) {
          for(i <- 0 until 12) {
            assert(io.request.bits.words(i) < Parameters.P.U(64.W))
            states(c)(i) := io.request.bits.words(i)
          }
          tags(c) := io.request.bits.tag; rounds(c) := 0.U; word(c) := 0.U
          initial(c) := true.B; phase(c) := ExtA
        }}
        is(ExtA) { phase(c) := ExtB }
        is(ExtB) {
          when(initial(c)) { initial(c) := false.B; phase(c) := Square }
          .otherwise { advance(c) }
        }
        is(Square) { phase(c) := Cube }
        is(Cube) { phase(c) := Fourth }
        is(Fourth) { phase(c) := Seventh }
        is(Seventh) {
          when(full && word(c) =/= 11.U) { word(c) := word(c) + 1.U; phase(c) := Square }
          .otherwise { phase(c) := Mux(full, ExtA, IntA) }
        }
        is(IntA) { phase(c) := IntB }
        is(IntB) { advance(c) }
        is(Done) { when(io.response.fire) { phase(c) := Idle } }
      }
    }
    when(baseValid && baseSlot===c.U) { base(c):=z }
    when(m4Valid && m4Slot===c.U) { states(c):=VecInit(m4) }
    when(ebValid && ebSlot===c.U) { states(c):=VecInit(extB) }
    when(iaValid && iaSlot===c.U) { total(c):=intSum }
    when(ibValid && ibSlot===c.U) { states(c):=VecInit(intB) }
    when(resultValid && resultSlot === c.U) {
      assert(slot =/= c.U, "feedback must precede context reuse")
      switch(resultPhase) {
        is(Square) { x2(c) := mul.io.product }
        is(Cube) { x3(c) := mul.io.product }
        is(Fourth) { x4(c) := mul.io.product }
        is(Seventh) { states(c)(resultWord) := mul.io.product }
      }
    }
  }
  // 仅绑定验证层包含统计寄存器；产品闭包不包含本层。
  chisel3.layer.block(chisel3.layers.Verification) {
    val traceCycle = RegInit(0.U(64.W))
    val traceMul = RegInit(0.U(64.W))
    val traceLinear = RegInit(VecInit(Seq.fill(4)(0.U(64.W))))
    val traceLinearPipe = RegInit(VecInit(Seq.fill(4)(0.U(64.W))))
    val traceLinearSlots = RegInit(VecInit(Seq.fill(4)(0.U(64.W))))
    val traceValid = RegInit(VecInit(Seq.fill(PoseidonGoldilocksMul.Latency)(false.B)))
    val tracePipe = RegInit(0.U(64.W))
    val traceStageSlots = RegInit(0.U(64.W))
    traceValid(0) := mulIssue
    for(i <- 1 until PoseidonGoldilocksMul.Latency) { traceValid(i) := traceValid(i-1) }
    when(traceValid.asUInt.orR) { tracePipe := tracePipe + 1.U }
    traceStageSlots := traceStageSlots + PopCount(traceValid)
    traceCycle := traceCycle + 1.U
    when(mulIssue) { traceMul := traceMul + 1.U }
    for ((s, i) <- Seq(ExtA, ExtB, IntA, IntB).zipWithIndex) {
      when(ph === s) { traceLinear(i) := traceLinear(i) + 1.U }
      // 上下文读后一拍进入实际线性流水，最长分支8/6/8/8拍；只计有效工作，不计无效翻转。
      val readValid = RegNext(ph === s,false.B)
      val valid = RegInit(VecInit(Seq.fill(Seq(8,6,8,8)(i))(false.B)))
      valid(0) := readValid
      for(j <- 1 until valid.length) { valid(j) := valid(j-1) }
      when(valid.asUInt.orR) { traceLinearPipe(i) := traceLinearPipe(i) + 1.U }
      traceLinearSlots(i) := traceLinearSlots(i) + PopCount(valid)
    }
    when(io.response.fire) {
      printf(p"CORE_STAT cycle=${traceCycle} tag=${tags(slot)} mul=${traceMul} pipe=${tracePipe} slots=${traceStageSlots} ea=${traceLinear(0)} eb=${traceLinear(1)} ia=${traceLinear(2)} ib=${traceLinear(3)} eap=${traceLinearPipe(0)} ebp=${traceLinearPipe(1)} iap=${traceLinearPipe(2)} ibp=${traceLinearPipe(3)} eas=${traceLinearSlots(0)} ebs=${traceLinearSlots(1)} ias=${traceLinearSlots(2)} ibs=${traceLinearSlots(3)}\n")
    }
  }
}
