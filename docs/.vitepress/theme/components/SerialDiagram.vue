<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'串口驱动帧解析', dev:'串口设备', devSub:'RS232 / RS485 / RS422', drvT:'dc3-driver-serial',
    conn:'SerialPortConnection', connSub:'jSerialComm · 每设备一连接',
    parser:'SerialFrameParser', parserSub:'定位帧 / 校验 / 切数据区',
    fmt:'formatValue', fmtSub:'HEX / ASCII / BINARY / FLOAT',
    pv:'位号值 PointValue', e1:'回包原始字节', e2:'写命令 sendCommand ${value}', e3:'HEX 指令字节'},
  en: {aria:'Serial driver frame parsing', dev:'Serial device', devSub:'RS232 / RS485 / RS422', drvT:'dc3-driver-serial',
    conn:'SerialPortConnection', connSub:'jSerialComm · one conn per device',
    parser:'SerialFrameParser', parserSub:'frame locate / validate / slice',
    fmt:'formatValue', fmtSub:'HEX / ASCII / BINARY / FLOAT',
    pv:'PointValue', e1:'raw reply bytes', e2:'write command sendCommand ${value}', e3:'HEX command bytes'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1120 340">
      <defs><marker id="se-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect fill="var(--dc3-bus-fill)" height="270" opacity="0.22" rx="12" width="450" x="290" y="35" stroke="var(--dc3-bus-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-bus-text)" font-size="10" font-weight="600" x="306" y="55">{{ s.drvT }}</text>
      <line marker-end="url(#se-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="210" x2="310" y1="170" y2="110"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="240" y="132">{{ s.e1 }}</text>
      <line marker-end="url(#se-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="500" x2="500" y1="135" y2="165"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="585" y="155">→</text>
      <line marker-end="url(#se-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="500" x2="500" y1="220" y2="250"/>
      <line marker-end="url(#se-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="690" x2="800" y1="275" y2="275"/>
      <path d="M800,255 Q745,180 690,110" fill="none" marker-end="url(#se-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="800" y="180">{{ s.e2 }}</text>
      <path d="M310,140 Q250,170 210,200" fill="none" marker-end="url(#se-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="240" y="220">{{ s.e3 }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="170" x="40" y="130"/>
      <rect fill="var(--dc3-ext-fill)" height="80" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="170" x="40" y="130"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="125" y="162">{{ s.dev }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="125" y="182">{{ s.devSub }}</text>
      <rect fill="var(--vp-c-bg)" height="55" rx="8" width="380" x="310" y="80"/>
      <rect fill="var(--dc3-be-fill)" height="55" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="380" x="310" y="80"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="600" text-anchor="middle" x="500" y="103">{{ s.conn }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="500" y="120">{{ s.connSub }}</text>
      <rect fill="var(--vp-c-bg)" height="55" rx="8" width="380" x="310" y="165"/>
      <rect fill="var(--dc3-amber-fill)" height="55" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="380" x="310" y="165"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="600" text-anchor="middle" x="500" y="188">{{ s.parser }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="500" y="205">{{ s.parserSub }}</text>
      <rect fill="var(--vp-c-bg)" height="55" rx="8" width="380" x="310" y="250"/>
      <rect fill="var(--dc3-amber-fill)" height="55" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="380" x="310" y="250"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="600" text-anchor="middle" x="500" y="273">{{ s.fmt }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="500" y="290">{{ s.fmtSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="180" x="800" y="245"/>
      <rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="800" y="245"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="890" y="280">{{ s.pv }}</text>
    </svg>
  </div></DiagramFrame>
</template>
