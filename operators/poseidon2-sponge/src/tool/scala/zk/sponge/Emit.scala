/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
 * Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
 * File: operators/poseidon2-sponge/src/tool/scala/zk/sponge/Emit.scala
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

package zk.sponge
import circt.stage.ChiselStage
import java.nio.file.{Files,Paths}
object Emit extends App {
  require(args.length == 4)
  val path=Paths.get(args(0)).normalize()
  require(!path.isAbsolute && path.startsWith(Paths.get("build")) && !Files.exists(path))
  val directory=Paths.get(args(3)).toAbsolutePath.normalize()
  val lines=scala.io.Source.fromFile(directory.resolve("tasks.txt").toFile)
  val rows=try lines.getLines().map(_.split(" ").map(_.toInt).toSeq).toSeq finally lines.close()
  val input=scala.io.Source.fromFile(directory.resolve("input.hex").toFile)
  val depth=try input.getLines().size finally input.close()
  Files.createDirectories(path)
  ChiselStage.emitSystemVerilog(new SpongeSimTop(SpongeConfig(args(1).toInt,args(2).toInt),directory.toString,rows,depth),
    firtoolOpts=Array("-O=release","-disable-all-randomization","--lowering-options=disallowLocalVariables",
      "-split-verilog",s"-o=$path"))
}
