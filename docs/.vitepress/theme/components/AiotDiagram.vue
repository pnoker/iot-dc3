<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'AIoT 采集-分析-决策-执行闭环', collect:'采集', collectSub:'位号值流 / 历史库',
    analyze:'分析 / 建模', analyzeSub:'规则 · 统计 · 模型 · 大模型', decide:'决策', decideSub:'越界 / 异常 / 预测 / 建议',
    act:'执行', actSub:'告警 · 通知 · 下发命令', field:'现场设备', human:'运维人员',
    e1:'作用于物理世界', e2:'新的采集值', e3:'人工确认高风险动作'},
  en: {aria:'AIoT collect-analyze-decide-act loop', collect:'Collect', collectSub:'point value stream / history',
    analyze:'Analyze / Model', analyzeSub:'rules · stats · model · LLM', decide:'Decide', decideSub:'threshold / anomaly / predict / advise',
    act:'Act', actSub:'alarm · notify · command', field:'Field device', human:'Operator',
    e1:'acts on physical world', e2:'new samples', e3:'human confirms high-risk'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 340">
      <defs><marker id="ai-ah2" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#ai-ah2)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="220" x2="270" y1="120" y2="120"/>
      <line marker-end="url(#ai-ah2)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="470" x2="520" y1="120" y2="120"/>
      <line marker-end="url(#ai-ah2)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="700" x2="750" y1="120" y2="120"/>
      <line marker-end="url(#ai-ah2)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="930" x2="990" y1="120" y2="120"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="960" y="112">{{ s.e1 }}</text>
      <path d="M1050,165 Q1050,300 110,300 Q110,200 110,165" fill="none" marker-end="url(#ai-ah2)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="580" y="294">{{ s.e2 }}</text>
      <line marker-end="url(#ai-ah2)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" x1="610" x2="610" y1="165" y2="250"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="700" y="212">{{ s.e3 }}</text>
      <line marker-end="url(#ai-ah2)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" x1="700" x2="800" y1="270" y2="165"/>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="180" x="40" y="75"/><rect fill="var(--dc3-fe-fill)" height="90" rx="10" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="40" y="75"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="14" font-weight="700" text-anchor="middle" x="130" y="110">{{ s.collect }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="130" y="132">{{ s.collectSub }}</text>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="200" x="270" y="75"/><rect fill="var(--dc3-amber-fill)" height="90" rx="10" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="200" x="270" y="75"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="370" y="110">{{ s.analyze }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="370" y="132">{{ s.analyzeSub }}</text>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="180" x="520" y="75"/><rect fill="var(--dc3-bus-fill)" height="90" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="180" x="520" y="75"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="14" font-weight="700" text-anchor="middle" x="610" y="110">{{ s.decide }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="610" y="132">{{ s.decideSub }}</text>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="180" x="750" y="75"/><rect fill="var(--dc3-be-fill)" height="90" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="180" x="750" y="75"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="14" font-weight="700" text-anchor="middle" x="840" y="110">{{ s.act }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="840" y="132">{{ s.actSub }}</text>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="120" x="990" y="75"/><rect fill="var(--dc3-ext-fill)" height="90" rx="10" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="120" x="990" y="75"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="1050" y="125">{{ s.field }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="180" x="520" y="250"/><rect fill="var(--dc3-rose-fill)" height="60" rx="8" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" width="180" x="520" y="250"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="610" y="285">{{ s.human }}</text>
    </svg>
  </div></DiagramFrame>
</template>
