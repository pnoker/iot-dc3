<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'无线协议分类与汇聚', shortT:'短距 (米~百米级)', ble:'BLE', bleSub:'低速 · 极省电', zigbee:'Zigbee', zigbeeSub:'低速 · 省电 · mesh', wifi:'Wi-Fi', wifiSub:'高速 · 高功耗',
    wanT:'广域 LPWAN (公里级)', lora:'LoRaWAN', loraSub:'极低速 · 极省电 · 自建网', nb:'NB-IoT', nbSub:'低速 · 省电 · 运营商网',
    cellT:'蜂窝高性能', g5:'5G', g5Sub:'高速 · 低时延 · 高功耗', agg:'网关 / broker 汇聚', plat:'IoT 平台'},
  en: {aria:'Wireless protocol categories', shortT:'Short range (m~100m)', ble:'BLE', bleSub:'low-speed · ultra-low-power', zigbee:'Zigbee', zigbeeSub:'low-speed · low-power · mesh', wifi:'Wi-Fi', wifiSub:'high-speed · high-power',
    wanT:'Wide-area LPWAN (km)', lora:'LoRaWAN', loraSub:'ultra-low · ultra-low-power · self-built', nb:'NB-IoT', nbSub:'low-speed · low-power · carrier',
    cellT:'Cellular high-perf', g5:'5G', g5Sub:'high-speed · low-latency · high-power', agg:'Gateway / broker aggregation', plat:'IoT platform'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1200 340">
      <defs><marker id="iw-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#iw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="300" x2="860" y1="160" y2="175"/>
      <line marker-end="url(#iw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="560" x2="860" y1="120" y2="170"/>
      <line marker-end="url(#iw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="780" x2="860" y1="85" y2="165"/>
      <line marker-end="url(#iw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1020" x2="1080" y1="180" y2="180"/>
      <!-- Short -->
      <rect fill="var(--dc3-fe-fill)" height="280" opacity="0.28" rx="12" width="270" x="30" y="30" stroke="var(--dc3-fe-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-fe-stroke)" font-size="11" font-weight="600" x="46" y="52">{{ s.shortT }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="230" x="50" y="70"/><rect fill="var(--dc3-fe-fill)" height="50" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="230" x="50" y="70"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="165" y="92">{{ s.ble }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="165" y="109">{{ s.bleSub }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="230" x="50" y="140"/><rect fill="var(--dc3-fe-fill)" height="50" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="230" x="50" y="140"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="165" y="162">{{ s.zigbee }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="165" y="179">{{ s.zigbeeSub }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="230" x="50" y="240"/><rect fill="var(--dc3-fe-fill)" height="50" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="230" x="50" y="240"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="165" y="262">{{ s.wifi }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="165" y="279">{{ s.wifiSub }}</text>
      <!-- WAN -->
      <rect fill="var(--dc3-bus-fill)" height="200" opacity="0.28" rx="12" width="230" x="330" y="30" stroke="var(--dc3-bus-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-bus-text)" font-size="11" font-weight="600" x="346" y="52">{{ s.wanT }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="190" x="350" y="70"/><rect fill="var(--dc3-bus-fill)" height="50" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="190" x="350" y="70"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="445" y="92">{{ s.lora }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="445" y="109">{{ s.loraSub }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="190" x="350" y="140"/><rect fill="var(--dc3-bus-fill)" height="50" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="190" x="350" y="140"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="445" y="162">{{ s.nb }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="445" y="179">{{ s.nbSub }}</text>
      <!-- Cell -->
      <rect fill="var(--dc3-amber-fill)" height="110" opacity="0.3" rx="12" width="200" x="600" y="30" stroke="var(--dc3-amber-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-amber-stroke)" font-size="11" font-weight="600" x="616" y="52">{{ s.cellT }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="160" x="620" y="65"/><rect fill="var(--dc3-amber-fill)" height="50" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="160" x="620" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="700" y="87">{{ s.g5 }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="700" y="104">{{ s.g5Sub }}</text>
      <!-- Agg / Plat -->
      <rect fill="var(--vp-c-bg)" height="70" rx="10" width="160" x="860" y="145"/><rect fill="var(--dc3-be-fill)" height="70" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="2" width="160" x="860" y="145"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="940" y="185">{{ s.agg }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="10" width="120" x="1080" y="145"/><rect fill="var(--dc3-be-fill)" height="70" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="120" x="1080" y="145"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="1140" y="185">{{ s.plat }}</text>
    </svg>
  </div></DiagramFrame>
</template>
