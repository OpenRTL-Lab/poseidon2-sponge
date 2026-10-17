#!/usr/bin/env python3
# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
# Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
# Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
# File: tools/verify.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""全链路复现：参考交叉校验、重新生成、随包电路复验及理想性能。"""
import argparse
import json
import os
from pathlib import Path
import re
import tempfile
from emit_sv import ROOT, OP, BUNDLED, code, generate, product_closure, run, simulation_list, relative_build
from metrics import parse, performance


def verify_case(output, contexts, max_elements, tasks, profile, single, jobs, bundled=False):
    rtl = generate(output, contexts, max_elements, tasks, profile)
    output = Path(output)
    run(['verilator', '--lint-only', '--assert', '--top-module', 'GoldilocksPoseidon2Sponge', '-f', rtl / 'product.f'], output, 'lint')
    if contexts == 16 and max_elements == 4096:
        originals = {p.name: p for p in BUNDLED.glob('*.sv')}
        generated = {p.name: p for p in product_closure(rtl)}
        assert originals.keys() == generated.keys(), '随包模块集合不一致'
        for name in originals:
            assert re.sub(r'\s+', '', code(originals[name].read_text())) == re.sub(r'\s+', '', code(generated[name].read_text())), '随包与生成电路不一致：' + name
    filelist = output / 'simulation.f'
    simulation_list(rtl, filelist, bundled)
    modes = [(4, 'single', 0)] if single else [(0, 'ideal', 0), (1, 'protocol', 0), (2, 'reset', 0), (3, 'negative', 1)] if profile == 'mixed' else [(0, 'ideal', 0), (0, 'repeat', 0)]
    results = {}
    with tempfile.TemporaryDirectory(prefix='sponge-') as temporary:
        obj = Path(temporary)
        run(['verilator', '--cc', '--exe', '--build', '-j', jobs, '--assert', '--output-split', 5000, '--output-split-cfuncs', 5000, '-DENABLE_INITIAL_MEM_', '--top-module', 'SpongeSimTop', '--Mdir', obj, '-f', filelist, ROOT / OP / 'sim/driver.cpp'], output, 'compile')
        for mode, label, expected in modes:
            run([obj / 'VSpongeSimTop', mode], output, label, expected)
            result = parse((output / (label + '.log')).read_text())
            assert result['pass'] == (0 if mode == 3 else 1)
            assert result['errors'] == (1 if mode == 3 else 0)
            results[label] = result
    report = {'contexts': contexts, 'max_elements': max_elements, 'tasks': tasks, 'profile': profile, 'bundled': bundled, 'tests': results}
    if profile != 'mixed':
        label = 'single' if single else 'ideal'
        report['performance'] = performance((output / (label + '.log')).read_text(), contexts, tasks, 8 if profile == 'compress' else max_elements)
        if not single:
            report['performance'].pop('first_task_units')
            repeated = performance((output / 'repeat.log').read_text(), contexts, tasks, 8 if profile == 'compress' else max_elements)
            repeated.pop('first_task_units')
            assert repeated == report['performance'], '重复运行性能不一致'
    report['status'] = 0
    (output / 'results.json').write_text(json.dumps(report, indent=2) + '\n')
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', default='build/verify')
    parser.add_argument('--jobs', type=int, default=2)
    parser.add_argument('--functional-only', action='store_true')
    args = parser.parse_args()
    assert 1 <= args.jobs <= 64
    os.chdir(ROOT)
    os.environ['CCACHE_DISABLE'] = '1'
    output = relative_build(args.out)
    assert not output.exists(), '输出目录必须是新目录'
    output.mkdir(parents=True)
    matrix = [('mixed', 16, 4096, 64, 'mixed', False, False),
              ('bundled', 16, 4096, 64, 'mixed', False, True),
              ('ctx32', 32, 8192, 64, 'mixed', False, False)]
    if not args.functional_only:
        matrix += [('compress-single', 16, 4096, 2, 'compress', True, False),
                   ('compress-steady', 16, 4096, 128, 'compress', False, False),
                   ('hash-single', 16, 4096, 2, 'hash', True, False),
                   ('hash-steady', 16, 4096, 64, 'hash', False, False)]
    reports = {}
    for name, contexts, maximum, tasks, profile, single, bundled in matrix:
        print('验证：' + name, flush=True)
        reports[name] = verify_case(output / name, contexts, maximum, tasks, profile, single, args.jobs, bundled)
    assert reports['mixed']['tests'] == reports['bundled']['tests']
    report = {'status': 0, 'version': '1.1.0', 'ideal_interfaces': True, 'cases': reports}
    (output / 'results.json').write_text(json.dumps(report, indent=2) + '\n')
    print('全部验证通过；功能、协议、复位、负控及所选性能结果见输出目录 results.json')


if __name__ == '__main__':
    main()
