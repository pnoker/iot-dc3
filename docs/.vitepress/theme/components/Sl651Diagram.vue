<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'SL651 接入解析', station:'遥测测站 RTU', stationSub:'站址 12345678',
    server:'SL651Server', serverSub:'port 5001', handler:'handleSl651Message', handlerSub:'解析站址 + 要素列表',
    device:'设备 Device', deviceSub:'按站址匹配 deviceCode/Name',
    pv:'位号值 PointValue', pvSub:'按 index 取要素', sender:'pointValueSender',
    e1:'SL651 报文 (TCP)', e2:'onMessage', e3:'按 index 取要素', e4:'上报'},
  en: {aria:'SL651 onboarding parsing', station:'Telemetry RTU', stationSub:'station addr 12345678',
    server:'SL651Server', serverSub:'port 5001', handler:'handleSl651Message', handlerSub:'parse station + elements',
    device:'Device', deviceSub:'match deviceCode/Name by station',
    pv:'PointValue', pvSub:'element by index', sender:'pointValueSender',
    e1:'SL651 message (TCP)', e2:'onMessage', e3:'element by index', e4:'report'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 200">
      <defs><marker id="sl-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#sl-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="180" x2="250" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="215" y="92">{{ s.e1 }}</text>
      <line marker-end="url(#sl-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="410" x2="480" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="445" y="92">{{ s.e2 }}</text>
      <line marker-end="url(#sl-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="640" x2="710" y1="100" y2="100"/>
      <line marker-end="url(#sl-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="870" x2="940" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="905" y="92">{{ s.e3 }}</text>
      <line marker-end="url(#sl-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1100" x2="1170" y1="100" y2="100"/>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="10" y="65"/>
      <rect fill="var(--dc3-ext-fill)" height="70" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="180" x="10" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="100" y="92">{{ s.station }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="100" y="110">{{ s.stationSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="160" x="250" y="65"/>
      <rect fill="var(--dc3-bus-fill)" height="70" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="160" x="250" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="330" y="92">{{ s.server }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="330" y="110">{{ s.serverSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="160" x="480" y="65"/>
      <rect fill="var(--dc3-amber-fill)" height="70" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="160" x="480" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="560" y="92">{{ s.handler }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="560" y="110">{{ s.handlerSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="160" x="710" y="65"/>
      <rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="160" x="710" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="790" y="92">{{ s.device }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="790" y="110">{{ s.deviceSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="160" x="940" y="65"/>
      <rect fill="var(--dc3-fe-fill)" height="70" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="160" x="940" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="1020" y="92">{{ s.pv }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="1020" y="110">{{ s.pvSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="100" x="1170" y="65"/>
      <rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="100" x="1170" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="9.5" text-anchor="middle" x="1220" y="104">{{ s.sender }}</text>
    </svg>
  </div></DiagramFrame>
</template>
