<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'第一个设备端到端流程', login:'登录拿 token', loginSub:'auth/token/salt + generate', profile:'加模板 Profile', profileSub:'manager/profile/add',
    point:'加位号 Point', pointSub:'manager/point/add', device:'加设备 Device', deviceSub:'manager/device/add', config:'配位号属性', configSub:'point_attribute_config/add',
    read:'读实时值', readSub:'data/point_value/latest', write:'下发写命令', writeSub:'data/point_command/write', poll:'轮询命令回执', pollSub:'point_command_history/get_by_command_id'},
  en: {aria:'First device end-to-end', login:'Login for token', loginSub:'auth/token/salt + generate', profile:'Add Profile', profileSub:'manager/profile/add',
    point:'Add Point', pointSub:'manager/point/add', device:'Add Device', deviceSub:'manager/device/add', config:'Config point attr', configSub:'point_attribute_config/add',
    read:'Read latest', readSub:'data/point_value/latest', write:'Issue write', writeSub:'data/point_command/write', poll:'Poll receipt', pollSub:'point_command_history/get_by_command_id'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1440 320">
      <defs><marker id="fd-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#fd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="200" x2="240" y1="120" y2="120"/>
      <line marker-end="url(#fd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="400" x2="440" y1="120" y2="120"/>
      <line marker-end="url(#fd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="600" x2="640" y1="120" y2="120"/>
      <line marker-end="url(#fd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="800" x2="840" y1="120" y2="120"/>
      <line marker-end="url(#fd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1000" x2="1040" y1="95" y2="95"/>
      <line marker-end="url(#fd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1000" x2="1040" y1="170" y2="170"/>
      <line marker-end="url(#fd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1200" x2="1240" y1="170" y2="240"/>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="190" x="20" y="80"/><rect fill="var(--dc3-amber-fill)" height="80" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="190" x="20" y="80"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="115" y="108">{{ s.login }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="115" y="128">{{ s.loginSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="160" x="240" y="80"/><rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="160" x="240" y="80"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="320" y="108">{{ s.profile }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="320" y="128">{{ s.profileSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="160" x="440" y="80"/><rect fill="var(--dc3-fe-fill)" height="80" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="160" x="440" y="80"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="520" y="108">{{ s.point }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="520" y="128">{{ s.pointSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="160" x="640" y="80"/><rect fill="var(--dc3-ext-fill)" height="80" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="160" x="640" y="80"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="720" y="108">{{ s.device }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="720" y="128">{{ s.deviceSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="160" x="840" y="80"/><rect fill="var(--dc3-amber-fill)" height="80" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="160" x="840" y="80"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="920" y="108">{{ s.config }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="920" y="128">{{ s.configSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="1040" y="60"/><rect fill="var(--dc3-fe-fill)" height="70" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="1040" y="60"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="1130" y="88">{{ s.read }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="1130" y="108">{{ s.readSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="160" x="1040" y="135"/><rect fill="var(--dc3-bus-fill)" height="70" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="160" x="1040" y="135"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="1120" y="163">{{ s.write }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="1120" y="183">{{ s.writeSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="1240" y="205"/><rect fill="var(--dc3-fe-fill)" height="70" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="1240" y="205"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="1330" y="233">{{ s.poll }}</text>
    </svg>
  </div></DiagramFrame>
</template>
