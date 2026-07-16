<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'监听虚拟驱动帧解析', dev:'设备 (GPS / 传感终端)', srv:'监听端口', srvSub:'Netty Server',
    parse:'NettyServerHandler', parseSub:'按帧解析',
    devid:'设备 ID 匹配', devidSub:'前 22 字节', key:'关键字 key 比对', keySub:'第 23 字节',
    val:'按位号名解析值', valSub:'载荷 start..end', send:'pointValueSender', pv:'位号值 PointValue',
    e1:'TCP 6270 / UDP 6271'},
  en: {aria:'Listening virtual driver frame parsing', dev:'Device (GPS / sensor)', srv:'Listen port', srvSub:'Netty Server',
    parse:'NettyServerHandler', parseSub:'parse per frame',
    devid:'Device ID match', devidSub:'first 22 bytes', key:'Keyword key compare', keySub:'23rd byte',
    val:'Parse value by point name', valSub:'payload start..end', send:'pointValueSender', pv:'PointValue',
    e1:'TCP 6270 / UDP 6271'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1140 340">
      <defs><marker id="lv-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#lv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="200" x2="280" y1="170" y2="170"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="240" y="162">{{ s.e1 }}</text>
      <line marker-end="url(#lv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="460" x2="560" y1="145" y2="80"/>
      <line marker-end="url(#lv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="460" x2="560" y1="170" y2="170"/>
      <line marker-end="url(#lv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="460" x2="560" y1="195" y2="260"/>
      <line marker-end="url(#lv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="740" x2="840" y1="80" y2="140"/>
      <line marker-end="url(#lv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="740" x2="840" y1="170" y2="170"/>
      <line marker-end="url(#lv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="740" x2="840" y1="260" y2="200"/>
      <line marker-end="url(#lv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="940" x2="1020" y1="170" y2="170"/>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="20" y="135"/>
      <rect fill="var(--dc3-ext-fill)" height="70" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="180" x="20" y="135"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="110" y="165">{{ s.dev }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="280" y="135"/>
      <rect fill="var(--dc3-bus-fill)" height="70" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="180" x="280" y="135"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="370" y="162">{{ s.srv }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="370" y="180">{{ s.srvSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="180" x="280" y="220"/>
      <rect fill="var(--dc3-amber-fill)" height="80" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="180" x="280" y="220"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="370" y="248">{{ s.parse }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="370" y="268">{{ s.parseSub }}</text>
      <rect fill="var(--vp-c-bg)" height="55" rx="8" width="180" x="560" y="52"/>
      <rect fill="var(--dc3-fe-fill)" height="55" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="560" y="52"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="650" y="76">{{ s.devid }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="650" y="93">{{ s.devidSub }}</text>
      <rect fill="var(--vp-c-bg)" height="55" rx="8" width="180" x="560" y="142"/>
      <rect fill="var(--dc3-fe-fill)" height="55" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="560" y="142"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="650" y="166">{{ s.key }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="650" y="183">{{ s.keySub }}</text>
      <rect fill="var(--vp-c-bg)" height="55" rx="8" width="180" x="560" y="232"/>
      <rect fill="var(--dc3-fe-fill)" height="55" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="560" y="232"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="650" y="256">{{ s.val }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="650" y="273">{{ s.valSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="100" x="840" y="135"/>
      <rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="100" x="840" y="135"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="9.5" text-anchor="middle" x="890" y="175">{{ s.send }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="100" x="1020" y="135"/>
      <rect fill="var(--dc3-fe-fill)" height="70" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="100" x="1020" y="135"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="1070" y="175">{{ s.pv }}</text>
    </svg>
  </div></DiagramFrame>
</template>
