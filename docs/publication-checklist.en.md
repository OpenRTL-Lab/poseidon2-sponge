<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/publication-checklist.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Publication checks

[简体中文](publication-checklist.md)

This document lists functional, performance, product-closure, and content checks for the release package, enabling readers to rebuild and verify the circuit while keeping design/simulation reproducible and commercial synthesis materials limited to written summaries.

Here p = 2^64 - 2^32 + 1; 16/4096 and 32/8192 mean hardware contexts / maximum field elements per message. HASH8 means HASH of eight elements, not COMPRESS2. Ideal/backpressure/reset/corrupted-reference entries are verification scenarios, not extra product modes.

## Functionality and protocol

- Independent C/Python comparisons for every valid task, including a fixed permutation test vector.
- Empty HASH, lengths1/2/7/8/9/16/17/31/32/33/63/64/65/127/4096, and two-digest compression, with all-zero, p−1, and deterministic random inputs.
- Mixed vectors include invalid modes, invalid compression length, oversized input, wrong count, early last, and noncanonical elements; check statuses, draining, digest counts, and tag matching.
- Ideal execution, backpressure, in-flight reset, and corrupted-reference control; the corrupted reference must produce exactly one detected error.
- Reference controls distinguish trailing zeros, incorrect padding, and HASH8 versus COMPRESS2.
- Regenerated circuit code matches all 12 bundled modules; bundled/generated mixed-test statistics match.
- Coverage of the formal16/4096 and additional32/8192 configurations.

## Performance and structure

Isolated tasks and saturated batches are measured separately; repeated steady runs match. Multiplier issues and stage slots satisfy conservation relations. Full metrics are given in [Performance](performance.en.md). Standalone product lint passes; the closure excludes simulation tops, offline ROM, instrumentation bindings, FPGA primitives, and process macros.

## Public content

Documents have Chinese/English counterparts and link to their own language. Link labels are consistent for each target; mathematical notation avoids unsupported macros. The public allowlist contains sources, parameters, SV, tools, and documentation, excluding runtime caches, machine paths, credentials, signatures, internal provenance identifiers, original DC materials, and chat records. [Content audit script](../tools/public_audit.py) performs read-only file/link checks. Git and archive metadata are reviewed separately; public GitHub commit IDs are normal platform version metadata.
