<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'CoAP 客户端/服务端双模式', client:'客户端模式 CLIENT (默认)', server:'服务端模式 SERVER / BOTH',
    drv1:'dc3-driver-coap', drv1Sub:'CoAP 客户端', dev1:'CoAP 设备', dev1Sub:'资源路径',
    dev2:'CoAP 设备', drv2:'dc3-driver-coap', drv2Sub:'CoAP 服务端',
    e1:'GET readPath 周期采集', e2:'PUT writePath 下发写值', e3:'POST /data 主动上报'},
  en: {aria:'CoAP client/server dual mode', client:'Client mode CLIENT (default)', server:'Server mode SERVER / BOTH',
    drv1:'dc3-driver-coap', drv1Sub:'CoAP client', dev1:'CoAP device', dev1Sub:'resource path',
    dev2:'CoAP device', drv2:'dc3-driver-coap', drv2Sub:'CoAP server',
    e1:'GET readPath periodic', e2:'PUT writePath write', e3:'POST /data push'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 240">
      <defs><marker id="cp-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect fill="var(--dc3-be-fill)" height="160" opacity="0.25" rx="10" width="500" x="20" y="40" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-be-text)" font-size="10" font-weight="600" x="36" y="60">{{ s.client }}</text>
      <rect fill="var(--dc3-amber-fill)" height="160" opacity="0.3" rx="10" width="500" x="560" y="40" stroke="var(--dc3-amber-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-amber-stroke)" font-size="10" font-weight="600" x="576" y="60">{{ s.server }}</text>
      <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="270" x2="330" y1="115" y2="115"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="300" y="107">{{ s.e1 }}</text>
      <path d="M330,135 Q300,165 270,145" fill="none" marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="300" y="180">{{ s.e2 }}</text>
      <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="740" x2="810" y1="115" y2="115"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="775" y="107">{{ s.e3 }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="200" x="60" y="95"/>
      <rect fill="var(--dc3-bus-fill)" height="70" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="2" width="200" x="60" y="95"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="160" y="125">{{ s.drv1 }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="160" y="145">{{ s.drv1Sub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="330" y="95"/>
      <rect fill="var(--dc3-ext-fill)" height="70" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="180" x="330" y="95"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="420" y="125">{{ s.dev1 }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="420" y="145">{{ s.dev1Sub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="570" y="95"/>
      <rect fill="var(--dc3-ext-fill)" height="70" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="180" x="570" y="95"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="660" y="135">{{ s.dev2 }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="200" x="810" y="95"/>
      <rect fill="var(--dc3-bus-fill)" height="70" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="2" width="200" x="810" y="95"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="910" y="125">{{ s.drv2 }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="910" y="145">{{ s.drv2Sub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
