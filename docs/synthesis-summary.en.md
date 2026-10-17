<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/synthesis-summary.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Synthesis summary

[简体中文](synthesis-summary.md)

This document summarizes DC results for the bundled16-context product with a 4096-element maximum: setup passes at the TSMC 28 nm worst corner,500 MHz, and 10% clock uncertainty;99.7993% of measured paths have strictly fewer than 50 logic levels. Total cell area is approximately 0.182615 mm², with no SRAM macros.

## Tools and constraints

| Item | Setting |
| --- | --- |
| Top | GoldilocksPoseidon2Sponge |
| Tool | Synopsys Design Compiler NXT X-2025.06-SP3 |
| Process | TSMC 28 nm HPC+ standard cells |
| Corner | SSG,0.81 V,−40 °C |
| Clock | 500 MHz,2 ns period, ideal clock |
| Setup clock uncertainty | 0.2 ns,10% of period |
| Input delay | max 1 ns, min 0 ns |
| Output delay | max 1 ns, min 0 ns |
| Input transition | 0.125 ns |
| Output load | 0.02 pF |
| Reset | Synchronous active-high |

These are standalone-IP logic-synthesis measurements. Area denotes mapped cell area; frequency checks use the listed setup constraints. The synthesis conditions and data scope are defined by the configuration listed here.

WNS is the worst setup slack; TNS is the sum of negative setup slacks.

## Setup and logic depth

| Metric | Result |
| --- | ---: |
| Setup | Pass |
| WNS | +0.000002384 ns |
| TNS | 0 ns |
| Violating endpoints | 0 |
| Constrained endpoints/measured paths | 59282 |
| Strictly fewer than 50 levels | 59163 (99.7992645%) |
| Exactly 50 levels | 37 |
| More than 50 levels | 82 |
| More than 60 levels | 18 |
| Maximum depth | 67 (2 paths) |

Take one worst max data path per constrained setup endpoint. Count only data pins whose leaf name is exactly `Z` or `ZN`, one level per pin; report lines, nets, and other pins are not levels. Exclude launch clock-to-Q and the clock tree. Full timing-point sequences were cross-checked against reports. The denominator includes57498 internal-to-internal,359 internal-to-IO, and 1425 IO-to-internal paths. Greater-than 50 and greater-than 60 thresholds are cumulative and must not be added.

## Area

| Disjoint category | Area µm² | Share |
| --- | ---: | ---: |
| Combinational excluding buffers/inverters | 78907.836 | 43.2100% |
| Buffers/inverters | 5539.744 | 3.0336% |
| Non-combinational | 98167.287 | 53.7565% |
| SRAM macros/black boxes | 0 | 0% |
| Total cell area | 182614.867 | 100% |

Total cell count is 241017. The shared permutation core occupies128440.663 µm² including children, or 70.3342%; its multiplier child occupies8912.316 µm², or 4.8804% of top area. Parent/child areas overlap and are not summed. Area excludes routing, whitespace, and chip periphery.

## Public scope

Design, parameters, generation, and functional/performance verification are fully available through [Reproduction](reproduction.en.md). We are sorry that commercial-tool and process-library licensing and confidentiality requirements prevent us from providing original DC scripts, reports, libraries, and synthesis snapshots. This page contains written summaries of basic synthesis conditions and results only.
