<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'DriverCustomService 聚合 7 接口', main:'DriverCustomService', mainT:'«聚合接口 · 无新方法»',
    i1:'DriverLifecycle', i1M:'initial 启动 / schedule 调度', i2:'DriverMetadataListener', i2M:'元数据 ADD/DELETE/UPDATE',
    i3:'DriverHealth', i3M:'ONLINE/OFFLINE/FAULT/MAINTAIN', i4:'DeviceHealth', i4M:'单设备健康',
    i5:'DriverProtocol', i5M:'read / write', i6:'DriverCommand', i6M:'自定义命令处理', i7:'DriverValidator', i7M:'validate / simulate'},
  en: {aria:'DriverCustomService aggregates 7 interfaces', main:'DriverCustomService', mainT:'«aggregate · no new methods»',
    i1:'DriverLifecycle', i1M:'initial / schedule', i2:'DriverMetadataListener', i2M:'metadata ADD/DELETE/UPDATE',
    i3:'DriverHealth', i3M:'ONLINE/OFFLINE/FAULT/MAINTAIN', i4:'DeviceHealth', i4M:'per-device health',
    i5:'DriverProtocol', i5M:'read / write', i6:'DriverCommand', i6M:'custom command', i7:'DriverValidator', i7M:'validate / simulate'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const left = [['i1',40,40],['i2',40,160],['i3',40,280],['i4',40,400]] as const
const right = [['i5',880,40],['i6',880,160],['i7',880,280]] as const
const all = [...left.map(([k,x,y])=>[s[k],x,y]),...right.map(([k,x,y])=>[s[k],x,y])]
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 500">
      <defs><marker id="mc-ah" markerHeight="8" markerWidth="10" orient="auto" refX="9" refY="4"><path d="M0,0 L9,4 L0,8 L3,4 Z" fill="var(--dc3-arrow)"/></marker></defs>
      <!-- aggregation edges main -> interfaces -->
      <line v-for="(it,idx) in all" :key="'e'+idx" :x1="idx<4?400:760" :x2="it[1]+250" :y1="250" :y2="it[2]+35" stroke="var(--dc3-arrow)" stroke-width="1.3" stroke-dasharray="5,4" marker-end="url(#mc-ah)"/>
      <!-- interfaces -->
      <g v-for="(it,idx) in all" :key="'i'+idx">
        <rect :x="it[1]" :y="it[2]" width="250" height="70" rx="6" fill="var(--vp-c-bg)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
        <rect :x="it[1]" :y="it[2]" width="250" height="26" rx="6" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
        <text :x="it[1]+125" :y="it[2]+18" class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle">{{ it[0] }}</text>
        <text :x="it[1]+125" :y="it[2]+50" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s[['i1M','i2M','i3M','i4M','i5M','i6M','i7M'][idx]] }}</text>
      </g>
      <!-- main hero -->
      <rect x="400" y="200" width="360" height="100" rx="12" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="2.5"/>
      <text x="580" y="226" fill="var(--dc3-amber-stroke)" font-size="9.5" font-style="italic" text-anchor="middle">{{ s.mainT }}</text>
      <text x="580" y="262" class="d-name" fill="var(--dc3-box-name)" font-size="15" font-weight="700" text-anchor="middle">{{ s.main }}</text>
      <text x="580" y="284" fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle">extends 7 interfaces</text>
    </svg>
  </div></DiagramFrame>
</template>
