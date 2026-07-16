<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria: '驱动读调度与写命令时序', quartz: 'Quartz 读调度', meta: 'DriverMetadata 缓存', impl: 'DriverCustomService.read()', mq: 'RabbitMQ', data: '数据中心 dc3-center-data',
    noteOut: '出站：周期采集', noteIn: '入站：写命令',
    m1: '遍历本驱动设备', m2: 'deviceIds', m3: 'read(driverConfig, pointConfig, device, point)', m4: 'ReadPointValue', m5: 'pointValueSender(PointValue)', m6: '落库 dc3_point_value',
    m7: '下发 dc3.e.point_command', m8: 'write(..., WritePointValue)', m9: 'Boolean(true/false)', m10: 'pointCommandResultSender(结果)'},
  en: {aria: 'Driver read scheduling and write command sequence', quartz: 'Quartz read scheduler', meta: 'DriverMetadata cache', impl: 'DriverCustomService.read()', mq: 'RabbitMQ', data: 'Data Center dc3-center-data',
    noteOut: 'Outbound: periodic collection', noteIn: 'Inbound: write command',
    m1: 'iterate this driver\'s devices', m2: 'deviceIds', m3: 'read(driverConfig, pointConfig, device, point)', m4: 'ReadPointValue', m5: 'pointValueSender(PointValue)', m6: 'persist to dc3_point_value',
    m7: 'dispatch dc3.e.point_command', m8: 'write(..., WritePointValue)', m9: 'Boolean(true/false)', m10: 'pointCommandResultSender(result)'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const PX: Record<string, number> = {quartz: 110, meta: 300, impl: 490, mq: 680, data: 870}
const cols = ['quartz', 'meta', 'impl', 'mq', 'data']
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 980 510">
      <defs><marker id="dasq-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <!-- lifelines -->
      <line v-for="p in cols" :key="'l'+p" :x1="PX[p]" :x2="PX[p]" y1="78" y2="486" stroke="var(--vp-c-divider)" stroke-width="1" stroke-dasharray="4,4"/>
      <!-- headers -->
      <rect v-for="(p,i) in cols" :key="'b'+p" :x="PX[p]-85" y="22" width="170" height="48" rx="8" :fill="['var(--dc3-amber-fill)','var(--dc3-fe-fill)','var(--dc3-bus-fill)','var(--dc3-be-fill)','var(--dc3-be-fill)'][i]" :stroke="['var(--dc3-amber-stroke)','var(--dc3-fe-stroke)','var(--dc3-bus-stroke)','var(--dc3-be-stroke)','var(--dc3-be-stroke)'][i]" stroke-width="1.5" opacity="0.7"/>
      <text v-for="p in cols" :key="'t'+p" class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle" :x="PX[p]" y="51">{{ s[p] }}</text>
      <!-- note: outbound -->
      <rect x="60" y="86" width="860" height="22" rx="4" fill="var(--vp-c-bg-soft)" stroke="var(--vp-c-divider)" stroke-width="1"/>
      <text fill="var(--dc3-text2)" font-size="9.5" font-weight="600" text-anchor="start" x="74" y="101">{{ s.noteOut }}</text>
      <!-- outbound messages -->
      <line marker-end="url(#dasq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.quartz" :x2="PX.meta" y1="124" y2="124"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.quartz+PX.meta)/2" y="117">{{ s.m1 }}</text>
      <line marker-end="url(#dasq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.meta" :x2="PX.quartz" y1="156" y2="156"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.quartz+PX.meta)/2" y="149">{{ s.m2 }}</text>
      <line marker-end="url(#dasq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.quartz" :x2="PX.impl" y1="188" y2="188"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.quartz+PX.impl)/2" y="181">{{ s.m3 }}</text>
      <line marker-end="url(#dasq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.impl" :x2="PX.quartz" y1="220" y2="220"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.quartz+PX.impl)/2" y="213">{{ s.m4 }}</text>
      <line marker-end="url(#dasq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.quartz" :x2="PX.mq" y1="252" y2="252"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.quartz+PX.mq)/2" y="245">{{ s.m5 }}</text>
      <line marker-end="url(#dasq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.mq" :x2="PX.data" y1="284" y2="284"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.mq+PX.data)/2" y="277">{{ s.m6 }}</text>
      <!-- note: inbound -->
      <rect x="450" y="312" width="470" height="22" rx="4" fill="var(--vp-c-bg-soft)" stroke="var(--vp-c-divider)" stroke-width="1"/>
      <text fill="var(--dc3-text2)" font-size="9.5" font-weight="600" text-anchor="start" x="464" y="327">{{ s.noteIn }}</text>
      <!-- inbound messages -->
      <line marker-end="url(#dasq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.data" :x2="PX.mq" y1="352" y2="352"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.mq+PX.data)/2" y="345">{{ s.m7 }}</text>
      <line marker-end="url(#dasq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.mq" :x2="PX.impl" y1="384" y2="384"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.impl+PX.mq)/2" y="377">{{ s.m8 }}</text>
      <line marker-end="url(#dasq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.impl" :x2="PX.mq" y1="416" y2="416"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.impl+PX.mq)/2" y="409">{{ s.m9 }}</text>
      <line marker-end="url(#dasq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.mq" :x2="PX.data" y1="448" y2="448"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.mq+PX.data)/2" y="441">{{ s.m10 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
