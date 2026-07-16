<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'识别与定位技术选型', need:'需求', needL1:'识别还是定位？', needL2:'室内还是室外？', needL3:'精度与成本预算？',
    idTitle:'自动识别（身份）', bc:'条码 / 二维码', bcSub:'近距 · 极低成本 · 需视线', nfc:'NFC', nfcSub:'约 4cm · 双向 · 手机内置',
    rfl:'RFID LF/HF', rflSub:'厘米级 · 抗干扰', rfu:'RFID UHF', rfuSub:'数米 · 批量盘点 · 怕金属液体',
    posTitle:'定位（位置）', ble:'蓝牙信标', bleSub:'米级 · 区域级 · 部署便宜', uwb:'UWB', uwbSub:'10–30cm · 室内高精度 · 需锚点',
    cell:'基站定位', cellSub:'几十–几百米 · 室内外兜底', gnss:'GNSS（GPS / 北斗）', gnssSub:'米级 · 室外 · 室内失效'},
  en: {aria:'Identification vs positioning', need:'Need', needL1:'identify or locate?', needL2:'indoor or outdoor?', needL3:'accuracy & budget?',
    idTitle:'Identification', bc:'Barcode / QR', bcSub:'close · ultra-cheap · line-of-sight', nfc:'NFC', nfcSub:'~4cm · two-way · phone-built-in',
    rfl:'RFID LF/HF', rflSub:'cm-level · interference-resistant', rfu:'RFID UHF', rfuSub:'meters · bulk scan · metal/liquid averse',
    posTitle:'Positioning', ble:'BLE beacon', bleSub:'meter · zone · cheap deploy', uwb:'UWB', uwbSub:'10–30cm · indoor precise · anchors',
    cell:'Cellular', cellSub:'tens–hundreds of m · indoor/outdoor fallback', gnss:'GNSS (GPS / BeiDou)', gnssSub:'meter · outdoor · fails indoors'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 500">
      <defs><marker id="ic-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#ic-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="220" x2="290" y1="170" y2="150"/>
      <line marker-end="url(#ic-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="220" x2="290" y1="260" y2="360"/>
      <rect fill="var(--vp-c-bg)" height="120" rx="10" width="200" x="20" y="155"/><rect fill="var(--dc3-amber-fill)" height="120" rx="10" stroke="var(--dc3-amber-stroke)" stroke-width="2" width="200" x="20" y="155"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="14" font-weight="700" text-anchor="middle" x="120" y="190">{{ s.need }}</text>
      <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="120" y="215">{{ s.needL1 }}</text>
      <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="120" y="233">{{ s.needL2 }}</text>
      <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="120" y="251">{{ s.needL3 }}</text>
      <!-- ID lane -->
      <rect fill="var(--dc3-fe-fill)" height="220" opacity="0.3" rx="12" width="860" x="290" y="40" stroke="var(--dc3-fe-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-fe-stroke)" font-size="12" font-weight="600" x="306" y="62">{{ s.idTitle }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="180" x="310" y="75"/><rect fill="var(--dc3-fe-fill)" height="50" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="310" y="75"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="400" y="97">{{ s.bc }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="400" y="114">{{ s.bcSub }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="180" x="510" y="75"/><rect fill="var(--dc3-fe-fill)" height="50" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="510" y="75"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="600" y="97">{{ s.nfc }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="600" y="114">{{ s.nfcSub }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="180" x="710" y="75"/><rect fill="var(--dc3-fe-fill)" height="50" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="710" y="75"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="800" y="97">{{ s.rfl }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="800" y="114">{{ s.rflSub }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="180" x="910" y="75"/><rect fill="var(--dc3-fe-fill)" height="50" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="910" y="75"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="1000" y="97">{{ s.rfu }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="1000" y="114">{{ s.rfuSub }}</text>
      <!-- POS lane -->
      <rect fill="var(--dc3-be-fill)" height="220" opacity="0.3" rx="12" width="860" x="290" y="280" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-be-text)" font-size="12" font-weight="600" x="306" y="302">{{ s.posTitle }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="180" x="310" y="315"/><rect fill="var(--dc3-be-fill)" height="50" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="180" x="310" y="315"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="400" y="337">{{ s.ble }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="400" y="354">{{ s.bleSub }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="180" x="510" y="315"/><rect fill="var(--dc3-be-fill)" height="50" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="180" x="510" y="315"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="600" y="337">{{ s.uwb }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="600" y="354">{{ s.uwbSub }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="180" x="710" y="315"/><rect fill="var(--dc3-be-fill)" height="50" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="180" x="710" y="315"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="800" y="337">{{ s.cell }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="800" y="354">{{ s.cellSub }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="180" x="910" y="315"/><rect fill="var(--dc3-be-fill)" height="50" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="180" x="910" y="315"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="1000" y="337">{{ s.gnss }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="1000" y="354">{{ s.gnssSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
