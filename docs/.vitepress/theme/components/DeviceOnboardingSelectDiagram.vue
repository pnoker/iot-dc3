<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'按协议选驱动', start:'设备用什么协议通信？', m1:'Modbus TCP', m2:'Modbus 串口 (RTU)', m3:'OPC UA', m4:'西门子 S7', m5:'MQTT 订阅/发布', m6:'HTTP REST 拉取', m7:'外部推送 (TCP/UDP)', m8:'先跑通链路', m9:'其它协议',
    d1:'dc3-driver-modbus-tcp', d2:'dc3-driver-modbus-rtu', d3:'dc3-driver-opc-ua', d4:'dc3-driver-plcs7', d5:'dc3-driver-mqtt', d6:'dc3-driver-http', d7:'dc3-driver-listening-virtual', d8:'dc3-driver-virtual', d9:'BACnet/IEC104/SNMP/CoAP/CAN/...'},
  en: {aria:'Pick driver by protocol', start:'What protocol does the device use?', m1:'Modbus TCP', m2:'Modbus serial (RTU)', m3:'OPC UA', m4:'Siemens S7', m5:'MQTT pub/sub', m6:'HTTP REST pull', m7:'External push (TCP/UDP)', m8:'Just test the link', m9:'Other protocol',
    d1:'dc3-driver-modbus-tcp', d2:'dc3-driver-modbus-rtu', d3:'dc3-driver-opc-ua', d4:'dc3-driver-plcs7', d5:'dc3-driver-mqtt', d6:'dc3-driver-http', d7:'dc3-driver-listening-virtual', d8:'dc3-driver-virtual', d9:'BACnet/IEC104/SNMP/CoAP/CAN/...'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const items = [['m1','d1'],['m2','d2'],['m3','d3'],['m4','d4'],['m5','d5'],['m6','d6'],['m7','d7'],['m8','d8'],['m9','d9']]
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1100 480">
      <defs><marker id="dos-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <polygon points="180,240 280,200 380,240 280,280" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="280" y="245">{{ s.start }}</text>
      <template v-for="(it,i) in items" :key="i">
        <line marker-end="url(#dos-ah)" stroke="var(--dc3-arrow)" stroke-width="1.2" x1="380" x2="540" :y1="240" :y2="30+i*50"/>
        <rect x="540" :y="10+i*50" width="220" height="40" rx="6" fill="var(--vp-c-bg)" stroke="var(--dc3-fe-stroke)" stroke-width="1"/>
        <rect x="540" :y="10+i*50" width="180" height="40" rx="6" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1"/>
        <text :x="630" :y="28+i*50" class="d-name" fill="var(--dc3-box-name)" font-size="10" text-anchor="middle">{{ s[it[0]] }}</text>
        <text :x="830" :y="35+i*50" fill="var(--dc3-text2)" font-size="9.5">{{ s[it[1]] }}</text>
      </template>
    </svg>
  </div></DiagramFrame>
</template>
