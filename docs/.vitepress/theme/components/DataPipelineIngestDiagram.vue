<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'位号值落库管道', drv:'驱动 dc3-driver-*', q:'队列 dc3.q.value.point', qSub:'TTL 7 天 + DLX',
    recv:'PointValueReceiver', recvSub:'dc3-center-data', ts:'TimescaleDB 超表', tsSub:'dc3_point_value',
    cache:'Caffeine 缓存', cacheSub:'每位号最新值', api:'读接口 latest / list',
    e1:'exchange dc3.e.value', e2:'routing dc3.r.value.point.{driver}'},
  en: {aria:'Point value ingest pipeline', drv:'Driver dc3-driver-*', q:'queue dc3.q.value.point', qSub:'TTL 7d + DLX',
    recv:'PointValueReceiver', recvSub:'dc3-center-data', ts:'TimescaleDB hypertable', tsSub:'dc3_point_value',
    cache:'Caffeine cache', cacheSub:'latest per point', api:'Read API latest / list',
    e1:'exchange dc3.e.value', e2:'routing dc3.r.value.point.{driver}'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 340">
      <defs><marker id="di-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <!-- Drv -> Q -->
      <line marker-end="url(#di-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="220" x2="290" y1="155" y2="155"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="255" y="147">{{ s.e1 }}</text>
      <text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="255" y="178">{{ s.e2 }}</text>
      <!-- Q -> Recv -->
      <line marker-end="url(#di-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="450" x2="520" y1="170" y2="170"/>
      <!-- Recv -> TS / Cache -->
      <line marker-end="url(#di-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="720" x2="790" y1="150" y2="110"/>
      <line marker-end="url(#di-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="720" x2="790" y1="195" y2="240"/>
      <!-- TS -> API / Cache -> API -->
      <line marker-end="url(#di-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="930" x2="1010" y1="110" y2="150"/>
      <line marker-end="url(#di-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="930" x2="1010" y1="240" y2="195"/>
      <!-- driver -->
      <rect fill="var(--vp-c-bg)" height="80" rx="10" width="200" x="20" y="130"/><rect fill="var(--dc3-bus-fill)" height="80" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="2" width="200" x="20" y="130"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="120" y="165">{{ s.drv }}</text>
      <!-- queue cylinder -->
      <path d="M290,130 a80,15 0 0 0 160,0 v75 a80,15 0 0 1 -160,0 z" fill="var(--vp-c-bg)"/>
      <path d="M290,130 a80,15 0 0 0 160,0 v75 a80,15 0 0 1 -160,0 z" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <ellipse cx="370" cy="130" fill="none" rx="80" ry="15" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="370" y="172">{{ s.q }}</text><text fill="var(--dc3-bus-text)" font-size="8.5" text-anchor="middle" x="370" y="190">{{ s.qSub }}</text>
      <!-- receiver -->
      <rect fill="var(--vp-c-bg)" height="80" rx="10" width="200" x="520" y="130"/><rect fill="var(--dc3-be-fill)" height="80" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="2" width="200" x="520" y="130"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="620" y="162">{{ s.recv }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="620" y="184">{{ s.recvSub }}</text>
      <!-- TS cylinder -->
      <path d="M790,65 a70,13 0 0 0 140,0 v90 a70,13 0 0 1 -140,0 z" fill="var(--vp-c-bg)"/>
      <path d="M790,65 a70,13 0 0 0 140,0 v90 a70,13 0 0 1 -140,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <ellipse cx="860" cy="65" fill="none" rx="70" ry="13" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="860" y="108">{{ s.ts }}</text><text fill="var(--dc3-db-text)" font-size="8.5" text-anchor="middle" x="860" y="126">{{ s.tsSub }}</text>
      <!-- cache -->
      <rect fill="var(--vp-c-bg)" height="65" rx="8" width="140" x="790" y="207"/><rect fill="var(--dc3-amber-fill)" height="65" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="140" x="790" y="207"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="860" y="235">{{ s.cache }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="860" y="253">{{ s.cacheSub }}</text>
      <!-- API -->
      <rect fill="var(--vp-c-bg)" height="80" rx="10" width="250" x="1010" y="130"/><rect fill="var(--dc3-amber-fill)" height="80" rx="10" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="250" x="1010" y="130"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="1135" y="175">{{ s.api }}</text>
    </svg>
  </div></DiagramFrame>
</template>
