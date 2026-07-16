<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'SNMP 管理多网络设备', dc3:'IoT DC3', drv:'dc3-driver-snmp', drvSub:'SNMP Manager (SNMP4J)',
    net:'网络层设备 (SNMP Agent)', sw:'交换机 / 路由器', ups:'UPS / PDU', srv:'服务器网卡',
    e1:'GET oid (UDP 161)', e2:'SET oid (UDP 161)', e3:'response: VariableBinding'},
  en: {aria:'SNMP managing network devices', dc3:'IoT DC3', drv:'dc3-driver-snmp', drvSub:'SNMP Manager (SNMP4J)',
    net:'Network devices (SNMP Agent)', sw:'Switch / Router', ups:'UPS / PDU', srv:'Server NIC',
    e1:'GET oid (UDP 161)', e2:'SET oid (UDP 161)', e3:'response: VariableBinding'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 340">
      <defs><marker id="sn-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect fill="var(--dc3-be-fill)" height="200" opacity="0.22" rx="12" width="240" x="40" y="70" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-be-text)" font-size="10" font-weight="600" x="56" y="90">{{ s.dc3 }}</text>
      <rect fill="var(--dc3-ext-fill)" height="220" opacity="0.3" rx="12" width="320" x="720" y="60" stroke="var(--dc3-ext-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-text2)" font-size="10" font-weight="600" x="736" y="80">{{ s.net }}</text>
      <line marker-end="url(#sn-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="250" x2="720" y1="130" y2="120"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="485" y="112">{{ s.e1 }}</text>
      <line marker-end="url(#sn-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="250" x2="720" y1="170" y2="190"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="485" y="184">{{ s.e2 }}</text>
      <path d="M720,135 Q480,260 250,160" fill="none" marker-end="url(#sn-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="485" y="248">{{ s.e3 }}</text>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="200" x="60" y="120"/>
      <rect fill="var(--dc3-bus-fill)" height="90" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="2" width="200" x="60" y="120"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="160" y="155">{{ s.drv }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="160" y="177">{{ s.drvSub }}</text>
      <rect fill="var(--vp-c-bg)" height="56" rx="8" width="280" x="730" y="95"/>
      <rect fill="var(--dc3-ext-fill)" height="56" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="280" x="730" y="95"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="870" y="128">{{ s.sw }}</text>
      <rect fill="var(--vp-c-bg)" height="56" rx="8" width="280" x="730" y="165"/>
      <rect fill="var(--dc3-ext-fill)" height="56" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="280" x="730" y="165"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="870" y="198">{{ s.ups }}</text>
      <rect fill="var(--vp-c-bg)" height="56" rx="8" width="280" x="730" y="235"/>
      <rect fill="var(--dc3-ext-fill)" height="56" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="280" x="730" y="235"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="870" y="268">{{ s.srv }}</text>
    </svg>
  </div></DiagramFrame>
</template>
