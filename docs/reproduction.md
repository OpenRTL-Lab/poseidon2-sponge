<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/reproduction.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 快速复现

[English](reproduction.en.md)

本流程从公开源码重建海绵加速器，交叉核对C/Python参考，验证生成SV与随包SV，并测量单任务延迟、连续任务吞吐率和计算单元忙率；命令均从仓库根目录执行，结果输出到新建build子目录。

## 获取版本快照

[Release v1.1.0](https://github.com/OpenRTL-Lab/poseidon2-sponge/releases/tag/v1.1.0)

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/poseidon2-sponge.git
cd poseidon2-sponge
```

版本标签标识包含来源声明和文档的当前源码树；Release 下载提供本版本硬件源码与随包 RTL。源码包与在线目录的注释及文档可能不同。RTL 集成使用产品文件清单，不把验证环境作为产品电路；支持配置、验证范围和测量条件见验证及综合文档。

## 安装与检查

先按 [工具链版本](toolchain.md) 安装工具。下面检查脚本运行和编译所需命令是否就绪：

```bash
java -version
mill --version
python3 --version
verilator --version
cc --version
g++ --version
```

下文 A/B 配置表示 A 个硬件任务状态上下文、每消息最多 B 个域元素；上下文共用一颗置换核。`--jobs` 控制编译并行度（1..64），不是硬件上下文数或任务并发数。

## 完整功能和性能复验

下面命令编译独立C参考、生成离线向量、用Python大整数模型逐项交叉核对、展开Chisel、独立lint产品并构建RTL仿真。默认包含16/4096混合任务、随包SV复验、32/8192配置覆盖、孤立与连续HASH/COMPRESS2性能；连续性能运行两次并要求统计完全一致。

```bash
python3 tools/verify.py --out build/verify --jobs 8
```

成功结束打印“全部验证通过”，`build/verify/results.json` 中 `status=0`。该文件汇总各案例测试与性能，各子目录保留独立日志和结果。`mixed` 与 `bundled` 的测试统计必须一致；每次生成的16/4096产品与随包产品逐模块比较，忽略注释与空白而不忽略电路代码。

## 仅功能验证

下面命令省略专门的性能矩阵，保留生成/随包混合任务和32上下文覆盖；不是完整性能复验的替代品：

```bash
python3 tools/verify.py --out build/functional --jobs 8 --functional-only
```

混合任务运行理想供数、协议背压、在途复位与故意破坏参考四种模式。负控二进制预期退出1且恰好发现一个错误；验证脚本确认这个预期结果后仍成功退出。参考层另外检测错误填充、尾零区分及HASH8/COMPRESS2差异。

## 仅生成产品

`--contexts` 选择16或32个硬件状态槽，`--max-elements` 设置生成的消息容量（8..1048576）。`--tasks` 设置验证环境工作负载数（至少2、为2的幂），不会增加硬件上下文。

下面只生成配置为16上下文、最大4096元素的产品SV及独立验证环境，不运行RTL仿真：

```bash
python3 tools/emit_sv.py --out build/emit --contexts 16 --max-elements 4096 --tasks 64
```

`build/emit/rtl/product.f` 是从仓库根目录使用的独立产品清单；`simulation.f` 另含验证顶层、离线ROM和统计绑定，不能当作产品清单。只lint产品可用：

```bash
verilator --lint-only --assert --top-module GoldilocksPoseidon2Sponge -f build/emit/rtl/product.f
```

## 随包产品与可配置项

无需生成即可使用 [SystemVerilog](../rtl/v1.1.0/w12-r8-d4-ctx16-max4096/) 中完整12模块及 `files.f`。生成支持16/32上下文、最大长度8..1048576。32/8192是公开测试覆盖配置；综合摘要对应随包16/4096配置。

所有输出必须是build下未存在的相对目录；重新运行应选择新目录，脚本不会覆盖或自动删除既有结果。`--jobs` 控制编译并行度1..64。功能运行与负控覆盖见 [发布检查](publication-checklist.md)，性能口径见 [性能指标](performance.md)。
