<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'OPC DA 接入链路', plc:'现场 PLC / 仪表', srv:'OPC DA Server', srvSub:'SCADA / 组态软件',
    drv:'dc3-driver-opc-da', drvSub:'OPC DA 客户端', dc:'IoT DC3 数据中心',
    e1:'数据采集', e2:'DCOM (CLSID)', e3:'位号值 PointValue'},
  en: {aria:'OPC DA onboarding chain', plc:'Field PLC / instrument', srv:'OPC DA Server', srvSub:'SCADA / HMI',
    drv:'dc3-driver-opc-da', drvSub:'OPC DA client', dc:'IoT DC3 Data Center',
    e1:'data acquisition', e2:'DCOM (CLSID)', e3:'PointValue'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1100 240">
      <defs><marker id="od-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#od-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="220" x2="300" y1="120" y2="120"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="260" y="112">{{ s.e1 }}</text>
      <line marker-end="url(#od-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="520" x2="600" y1="120" y2="120"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="560" y="112">{{ s.e2 }}</text>
      <line marker-end="url(#od-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="820" x2="900" y1="120" y2="120"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="860" y="112">{{ s.e3 }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="20" y="80"/>
      <rect fill="var(--dc3-ext-fill)" height="80" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="200" x="20" y="80"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="120" y="115">{{ s.plc }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="220" x="300" y="80"/>
      <rect fill="var(--dc3-bus-fill)" height="80" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="220" x="300" y="80"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="410" y="110">{{ s.srv }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="410" y="130">{{ s.srvSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="220" x="600" y="80"/>
      <rect fill="var(--dc3-bus-fill)" height="80" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="2" width="220" x="600" y="80"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="710" y="112">{{ s.drv }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="710" y="132">{{ s.drvSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="900" y="80"/>
      <rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="200" x="900" y="80"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="1000" y="125">{{ s.dc }}</text>
    </svg>
  </div></DiagramFrame>
</template>
