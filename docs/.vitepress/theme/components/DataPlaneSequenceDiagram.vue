<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'读最新位号值时序', client:'客户端', data:'数据中心 dc3-center-data', cache:'Caffeine 最新值缓存', ts:'TimescaleDB dc3_point_value',
    m1:'POST /api/v3/data/point_value/latest', m2:'selectLatestPointValue(tenant, device, points)', m3:'命中的位号最新值', m4:'listLatestPointValues(未命中的 pointIds)', m5:'回源补齐', m6:'分页 PointValueVO（租户隔离）'},
  en: {aria:'Read latest point value sequence', client:'Client', data:'Data Center dc3-center-data', cache:'Caffeine latest cache', ts:'TimescaleDB dc3_point_value',
    m1:'POST /api/v3/data/point_value/latest', m2:'selectLatestPointValue(tenant, device, points)', m3:'cached latest values', m4:'listLatestPointValues(missed pointIds)', m5:'backfill from DB', m6:'paged PointValueVO (tenant-scoped)'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const PX: Record<string, number> = {client: 120, data: 390, cache: 650, ts: 870}
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 990 400">
      <defs><marker id="dps-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line v-for="p in ['client','data','cache','ts']" :key="'l'+p" :x1="PX[p]" :x2="PX[p]" y1="70" y2="370" stroke="var(--dc3-divider)" stroke-width="1" stroke-dasharray="4,4"/>
      <rect v-for="p in ['client','data','cache','ts']" :key="'b'+p" :x="PX[p]-85" y="22" width="170" height="48" rx="8" :fill="['var(--dc3-ext-fill)','var(--dc3-be-fill)','var(--dc3-amber-fill)','var(--dc3-db-fill)'][['client','data','cache','ts'].indexOf(p)]" :stroke="['var(--dc3-ext-stroke)','var(--dc3-be-stroke)','var(--dc3-amber-stroke)','var(--dc3-db-stroke)'][['client','data','cache','ts'].indexOf(p)]" stroke-width="1.5" opacity="0.65"/>
      <text v-for="p in ['client','data','cache','ts']" :key="'t'+p" :x="PX[p]" y="51" class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle">{{ s[p] }}</text>
      <line marker-end="url(#dps-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.client" :x2="PX.data" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.client+PX.data)/2" y="92">{{ s.m1 }}</text>
      <line marker-end="url(#dps-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.data" :x2="PX.cache" y1="140" y2="140"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.data+PX.cache)/2" y="132">{{ s.m2 }}</text>
      <line marker-end="url(#dps-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.cache" :x2="PX.data" y1="175" y2="175"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.cache+PX.data)/2" y="167">{{ s.m3 }}</text>
      <line marker-end="url(#dps-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.data" :x2="PX.ts" y1="215" y2="215"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.data+PX.ts)/2" y="207">{{ s.m4 }}</text>
      <line marker-end="url(#dps-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.ts" :x2="PX.data" y1="250" y2="250"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.ts+PX.data)/2" y="242">{{ s.m5 }}</text>
      <path :d="`M${PX.data},300 Q${(PX.data+PX.client)/2},340 ${PX.client},320`" fill="none" marker-end="url(#dps-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.data+PX.client)/2" y="350">{{ s.m6 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
