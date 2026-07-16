<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'OPC UA 客户端接入', server:'OPC UA 服务端 (PLC / SCADA / 网关)', tree:'节点树 Node Tree',
    n1:'节点 ns=2;s=Demo.Static.Float', n2:'节点 ns=2;s=Demo.Static.Bool',
    drv:'dc3-driver-opc-ua', drvSub:'OPC UA 客户端 (Eclipse Milo)', dc:'IoT DC3 数据中心',
    e1:'opc.tcp://host:port read/write', e2:'PointValue 上报'},
  en: {aria:'OPC UA client onboarding', server:'OPC UA Server (PLC / SCADA / gateway)', tree:'Node Tree',
    n1:'node ns=2;s=Demo.Static.Float', n2:'node ns=2;s=Demo.Static.Bool',
    drv:'dc3-driver-opc-ua', drvSub:'OPC UA client (Eclipse Milo)', dc:'IoT DC3 Data Center',
    e1:'opc.tcp://host:port read/write', e2:'PointValue report'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 340">
      <defs><marker id="ou-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect fill="var(--dc3-bus-fill)" height="270" opacity="0.25" rx="12" width="340" x="30" y="40" stroke="var(--dc3-bus-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-bus-text)" font-size="10" font-weight="600" x="46" y="60">{{ s.server }}</text>
      <line marker-end="url(#ou-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="200" x2="200" y1="120" y2="150"/>
      <line marker-end="url(#ou-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="200" x2="200" y1="120" y2="230"/>
      <rect fill="var(--vp-c-bg)" height="46" rx="8" width="260" x="70" y="74"/>
      <rect fill="var(--dc3-fe-fill)" height="46" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="260" x="70" y="74"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="200" y="102">{{ s.tree }}</text>
      <rect fill="var(--vp-c-bg)" height="46" rx="8" width="260" x="70" y="150"/>
      <rect fill="var(--dc3-fe-fill)" height="46" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="260" x="70" y="150"/>
      <text fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="200" y="178">{{ s.n1 }}</text>
      <rect fill="var(--vp-c-bg)" height="46" rx="8" width="260" x="70" y="230"/>
      <rect fill="var(--dc3-fe-fill)" height="46" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="260" x="70" y="230"/>
      <text fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="200" y="258">{{ s.n2 }}</text>
      <line marker-end="url(#ou-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="370" y1="160" y2="175"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="395" y="152">{{ s.e1 }}</text>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="220" x="440" y="125"/>
      <rect fill="var(--dc3-bus-fill)" height="90" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="2" width="220" x="440" y="125"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="550" y="160">{{ s.drv }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="550" y="182">{{ s.drvSub }}</text>
      <line marker-end="url(#ou-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="660" x2="800" y1="170" y2="170"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="730" y="162">{{ s.e2 }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="220" x="800" y="135"/>
      <rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="220" x="800" y="135"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="910" y="176">{{ s.dc }}</text>
    </svg>
  </div></DiagramFrame>
</template>
