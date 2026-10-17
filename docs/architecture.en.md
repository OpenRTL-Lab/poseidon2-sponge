<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/architecture.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Circuit architecture

[简体中文](architecture.md)

The accelerator interleaves multiple message contexts through a shared Poseidon2 pipeline and registered handshake boundaries to implement streaming HASH and two-digest compression. The product requires neither FPGA primitives nor SRAM macros; instrumentation resides only in the simulation layer.

The 12-element state consists of an eight-element absorption rate and four retained capacity elements. Capacity here names part of the hash state, not maximum message length. Configuration 16/4096 means 16 in-flight task-state slots sharing one core and a 4096-field-element message limit.

## Data flow

Credit admission → two-entry task FIFO → context allocation; two-entry input FIFO → tagged absorption → permutation scheduling → shared permutation core → two-entry return FIFO → further absorption or digest output → completion FIFO → credit return.

Each context holds a 12×64-bit state, length, tag, and control. HASH retains capacity between blocks. Input is processed in eight-element blocks without buffering a whole message. The16 contexts are interleaved states, not16 independent cores. Generation supports16/32 contexts and maximum lengths8..1048576. The bundled product uses 16/4096; public tests also cover32/8192.

## Arithmetic and pipelines

II is the initiation interval between arithmetic issues. A/B name two registered segments of each external or internal linear layer, not distinct permutations. The shared modular multiplier has 11 stages and II=1. Including operand preparation, feedback takes 15 cycles, hidden by 16 contexts. Exponent7 is decomposed into four modular multiplications. A permutation issues472 useful multiplications:384 in full rounds and 88 in partial rounds. External linear layers use A/B stages of depths 8/6; internal layers use A/B depths 8/8. Each permutation executes9 external and 22 internal layers, totaling 62 linear phases.

Multiplier II=1 describes issue capacity, not one completed message per cycle. Actual task throughput, latency, and useful-issue utilization are specified in [Performance](performance.en.md).

Task, input, digest, completion, and permutation-return boundaries use two-entry non-flow-through FIFOs to separate combinational control between modules. Small queue storage is described with generic registers; `ram_*` names do not imply proprietary memory macros. The product closure contains no initialized ROM, runtime parameter-file reads, hardware modulo operator, or device-specific primitives.

## Verification/product separation

`SpongeSimTop` supplies offline vectors, C-reference digests, fault injection, handshake assertions, and issue/stage-occupancy counters. The product list contains only 12 SV modules, excluding the verification top, input ROM, and instrumentation bindings. Dependency-closure extraction and standalone lint check this separation after generation.
