<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/reproduction.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Reproduction

[简体中文](reproduction.md)

This flow rebuilds the sponge accelerator from public sources, cross-checks C/Python references, verifies regenerated and bundled SV, and measures single-task latency, sustained throughput, and compute-unit utilization. Run commands at the repository root; outputs go into new build subdirectories.

## Obtain the release snapshot

[Release v1.1.0](https://github.com/OpenRTL-Lab/poseidon2-sponge/releases/tag/v1.1.0)

```bash
git clone --branch v1.1.0 --depth 1 https://github.com/OpenRTL-Lab/poseidon2-sponge.git
cd poseidon2-sponge
```

The version tag identifies the current source tree, including source notices and documentation. Release downloads provide hardware sources and bundled RTL for this version. Archive annotations and documentation can differ from the online tree. For RTL integration, use the product file list rather than verification environments; use the verification and synthesis documents for supported configurations and measurement conditions.

## Install and check

Install tools listed in [Toolchain](toolchain.en.md). The following commands check the programs required by scripts and compilation:

```bash
java -version
mill --version
python3 --version
verilator --version
cc --version
g++ --version
```

Configuration A/B below means A hardware task-state contexts and a B-field-element maximum message length. Contexts share one permutation core. `--jobs` controls compilation parallelism (1..64), not hardware contexts or task concurrency.

## Full functional and performance verification

The following command compiles the independent C reference, generates offline vectors, cross-checks each result using Python big integers, elaborates Chisel, independently lints the product, and builds RTL simulation. Defaults cover16/4096 mixed tasks, bundled-SV verification,32/8192 coverage, and isolated/sustained HASH and COMPRESS2 benchmarks. Sustained runs are repeated twice and must produce identical statistics.

```bash
python3 tools/verify.py --out build/verify --jobs 8
```

Successful execution prints that all checks passed; `build/verify/results.json` has `status=0`. It aggregates tests and performance by case; subdirectories retain logs and individual results. `mixed` and `bundled` test statistics must match. Every generated16/4096 product is compared module-by-module with the bundled product, ignoring comments and whitespace but not circuit code.

## Functional-only verification

The following command omits the dedicated performance matrix while retaining mixed-task tests for generated/bundled products and 32-context coverage. It is not a replacement for full performance verification:

```bash
python3 tools/verify.py --out build/functional --jobs 8 --functional-only
```

Mixed tasks exercise ideal supply, protocol backpressure, in-flight reset, and deliberately corrupted-reference modes. The negative-control binary must exit1 and detect exactly one error; the verification script treats this checked behavior as success. Reference checks also detect incorrect padding, distinguish trailing zeros, and distinguish HASH8 from COMPRESS2.

## Generate only

`--contexts` selects 16 or 32 hardware state slots; `--max-elements` sets generated message capacity (8..1048576). `--tasks` sets the verification environment workload (a power of two, at least 2), not additional hardware contexts.

The following command generates a 16-context product with a 4096-element maximum and its independent verification environment, without RTL simulation:

```bash
python3 tools/emit_sv.py --out build/emit --contexts 16 --max-elements 4096 --tasks 64
```

`build/emit/rtl/product.f` lists the standalone product, with paths relative to the repository root. `simulation.f` also includes the verification top, offline ROM, and instrumentation bindings; it is not a product list. To lint only the product:

```bash
verilator --lint-only --assert --top-module GoldilocksPoseidon2Sponge -f build/emit/rtl/product.f
```

## Bundled product and options

The12 modules and `files.f` in [SystemVerilog](../rtl/v1.1.0/w12-r8-d4-ctx16-max4096/) can be used without regeneration. Generation supports16/32 contexts and maximum lengths8..1048576. Public tests cover32/8192 as an additional configuration; the synthesis summary corresponds to the bundled16/4096 product.

Outputs must be new relative directories under build. Use a different directory for each run; scripts neither overwrite nor automatically delete existing results. `--jobs` selects compilation parallelism1..64. Functional coverage is documented in [Publication checks](publication-checklist.en.md); metric definitions are in [Performance](performance.en.md).
