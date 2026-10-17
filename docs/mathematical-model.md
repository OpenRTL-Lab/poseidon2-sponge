<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/mathematical-model.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 数学模型

[English](mathematical-model.en.md)

本文件完整定义电路计算的 Goldilocks Poseidon2 置换、可变长度 HASH 及二摘要 COMPRESS2，明确状态布局、常量、轮次、覆盖吸收、尾块填充和摘要截取；所有运算均在同一个素域进行。

## 域和参数

令 $p=2^{64}-2^{32}+1$，$\mathbb{F}_p=\mathbb{Z}/p\mathbb{Z}$。接口采用64位规范无符号整数 $0\le x<p$，域运算结果再次规范化。状态 $x\in\mathbb{F}_p^{12}$，rate 为8，capacity 为4，摘要为4元素；非线性幂7，8个完整轮和22个部分轮。

rate 指每块覆盖的状态元素0..7，capacity 指保留的状态元素8..11；两者都不是吞吐率或消息长度上限。

具体常量以随包 [参数集](../operators/poseidon2-sponge/reference/parameters.json) 为完整定义，每个十六进制字符串按无符号整数解释。`GOLDILOCKS_POSEIDON2_RC_12_EXTERNAL_INITIAL` 和 `...EXTERNAL_FINAL` 各48项，按行组成4×12轮常量；`...RC_12_INTERNAL` 为22项；`MATRIX_DIAG_12_GOLDILOCKS` 为12项内部对角参数。它们对应 Plonky3 的 Goldilocks 12元素 Poseidon2 参数，外部层采用 MDSMat4。

## 置换 $P$

先定义四元素矩阵和12元素外部线性层：

$$
M=\begin{pmatrix}2&3&1&1\\1&2&3&1\\1&1&2&3\\3&1&1&2\end{pmatrix},\qquad
E(x)_i=\sum_{j=0}^{11}\bigl(1+\delta_{\lfloor i/4\rfloor,\lfloor j/4\rfloor}\bigr)M_{i\bmod4,j\bmod4}x_j.
$$

$\delta_{a,b}$ 在 $a=b$ 时为1，否则为0。内部层为 $I(x)_i=\sum_{j=0}^{11}x_j+d_i x_i$，其中 $d_i$ 是上述内部对角参数。

令 $x^{(0)}=E(x)$。对 $r=0,\ldots,29$：完整轮 $r\in\{0,1,2,3,26,27,28,29\}$ 先将对应12项轮常量逐项相加，再逐项求7次幂，最后应用 $E$；部分轮 $r=4,\ldots,25$ 仅对第0项加内部常量并求7次幂，其余项保持，然后应用 $I$。前四完整轮用初始常量，后四用最终常量，部分轮按索引 $r-4$ 用内部常量。$P(x)=x^{(30)}$，所有加乘均模 $p$。

## HASH：mode=0

消息 $m=(m_0,\ldots,m_{n-1})\in\mathbb{F}_p^n$，$0\le n\le N_{\max}$，随包配置 $N_{\max}=4096$。分成连续的至多8元素块；空消息视为一个空块。初始状态 $s=(0,\ldots,0)$。

每一非末完整块 $b$ 将 $s_0,\ldots,s_7$ **覆盖**为 $b_0,\ldots,b_7$，保留 $s_8,\ldots,s_{11}$，随后令 $s=P(s)$。这不是累加或异或吸收。

末块长度为 $q$ 时：

- $0\le q<8$：前 $q$ 项覆盖为消息，$s_q=1$，$s_{q+1},\ldots,s_7=0$，容量保留，再执行一次 $P$。
- $q=8$：覆盖全部rate，先令 $s_8=s_8+1$，再执行一次 $P$；不追加额外填充块。

输出 $H(m)=(s_0,s_1,s_2,s_3)$。因此空消息也执行一次置换；置换次数为 $\max(1,\lceil n/8\rceil)$。此定义采用 Pad10Sponge 的覆盖及尾块标记规则。

## COMPRESS2：mode=1

给定 $L,R\in\mathbb{F}_p^4$，按 $L_0,\ldots,L_3,R_0,\ldots,R_3$ 接收8元素，定义

$$
C(L,R)=\bigl(P(L\Vert R\Vert 0^4)\bigr)_{0:4}.
$$

$\Vert$ 为拼接，$0^4$ 为四个零，$0:4$ 取索引0到3。仅执行一次置换，不加 HASH 容量标记。因而 $C(L,R)$ 和 $H(L\Vert R)$ 是不同的函数，不能互换。

## 表示与协议

本定义以域元素为输入，不自动编码字节或引入协议域分离。更换常量、域、状态宽度、吸收或填充规则得到的是另一种函数。32位tag仅关联任务，原样返回，不参与哈希；握手、错误码与消息分帧见 [原生接口](interface.md)。
