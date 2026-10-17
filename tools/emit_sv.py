#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
# Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
# Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
# File: tools/emit_sv.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""便携生成：独立参考向量、产品SV与验证环境。"""
from source_headers import stamp_generated
import argparse
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
OP = Path('operators/poseidon2-sponge')
BUNDLED = Path('rtl/v1.1.0/w12-r8-d4-ctx16-max4096')
sys.path.insert(0, str(ROOT / OP / 'tools'))
from vectors import generate as make_vectors, check


def relative_build(value):
    path = Path(value)
    if path.is_absolute() or '..' in path.parts or not path.parts or path.parts[0] != 'build':
        raise ValueError('输出必须是 build 内的新相对目录')
    return path


def run(command, output, label, expected=0):
    with (output / (label + '.log')).open('x') as log:
        result = subprocess.run([str(x) for x in command], cwd=ROOT, stdout=log, stderr=subprocess.STDOUT)
    if result.returncode != expected:
        raise RuntimeError(f'{label} 失败，请检查输出目录对应日志')


def code(text):
    return re.sub(r'/\*.*?\*/|//[^\n]*', '', text, flags=re.S)


def product_closure(rtl):
    stamp_generated(rtl)
    definitions, texts = {}, {}
    for path in sorted(rtl.glob('*.sv')):
        text = re.sub(r'[ \t]*//[^\n]*\.scala:\d+:\d+[^\n]*', '', path.read_text())
        path.write_text(text)
        texts[path] = code(text)
        for module in re.findall(r'\bmodule\s+(\w+)', texts[path]):
            assert module not in definitions
            definitions[module] = path
    selected, pending = set(), ['GoldilocksPoseidon2Sponge']
    while pending:
        path = definitions[pending.pop()]
        if path in selected:
            continue
        selected.add(path)
        instances = set(re.findall(r'^[ \t]*(\w+)\b[ \t]+(?:#\s*\([^;]*?\)\s*)?\w+\s*\(', texts[path], re.M | re.S))
        pending.extend(instances & definitions.keys())
    for path in selected:
        stripped = re.sub(r'"(?:[^"\\]|\\.)*"', '""', texts[path])
        assert not re.search(r'\$readmem|%|\b(?:DSP48\w*|RAMB\w*|URAM\w*|BUFG\w*|IOBUF)\b', stripped)
        assert 'SimTop' not in stripped
    return sorted(selected)


def simulation_list(rtl, output, bundled=False):
    sources = sorted(rtl.rglob('*.sv'))
    if bundled:
        product = set(product_closure(rtl))
        sources = [p for p in sources if p not in product] + sorted(BUNDLED.glob('*.sv'))
    includes = sorted({p.parent for p in sources})
    output.write_text(''.join('+incdir+' + str(p) + '\n' for p in includes) + ''.join(str(p) + '\n' for p in sources))


def generate(output, contexts=16, max_elements=4096, tasks=64, profile='mixed'):
    os.chdir(ROOT)
    output = relative_build(output)
    if output.exists():
        raise ValueError('输出目录已存在，请换一个新目录')
    if contexts not in (16, 32) or not 8 <= max_elements <= 1048576 or tasks < 2 or tasks & (tasks - 1):
        raise ValueError('上下文16/32；最大长度8到1048576；任务数为不小于2的2次幂')
    output.mkdir(parents=True)
    cases = make_vectors(output, profile, tasks, max_elements)
    run([os.environ.get('CC', 'cc'), '-std=c11', '-O2', '-Wall', '-Wextra', '-Werror', OP / 'cmodel/reference.c', '-o', output / 'reference'], output, 'c-compile')
    run([output / 'reference', output / 'reference-input.txt', output / 'expected.hex'], output, 'c-reference')
    check(output, cases, full=True)
    rtl = output / 'rtl'
    run(shlex.split(os.environ.get('MILL', 'mill')) + ['-i', 'poseidon2Sponge.tool.runMain', 'zk.sponge.Emit', rtl, contexts, max_elements, output], output, 'emit')
    product = product_closure(rtl)
    (rtl / 'product.f').write_text(''.join(str(p) + '\n' for p in product))
    simulation_list(rtl, rtl / 'simulation.f')
    (output / 'configuration.json').write_text(json.dumps({'version': '1.1.0', 'contexts': contexts, 'max_elements': max_elements, 'tasks': tasks, 'profile': profile}, indent=2) + '\n')
    return rtl


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', default='build/emit')
    parser.add_argument('--contexts', type=int, default=16)
    parser.add_argument('--max-elements', type=int, default=4096)
    parser.add_argument('--tasks', type=int, default=64)
    args = parser.parse_args()
    generate(args.out, args.contexts, args.max_elements, args.tasks)
    print('生成通过；独立产品清单为输出目录 rtl/product.f')
