<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/interface.en.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# Native interface

[简体中文](interface.md)

This document specifies the standalone top's task, data, digest, and completion channels, including handshakes, in-flight credits, error draining, and reset behavior, so integrators can schedule variable-length hashes and two-digest compressions by tag.

The top is `GoldilocksPoseidon2Sponge`, defaulting to 16 contexts and 4096 field elements per message. Field representation and functions are defined in [Mathematical model](mathematical-model.en.md).

Let n=elementCount be the declared field-element length. Valid elements are unsigned 64-bit canonical values 0 <= x < p, where p = 2^64 - 2^32 + 1; they are not arbitrary bytes. Each context stores one in-flight task state sharing one permutation core, not a separate core. The credit count below refers to the default 16-context configuration.

| Channel/signal | Direction and fields | Meaning |
| --- | --- | --- |
| `clock/reset` | Input; synchronous active-high reset | Discards all in-flight tasks without producing old completions |
| `io_enable` | 1-bit input | Previous-cycle sampled value controls new admission; accepted tasks continue; no admission on first post-reset cycle |
| `io_task` | Input ready/valid; tag32, mode2, elementCount32 | mode0 HASH: length 0..maximum; mode1 COMPRESS2: length 8 |
| `io_input` | Input ready/valid; tag32, 8×64-bit words, count 4, last 1 | Ordered blocks per task; count=min(8,remaining); last=1 only on final beat |
| `io_digest` | Output ready/valid; tag32, 4×64-bit words, count 4, last 1 | One beat per successful task; count 4, last 1 |
| `io_completion` | Output ready/valid; tag32, status2, elementCount32 | Echoes tag and declared length; success follows external digest handshake |
| `io_busy` | 1-bit output | Context/queue occupancy indication, not arithmetic utilization |

Fields are flattened in SV, for example `io_task_bits_tag`, `io_input_bits_words_0`, and `io_digest_bits_words_3`. Handshakes occur on rising edges when valid and ready are both1. Under backpressure the producer holds valid and all payload fields stable.

## Scheduling and credits

The caller guarantees unique in-flight tags and supplies input only for accepted tasks requiring data. Inputs may interleave across tasks; completion may be out of order, matched by tag. Blocks within each task stay ordered. Empty HASH requires no input. Unused final-beat words are ignored and may contain any 64-bit value.

Task acceptance reserves a credit, including tasks still in the two-entry task FIFO. External completion returns it. When all 16 credits are used, a same-cycle return does not combinationally admit a new task; admission resumes next cycle. Control and data boundaries use registered two-entry non-flow-through FIFOs. input.ready reflects only local FIFO capacity. An accepted beat is buffered and must not be resent. At most two input beats are buffered globally; internal consumption waits until the tagged context can absorb them. Input must not target nonexistent tags or BAD_PARAM tasks.

## Status and errors

| status | Name | Behavior |
| --- | --- | --- |
| 0 | SUCCESS | Canonical valid elements, legal frame, digest externally accepted |
| 1 | BAD_PARAM | Invalid mode, oversized HASH, or COMPRESS2 length other than 8; immediate completion, no input for this task |
| 2 | BAD_FRAME | count/last disagree with declared length; takes priority over field errors in the same beat |
| 3 | INVALID_FIELD | A valid word is≥p; no automatic reduction |

After frame or field errors, drain the total $\lceil n/8\rceil$ beats implied by the declared length, decrementing by the expected count. An erroneous count 0 does not prevent progress; early last does not terminate draining. The caller still supplies remaining beats. Error tasks produce no digest and complete after draining. Synchronous reset cancels unfinished tasks. Duplicate tags violate the caller contract and are asserted by the verification layer rather than automatically assigned an error status by the product.
