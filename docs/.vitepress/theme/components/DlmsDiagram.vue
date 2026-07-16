<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'DLMS/COSEM 接入', meter:'DLMS/COSEM 电表', meterSub:'对象 1.0.1.8.0.255 总有功电能',
    net:'网络层 (DLMS/COSEM, IEC 62056)', link:'传输', linkSub:'TCP 4059 / 串口',
    cosem:'COSEM 对象模型', cosemSub:'OBIS 编码 + 属性编号',
    drv:'dc3-driver-dlms', drvSub:'code=DlmsDriver', plat:'IoT DC3', platSub:'位号值 PointValue',
    e1:'按 logicalName 读属性 2', e2:'采集值'},
  en: {aria:'DLMS/COSEM onboarding', meter:'DLMS/COSEM meter', meterSub:'object 1.0.1.8.0.255 total active energy',
    net:'Network layer (DLMS/COSEM, IEC 62056)', link:'Transport', linkSub:'TCP 4059 / serial',
    cosem:'COSEM object model', cosemSub:'OBIS code + attribute id',
    drv:'dc3-driver-dlms', drvSub:'code=DlmsDriver', plat:'IoT DC3', platSub:'PointValue',
    e1:'read attribute 2 by logicalName', e2:'collected value'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1120 340">
      <defs><marker id="dm-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect fill="var(--dc3-bus-fill)" height="240" opacity="0.22" rx="12" width="320" x="300" y="50" stroke="var(--dc3-bus-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-bus-text)" font-size="10" font-weight="600" x="316" y="70">{{ s.net }}</text>
      <line marker-end="url(#dm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="240" x2="330" y1="170" y2="130"/>
      <line marker-end="url(#dm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="460" x2="460" y1="155" y2="200"/>
      <line marker-end="url(#dm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="590" x2="680" y1="225" y2="180"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="640" y="195">{{ s.e1 }}</text>
      <line marker-end="url(#dm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="880" x2="960" y1="180" y2="180"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="920" y="172">{{ s.e2 }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="40" y="130"/>
      <rect fill="var(--dc3-ext-fill)" height="80" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="200" x="40" y="130"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="140" y="160">{{ s.meter }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="140" y="180">{{ s.meterSub }}</text>
      <rect fill="var(--vp-c-bg)" height="55" rx="8" width="260" x="330" y="100"/>
      <rect fill="var(--dc3-fe-fill)" height="55" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="260" x="330" y="100"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="460" y="124">{{ s.link }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="460" y="142">{{ s.linkSub }}</text>
      <rect fill="var(--vp-c-bg)" height="55" rx="8" width="260" x="330" y="200"/>
      <rect fill="var(--dc3-fe-fill)" height="55" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="260" x="330" y="200"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="460" y="224">{{ s.cosem }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="460" y="242">{{ s.cosemSub }}</text>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="200" x="680" y="135"/>
      <rect fill="var(--dc3-bus-fill)" height="90" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="2" width="200" x="680" y="135"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="780" y="170">{{ s.drv }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="780" y="192">{{ s.drvSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="160" x="960" y="145"/>
      <rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="160" x="960" y="145"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="1040" y="175">{{ s.plat }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="1040" y="193">{{ s.platSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
