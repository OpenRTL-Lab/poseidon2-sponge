<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/mathematical-model.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Mathematical model

[简体中文](mathematical-model.md)

This document completely defines the circuit's Goldilocks Poseidon2 permutation, variable-length HASH, and two-digest COMPRESS2, including state layout, constants, rounds, overwrite absorption, final-block padding, and digest extraction. All arithmetic takes place in the same prime field.

## Field and parameters

Let $p=2^{64}-2^{32}+1$ and $\mathbb{F}_p=\mathbb{Z}/p\mathbb{Z}$. Interfaces use canonical unsigned 64-bit integers $0\le x<p$; field-operation outputs are canonicalized. The state belongs to $\mathbb{F}_p^{12}$, with rate 8, capacity 4, and a four-element digest. The nonlinear exponent is 7; there are 8 full and 22 partial rounds.

Rate denotes state elements 0..7 overwritten by an input block; capacity denotes retained elements 8..11. Neither term specifies a throughput or message-length bound.

The bundled [Parameter set](../operators/poseidon2-sponge/reference/parameters.json) defines every constant exactly; interpret each hexadecimal string as an unsigned integer. `GOLDILOCKS_POSEIDON2_RC_12_EXTERNAL_INITIAL` and `...EXTERNAL_FINAL` contain48 entries each, arranged row-wise into 4×12 constants. `...RC_12_INTERNAL` contains 22 entries; `MATRIX_DIAG_12_GOLDILOCKS` contains 12 internal diagonal parameters. These are the Plonky3 Goldilocks 12-element Poseidon2 parameters, with MDSMat4 for the external layer.

## Permutation $P$

Define the four-element matrix and 12-element external layer:

$$
M=\begin{pmatrix}2&3&1&1\\1&2&3&1\\1&1&2&3\\3&1&1&2\end{pmatrix},\qquad
E(x)_i=\sum_{j=0}^{11}\bigl(1+\delta_{\lfloor i/4\rfloor,\lfloor j/4\rfloor}\bigr)M_{i\bmod4,j\bmod4}x_j.
$$

Here $\delta_{a,b}$ is 1 when $a=b$ and 0 otherwise. The internal layer is $I(x)_i=\sum_{j=0}^{11}x_j+d_i x_i$, where $d_i$ is the corresponding internal diagonal parameter.

Start with $x^{(0)}=E(x)$. For $r=0,\ldots,29$, full rounds $r\in\{0,1,2,3,26,27,28,29\}$ add their12 round constants, raise every element to the seventh power, and apply $E$. Partial rounds $r=4,\ldots,25$ add the internal constant only to element0, raise only that element to the seventh power, leave other elements unchanged, and apply $I$. The first four full rounds use the initial constants; the final four use the final constants; partial rounds use internal entry $r-4$. Define $P(x)=x^{(30)}$. All additions and multiplications are modulo $p$.

## HASH: mode=0

For $m=(m_0,\ldots,m_{n-1})\in\mathbb{F}_p^n$, require $0\le n\le N_{\max}$. The bundled configuration has $N_{\max}=4096$. Partition the message into consecutive blocks of at most8 elements; treat an empty message as one empty block. Initially $s=0^{12}$.

Each non-final full block $b$ **overwrites** $s_0,\ldots,s_7$ with $b_0,\ldots,b_7$, retains $s_8,\ldots,s_{11}$, and then sets $s=P(s)$. Absorption is neither addition nor XOR.

For a final block of length $q$:

- If $0\le q<8$, overwrite the first $q$ elements, set $s_q=1$, set the remaining rate elements to 0, preserve capacity, and apply $P$ once.
- If $q=8$, overwrite the rate, set $s_8=s_8+1$, and apply $P$ once. Do not append another padding block.

Return $H(m)=(s_0,s_1,s_2,s_3)$. An empty message therefore uses one permutation. The permutation count is $\max(1,\lceil n/8\rceil)$. This is the overwrite and final-block marker convention of Pad10Sponge.

## COMPRESS2: mode=1

For $L,R\in\mathbb{F}_p^4$, receive8 elements in the order $L_0,\ldots,L_3,R_0,\ldots,R_3$ and define

$$
C(L,R)=\bigl(P(L\Vert R\Vert 0^4)\bigr)_{0:4}.
$$

$\Vert$ denotes concatenation, $0^4$ denotes four zeros, and $0:4$ selects indices 0 through3. Execute exactly one permutation without the HASH capacity marker. Thus $C(L,R)$ and $H(L\Vert R)$ are different functions and are not interchangeable.

## Representation and protocol

Inputs are field elements; byte encoding and protocol domain separation are supplied by the integrating system. Changing constants, field, width, absorption, or padding defines a different function. The32-bit tag associates tasks and is returned unchanged; it does not enter the hash. Handshakes, errors, and framing are specified in [Native interface](interface.en.md).
