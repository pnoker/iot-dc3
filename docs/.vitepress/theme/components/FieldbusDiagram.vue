<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'工业协议分类', root:'工业协议 · 按领域与模型分类',
    plc:'PLC / 现场总线', plcSub:'主从 · 请求-响应', scada:'SCADA / 电力 · 楼宇', scadaSub:'客户端-服务器 / 主站轮询',
    meter:'计量 / 能源', meterSub:'抄表 · 多为只读', bus:'嵌入式总线 / 广播', busSub:'发布-订阅',
    modbus:'Modbus RTU/TCP', modbusSub:'寄存器 + 功能码', s7:'Siemens S7', s7Sub:'DB/区地址', melsec:'Mitsubishi MELSEC', melsecSub:'软元件地址', fins:'Omron FINS', finsSub:'内存区地址', eip:'EtherNet/IP (CIP)', eipSub:'标签名寻址',
    opcua:'OPC UA', opcuaSub:'NodeId + 订阅', opcda:'OPC DA', opcdaSub:'ItemId (Windows)', iec104:'IEC 60870-5-104', iec104Sub:'IOA + ASDU', bacnet:'BACnet/IP', bacnetSub:'对象 + 属性',
    dlms:'DLMS/COSEM', dlmsSub:'OBIS 编码', can:'CAN', canSub:'CAN ID 广播'},
  en: {aria:'Industrial protocol taxonomy', root:'Industrial protocols · by domain & model',
    plc:'PLC / fieldbus', plcSub:'master-slave · req-resp', scada:'SCADA / power · building', scadaSub:'client-server / master poll',
    meter:'Metering / energy', meterSub:'reading · mostly read-only', bus:'Embedded bus / broadcast', busSub:'pub-sub',
    modbus:'Modbus RTU/TCP', modbusSub:'register + function code', s7:'Siemens S7', s7Sub:'DB/area addr', melsec:'Mitsubishi MELSEC', melsecSub:'device addr', fins:'Omron FINS', finsSub:'memory area addr', eip:'EtherNet/IP (CIP)', eipSub:'tag name',
    opcua:'OPC UA', opcuaSub:'NodeId + subscribe', opcda:'OPC DA', opcdaSub:'ItemId (Windows)', iec104:'IEC 60870-5-104', iec104Sub:'IOA + ASDU', bacnet:'BACnet/IP', bacnetSub:'object + property',
    dlms:'DLMS/COSEM', dlmsSub:'OBIS code', can:'CAN', canSub:'CAN ID broadcast'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1200 560">
      <defs><marker id="fb-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect fill="var(--vp-c-bg)" height="50" rx="25" width="280" x="460" y="20"/><rect fill="var(--dc3-ext-fill)" height="50" rx="25" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="280" x="460" y="20"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="600" y="51">{{ s.root }}</text>
      <line marker-end="url(#fb-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="600" x2="180" y1="70" y2="120"/>
      <line marker-end="url(#fb-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="600" x2="480" y1="70" y2="120"/>
      <line marker-end="url(#fb-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="600" x2="780" y1="70" y2="120"/>
      <line marker-end="url(#fb-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="600" x2="1050" y1="70" y2="120"/>
      <!-- category headers -->
      <rect fill="var(--vp-c-bg)" height="56" rx="8" width="240" x="60" y="120"/><rect fill="var(--dc3-amber-fill)" height="56" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="240" x="60" y="120"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="180" y="144">{{ s.plc }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="180" y="162">{{ s.plcSub }}</text>
      <rect fill="var(--vp-c-bg)" height="56" rx="8" width="240" x="360" y="120"/><rect fill="var(--dc3-bus-fill)" height="56" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="240" x="360" y="120"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="480" y="144">{{ s.scada }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="480" y="162">{{ s.scadaSub }}</text>
      <rect fill="var(--vp-c-bg)" height="56" rx="8" width="200" x="680" y="120"/><rect fill="var(--dc3-db-fill)" height="56" rx="8" stroke="var(--dc3-db-stroke)" stroke-width="1.5" width="200" x="680" y="120"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="780" y="144">{{ s.meter }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="780" y="162">{{ s.meterSub }}</text>
      <rect fill="var(--vp-c-bg)" height="56" rx="8" width="220" x="940" y="120"/><rect fill="var(--dc3-be-fill)" height="56" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="220" x="940" y="120"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="1050" y="144">{{ s.bus }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="1050" y="162">{{ s.busSub }}</text>
      <!-- PLC leaves -->
      <line stroke="var(--dc3-arrow)" stroke-width="1.2" x1="180" y1="176" x2="180" y2="210"/>
      <rect fill="var(--vp-c-bg)" height="42" rx="6" width="220" x="70" y="210"/><rect fill="var(--dc3-fe-fill)" height="42" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="220" x="70" y="210"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="180" y="228">{{ s.modbus }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="180" y="244">{{ s.modbusSub }}</text>
      <rect fill="var(--vp-c-bg)" height="42" rx="6" width="220" x="70" y="262"/><rect fill="var(--dc3-fe-fill)" height="42" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="220" x="70" y="262"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="180" y="280">{{ s.s7 }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="180" y="296">{{ s.s7Sub }}</text>
      <rect fill="var(--vp-c-bg)" height="42" rx="6" width="220" x="70" y="314"/><rect fill="var(--dc3-fe-fill)" height="42" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="220" x="70" y="314"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="180" y="332">{{ s.melsec }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="180" y="348">{{ s.melsecSub }}</text>
      <rect fill="var(--vp-c-bg)" height="42" rx="6" width="220" x="70" y="366"/><rect fill="var(--dc3-fe-fill)" height="42" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="220" x="70" y="366"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="180" y="384">{{ s.fins }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="180" y="400">{{ s.finsSub }}</text>
      <rect fill="var(--vp-c-bg)" height="42" rx="6" width="220" x="70" y="418"/><rect fill="var(--dc3-fe-fill)" height="42" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="220" x="70" y="418"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="180" y="436">{{ s.eip }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="180" y="452">{{ s.eipSub }}</text>
      <!-- SCADA leaves -->
      <line stroke="var(--dc3-arrow)" stroke-width="1.2" x1="480" y1="176" x2="480" y2="210"/>
      <rect fill="var(--vp-c-bg)" height="42" rx="6" width="220" x="370" y="210"/><rect fill="var(--dc3-fe-fill)" height="42" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="220" x="370" y="210"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="480" y="228">{{ s.opcua }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="480" y="244">{{ s.opcuaSub }}</text>
      <rect fill="var(--vp-c-bg)" height="42" rx="6" width="220" x="370" y="262"/><rect fill="var(--dc3-fe-fill)" height="42" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="220" x="370" y="262"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="480" y="280">{{ s.opcda }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="480" y="296">{{ s.opcdaSub }}</text>
      <rect fill="var(--vp-c-bg)" height="42" rx="6" width="220" x="370" y="314"/><rect fill="var(--dc3-fe-fill)" height="42" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="220" x="370" y="314"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="480" y="332">{{ s.iec104 }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="480" y="348">{{ s.iec104Sub }}</text>
      <rect fill="var(--vp-c-bg)" height="42" rx="6" width="220" x="370" y="366"/><rect fill="var(--dc3-fe-fill)" height="42" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="220" x="370" y="366"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="480" y="384">{{ s.bacnet }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="480" y="400">{{ s.bacnetSub }}</text>
      <!-- Meter / Bus leaves -->
      <line stroke="var(--dc3-arrow)" stroke-width="1.2" x1="780" y1="176" x2="780" y2="210"/>
      <rect fill="var(--vp-c-bg)" height="42" rx="6" width="200" x="680" y="210"/><rect fill="var(--dc3-fe-fill)" height="42" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="200" x="680" y="210"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="780" y="228">{{ s.dlms }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="780" y="244">{{ s.dlmsSub }}</text>
      <line stroke="var(--dc3-arrow)" stroke-width="1.2" x1="1050" y1="176" x2="1050" y2="210"/>
      <rect fill="var(--vp-c-bg)" height="42" rx="6" width="200" x="950" y="210"/><rect fill="var(--dc3-fe-fill)" height="42" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="200" x="950" y="210"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="1050" y="228">{{ s.can }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="1050" y="244">{{ s.canSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
