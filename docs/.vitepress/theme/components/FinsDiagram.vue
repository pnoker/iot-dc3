<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'FINS/TCP 帧结构', tcp:'FINS/TCP 帧', len:'TCP 长度前缀', lenSub:'4 字节',
    fins:'FINS 帧', hdr:'FINS 头', hdrSub:'10 字节 · ICF 0x80 / RSV / GCT 0x02 / 节点 / SID',
    cmd:'命令码', cmdSub:'2 字节 · MRC 0x01 / SRC 0x01 (内存读)',
    par:'读参数', parSub:'4 字节 · 区代码 + 字地址 + 位 + 长度'},
  en: {aria:'FINS/TCP frame structure', tcp:'FINS/TCP frame', len:'TCP length prefix', lenSub:'4 bytes',
    fins:'FINS frame', hdr:'FINS header', hdrSub:'10 bytes · ICF 0x80 / RSV / GCT 0x02 / nodes / SID',
    cmd:'Command code', cmdSub:'2 bytes · MRC 0x01 / SRC 0x01 (memory read)',
    par:'Read params', parSub:'4 bytes · area code + word addr + bit + length'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1100 320">
      <defs><marker id="fi-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect fill="var(--dc3-bus-fill)" height="250" opacity="0.18" rx="12" width="1040" x="30" y="40" stroke="var(--dc3-bus-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-bus-text)" font-size="11" font-weight="600" x="48" y="62">{{ s.tcp }}</text>
      <rect fill="var(--vp-c-bg)" height="170" rx="10" width="800" x="250" y="100" stroke="var(--dc3-fe-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-fe-stroke)" font-size="10" font-weight="600" x="268" y="120">{{ s.fins }}</text>
      <line marker-end="url(#fi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="220" x2="270" y1="180" y2="180"/>
      <line marker-end="url(#fi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="650" x2="700" y1="180" y2="180"/>
      <rect fill="var(--vp-c-bg)" height="120" rx="8" width="180" x="40" y="120"/>
      <rect fill="var(--dc3-amber-fill)" height="120" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="180" x="40" y="120"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle" x="130" y="170">{{ s.len }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="130" y="195">{{ s.lenSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="380" x="270" y="100"/>
      <rect fill="var(--dc3-be-fill)" height="60" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="380" x="270" y="100"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle" x="460" y="124">{{ s.hdr }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="460" y="144">{{ s.hdrSub }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="380" x="270" y="175"/>
      <rect fill="var(--dc3-fe-fill)" height="50" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="380" x="270" y="175"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="460" y="198">{{ s.cmd }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="460" y="215">{{ s.cmdSub }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="380" x="270" y="240"/>
      <rect fill="var(--dc3-fe-fill)" height="50" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="380" x="270" y="240"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="460" y="263">{{ s.par }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="460" y="280">{{ s.parSub }}</text>
      <text fill="var(--dc3-text2)" font-size="20" text-anchor="middle" x="685" y="186">⌐</text>
      <text fill="var(--dc3-text2)" font-size="20" text-anchor="middle" x="245" y="186">⌐</text>
    </svg>
  </div></DiagramFrame>
</template>
