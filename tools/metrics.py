# OpenRTL-Header-Begin
# OpenRTL — Open-source hardware accelerators.
# Organization: https://github.com/OpenRTL-Lab
# Website: https://openrtl.top/
#
# Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
# Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
# Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
# File: tools/metrics.py
#
# SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
# Copyright applies to original project contributions only.
# SPDX-License-Identifier: MIT
# See LICENSE and applicable third-party notices.
# OpenRTL-Header-End

"""从验证层握手与有效阶段计数计算性能，不改动产品电路。"""
import re

def parse(text):
    result=re.search(r'RESULT pass=(\d+) errors=(\d+) checked=(\d+) cycles=(\d+)',text)
    assert result,'missing RESULT'
    return dict(zip(('pass','errors','checked','cycles'),map(int,result.groups())))

def performance(text,contexts,tasks,words):
    events={kind:{} for kind in 'AFOC'};stats={};perms={};timeline=[]
    for line in text.splitlines():
        if line.startswith(tuple(k+' ' for k in events)):
            d={k:int(v) for k,v in re.findall(r'(\w+)\s*=\s*(\d+)',line)}
            events[line[0]][d['tag']]=d['cycle']
        elif line.startswith('CORE_STAT '):
            d={k:int(v) for k,v in re.findall(r'(\w+)\s*=\s*(\d+)',line)};stats[d['cycle']]=d
        elif line.startswith('PERM_OUT '):
            d={k:int(v) for k,v in re.findall(r'(\w+)\s*=\s*(\d+)',line)}
            timeline.append(d)
            if d['final']:perms[d['tag']]=d['cycle']
    assert len(events['A'])==len(events['C'])==tasks
    latency=[events['C'][t]-events['A'][t] for t in range(tasks)]
    calc=[events['O'][t]-events['F'][t] for t in range(tasks) if t in events['F']]
    out=dict(task_latency_clks=dict(min=min(latency),max=max(latency),mean=sum(latency)/tasks),
        task_to_digest_clks=dict(min=min(events['O'][t]-events['A'][t] for t in range(tasks)),
            max=max(events['O'][t]-events['A'][t] for t in range(tasks))),
        input_to_digest_clks=dict(min=min(calc),max=max(calc),mean=sum(calc)/len(calc)) if calc else None,
        tasks=tasks,fields_per_task=words,permutations=len(timeline),total_clks=parse(text)['cycles'])
    # 全运行窗使用首次任务准入到最终完成；计数直接来自验证层实际有效发射。
    last_stat=stats[max(stats)]
    assert last_stat['mul']==len(timeline)*472
    assert last_stat['slots']==last_stat['mul']*11
    for k,depth in zip(('ea','eb','ia','ib'),(8,6,8,8)):
        assert last_stat[k+'s']==last_stat[k]*depth,(k,'linear valid-stage conservation')
    span=max(events['C'].values())-min(events['A'].values())
    out['whole_run_units']=dict(window_clks=span,mul_valid_clks=last_stat['mul'],
        mul_valid_ratio=last_stat['mul']/span,mul_idle_clks=span-last_stat['mul'],
        pipeline_occupied_clks=last_stat['pipe'],pipeline_occupied_ratio=last_stat['pipe']/span,
        pipeline_idle_clks=span-last_stat['pipe'],valid_stage_slots=last_stat['slots'],
        linear_phase_clks={k:last_stat[k] for k in ('ea','eb','ia','ib')},
        linear_pipeline_clks={k:last_stat[k+'p'] for k in ('ea','eb','ia','ib')},
        linear_valid_stage_slots={k:last_stat[k+'s'] for k in ('ea','eb','ia','ib')})
    first_stat=stats[perms[0]]
    out['first_task_units']=dict(task_window_clks=latency[0],
        compute_window_clks=events['O'][0]-events['F'][0],mul_valid_clks=first_stat['mul'],
        mul_valid_ratio_in_task=first_stat['mul']/latency[0],
        mul_valid_ratio_in_compute=first_stat['mul']/(events['O'][0]-events['F'][0]),
        pipeline_occupied_clks=first_stat['pipe'],valid_stage_slots=first_stat['slots'])
    if tasks>=contexts*4:
        ordered=sorted(events['C'],key=events['C'].get)
        first,last=ordered[contexts],ordered[tasks-contexts]
        intervals=tasks-2*contexts;delta=events['C'][last]-events['C'][first]
        s0,s1=stats[perms[first]],stats[perms[last]];core_delta=s1['cycle']-s0['cycle']
        out['steady']=dict(first_tag=first,last_tag=last,completion_intervals=intervals,
            window_clks=delta,clks_per_task=delta/intervals,tasks_per_clk=intervals/delta,
            fields_per_clk=words*intervals/delta,unit_window_clks=core_delta,
            mul_valid_clks=s1['mul']-s0['mul'],mul_valid_ratio=(s1['mul']-s0['mul'])/core_delta,
            mul_idle_clks=core_delta-(s1['mul']-s0['mul']),
            pipeline_occupied_clks=s1['pipe']-s0['pipe'],pipeline_occupied_ratio=(s1['pipe']-s0['pipe'])/core_delta,
            pipeline_idle_clks=core_delta-(s1['pipe']-s0['pipe']),
            valid_stage_slots=s1['slots']-s0['slots'],
            accepted_interval_clks=dict(min=min(events['A'][t+1]-events['A'][t] for t in range(tasks-1)),
                max=max(events['A'][t+1]-events['A'][t] for t in range(tasks-1))),
            linear_phase_clks={k:s1[k]-s0[k] for k in ('ea','eb','ia','ib')},
            linear_pipeline_clks={k:s1[k+'p']-s0[k+'p'] for k in ('ea','eb','ia','ib')},
            linear_valid_stage_slots={k:s1[k+'s']-s0[k+'s'] for k in ('ea','eb','ia','ib')})
    return out
