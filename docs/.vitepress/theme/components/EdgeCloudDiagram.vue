<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'端-边-云三层架构', endT:'端 · 现场设备', sensor:'传感器 / 执行器', sensorSub:'PLC / 电表',
    edgeT:'边 · 现场就近 (局域网一跳)', gw:'边缘网关', gwSub:'协议适配 / 过滤 / 聚合 / 本地缓存', local:'本地自治', localSub:'断网采集 / 本地联锁 / 边缘告警',
    cloudT:'云 · 数据中心 (广域网)', platform:'云平台', platformSub:'设备管理 / 连接管理', engine:'规则引擎 / 数据服务', twin:'数字孪生 / 全局分析',
    e1:'原始信号 (现场协议)', e2:'聚合 / 脱敏后上行', e3:'下行命令 / 配置', e4:'下发到寄存器'},
  en: {aria:'End-edge-cloud three-tier', endT:'End · field device', sensor:'Sensor / actuator', sensorSub:'PLC / meter',
    edgeT:'Edge · near field (LAN one hop)', gw:'Edge gateway', gwSub:'protocol / filter / aggregate / local cache', local:'Local autonomy', localSub:'offline collect / local interlock / edge alarm',
    cloudT:'Cloud · data center (WAN)', platform:'Cloud platform', platformSub:'device / connection mgmt', engine:'Rule engine / data service', twin:'Digital twin / global analysis',
    e1:'raw signal (field protocol)', e2:'aggregated / desensitized uplink', e3:'downstream command / config', e4:'write to register'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1240 420">
      <defs><marker id="ec-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <!-- end -->
      <rect fill="var(--dc3-ext-fill)" height="340" opacity="0.3" rx="12" width="240" x="20" y="40" stroke="var(--dc3-ext-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-text2)" font-size="11" font-weight="600" x="36" y="62">{{ s.endT }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="190" x="45" y="170"/><rect fill="var(--dc3-ext-fill)" height="80" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="190" x="45" y="170"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="140" y="200">{{ s.sensor }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="140" y="220">{{ s.sensorSub }}</text>
      <!-- edge -->
      <rect fill="var(--dc3-amber-fill)" height="340" opacity="0.3" rx="12" width="340" x="300" y="40" stroke="var(--dc3-amber-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-amber-stroke)" font-size="11" font-weight="600" x="316" y="62">{{ s.edgeT }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="290" x="325" y="85"/><rect fill="var(--dc3-amber-fill)" height="80" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="2" width="290" x="325" y="85"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="470" y="115">{{ s.gw }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="470" y="138">{{ s.gwSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="290" x="325" y="240"/><rect fill="var(--dc3-amber-fill)" height="70" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="290" x="325" y="240"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="470" y="268">{{ s.local }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="470" y="288">{{ s.localSub }}</text>
      <!-- cloud -->
      <rect fill="var(--dc3-be-fill)" height="340" opacity="0.3" rx="12" width="560" x="660" y="40" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-be-text)" font-size="11" font-weight="600" x="676" y="62">{{ s.cloudT }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="220" x="685" y="85"/><rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="2" width="220" x="685" y="85"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="795" y="115">{{ s.platform }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="795" y="138">{{ s.platformSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="990" y="85"/><rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="200" x="990" y="85"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="1090" y="120">{{ s.engine }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="990" y="230"/><rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="200" x="990" y="230"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="1090" y="265">{{ s.twin }}</text>
      <!-- edges -->
      <line marker-end="url(#ec-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="235" x2="325" y1="205" y2="125"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="280" y="160">{{ s.e1 }}</text>
      <line marker-end="url(#ec-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="615" x2="685" y1="125" y2="125"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="650" y="117">{{ s.e2 }}</text>
      <line marker-end="url(#ec-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="905" x2="990" y1="125" y2="125"/>
      <line marker-end="url(#ec-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1090" x2="1090" y1="165" y2="230"/>
      <path d="M685,150 Q500,200 615,165" fill="none" marker-end="url(#ec-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="560" y="200">{{ s.e3 }}</text>
      <path d="M325,165 Q280,210 235,215" fill="none" marker-end="url(#ec-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="280" y="250">{{ s.e4 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
