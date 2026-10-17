# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
# Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
# Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
# File: operators/poseidon2-sponge/tools/vectors.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""独立 Python 大整数参考；只生成离线向量，不驱动 RTL。"""
import json, random
from pathlib import Path
P=0xffffffff00000001
PARAM=json.loads((Path(__file__).resolve().parents[1]/'reference/parameters.json').read_text())

def permutation(x):
    ini=[int(v,16) for v in PARAM['GOLDILOCKS_POSEIDON2_RC_12_EXTERNAL_INITIAL']]
    fin=[int(v,16) for v in PARAM['GOLDILOCKS_POSEIDON2_RC_12_EXTERNAL_FINAL']]
    rc=[ini[i:i+12] for i in range(0,48,12)]+[[int(v,16)]+[0]*11 for v in PARAM['GOLDILOCKS_POSEIDON2_RC_12_INTERNAL']]+[fin[i:i+12] for i in range(0,48,12)]
    diag=[int(v,16) for v in PARAM['MATRIX_DIAG_12_GOLDILOCKS']]
    def num(v): return v
    m=[[2,3,1,1],[1,2,3,1],[1,1,2,3],[3,1,1,2]]
    def ext(s):
        return [sum((2 if i//4==j//4 else 1)*m[i%4][j%4]*s[j] for j in range(12))%P for i in range(12)]
    x=ext(list(x))
    for r in range(30):
        if r<4 or r>=26:x=ext([pow((v+num(rc[r][i]))%P,7,P) for i,v in enumerate(x)])
        else:
            x[0]=pow((x[0]+num(rc[r][0]))%P,7,P); total=sum(x)
            x=[(total+num(diag[i])*v)%P for i,v in enumerate(x)]
    return x

def digest(words,mode=0,wrong_padding=False):
    if mode==1: return permutation(words+[0]*4)[:4]
    s=[0]*12
    blocks=[words[i:i+8] for i in range(0,len(words),8)] or [[]]
    for k,b in enumerate(blocks):
        final=k==len(blocks)-1
        s[:len(b)]=b
        if final:
            if len(b)==8:s[8]=(s[8]+(0 if wrong_padding else 1))%P
            else:s[len(b):8]=[0 if wrong_padding else 1]+[0]*(7-len(b))
        s=permutation(s)
    return s[:4]

def generate(out,profile,tasks,max_elements):
    rng=random.Random(0x501010); cases=[]
    for t in range(tasks):
        mode=0;status=0;fault=0
        if profile=='compress':mode=1;n=8
        elif profile=='hash':n=max_elements
        else:
            lengths=[0,1,2,7,8,9,16,17,31,32,33,63,64,65,127,max_elements]
            n=lengths[t%len(lengths)]
            if t%7==6:mode=1;n=8
            if t>=16 and t%11==0: mode=2;status=1;n=8
            elif t>=16 and t%11==1:mode=1;n=7;status=1
            elif t>=16 and t%11==2:mode=0;n=17;status=2;fault=1
            elif t>=16 and t%11==3:mode=0;n=17;status=2;fault=2
            elif t>=16 and t%11==4:mode=0;n=17;status=3;fault=3
            elif t>=16 and t%11==5:mode=0;n=max_elements+1;status=1
        words=[rng.randrange(P) for _ in range(n)]
        if t%5==0:words=[0]*n
        elif t%5==1:words=[P-1]*n
        if profile=='mixed' and t==1:words=[1]
        if profile=='mixed' and t==2:words=[1,0]
        if fault==3:words[0]=P
        cases.append(dict(mode=mode,length=n,status=status,fault=fault,words=words))
    (out/'reference-input.txt').write_text(''.join(f"{c['mode']} {c['length']} {c['status']}\n"+' '.join(f'{w:016x}' for w in c['words'])+'\n' for c in cases))
    offset=0;rom=[];meta=[]
    for c in cases:
        meta.append(f"{c['mode']} {c['length']} {c['status']} {c['fault']} {offset}")
        words=c['words'];padded=words+[0]*((-len(words))%8)
        rom+=padded;offset+=len(padded)
    (out/'input.hex').write_text(''.join(f'{w:016x}\n' for w in (rom or [0])))
    (out/'tasks.txt').write_text('\n'.join(meta)+'\n')
    (out/'cases.json').write_text(json.dumps(cases,indent=2)+'\n')
    return cases

def check(out,cases,full=True):
    assert permutation(list(range(12)))==[int(v,16) for v in PARAM['kat']]
    values=[int(s,16) for s in (out/'expected.hex').read_text().split()]
    for t,c in enumerate(cases):
        if not c['status'] and (full or t<4):assert values[t*4:t*4+4]==digest(c['words'],c['mode']),('C/Python',t)
    a=digest([1]); b=digest([1,0]);assert a!=b
    assert digest(list(range(8)))!=digest(list(range(8)),1)
    for n in (0,1,7,8,9,16):assert digest(list(range(n)))!=digest(list(range(n)),wrong_padding=True)
    (out/'reference-check.json').write_text(json.dumps(dict(status=0,all_tasks_checked=full,
        checked_tasks=sum(not c['status'] for c in cases) if full else sum(not c['status'] for c in cases[:4]),
        wrong_padding_detected=[0,1,7,8,9,16],trailing_zero_distinct=True,hash8_compress8_distinct=True),indent=2)+'\n')
