<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/publication-checklist.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 发布检查

[English](publication-checklist.en.md)

本文件列出发布包的功能、性能、产品闭包和公开内容检查，确保读者可以重建并验证交付电路，同时保持设计与仿真公开、商业综合材料仅文字转述的边界。

这里 p = 2^64 - 2^32 + 1；16/4096、32/8192 表示硬件上下文数／每消息最大域元素数。HASH8 是八元素 HASH，不是 COMPRESS2。理想／背压／复位／破坏参考是验证场景，不是额外产品模式。

## 功能和协议

- 独立C与Python模型逐项对比，固定置换向量校验通过。
- 覆盖空HASH、长度1/2/7/8/9/16/17/31/32/33/63/64/65/127/4096与二摘要压缩，以及全零、p−1和确定随机输入。
- 混合向量包含非法mode、非法压缩长度、超长、错误count、提前last、非规范元素；检查错误状态、排空、摘要数与tag配对。
- 理想运行、背压、在途复位与参考破坏负控；参考破坏必须检测恰好一个错误。
- 参考负控区分消息尾零、错误填充以及HASH8和COMPRESS2。
- 重新生成产品与随包12模块电路代码一致；随包和生成电路混合测试统计一致。
- 正式16/4096与额外32/8192配置覆盖。

## 性能和结构

孤立任务与饱和连续任务分别测量，两次稳态运行统计一致；模乘发射数和阶段槽守恒。完整指标见 [性能指标](performance.md)。独立产品lint通过，闭包不含仿真顶层、离线ROM、统计绑定、FPGA原语或工艺宏。

## 公开内容

文档中英配对，各自索引对应语言；相同目标链接名称统一，数学公式不用不支持的宏。公开文件白名单包含源码、参数、SV、工具及文档，不含运行缓存、机路径、凭证、签名、内部追溯标识、原始DC材料或聊天记录。[内容检查脚本](../tools/public_audit.py) 只读检查文件和链接。Git元数据及归档元数据另行审核，公开的GitHub提交ID是平台正常版本信息。
