<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'AI 接入与鉴权', user:'运营人员', agentic:'Agentic 中心（对话式 / 内置 10 @Tool）', agent:'外部 AI Agent', mcp:'MCP 入口 POST /mcp', gw:'网关 dc3-gateway :8000', rbac:'RBAC + 租户校验 dc3-center-auth', svc:'管理 / 数据 / 智能中心', deny:'拒绝 (403 / 404)', e1:'Bearer access_token', e2:'放行', e3:'越权 / 跨租户'},
  en: {aria:'AI access & auth', user:'Operator', agentic:'Agentic Center (chat / 10 built-in @Tool)', agent:'External AI Agent', mcp:'MCP entry POST /mcp', gw:'Gateway dc3-gateway :8000', rbac:'RBAC + tenant check dc3-center-auth', svc:'Manager / Data / Agentic', deny:'Deny (403 / 404)', e1:'Bearer access_token', e2:'allow', e3:'over-permission / cross-tenant'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1260 340">
      <defs><marker id="aii-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#aii-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="180" x2="250" y1="80" y2="80"/>
      <line marker-end="url(#aii-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="180" x2="250" y1="225" y2="225"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="215" y="217">{{ s.e1 }}</text>
      <path d="M470,95 L560,150" fill="none" marker-end="url(#aii-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
      <path d="M430,215 L560,170" fill="none" marker-end="url(#aii-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
      <line marker-end="url(#aii-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="720" x2="760" y1="160" y2="160"/>
      <line marker-end="url(#aii-ah)" stroke="var(--dc3-be-stroke)" stroke-width="1.5" x1="960" x2="1000" y1="140" y2="110"/><text fill="var(--dc3-be-text)" font-size="9.5" font-weight="600" text-anchor="middle" x="1015" y="95">{{ s.e2 }}</text>
      <line marker-end="url(#aii-ah)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" stroke-dasharray="5,4" x1="960" x2="1000" y1="180" y2="215"/><text fill="var(--dc3-rose-stroke)" font-size="9.5" font-weight="600" text-anchor="middle" x="1015" y="245">{{ s.e3 }}</text>
      <rect x="30" y="55" width="150" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="30" y="55" width="150" height="50" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="105" y="85">{{ s.user }}</text>
      <rect x="250" y="50" width="220" height="65" rx="8" fill="var(--vp-c-bg)"/><rect x="250" y="50" width="220" height="65" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="360" y="80">{{ s.agentic }}</text>
      <rect x="30" y="200" width="150" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="30" y="200" width="150" height="50" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="105" y="230">{{ s.agent }}</text>
      <rect x="250" y="195" width="180" height="60" rx="8" fill="var(--vp-c-bg)"/><rect x="250" y="195" width="180" height="60" rx="8" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="340" y="230">{{ s.mcp }}</text>
      <rect x="560" y="130" width="160" height="60" rx="8" fill="var(--vp-c-bg)"/><rect x="560" y="130" width="160" height="60" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="640" y="165">{{ s.gw }}</text>
      <rect x="760" y="125" width="200" height="70" rx="8" fill="var(--vp-c-bg)"/><rect x="760" y="125" width="200" height="70" rx="8" fill="var(--dc3-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="860" y="165">{{ s.rbac }}</text>
      <rect x="1000" y="75" width="240" height="55" rx="8" fill="var(--vp-c-bg)"/><rect x="1000" y="75" width="240" height="55" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="1120" y="107">{{ s.svc }}</text>
      <rect x="1000" y="200" width="240" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="1000" y="200" width="240" height="50" rx="8" fill="var(--dc3-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-rose-stroke)" font-size="12" font-weight="700" text-anchor="middle" x="1120" y="230">{{ s.deny }}</text>
    </svg>
  </div></DiagramFrame>
</template>
