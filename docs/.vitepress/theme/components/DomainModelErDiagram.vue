<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'DC3 完整领域模型', profile:'模板 Profile', point:'位号 Point', command:'指令 Command', event:'事件 Event',
    device:'设备 Device', driver:'驱动 Driver', da:'DriverAttribute', pa:'PointAttribute', pac:'PointAttributeConfig',
    e1:'包含位号', e2:'包含命令', e3:'包含事件', e4:'绑定 profileId', e5:'由驱动接入', e6:'注册驱动属性', e7:'注册位号属性', e8:'被实例化', e9:'按位号填值', e10:'为属性填值'},
  en: {aria:'DC3 full domain model', profile:'Profile', point:'Point', command:'Command', event:'Event',
    device:'Device', driver:'Driver', da:'DriverAttribute', pa:'PointAttribute', pac:'PointAttributeConfig',
    e1:'has points', e2:'has commands', e3:'has events', e4:'binds profileId', e5:'via driver', e6:'registers driver attrs', e7:'registers point attrs', e8:'instantiated', e9:'per point', e10:'per device'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 540">
      <defs><marker id="er2-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker>
      <filter id="er2-g" x="-40%" y="-40%" width="180%" height="180%"><feGaussianBlur stdDeviation="7"/></filter></defs>
      <!-- Profile -> Point/Command/Event -->
      <line marker-end="url(#er2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="500" x2="330" y1="250" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="380" y="165">{{ s.e1 }}</text>
      <line marker-end="url(#er2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="500" x2="330" y1="280" y2="262"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="400" y="262">{{ s.e2 }}</text>
      <line marker-end="url(#er2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="500" x2="330" y1="310" y2="425"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="380" y="385">{{ s.e3 }}</text>
      <!-- Device -> Profile / Driver -->
      <line marker-end="url(#er2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="810" x2="710" y1="135" y2="250"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="785" y="180">{{ s.e4 }}</text>
      <line marker-end="url(#er2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="900" x2="900" y1="170" y2="360"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="955" y="272">{{ s.e5 }}</text>
      <!-- Driver -> DA / PA -->
      <line marker-end="url(#er2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="990" x2="1060" y1="390" y2="335"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="1055" y="380">{{ s.e6 }}</text>
      <line marker-end="url(#er2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="990" x2="1060" y1="420" y2="445"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="1055" y="420">{{ s.e7 }}</text>
      <!-- PA -> PAC / Point -> PAC / Device -> PAC -->
      <path d="M1150,420 Q1230,260 1180,170" fill="none" marker-end="url(#er2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="1235" y="300">{{ s.e8 }}</text>
      <path d="M240,100 Q650,40 1060,120" fill="none" marker-end="url(#er2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="650" y="48">{{ s.e9 }}</text>
      <line marker-end="url(#er2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="990" x2="1060" y1="135" y2="135"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="1025" y="127">{{ s.e10 }}</text>
      <!-- Point/Command/Event -->
      <rect x="150" y="70" width="180" height="56" rx="8" fill="var(--vp-c-bg)"/><rect x="150" y="70" width="180" height="56" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="240" y="103">{{ s.point }}</text>
      <rect x="150" y="232" width="180" height="56" rx="8" fill="var(--vp-c-bg)"/><rect x="150" y="232" width="180" height="56" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="240" y="265">{{ s.command }}</text>
      <rect x="150" y="395" width="180" height="56" rx="8" fill="var(--vp-c-bg)"/><rect x="150" y="395" width="180" height="56" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="240" y="428">{{ s.event }}</text>
      <!-- Profile HERO -->
      <rect x="490" y="225" width="220" height="100" rx="12" fill="var(--dc3-be-stroke)" opacity="0.2" filter="url(#er2-g)"/>
      <rect x="500" y="235" width="200" height="80" rx="10" fill="var(--vp-c-bg)"/><rect x="500" y="235" width="200" height="80" rx="10" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="15" font-weight="700" text-anchor="middle" x="600" y="280">{{ s.profile }}</text>
      <!-- Device / Driver -->
      <rect x="810" y="100" width="180" height="70" rx="8" fill="var(--vp-c-bg)"/><rect x="810" y="100" width="180" height="70" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="900" y="140">{{ s.device }}</text>
      <rect x="810" y="360" width="180" height="70" rx="8" fill="var(--vp-c-bg)"/><rect x="810" y="360" width="180" height="70" rx="8" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="900" y="400">{{ s.driver }}</text>
      <!-- DA / PA / PAC -->
      <rect x="1060" y="305" width="180" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="1060" y="305" width="180" height="50" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="1150" y="335">{{ s.da }}</text>
      <rect x="1060" y="415" width="180" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="1060" y="415" width="180" height="50" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="1150" y="445">{{ s.pa }}</text>
      <rect x="1060" y="100" width="200" height="70" rx="8" fill="var(--vp-c-bg)"/><rect x="1060" y="100" width="200" height="70" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="1160" y="140">{{ s.pac }}</text>
    </svg>
  </div></DiagramFrame>
</template>
