/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
 * Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
 * File: operators/poseidon2-sponge/sim/driver.cpp
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

#include "VSpongeSimTop.h"
#include "verilated.h"
#include <cstdio>
#include <cstdlib>
int main(int argc,char **argv){
  Verilated::commandArgs(argc,argv);VSpongeSimTop d;
  unsigned mode=argc>1?std::strtoul(argv[1],nullptr,10):0;
  d.io_mode=mode;d.io_trigger=0;d.reset=1;
  auto tick=[&](){d.clock=0;d.eval();d.clock=1;d.eval();};
  for(unsigned i=0;i<5;i++)tick();d.reset=0;d.io_trigger=1;tick();d.io_trigger=0;
  for(unsigned long long c=0;c<200000100ULL&&!d.io_done;c++)tick();
  std::printf("RESULT pass=%u errors=%u checked=%u cycles=%llu\n",unsigned(d.io_pass),unsigned(d.io_errors),unsigned(d.io_checked),(unsigned long long)d.io_cycles);
  bool ok=d.io_done&&d.io_pass;d.final();return ok?0:1;
}
