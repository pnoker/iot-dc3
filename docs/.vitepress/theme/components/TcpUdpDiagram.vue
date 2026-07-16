<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'TCP/UDP 帧解析流水线', sched:'驱动 SDK', schedSub:'每 30 秒一轮', read:'read()',
    tcp:'sendTcp()', tcpSub:'复用缓存长连接', udp:'sendUdp()', udpSub:'临时 DatagramSocket',
    raw:'原始回包 HEX', parse:'parseFrame()', parseSub:'dataOffset/dataLength 切片',
    conv:'parseDataValue()', convSub:'dataFormat + byteOrder 转值', pv:'位号值 PointValue',
    e1:'protocol=TCP', e2:'protocol=UDP'},
  en: {aria:'TCP/UDP frame parsing pipeline', sched:'Driver SDK', schedSub:'every 30s a round', read:'read()',
    tcp:'sendTcp()', tcpSub:'reused cached connection', udp:'sendUdp()', udpSub:'ephemeral DatagramSocket',
    raw:'raw reply HEX', parse:'parseFrame()', parseSub:'slice by dataOffset/dataLength',
    conv:'parseDataValue()', convSub:'convert by dataFormat + byteOrder', pv:'PointValue',
    e1:'protocol=TCP', e2:'protocol=UDP'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 280">
      <defs><marker id="tu-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#tu-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="170" x2="250" y1="140" y2="140"/>
      <line marker-end="url(#tu-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="370" x2="490" y1="120" y2="80"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="430" y="90">{{ s.e1 }}</text>
      <line marker-end="url(#tu-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="370" x2="490" y1="160" y2="200"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="430" y="195">{{ s.e2 }}</text>
      <line marker-end="url(#tu-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="610" x2="690" y1="80" y2="130"/>
      <line marker-end="url(#tu-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="610" x2="690" y1="200" y2="150"/>
      <line marker-end="url(#tu-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="810" x2="890" y1="140" y2="140"/>
      <line marker-end="url(#tu-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1010" x2="1090" y1="140" y2="140"/>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="160" x="20" y="100"/>
      <rect fill="var(--dc3-amber-fill)" height="80" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="160" x="20" y="100"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="100" y="130">{{ s.sched }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="100" y="150">{{ s.schedSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="120" x="250" y="110"/>
      <rect fill="var(--dc3-be-fill)" height="60" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="120" x="250" y="110"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="310" y="145">{{ s.read }}</text>
      <rect fill="var(--vp-c-bg)" height="56" rx="8" width="120" x="490" y="52"/>
      <rect fill="var(--dc3-bus-fill)" height="56" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="120" x="490" y="52"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="550" y="76">{{ s.tcp }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="550" y="93">{{ s.tcpSub }}</text>
      <rect fill="var(--vp-c-bg)" height="56" rx="8" width="120" x="490" y="172"/>
      <rect fill="var(--dc3-bus-fill)" height="56" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="120" x="490" y="172"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="550" y="196">{{ s.udp }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="550" y="213">{{ s.udpSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="120" x="690" y="110"/>
      <rect fill="var(--dc3-ext-fill)" height="60" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="120" x="690" y="110"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="750" y="145">{{ s.raw }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="120" x="890" y="110"/>
      <rect fill="var(--dc3-amber-fill)" height="60" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="120" x="890" y="110"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="950" y="138">{{ s.parse }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="950" y="156">{{ s.parseSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="90" x="1090" y="110"/>
      <rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="90" x="1090" y="110"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="1135" y="145">{{ s.pv }}</text>
    </svg>
  </div></DiagramFrame>
</template>
