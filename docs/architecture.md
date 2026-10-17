<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/architecture.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 电路架构

[English](architecture.en.md)

本加速器以多消息上下文交织共享 Poseidon2 流水线，配合寄存握手边界实现流式HASH和二摘要压缩；产品不需要FPGA原语或SRAM宏，验证统计仅存在于仿真层。

12元素状态由前8元素吸收区和后4元素保留容量区组成；这里的容量区不是消息长度上限。16/4096 表示一颗核共享16个在途任务状态槽、每消息最多4096个域元素。

## 数据流

任务信用准入 → 两槽任务队列 → 上下文分配；两槽数据队列 → 按tag吸收 → 置换调度 → 共享置换核 → 两槽返回队列 → 继续吸收或输出摘要 → 完成队列 → 信用归还。

每个上下文保存12×64位状态、长度、tag和控制，容量在HASH块之间保留。输入按8元素块处理，不缓存整条消息。16上下文是交织状态数，不是16个独立置换核。编译时支持16或32上下文、最大消息长度8..1048576；随包产品16/4096，公开复测也覆盖32/8192。

## 算术与流水线

II 为算术发射间隔；A/B 是同一外部或内部线性层的两个寄存分段，不是不同置换。共享模乘流水线11级、II=1；含操作数准备的反馈间隔15周期，由16上下文隐藏等待。非线性幂7分解为四次模乘。每次置换共472次有效模乘发射，其中完整轮384次、部分轮88次。外部线性层分A/B两段，深度8/6；内部层分A/B两段，深度8/8。一次置换执行9组外部层和22组内部层，共62个线性阶段。

这里“流水线II=1”指模乘发射能力，并不是整条消息每周期完成一次；实际任务吞吐率、延迟和有效发射率见 [性能指标](performance.md)。

任务、输入、摘要、完成和置换返回边界均使用两槽非直通队列，把模块边界的组合控制隔开。小型队列存储由通用寄存器描述，文件名 `ram_*` 不表示专有存储宏。产品闭包没有初始化ROM、外部参数读文件、硬件取模运算符或器件专用原语。

## 验证层与产品分离

`SpongeSimTop` 负责离线输入、C参考摘要、故障注入、握手断言及有效发射/阶段占用计数。正式产品清单仅包括12个SV模块，既不包含验证顶层、输入ROM，也不包含统计绑定文件。生成后通过依赖闭包提取和独立lint检查分离正确性。
