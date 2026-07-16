<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'MQTT 发布/订阅模型', dev1:'设备 A', dev1Sub:'publish device/1001/up', dev2:'设备 B', dev2Sub:'publish device/1002/up',
    broker:'MQTT Broker', brokerSub:'EMQX / Mosquitto', plat:'平台 / 订阅方', platSub:'DC3 MQTT 驱动',
    e1:'publish', e2:'subscribe device/+/up', e3:'publish device/1001/down (QoS/retain)', e4:'下行命令主题'},
  en: {aria:'MQTT publish/subscribe model', dev1:'Device A', dev1Sub:'publish device/1001/up', dev2:'Device B', dev2Sub:'publish device/1002/up',
    broker:'MQTT Broker', brokerSub:'EMQX / Mosquitto', plat:'Platform / subscriber', platSub:'DC3 MQTT driver',
    e1:'publish', e2:'subscribe device/+/up', e3:'publish device/1001/down (QoS/retain)', e4:'downstream command topic'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 320">
      <defs><marker id="im-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#im-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="230" x2="380" y1="80" y2="130"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="300" y="100">{{ s.e1 }}</text>
      <line marker-end="url(#im-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="230" x2="380" y1="200" y2="160"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="300" y="195">{{ s.e1 }}</text>
      <line marker-end="url(#im-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="600" x2="750" y1="130" y2="130"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="675" y="122">{{ s.e2 }}</text>
      <path d="M750,150 Q600,230 380,165" fill="none" marker-end="url(#im-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="600" y="245">{{ s.e3 }}</text>
      <path d="M380,140 Q310,100 230,100" fill="none" marker-end="url(#im-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="300" y="60">{{ s.e4 }}</text>
      <rect fill="var(--vp-c-bg)" height="62" rx="8" width="210" x="20" y="50"/><rect fill="var(--dc3-ext-fill)" height="62" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="210" x="20" y="50"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="125" y="75">{{ s.dev1 }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="125" y="93">{{ s.dev1Sub }}</text>
      <rect fill="var(--vp-c-bg)" height="62" rx="8" width="210" x="20" y="170"/><rect fill="var(--dc3-ext-fill)" height="62" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="210" x="20" y="170"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="125" y="195">{{ s.dev2 }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="125" y="213">{{ s.dev2Sub }}</text>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="220" x="380" y="105"/><rect fill="var(--dc3-bus-fill)" height="90" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="2" width="220" x="380" y="105"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="490" y="140">{{ s.broker }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="490" y="162">{{ s.brokerSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="240" x="750" y="115"/><rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="240" x="750" y="115"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="870" y="148">{{ s.plat }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="870" y="170">{{ s.platSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
