<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'MQTT 上报链路', dev:'现场设备', devSub:'publish device/{id}/up',
    broker:'MQTT Broker', brokerSub:'RabbitMQ MQTT 插件',
    recv:'MqttReceiveServiceImpl', recvSub:'receiveValue() · 按 topic 订阅',
    sender:'DriverSenderService', senderSub:'pointValueSender()',
    event:'事件上报', eventSub:'eventReportSender()',
    dc:'数据中心 dc3-center-data',
    e1:'publish', e2:'subscribe', e3:'解析为 PointValue', e4:'主题匹配 sourceTopic', e5:'转发'},
  en: {aria:'MQTT ingest chain', dev:'Field Device', devSub:'publish device/{id}/up',
    broker:'MQTT Broker', brokerSub:'RabbitMQ MQTT plugin',
    recv:'MqttReceiveServiceImpl', recvSub:'receiveValue() · topic subscribe',
    sender:'DriverSenderService', senderSub:'pointValueSender()',
    event:'Event report', eventSub:'eventReportSender()',
    dc:'Data Center dc3-center-data',
    e1:'publish', e2:'subscribe', e3:'parse to PointValue', e4:'topic matches sourceTopic', e5:'forward'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1120 360">
      <defs><marker id="mq-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#mq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="180" x2="280" y1="110" y2="110"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="230" y="102">{{ s.e1 }}</text>
      <line marker-end="url(#mq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="460" x2="560" y1="110" y2="110"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="510" y="102">{{ s.e2 }}</text>
      <line marker-end="url(#mq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="740" x2="840" y1="95" y2="95"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="790" y="87">{{ s.e3 }}</text>
      <line marker-end="url(#mq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="650" x2="650" y1="160" y2="250"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="730" y="210">{{ s.e4 }}</text>
      <line marker-end="url(#mq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1020" x2="1020" y1="130" y2="250"/>
      <line marker-end="url(#mq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="930" x2="1020" y1="280" y2="280"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="975" y="272">{{ s.e5 }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="20" y="75"/>
      <rect fill="var(--dc3-ext-fill)" height="70" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="180" x="20" y="75"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="110" y="105">{{ s.dev }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="110" y="124">{{ s.devSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="280" y="75"/>
      <rect fill="var(--dc3-bus-fill)" height="70" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="180" x="280" y="75"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="370" y="105">{{ s.broker }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="370" y="124">{{ s.brokerSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="180" x="560" y="80"/>
      <rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="2" width="180" x="560" y="80"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="650" y="112">{{ s.recv }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="650" y="134">{{ s.recvSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="840" y="60"/>
      <rect fill="var(--dc3-fe-fill)" height="70" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="840" y="60"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="930" y="88">{{ s.sender }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="930" y="106">{{ s.senderSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="180" x="560" y="250"/>
      <rect fill="var(--dc3-amber-fill)" height="60" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="180" x="560" y="250"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="650" y="276">{{ s.event }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="650" y="294">{{ s.eventSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="180" x="930" y="250"/>
      <rect fill="var(--dc3-be-fill)" height="60" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="180" x="930" y="250"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="1020" y="285">{{ s.dc }}</text>
    </svg>
  </div></DiagramFrame>
</template>
