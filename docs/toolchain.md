<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/toolchain.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 工具链版本

[English](toolchain.en.md)

本文件给出公开生成和仿真流程使用的工具版本、依赖和环境配置；所有设计与验证工具均可独立安装，项目不绑定特定机器、代理或缓存目录。

| 工具 | 版本 | 用途 |
| --- | --- | --- |
| Mill | 1.1.7 | Scala构建与生成入口 |
| Scala | 2.13.18 | Chisel源代码 |
| Chisel及编译插件 | 7.13.0 | 电路展开 |
| CIRCT firtool | 1.149.0 | SV降级与拆分 |
| OpenJDK | 21.0.11 | JVM |
| Python | 3.12 | 脚本与独立大整数参考 |
| Verilator | 5.050 | lint、C++周期仿真 |
| GCC/G++ | 16.2.0 | C参考及仿真编译 |

安装上述工具，使 `java`、`mill`、`python3`、`verilator`、`cc`、`g++` 可通过PATH调用。Scala/Chisel/插件由 [构建配置](../build.mill) 固定，Mill从公开Maven依赖解析；首次运行需要网络或自行准备的依赖缓存。使用Chisel匹配的firtool解析流程或安装上述固定版本。

`MILL` 可指定非默认启动命令，例如 `MILL='bash mill-launcher'`；`CC` 可指定C编译器；缓存由环境变量自行选择。仿真使用临时目录，必要时设置 `TMPDIR` 指向可写目录。仓库不包含工具二进制或机器专用启动器。

安装之后按 [快速复现](reproduction.md) 执行生成和验证。
