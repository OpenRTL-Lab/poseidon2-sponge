<!--
OpenRTL-Header-Begin
OpenRTL — Open-source hardware accelerators.
Organization: https://github.com/OpenRTL-Lab
Website: https://openrtl.top/

Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
File: docs/interface.md

SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
Copyright applies to original project contributions only.
SPDX-License-Identifier: MIT
See LICENSE and applicable third-party notices.
OpenRTL-Header-End
-->

# 原生接口

[English](interface.en.md)

本文件说明独立顶层的任务、数据、摘要和完成通道，定义握手、在途信用、错误排空与复位行为，使集成方能够按tag调度可变长度哈希和二摘要压缩任务。

顶层 `GoldilocksPoseidon2Sponge`，默认16上下文、最大4096域元素，接口域表示及函数定义见 [数学模型](mathematical-model.md)。

令 n=elementCount 为声明的域元素长度。有效元素是无符号64位规范值 0 ≤ x < p，p = 2^64 - 2^32 + 1，不是任意字节。每个上下文保存一个在途任务状态，共用一颗置换核，并非一套独立核；下文16信用对应默认16上下文配置。

| 通道/信号 | 方向与字段 | 语义 |
| --- | --- | --- |
| `clock/reset` | 输入；同步高有效reset | 复位丢弃全部在途任务，不生成旧完成 |
| `io_enable` | 输入1位 | 上周期采样值控制新任务准入；已接受任务继续；复位后首周期不准入 |
| `io_task` | 输入ready/valid；tag32、mode2、elementCount32 | mode0 HASH长度0..最大值；mode1 COMPRESS2长度8 |
| `io_input` | 输入ready/valid；tag32、8×64位words、count4、last1 | 同任务按块保序；count=min(8,剩余长度)，last仅末拍为1 |
| `io_digest` | 输出ready/valid；tag32、4×64位words、count4、last1 | 每成功任务输出一拍，count4、last1 |
| `io_completion` | 输出ready/valid；tag32、status2、elementCount32 | 原样返回tag和声明长度；成功必须在摘要外部握手后 |
| `io_busy` | 输出1位 | 上下文或队列占用指示，不是计算单元忙率 |

通道字段在SV中展开，例如 `io_task_bits_tag`、`io_input_bits_words_0`、`io_digest_bits_words_3`。握手发生在上升沿valid与ready同时为1；背压期间源端保持valid及全部载荷稳定。

## 调度与信用

调用方保证在途tag唯一，输入tag属于已接受且需输入的任务。不同任务可交织输入、乱序完成，以tag配对；任务内部块顺序不可改变。空HASH不发送数据。尾拍无效words不参与检查，可为任意64位值。

任务握手即预留信用，含尚在两槽任务队列中的任务；外部completion握手归还信用。在16信用用尽时，同拍归还不会组合准入新任务，下一拍恢复。所有控制和数据边界均通过寄存的两槽非直通队列；input.ready只反映本地FIFO容量。输入握手表示已缓存，不能重发。全局最多缓存两个输入拍，内部等待对应上下文可吸收后消费。调用方不能给不存在tag或BAD_PARAM任务发送数据。

## 状态与错误

| status | 名称 | 行为 |
| --- | --- | --- |
| 0 | SUCCESS | 所有有效元素和分帧合法，摘要已外部握手 |
| 1 | BAD_PARAM | 非法mode、HASH超长或COMPRESS2长度非8；直接完成，不接收该任务数据 |
| 2 | BAD_FRAME | count或last与声明长度不符；同拍优先于域元素错误 |
| 3 | INVALID_FIELD | 有效words含值≥p；不自动取模 |

分帧或域错误后按声明长度对应的 $\lceil n/8\rceil$ 总拍数排空，使用期望count递减剩余长度；错误count0不阻止进展，提前last不提前结束。调用方仍需发送其余拍；错误任务不输出摘要，排空后完成。同步复位可取消所有未完成任务。重复tag属于调用方违约，由验证层断言检查，不是产品自动返回的状态码。
