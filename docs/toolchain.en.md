<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/toolchain.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Toolchain

[简体中文](toolchain.md)

This document lists versions, dependencies, and environment settings for public circuit generation and simulation. Design and verification tools can be installed independently; the project is not tied to a particular machine, proxy, or cache directory.

| Tool | Version | Purpose |
| --- | --- | --- |
| Mill | 1.1.7 | Scala build and generation entry |
| Scala | 2.13.18 | Chisel sources |
| Chisel and compiler plugin | 7.13.0 | Circuit elaboration |
| CIRCT firtool | 1.149.0 | SV lowering and splitting |
| OpenJDK | 21.0.11 | JVM |
| Python | 3.12 | Scripts and independent big-integer reference |
| Verilator | 5.050 | Lint and cycle simulation |
| GCC/G++ | 16.2.0 | C reference and simulation build |

Install these tools so `java`, `mill`, `python3`, `verilator`, `cc`, and `g++` are available on PATH. Scala, Chisel, and plugin versions are pinned in [Build configuration](../build.mill); Mill resolves public Maven dependencies. The first run requires network access or a user-prepared dependency cache. Use Chisel's matching firtool resolution flow or install the pinned version above.

`MILL` may specify a custom launcher, such as `MILL='bash mill-launcher'`; `CC` selects a C compiler. Environment settings select cache locations. Simulation uses temporary directories; set `TMPDIR` to a writable directory if needed. Tool binaries and machine-specific launchers are not bundled.

After installation, follow [Reproduction](reproduction.en.md) to generate and verify the circuit.
