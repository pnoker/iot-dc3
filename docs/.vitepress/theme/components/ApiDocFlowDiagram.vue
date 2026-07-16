<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'聚合 Swagger UI', browser:'浏览器 /swagger-ui.html', gw:'网关 dc3-gateway 聚合 Swagger UI', auth:'鉴权中心 :8300', mgr:'管理中心 :8400', data:'数据中心 :8500', agentic:'智能中心 :8600', e1:'/v3/api-docs/auth', e2:'/v3/api-docs/manager', e3:'/v3/api-docs/data', e4:'/v3/api-docs/agentic'},
  en: {aria:'Aggregated Swagger UI', browser:'Browser /swagger-ui.html', gw:'Gateway aggregates Swagger UI', auth:'Auth :8300', mgr:'Manager :8400', data:'Data :8500', agentic:'Agentic :8600', e1:'/v3/api-docs/auth', e2:'/v3/api-docs/manager', e3:'/v3/api-docs/data', e4:'/v3/api-docs/agentic'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1100 260">
      <defs><marker id="adf-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#adf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="250" x2="360" y1="130" y2="130"/>
      <line marker-end="url(#adf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="580" x2="680" y1="65" y2="65"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="630" y="57">{{ s.e1 }}</text>
      <line marker-end="url(#adf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="580" x2="680" y1="110" y2="110"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="630" y="102">{{ s.e2 }}</text>
      <line marker-end="url(#adf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="580" x2="680" y1="150" y2="150"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="630" y="142">{{ s.e3 }}</text>
      <line marker-end="url(#adf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="580" x2="680" y1="195" y2="195"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="630" y="187">{{ s.e4 }}</text>
      <rect x="30" y="105" width="220" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="30" y="105" width="220" height="50" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="140" y="135">{{ s.browser }}</text>
      <rect x="360" y="100" width="220" height="60" rx="8" fill="var(--vp-c-bg)"/><rect x="360" y="100" width="220" height="60" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="470" y="135">{{ s.gw }}</text>
      <rect x="680" y="40" width="220" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="680" y="40" width="220" height="50" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="790" y="70">{{ s.auth }}</text>
      <rect x="680" y="85" width="220" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="680" y="85" width="220" height="50" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="790" y="115">{{ s.mgr }}</text>
      <rect x="680" y="125" width="220" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="680" y="125" width="220" height="50" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="790" y="155">{{ s.data }}</text>
      <rect x="680" y="170" width="220" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="680" y="170" width="220" height="50" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="790" y="200">{{ s.agentic }}</text>
    </svg>
  </div></DiagramFrame>
</template>
