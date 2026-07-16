<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'设备接入数据链路', dev:'现场设备（或 virtual 合成值）', drv:'驱动 dc3-driver-*', data:'数据中心 dc3-center-data', ts:'TimescaleDB dc3_point_value', gw:'网关 dc3-gateway :8000', client:'调用方 / Web / CLI', e1:'PointValue 经 RabbitMQ', e2:'POST /api/v3/data/point_value/latest'},
  en: {aria:'Device onboarding data link', dev:'Field device (or virtual)', drv:'Driver dc3-driver-*', data:'Data Center dc3-center-data', ts:'TimescaleDB dc3_point_value', gw:'Gateway dc3-gateway :8000', client:'Caller / Web / CLI', e1:'PointValue via RabbitMQ', e2:'POST /api/v3/data/point_value/latest'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1200 280">
      <defs><marker id="dof-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#dof-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="210" x2="280" y1="100" y2="100"/>
      <line marker-end="url(#dof-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="480" x2="560" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="520" y="92">{{ s.e1 }}</text>
      <line marker-end="url(#dof-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="760" x2="840" y1="80" y2="80"/>
      <line marker-end="url(#dof-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="760" x2="840" y1="150" y2="150"/>
      <line marker-end="url(#dof-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1040" x2="1080" y1="150" y2="150"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="1060" y="142">{{ s.e2 }}</text>
      <rect x="20" y="75" width="190" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="20" y="75" width="190" height="50" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10" text-anchor="middle" x="115" y="105">{{ s.dev }}</text>
      <rect x="280" y="75" width="200" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="280" y="75" width="200" height="50" rx="8" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="380" y="105">{{ s.drv }}</text>
      <rect x="560" y="75" width="200" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="560" y="75" width="200" height="50" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="660" y="105">{{ s.data }}</text>
      <path d="M840,55 a85,14 0 0 0 170,0 v50 a85,14 0 0 1 -170,0 z" fill="var(--vp-c-bg)"/><path d="M840,55 a85,14 0 0 0 170,0 v50 a85,14 0 0 1 -170,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <ellipse cx="925" cy="55" fill="none" rx="85" ry="14" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10" text-anchor="middle" x="925" y="88">{{ s.ts }}</text>
      <rect x="840" y="125" width="200" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="840" y="125" width="200" height="50" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="940" y="155">{{ s.gw }}</text>
      <rect x="1080" y="125" width="110" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="1080" y="125" width="110" height="50" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="9.5" text-anchor="middle" x="1135" y="155">{{ s.client }}</text>
    </svg>
  </div></DiagramFrame>
</template>
