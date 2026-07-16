<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'S7 驱动接入多 PLC', dc3:'IoT DC3', drv:'dc3-driver-plcs7', drvSub:'S7 客户端 (master)',
    plc1:'S7-1200 PLC', plc1Sub:'host=192.168.0.20', plc2:'S7-1500 PLC', plc2Sub:'host=192.168.0.21',
    e1:'TCP:102 读写 DB{n}.{byte}[.{bit}]', e2:'TCP:102', e3:'应答 DB 字节'},
  en: {aria:'S7 driver onboarding multiple PLCs', dc3:'IoT DC3', drv:'dc3-driver-plcs7', drvSub:'S7 client (master)',
    plc1:'S7-1200 PLC', plc1Sub:'host=192.168.0.20', plc2:'S7-1500 PLC', plc2Sub:'host=192.168.0.21',
    e1:'TCP:102 read/write DB{n}.{byte}[.{bit}]', e2:'TCP:102', e3:'DB byte reply'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 320">
      <defs><marker id="s7-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect fill="var(--dc3-be-fill)" height="200" opacity="0.25" rx="12" width="280" x="40" y="60" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-be-text)" font-size="10" font-weight="600" x="56" y="80">{{ s.dc3 }}</text>
      <line marker-end="url(#s7-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="320" x2="430" y1="130" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="345" y="108">{{ s.e1 }}</text>
      <line marker-end="url(#s7-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="320" x2="430" y1="190" y2="220"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="345" y="218">{{ s.e2 }}</text>
      <path d="M430,110 Q360,150 320,150" fill="none" marker-end="url(#s7-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/>
      <path d="M430,220 Q360,200 320,200" fill="none" marker-end="url(#s7-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="380" y="178">{{ s.e3 }}</text>
      <rect fill="var(--vp-c-bg)" height="100" rx="10" width="220" x="70" y="115"/>
      <rect fill="var(--dc3-bus-fill)" height="100" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="2" width="220" x="70" y="115"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="180" y="152">{{ s.drv }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="180" y="174">{{ s.drvSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="240" x="430" y="65"/>
      <rect fill="var(--dc3-ext-fill)" height="70" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="240" x="430" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="550" y="92">{{ s.plc1 }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="550" y="112">{{ s.plc1Sub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="240" x="430" y="185"/>
      <rect fill="var(--dc3-ext-fill)" height="70" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="240" x="430" y="185"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="550" y="212">{{ s.plc2 }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="550" y="232">{{ s.plc2Sub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
