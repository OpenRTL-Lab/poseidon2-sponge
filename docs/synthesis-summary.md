<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/synthesis-summary.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 综合摘要

[English](synthesis-summary.en.md)

本文件转述随包16上下文、最大4096元素产品的 DC 综合结果：TSMC 28 nm 最差角、500 MHz、10%时钟不确定度下setup通过，统计路径99.7993%严格小于50级，总cell面积约0.182615 mm²，无SRAM宏。

## 工具与约束

| 项目 | 设置 |
| --- | --- |
| 顶层 | GoldilocksPoseidon2Sponge |
| 工具 | Synopsys Design Compiler NXT X-2025.06-SP3 |
| 工艺 | TSMC 28 nm HPC+标准单元 |
| 工艺角 | SSG，0.81 V，−40 °C |
| 时钟 | 500 MHz，周期2 ns，理想时钟 |
| setup时钟不确定度 | 0.2 ns，周期10% |
| 输入延迟 | max1 ns，min0 ns |
| 输出延迟 | max1 ns，min0 ns |
| 输入转换时间 | 0.125 ns |
| 输出负载 | 0.02 pF |
| 复位 | 同步高有效 |

这些指标是独立IP的逻辑综合结果；面积是映射cell面积，频率检查采用上述setup约束。综合条件与数据范围以本文列出的配置为准。

WNS 为最差 setup 裕量，TNS 为负 setup 裕量之和。

## Setup与逻辑级数

| 指标 | 结果 |
| --- | ---: |
| Setup | 通过 |
| WNS | +0.000002384 ns |
| TNS | 0 ns |
| 违例端点 | 0 |
| 已约束端点/统计路径 | 59282 |
| 严格小于50级 | 59163（99.7992645%） |
| 恰为50级 | 37 |
| 大于50级 | 82 |
| 大于60级 | 18 |
| 最大逻辑级数 | 67（2条） |

每个已约束setup端点取一条最差max数据路径。仅叶名称严格等于 `Z` 或 `ZN` 的数据引脚各计一级，不按报告行数、net数量或其他引脚计数；起点clock-to-Q与时钟树不计。完整路径点序列已与报告交叉核对。内部到内部57498条，内部到IO359条，IO到内部1425条，分母未剔除IO。阈值“大于50”和“大于60”是累计统计，不能相加。

## 面积

| 互不重叠分类 | 面积µm² | 占比 |
| --- | ---: | ---: |
| 组合逻辑，不含缓冲/反相器 | 78907.836 | 43.2100% |
| 缓冲/反相器 | 5539.744 | 3.0336% |
| 非组合逻辑 | 98167.287 | 53.7565% |
| SRAM宏/Black Box | 0 | 0% |
| 总cell面积 | 182614.867 | 100% |

总cell数量241017。共享置换核含子层面积128440.663 µm²，占70.3342%；其模乘子层8912.316 µm²，占顶层4.8804%。父子层面积有包含关系，不重复相加。面积不包含布线、留白或芯片外围。

## 公开范围

设计、参数、生成和功能/性能验证链路完整公开，见 [快速复现](reproduction.md)。很抱歉，由于商业工具及工艺库授权与保密要求，无法提供原始DC脚本、报告、库文件和综合快照；本页仅提供综合基本信息与结果转述。
