<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'传感测量链', phy:'物理量', phySub:'温度 / 压力 / 振动', sen:'敏感元件', senSub:'能量转换',
    cond:'信号调理', condSub:'放大 / 滤波 / 线性化', adc:'A/D 转换', adcSub:'采样 + 量化',
    dig:'数字量', digSub:'原始码', eng:'工程值', engSub:'标定 + 换算'},
  en: {aria:'Sensing measurement chain', phy:'Physical quantity', phySub:'temp / pressure / vibration', sen:'Sensing element', senSub:'energy conversion',
    cond:'Signal conditioning', condSub:'amplify / filter / linearize', adc:'A/D conversion', adcSub:'sample + quantize',
    dig:'Digital value', digSub:'raw code', eng:'Engineering value', engSub:'calibrate + convert'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1240 140">
      <defs><marker id="sg-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#sg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="200" x2="220" y1="75" y2="75"/>
      <line marker-end="url(#sg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="400" x2="420" y1="75" y2="75"/>
      <line marker-end="url(#sg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="600" x2="620" y1="75" y2="75"/>
      <line marker-end="url(#sg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="800" x2="820" y1="75" y2="75"/>
      <line marker-end="url(#sg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1000" x2="1020" y1="75" y2="75"/>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="20" y="40"/><rect fill="var(--dc3-ext-fill)" height="70" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="180" x="20" y="40"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="110" y="68">{{ s.phy }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="110" y="88">{{ s.phySub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="220" y="40"/><rect fill="var(--dc3-amber-fill)" height="70" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="180" x="220" y="40"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="310" y="68">{{ s.sen }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="310" y="88">{{ s.senSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="420" y="40"/><rect fill="var(--dc3-amber-fill)" height="70" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="180" x="420" y="40"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="510" y="68">{{ s.cond }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="510" y="88">{{ s.condSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="620" y="40"/><rect fill="var(--dc3-amber-fill)" height="70" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="180" x="620" y="40"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="710" y="68">{{ s.adc }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="710" y="88">{{ s.adcSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="820" y="40"/><rect fill="var(--dc3-fe-fill)" height="70" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="820" y="40"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="910" y="68">{{ s.dig }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="910" y="88">{{ s.digSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="200" x="1020" y="40"/><rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="200" x="1020" y="40"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="1120" y="68">{{ s.eng }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="1120" y="88">{{ s.engSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
