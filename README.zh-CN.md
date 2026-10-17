<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: README.zh-CN.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# poseidon2-sponge 硬件加速器

[English](README.md)

本仓库提供使用 Chisel 开发的独立硬件加速器，支持 Goldilocks 域元素消息的 Poseidon2 海绵哈希和两个四元素摘要的压缩。它可通过原生接口集成到 ASIC 或 FPGA，用于采用相同参数与填充规则的零知识证明系统中的 Merkle 叶子承诺、父节点计算、认证路径核验及其他域元素哈希计算。

采用 [MIT 许可证](LICENSE)，允许免费商用、修改和再分发，须保留版权及许可声明；第三方参数见[第三方声明](THIRD_PARTY_NOTICES.md)。

[Release v1.1.0](https://github.com/OpenRTL-Lab/poseidon2-sponge/releases/tag/v1.1.0) · [原生接口](docs/interface.md) · [快速复现](docs/reproduction.md) · [性能指标](docs/performance.md) · [综合摘要](docs/synthesis-summary.md)

## 计算与任务单位

一个任务是对一条域元素消息执行 HASH，或对两个四元素摘要执行 COMPRESS2，均返回一个四元素摘要。元素是规范无符号 64 位 Goldilocks 值，满足 0 <= x < p，p = 2^64 - 2^32 + 1，并非任意字节。12 元素状态具有八元素吸收区（rate）和四元素保留区（capacity）。rate 8 描述块宽，而非每拍持续吞吐八个元素。16 个上下文保存共用一颗置换核的独立在途消息状态，不是 16 颗哈希核。

HASH 从全零状态开始，每个完整块覆盖前八个状态元素、保留四个容量区元素；吸收不是相加或 XOR。不完整末块在 rate 内追加域元素 1，再补零。完整末块则把状态元素 8 加 1（模 p），不追加独立填充块。空 HASH 执行一次带填充置换。COMPRESS2 对左摘要、右摘要和四个零组成的状态只置换一次，不加 HASH 标记。两种模式都取前四个状态元素作为摘要，但它们是不同函数。

## 配置速览

| 集成问题 | 发布的硬件 |
| --- | --- |
| 计算 | 域元素消息 HASH 及两个四元素摘要的 COMPRESS2 |
| 固定哈希配置 | Goldilocks Poseidon2 状态 12、rate 8、摘要 4；规定的 HASH 填充及无填充 COMPRESS2 |
| 随包配置 | 16 个交织上下文，最大消息长度 4,096 个域元素 |
| 产品顶层 | `GoldilocksPoseidon2Sponge`；一颗共享置换核，而非 16 颗独立核 |
| 原生边界 | 带标签的任务、8 元素消息块、4 元素摘要及完成 ready/valid 通道 |

## 快速开始

从 [Release v1.1.0](https://github.com/OpenRTL-Lab/poseidon2-sponge/releases/tag/v1.1.0) 下载源码包，或克隆当前发布快照：

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/poseidon2-sponge.git
cd poseidon2-sponge
```

按[工具链版本](docs/toolchain.md)安装准确依赖，然后在仓库根目录使用全新输出目录运行：

```bash
python3 tools/emit_sv.py --out build/emit --contexts 16 --max-elements 4096 --tasks 64
```

该生成命令不是完整 RTL 回归。文档规定的验证入口是：

```bash
python3 tools/verify.py --out build/verify --jobs 8
```

预期结果、负控判读以及短功能验证与完整性能运行的区别见[快速复现](docs/reproduction.md)。

## 验证与集成边界

文档规定的默认流程核对独立 C/Python 参考、新生成及随包 16 上下文产品，并额外覆盖 32 上下文/8,192 元素配置。混合测试包含背压、复位恢复和故意破坏参考；专门的孤立/连续测试区分 HASH 与 COMPRESS2 性能。填充和尾零负控明确检查八元素 HASH 不会被静默当作 COMPRESS2。

消息是规范 Goldilocks 元素，不是任意字节。字节编码、协议域分离和树调度不属于该模块。COMPRESS2 恰好消费八元素且不应用 HASH 填充。空 HASH、摘要/完成背压及复位均有明确语义。公开综合配置为 16 上下文/4,096 元素，不代表所有生成容量或协议配置。

### 正确理解性能

理想原生接口下，连续完成间隔平均为每个 COMPRESS2 任务 538 周期、每个 4,096 元素 HASH 任务 274,434 周期。这是在上下文重叠条件下的吞吐间隔，不是单任务延迟：两者的孤立完成延迟分别为 8,607–8,608 和 4,390,943–4,390,944 周期。共享模乘有效发射数除以实测稳态窗口周期得到 87.7323% 与 88.0591% 的忙率，不同于统计流水线任一级被占用的周期。

这些是理想原生接口条件下记录的 RTL 周期测量。按 MHz 换算的时间/速率不是板级或硅后实测。Design Compiler 结果仅适用于文档规定的配置、库工艺角和约束，不代表布局布线签核或系统吞吐率。条件见[性能指标](docs/performance.md)与[综合摘要](docs/synthesis-summary.md)。

## 仓库内容

- Chisel 海绵与原生接口：[海绵设计](operators/poseidon2-sponge/src/main/scala/zk/sponge/)。
- 共享置换核与域算术：[置换设计](common/poseidon2/src/main/scala/zk/poseidon2/)。
- 生成好的独立产品：[SystemVerilog](rtl/v1.1.0/w12-r8-d4-ctx16-max4096/)，包含顶层及全部12个依赖模块。
- 电路生成入口：[生成脚本](tools/emit_sv.py)；完整复验入口：[验证脚本](tools/verify.py)。
- 独立参考：[C 模型](operators/poseidon2-sponge/cmodel/)、[Python 模型](operators/poseidon2-sponge/tools/vectors.py)与[仿真驱动](operators/poseidon2-sponge/sim/driver.cpp)。
- 集成和实现说明：[原生接口](docs/interface.md)、[电路架构](docs/architecture.md)、[工具链版本](docs/toolchain.md)。

## 应用场景

HASH 接收可变长度域元素消息，COMPRESS2 接收两个四元素摘要；两者可组合完成叶子哈希、二叉 Merkle 父节点压缩与路径重算，也可用于其他采用相同哈希定义的承诺与证明计算。用户系统负责字节到域元素的编码、协议域分离和树结构调度，具体通道用法见 [原生接口](docs/interface.md)。

## 性能指标

16 上下文配置每个压缩任务的平均完成间隔为 538 周期，每个 4096 元素 HASH 任务为 274434 周期，而非孤立延迟。按 500 MHz 换算分别约为 929368 和 1821.93 任务/s。单任务延迟与单元忙率的定义见[性能指标](docs/performance.md)。

## 数学模型

电路使用模数 $2^{64}-2^{32}+1$、12元素状态、8元素吸收块、4元素摘要及固定的 Poseidon2 轮参数；HASH 填充和 COMPRESS2 无填充压缩的完整定义见 [数学模型](docs/mathematical-model.md)。

## 快速复现

公开流程从独立 C/Python 参考校验开始，重新生成 SV，并对生成电路和随包电路进行功能、协议、复位及理想接口性能验证；安装与逐步命令见 [快速复现](docs/reproduction.md)。

## 综合结果

16上下文、最大4096元素配置通过 TSMC 28 nm 最差角、500 MHz及10%时钟不确定度的 DC setup 检查，99.7993%的统计路径逻辑级数严格小于50；面积与完整测试条件见 [综合摘要](docs/synthesis-summary.md)。

## 公开边界

本仓库提供可复现的 RTL 设计、参数、生成与仿真链路，以及综合条件和结果的文字摘要。很抱歉，由于工艺库与商业工具的授权及保密要求，无法提供工艺库、工具安装包、原始 DC 脚本、报告及综合快照。仓库交付独立算子及原生接口，系统协议封装由集成方实现。

## OpenRTL与定制集成

通过[OpenRTL组织主页](https://github.com/OpenRTL-Lab)、[ZK加速器集合](https://github.com/OpenRTL-Lab/zk-accelerators)及[OpenRTL网站](https://openrtl.top/)查找相关模块与合作联系入口。需要定制数据通路、接口或目标平台适配时可联系维护者；MIT商用权不附加购买服务的要求，不要在公开Issue提交保密规格。

## 发布源码与集成

版本标签标识包含来源声明和文档的当前源码树；Release 下载提供本版本硬件源码与随包 RTL。源码包与在线目录的注释及文档可能不同。RTL 集成使用产品文件清单，不把验证环境作为产品电路；支持配置、验证范围和测量条件见验证及综合文档。
