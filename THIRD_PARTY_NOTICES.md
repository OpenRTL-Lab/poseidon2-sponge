<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: THIRD_PARTY_NOTICES.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 第三方声明

[English](THIRD_PARTY_NOTICES.en.md)

本项目使用 Plonky3 的 Goldilocks 12元素 Poseidon2 参数并实现对应覆盖海绵和二摘要压缩语义；本文件列出参数来源、保留的许可文本及兼容性核对方式，项目自有代码采用MIT许可。

公开项目：[Plonky3](https://github.com/Plonky3/Plonky3)。参数与许可元数据保存在 [参数集](operators/poseidon2-sponge/reference/parameters.json)，上游MIT许可原文保存在 [参数许可](operators/poseidon2-sponge/reference/LICENSE-MIT)。参数元数据的上游许可选择为MIT或Apache-2.0，本项目保留并按MIT选项使用相关参数。自有代码许可见 [MIT 许可证](LICENSE)。

海绵与压缩的参考实现分别见 [上游海绵实现](https://github.com/Plonky3/Plonky3/blob/main/symmetric/src/sponge.rs) 和 [上游压缩实现](https://github.com/Plonky3/Plonky3/blob/main/symmetric/src/compression.rs)。链接便于了解上游；本仓库交付函数由随包完整常量及 [数学模型](docs/mathematical-model.md) 固定，不随上游main变化。

本仓库保留参数所需原始版权和许可声明；独立C/Python模型交叉验证全部有效任务，另含已知置换测试向量。相同算法名称不自动意味着相同参数与填充，应核对域、常量、线性层、轮次数和消息语义。
