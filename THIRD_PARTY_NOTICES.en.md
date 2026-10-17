<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: THIRD_PARTY_NOTICES.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Third-party notices

[简体中文](THIRD_PARTY_NOTICES.md)

This project uses Plonky3's Goldilocks 12-element Poseidon2 parameters and implements the corresponding overwrite sponge and two-digest compression semantics. This document identifies parameter sources, retained license texts, and compatibility checks. Original project code is MIT-licensed.

Public project: [Plonky3](https://github.com/Plonky3/Plonky3). Parameters and license metadata are included in [Parameter set](operators/poseidon2-sponge/reference/parameters.json); the original upstream MIT text is retained in [Parameter license](operators/poseidon2-sponge/reference/LICENSE-MIT). Upstream metadata offers MIT or Apache-2.0; this project retains and uses the MIT option for these parameters. Original project code is covered by [MIT License](LICENSE).

Reference implementations are available in [Upstream sponge implementation](https://github.com/Plonky3/Plonky3/blob/main/symmetric/src/sponge.rs) and [Upstream compression implementation](https://github.com/Plonky3/Plonky3/blob/main/symmetric/src/compression.rs). These links provide upstream context; the delivered functions are fixed by bundled constants and [Mathematical model](docs/mathematical-model.en.md), not by changes to upstream main.

The repository retains original copyright and license notices required for its parameters. Independent C/Python models cross-check every valid task and include a known permutation test vector. Matching algorithm names do not by themselves establish matching parameters or padding: check the field, constants, linear layers, rounds, and message semantics.
