<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/performance.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Performance

[简体中文](performance.md)

This document defines and reports task latency, sustained throughput, and compute-unit utilization for the 16-context product under ideal native interfaces:538 cycles per compression task and 274434 cycles per 4096-element hash, with shared-multiplier steady useful-issue rates of 87.7323% and 88.0591%, respectively.

One COMPRESS2 task compresses two four-element digests with one permutation; one HASH task hashes a field-element message. Rate 8 is block width, not elements per clock. The 16 contexts hold in-flight task states sharing one permutation core.

## Conditions and reproduction

The product has a 12-element state, rate 8, four-element digest,16 contexts, and 4096-element maximum. Inputs are supplied continuously when legal and ready; digest/completion sinks remain ready and enable stays asserted. No external bandwidth restriction is imposed. Real internal queues, credits, and feedback waits remain present. [Verification script](../tools/verify.py) cross-checks results against independent C/Python references; complete commands are given in [Reproduction](reproduction.en.md).

Isolated latency uses two tasks, admitting the next only after external completion of the previous. Sustained compression submits128 tasks and excludes startup/drain by measuring96 completion intervals between zero-based completion positions16 and 112. Sustained HASH submits64 tasks and measures32 intervals between positions16 and 48. Two sustained runs must produce identical performance statistics.

## Definitions

Let task-control acceptance occur at $A$, first input acceptance at $F$, external digest acceptance at $O$, and completion acceptance at $C$. Task latency is $L=C-A$; digest latency is $O-A$; first-input-to-digest latency is $O-F$. Isolated latency excludes interference from simultaneous tasks. Empty HASH has no $F$.

Let K count the completion intervals between sampled completion times C0 and C1, and let n be input field elements per task (8 or 4096 here). Steady task throughput is $T=K/(C_1-C_0)$ tasks/cycle; the equivalent average interval is $1/T$. It is measured from successive completions, not inferred as the inverse of isolated latency. At frequency $f$, tasks/second=$fT$, input elements/second=$nfT$, and latency in seconds=$L/f$. The conversions are not board or silicon measurements. The500 MHz conversion uses the configuration in [Synthesis summary](synthesis-summary.en.md).

## Latency and throughput

| Metric | COMPRESS2:8 elements | HASH:4096 elements |
| --- | ---: | ---: |
| Permutations per task | 1 | 512 |
| Isolated completion latency, cycles | 8607–8608 | 4390943–4390944 |
| Control-to-digest latency, cycles | 8591–8592 | 4390927–4390928 |
| First-input-to-digest latency, cycles | 8590–8591 | 4390926–4390927 |
| Steady completion intervals | 96 | 32 |
| Steady completion window, cycles | 51648 | 8781888 |
| Average cycles/task | 538 | 274434 |
| Tasks/second at 500 MHz | 929368.030 | 1821.932 |
| Input elements/second at 500 MHz | 7434944.238 | 7462632.181 |
| Isolated latency at 500 MHz, microseconds | 17.214–17.216 | 8781.886–8781.888 |

A4096-element HASH input occupies32 KiB of field representation and yields32 bytes of digest. COMPRESS2 consumes64 bytes and yields32 bytes. These byte counts describe interface fields, not a separate byte-string hash encoding.

## Utilization definitions

Here W is measured window cycles; N_issue counts useful launches, N_occupied counts cycles with at least one valid pipeline stage, and N_slots counts valid (stage, cycle) pairs. The verification layer directly observes useful work each cycle; counters are excluded from product SV. Useful issue utilization is $U=N_{\mathrm{issue}}/W$; issue-idle cycles=$W-N_{\mathrm{issue}}$. Any-stage-active utilization is $N_{\mathrm{occupied}}/W$. Average stage occupancy is $N_{\mathrm{slots}}/(dW)$ for depth $d$. Arithmetic utilization is not inferred from `io_busy`. Core windows are bounded by final-permutation returns for the selected steady tasks, preventing subsequent-task activity from leaking into the window.

| Multiplier metric | Compression window | HASH window |
| --- | ---: | ---: |
| Core window W, cycles | 51648 | 8781888 |
| Useful issue cycles | 45312 | 7733248 |
| Issue-idle cycles | 6336 | 1048640 |
| Useful issue utilization | 87.732342% | 88.059060% |
| Any-stage-active cycles | 47112 | 8040448 |
| Entire-pipeline-idle cycles | 4536 | 741440 |
| Any-stage-active utilization | 91.217472% | 91.557169% |
| Valid stage slots | 498432 | 85065728 |
| Average occupancy across 11 stages | 87.732342% | 88.059060% |

A/B are the two registered segments of each linear layer, not separate permutation cores. Linear-layer counters cover external A/B and internal A/B separately. The table distinguishes issue cycles, any-stage-active cycles, and average occupancy; pipeline depth must not be multiplied into a cycle count.

| Unit/depth | Compression: issue/active/average occupancy | HASH: issue/active/average occupancy |
| --- | ---: | ---: |
| External A/8 | 864 / 1242 / 1.672862% | 147456 / 211968 / 1.679092% |
| External B/6 | 864 / 1134 / 1.672862% | 147456 / 193536 / 1.679092% |
| Internal A/8 | 2112 / 3036 / 4.089219% | 360448 / 518144 / 4.104448% |
| Internal B/8 | 2112 / 3036 / 4.089219% | 360448 / 518144 / 4.104448% |

Linear issue-idle cycles equal W minus issue count; any-stage utilization equals active count divided by W. For example, HASH internal A has 5.900144% any-stage utilization. Every permutation has 472 useful multiplier issues and 472×11 valid stage slots; tests verify these conservation relations. An issue-share reference including scheduling phases is $472/(472+62+2)=88.0597\%$;16 contexts effectively hide multiplier feedback waits.
