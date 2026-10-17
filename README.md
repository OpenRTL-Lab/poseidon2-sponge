<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: README.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Goldilocks Poseidon2 Sponge Hash Hardware Accelerator

[简体中文](README.zh-CN.md)

This repository provides a standalone hardware accelerator developed in Chisel for Poseidon2 sponge hashing of Goldilocks field-element messages and compression of two four-element digests. Through its native interface, it can be integrated into ASIC or FPGA designs for Merkle leaf commitments, parent-node computation, authentication-path verification, and other field-element hashing operations in zero-knowledge systems using the same parameters and padding rules.

It is licensed under the [MIT License](LICENSE), permitting free commercial use, modification, and redistribution provided the copyright and license notices are retained; see [Third-party notices](THIRD_PARTY_NOTICES.en.md) for the third-party parameters.

[Release v1.1.0](https://github.com/OpenRTL-Lab/poseidon2-sponge/releases/tag/v1.1.0) · [Native interface](docs/interface.en.md) · [Reproduction](docs/reproduction.en.md) · [Performance](docs/performance.en.md) · [Synthesis summary](docs/synthesis-summary.en.md)

## Computation and task units

One task is either HASH of one field-element message or COMPRESS2 of two four-element digests, returning one four-element digest. Elements are canonical unsigned 64-bit Goldilocks values, 0 <= x < p for p = 2^64 - 2^32 + 1, not arbitrary bytes. The 12-element state has an eight-element absorption part (rate) and a four-element retained part (capacity). Rate 8 describes the block width, not eight elements of sustained throughput per clock. The 16 contexts store independent in-flight message states sharing one permutation core, not 16 hash cores.

HASH starts from zero state and overwrites the first eight state elements with each full block, preserving the four capacity elements; absorption is not addition or XOR. A short final block appends a field-element 1 then zeros within the rate. A full final block instead increments state element 8 modulo p; it does not add a separate padding block. Empty HASH uses one padded permutation. COMPRESS2 permutes left digest, right digest and four zeros exactly once, without that HASH marker. Both modes take the first four state elements as the digest, but are different functions.

## At a glance

| Integration question | Released hardware |
| --- | --- |
| Computation | HASH of field-element messages and COMPRESS2 of two four-element digests |
| Fixed hash profile | Goldilocks Poseidon2 state 12, rate 8, digest 4; documented HASH padding and unpadded COMPRESS2 |
| Bundled configuration | 16 interleaved contexts, maximum message length 4,096 field elements |
| Product top | `GoldilocksPoseidon2Sponge`; one shared permutation core, not 16 independent cores |
| Native boundary | Tagged task, 8-element message blocks, 4-element digest and completion ready/valid channels |

## Quick start

Download the source archive from [Release v1.1.0](https://github.com/OpenRTL-Lab/poseidon2-sponge/releases/tag/v1.1.0), or clone the current release snapshot:

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/poseidon2-sponge.git
cd poseidon2-sponge
```

Install the exact prerequisites in [Toolchain](docs/toolchain.en.md), then run from the repository root with a fresh output directory:

```bash
python3 tools/emit_sv.py --out build/emit --contexts 16 --max-elements 4096 --tasks 64
```

This emission command is not a complete RTL regression. The documented verification entry is:

```bash
python3 tools/verify.py --out build/verify --jobs 8
```

Follow [Reproduction](docs/reproduction.en.md) for expected results, negative-control interpretation and the distinction between short functional and full performance runs.

## Verification and integration boundary

The documented default flow checks independent C/Python references, generated/bundled 16-context products and additional 32-context/8,192-element coverage. Mixed tests include backpressure, reset recovery and deliberate-reference faults; dedicated isolated/sustained tests distinguish HASH and COMPRESS2 performance. Padding and trailing-zero controls explicitly check that HASH of eight elements is not silently treated as COMPRESS2.

Messages are canonical Goldilocks elements, not arbitrary bytes. Byte encoding, protocol domain separation and tree scheduling remain outside this block. COMPRESS2 consumes exactly eight elements and does not apply HASH padding. Empty HASH, blocked digest/completion and reset have defined semantics. The published synthesis configuration is 16 contexts / 4,096 elements, not every generator capacity or protocol profile.

### Reading performance correctly

Under ideal native interfaces, sustained completion intervals average 538 cycles per COMPRESS2 task and 274,434 cycles per 4,096-element HASH task. These are throughput intervals with overlapping contexts, not single-task latencies: isolated completion latency is 8,607–8,608 and 4,390,943–4,390,944 cycles, respectively. Shared-multiplier useful issues divided by measured steady-window cycles give 87.7323% and 88.0591% utilization; this differs from counting cycles with any occupied pipeline stage.

These are documented RTL cycle measurements under ideal native interfaces. MHz-based time/rate conversions are not board or silicon measurements. The reported Design Compiler results apply only to the documented configuration, library corner and constraints; they do not establish place-and-route signoff or system throughput. See [Performance](docs/performance.en.md) and [Synthesis summary](docs/synthesis-summary.en.md) for conditions.

## Repository contents

- Chisel sponge and native interface: [Sponge design](operators/poseidon2-sponge/src/main/scala/zk/sponge/).
- Shared permutation core and field arithmetic: [Permutation design](common/poseidon2/src/main/scala/zk/poseidon2/).
- Generated standalone product: [SystemVerilog](rtl/v1.1.0/w12-r8-d4-ctx16-max4096/), including the top and all 12 dependency modules.
- Circuit generation: [Generation script](tools/emit_sv.py); full verification: [Verification script](tools/verify.py).
- Independent references: [C model](operators/poseidon2-sponge/cmodel/), [Python model](operators/poseidon2-sponge/tools/vectors.py), and [Simulation driver](operators/poseidon2-sponge/sim/driver.cpp).
- Integration and implementation: [Native interface](docs/interface.en.md), [Circuit architecture](docs/architecture.en.md), and [Toolchain](docs/toolchain.en.md).

## Applications

HASH accepts variable-length field-element messages; COMPRESS2 accepts two four-element digests. Together they support leaf hashing, binary Merkle parent compression, and path recomputation, as well as other commitments and proof computations using the same hash definition. The integrating system supplies byte-to-field encoding, protocol domain separation, and tree scheduling. Channel usage is specified in [Native interface](docs/interface.en.md).

## Performance

The 16-context configuration sustains a mean completion interval of 538 cycles per compression task and 274434 cycles per 4096-element hash task, not isolated latency. At 500 MHz these correspond to approximately 929368 and 1821.93 tasks per second. Single-task latency and unit utilization are defined in [Performance](docs/performance.en.md).

## Mathematical model

The circuit uses modulus $2^{64}-2^{32}+1$, a 12-element state, an 8-element absorption block, a 4-element digest, and fixed Poseidon2 round parameters. The precise HASH padding and unpadded COMPRESS2 definitions are given in [Mathematical model](docs/mathematical-model.en.md).

## Reproduction

The public flow cross-checks independent C/Python references, regenerates SV, and verifies both generated and bundled circuits for functionality, protocol behavior, reset recovery, and ideal-interface performance. Installation and step-by-step commands are provided in [Reproduction](docs/reproduction.en.md).

## Synthesis results

The 16-context, 4096-element maximum configuration passes DC setup checks at the TSMC 28 nm worst corner, 500 MHz, and 10% clock uncertainty. Of the measured paths, 99.7993% have strictly fewer than 50 logic levels. Area and complete test conditions are summarized in [Synthesis summary](docs/synthesis-summary.en.md).

## Public scope

This repository provides reproducible RTL, parameters, generation and simulation flows, and written summaries of synthesis conditions and results. We are sorry that process-library and commercial-tool licensing and confidentiality requirements prevent us from providing process libraries, tool installers, original DC scripts, reports, and synthesis snapshots. The deliverable is a standalone operator with native interfaces; system-specific protocol wrappers are supplied by the integrator.

## OpenRTL and custom integration

Browse [OpenRTL](https://github.com/OpenRTL-Lab), the [ZK accelerator catalogue](https://github.com/OpenRTL-Lab/zk-accelerators) and [OpenRTL website](https://openrtl.top/) for related modules and the cooperation/contact entry. Custom datapaths, interfaces and target-platform adaptation are optional services; MIT commercial use does not require buying a service. Do not submit confidential specifications in public issues.

## Release sources and integration

The version tag identifies the current source tree, including source notices and documentation. Release downloads provide hardware sources and bundled RTL for this version. Archive annotations and documentation can differ from the online tree. For RTL integration, use the product file list rather than verification environments; use the verification and synthesis documents for supported configurations and measurement conditions.
