<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'LwM2M 接入', dev:'LwM2M 设备', devSub:'客户端', srv:'内嵌 LwM2M 服务端', srvSub:'Eclipse Leshan',
    reg:'注册表', regSub:'registrations', drv:'LwM2M 驱动', drvSub:'Lwm2mDriver',
    e1:'注册 endpoint 名', e2:'维护 endpoint→Registration', e3:'read: ReadRequest /objectId/instId/resId',
    e4:'write: WriteRequest', e5:'CoAP over UDP 5683'},
  en: {aria:'LwM2M onboarding', dev:'LwM2M device', devSub:'client', srv:'embedded LwM2M server', srvSub:'Eclipse Leshan',
    reg:'Registry', regSub:'registrations', drv:'LwM2M driver', drvSub:'Lwm2mDriver',
    e1:'register endpoint name', e2:'maintain endpoint→Registration', e3:'read: ReadRequest /objectId/instId/resId',
    e4:'write: WriteRequest', e5:'CoAP over UDP 5683'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1120 340">
      <defs><marker id="lm-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#lm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="220" x2="330" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="275" y="92">{{ s.e1 }}</text>
      <line marker-end="url(#lm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="550" x2="660" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="605" y="92">{{ s.e2 }}</text>
      <path d="M440,145 Q440,200 440,235" fill="none" marker-end="url(#lm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="500" y="200">{{ s.e3 }}</text>
      <path d="M470,235 Q510,200 470,145" fill="none" marker-end="url(#lm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="545" y="200">{{ s.e4 }}</text>
      <path d="M330,145 Q200,230 220,140" fill="none" marker-end="url(#lm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="150" y="220">{{ s.e5 }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="20" y="60"/>
      <rect fill="var(--dc3-ext-fill)" height="80" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="200" x="20" y="60"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="120" y="90">{{ s.dev }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="120" y="110">{{ s.devSub }}</text>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="220" x="330" y="55"/>
      <rect fill="var(--dc3-bus-fill)" height="90" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="2" width="220" x="330" y="55"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="440" y="90">{{ s.srv }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="440" y="112">{{ s.srvSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="660" y="65"/>
      <rect fill="var(--dc3-db-fill)" height="70" rx="8" stroke="var(--dc3-db-stroke)" stroke-width="1.5" width="180" x="660" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="750" y="93">{{ s.reg }}</text><text fill="var(--dc3-db-text)" font-size="9" text-anchor="middle" x="750" y="113">{{ s.regSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="220" x="330" y="235"/>
      <rect fill="var(--dc3-fe-fill)" height="80" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="2" width="220" x="330" y="235"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="440" y="268">{{ s.drv }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="440" y="288">{{ s.drvSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
