<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'Modbus TCP 四种数据区', drv:'dc3-driver-modbus-tcp', drvSub:'Modbus 主站',
    coil:'线圈 Coils', coilSub:'可读可写 · 位', di:'离散输入 Discrete Inputs', diSub:'只读 · 位',
    hr:'保持寄存器 Holding Registers', hrSub:'可读可写 · 16 位', ir:'输入寄存器 Input Registers', irSub:'只读 · 16 位',
    fc1:'FC01 读', fc2:'FC02 读', fc3:'FC03 读', fc4:'FC04 读', fcw:'FC05/06 写'},
  en: {aria:'Modbus TCP four data areas', drv:'dc3-driver-modbus-tcp', drvSub:'Modbus master',
    coil:'Coils', coilSub:'R/W · bit', di:'Discrete Inputs', diSub:'read-only · bit',
    hr:'Holding Registers', hrSub:'R/W · 16-bit', ir:'Input Registers', irSub:'read-only · 16-bit',
    fc1:'FC01 read', fc2:'FC02 read', fc3:'FC03 read', fc4:'FC04 read', fcw:'FC05/06 write'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 320">
      <defs><marker id="mt-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <!-- reads (solid) -->
      <line marker-end="url(#mt-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="240" x2="420" y1="120" y2="80"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="330" y="90">{{ s.fc1 }}</text>
      <line marker-end="url(#mt-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="240" x2="420" y1="155" y2="155"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="330" y="148">{{ s.fc2 }}</text>
      <line marker-end="url(#mt-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="240" x2="740" y1="140" y2="85"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="490" y="100">{{ s.fc3 }}</text>
      <line marker-end="url(#mt-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="240" x2="740" y1="170" y2="170"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="490" y="186">{{ s.fc4 }}</text>
      <!-- writes (dashed) -->
      <path d="M240,185 Q330,260 420,110" fill="none" marker-end="url(#mt-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="300" y="250">{{ s.fcw }}</text>
      <path d="M240,195 Q490,280 740,115" fill="none" marker-end="url(#mt-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/>
      <!-- driver -->
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="200" x="40" y="105"/>
      <rect fill="var(--dc3-bus-fill)" height="90" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="2" width="200" x="40" y="105"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="140" y="142">{{ s.drv }}</text>
      <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="140" y="162">{{ s.drvSub }}</text>
      <!-- coils / di / hr / ir -->
      <rect fill="var(--vp-c-bg)" height="64" rx="8" width="240" x="420" y="48"/>
      <rect fill="var(--dc3-fe-fill)" height="64" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="240" x="420" y="48"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="540" y="75">{{ s.coil }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="540" y="93">{{ s.coilSub }}</text>
      <rect fill="var(--vp-c-bg)" height="64" rx="8" width="240" x="420" y="168"/>
      <rect fill="var(--dc3-fe-fill)" height="64" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="240" x="420" y="168"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="540" y="195">{{ s.di }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="540" y="213">{{ s.diSub }}</text>
      <rect fill="var(--vp-c-bg)" height="64" rx="8" width="240" x="740" y="48"/>
      <rect fill="var(--dc3-fe-fill)" height="64" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="240" x="740" y="48"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="860" y="75">{{ s.hr }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="860" y="93">{{ s.hrSub }}</text>
      <rect fill="var(--vp-c-bg)" height="64" rx="8" width="240" x="740" y="168"/>
      <rect fill="var(--dc3-fe-fill)" height="64" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="240" x="740" y="168"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="860" y="195">{{ s.ir }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="860" y="213">{{ s.irSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
