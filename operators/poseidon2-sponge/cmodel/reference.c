/*
 * OpenRTL-Header-Begin
 * OpenRTL — Open-source hardware accelerators.
 * Organization: https://github.com/OpenRTL-Lab
 * Website: https://openrtl.top/
 *
 * Project: Goldilocks Poseidon2 Sponge Hardware Accelerator
 * Chisel hardware for fixed-profile Poseidon2 sponge hashing and digest compression.
 * Repository: https://github.com/OpenRTL-Lab/poseidon2-sponge
 * File: operators/poseidon2-sponge/cmodel/reference.c
 *
 * SPDX-FileCopyrightText: 2026 Goldilocks Poseidon2 contributors
 * Copyright applies to original project contributions only.
 * SPDX-License-Identifier: MIT
 * See LICENSE and applicable third-party notices.
 * OpenRTL-Header-End
 */

#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include "parameters.h"
static const uint64_t P=UINT64_C(0xffffffff00000001);
static uint64_t add(uint64_t a,uint64_t b){return (uint64_t)(((__uint128_t)a+b)%P);}
static uint64_t mul(uint64_t a,uint64_t b){return (uint64_t)(((__uint128_t)a*b)%P);}
static uint64_t power7(uint64_t x){uint64_t a=mul(x,x),b=mul(a,x),c=mul(a,a);return mul(b,c);}
static void external(uint64_t s[12]){
  static const unsigned m[4][4]={{2,3,1,1},{1,2,3,1},{1,1,2,3},{3,1,1,2}};
  uint64_t v[12]={0},sum[4]={0};
  for(unsigned b=0;b<3;b++)for(unsigned i=0;i<4;i++)for(unsigned j=0;j<4;j++)
    v[4*b+i]=add(v[4*b+i],mul(m[i][j],s[4*b+j]));
  for(unsigned i=0;i<12;i++)sum[i%4]=add(sum[i%4],v[i]);
  for(unsigned i=0;i<12;i++)s[i]=add(v[i],sum[i%4]);
}
static void permutation(uint64_t s[12]){
  external(s);
  for(unsigned r=0;r<30;r++){
    if(r<4||r>=26){for(unsigned i=0;i<12;i++)s[i]=power7(add(s[i],RC[r][i]));external(s);}
    else{ s[0]=power7(add(s[0],RC[r][0])); uint64_t sum=0;
      for(unsigned i=0;i<12;i++)sum=add(sum,s[i]);
      for(unsigned i=0;i<12;i++)s[i]=add(sum,mul(s[i],DIAG[i])); }
  }
}
int main(int argc,char **argv){
  uint64_t kat[12];for(unsigned i=0;i<12;i++)kat[i]=i;permutation(kat);
  for(unsigned i=0;i<12;i++)if(kat[i]!=KAT[i])return 2;
  if(argc!=3)return 3;
  FILE *in=fopen(argv[1],"r"),*out=fopen(argv[2],"wx");if(!in||!out)return 4;
  unsigned mode,n,status,tasks=0;
  while(fscanf(in,"%u %u %u",&mode,&n,&status)==3){
    if(n>1048576)return 5;
    uint64_t *x=calloc(n?n:1,sizeof(uint64_t)),s[12]={0};if(!x)return 6;
    for(unsigned i=0;i<n;i++)if(fscanf(in,"%" SCNx64,&x[i])!=1)return 7;
    if(!status){
      if(mode==1){if(n!=8)return 8;for(unsigned i=0;i<8;i++)s[i]=x[i];permutation(s);}
      else if(mode==0){
        unsigned pos=0;
        while(n-pos>8){for(unsigned i=0;i<8;i++)s[i]=x[pos+i];permutation(s);pos+=8;}
        unsigned left=n-pos;
        for(unsigned i=0;i<8;i++)s[i]=i<left?x[pos+i]:(i==left?1:0);
        if(left==8)s[8]=add(s[8],1);
        permutation(s);
      }else return 9;
    }
    for(unsigned i=0;i<4;i++)fprintf(out,"%016" PRIx64 "\n",s[i]);
    free(x);tasks++;
  }
  if(ferror(in)||fclose(in)||fclose(out))return 10;
  printf("REFERENCE kat=1 tasks=%u\n",tasks);return 0;
}
