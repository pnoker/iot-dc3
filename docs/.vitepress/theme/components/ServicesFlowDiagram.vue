<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'服务依赖拓扑', client:'调用方 API / Web / CLI / AI', gw:'网关 dc3-gateway · HTTP :8000', centersT:'中心服务（HTTP 内部 + gRPC facade 互联）', auth:'鉴权中心 :8300 / :9300', mgr:'管理中心 :8400 / :9400', data:'数据中心 :8500 / :9500', ai:'智能中心 :8600', drvT:'接入层 · 南向', drv:'协议驱动 dc3-driver-*（compose 默认 22）', field:'现场设备 / 数据源', infra:'基础依赖 PostgreSQL :5432 / RabbitMQ :5672', e1:'depends_on'},
  en: {aria:'Service dependency topology', client:'Caller API / Web / CLI / AI', gw:'Gateway dc3-gateway · HTTP :8000', centersT:'Centers (internal HTTP + gRPC facade)', auth:'Auth :8300 / :9300', mgr:'Manager :8400 / :9400', data:'Data :8500 / :9500', ai:'Agentic :8600', drvT:'Edge · southbound', drv:'Protocol drivers dc3-driver-* (22 by default)', field:'Field devices / sources', infra:'Infra PostgreSQL :5432 / RabbitMQ :5672', e1:'depends_on'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1300 500">
      <defs><marker id="sf-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect x="540" y="12" width="220" height="40" rx="8" fill="var(--vp-c-bg)"/><rect x="540" y="12" width="220" height="40" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="650" y="37">{{ s.client }}</text>
      <rect x="540" y="65" width="220" height="48" rx="8" fill="var(--vp-c-bg)"/><rect x="540" y="65" width="220" height="48" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="650" y="94">{{ s.gw }}</text>
      <rect x="30" y="140" width="1000" height="145" rx="12" fill="var(--dc3-be-fill)" opacity="0.2" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-be-text)" font-size="10" font-weight="600" x="44" y="160">{{ s.centersT }}</text>
      <line marker-end="url(#sf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="650" x2="150" y1="113" y2="180"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="380" y="140">{{ s.e1 }}</text>
      <line marker-end="url(#sf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.2" opacity="0.5" x1="250" x2="290" y1="220" y2="220"/>
      <line marker-end="url(#sf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.2" opacity="0.5" x1="470" x2="510" y1="220" y2="220"/>
      <line marker-end="url(#sf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.2" opacity="0.5" x1="690" x2="730" y1="220" y2="220"/>
      <rect x="50" y="180" width="200" height="80" rx="8" fill="var(--vp-c-bg)"/><rect x="50" y="180" width="200" height="80" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="150" y="226">{{ s.auth }}</text>
      <rect x="270" y="180" width="200" height="80" rx="8" fill="var(--vp-c-bg)"/><rect x="270" y="180" width="200" height="80" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="370" y="226">{{ s.mgr }}</text>
      <rect x="490" y="180" width="200" height="80" rx="8" fill="var(--vp-c-bg)"/><rect x="490" y="180" width="200" height="80" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="590" y="226">{{ s.data }}</text>
      <rect x="710" y="180" width="200" height="80" rx="8" fill="var(--vp-c-bg)"/><rect x="710" y="180" width="200" height="80" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="810" y="226">{{ s.ai }}</text>
      <rect x="30" y="315" width="1000" height="80" rx="12" fill="var(--dc3-bus-fill)" opacity="0.2" stroke="var(--dc3-bus-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-bus-text)" font-size="10" font-weight="600" x="44" y="335">{{ s.drvT }}</text>
      <line marker-end="url(#sf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="250" x2="320" y1="360" y2="360"/>
      <rect x="50" y="335" width="200" height="48" rx="8" fill="var(--vp-c-bg)"/><rect x="50" y="335" width="200" height="48" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="150" y="364">{{ s.field }}</text>
      <rect x="320" y="335" width="320" height="48" rx="8" fill="var(--vp-c-bg)"/><rect x="320" y="335" width="320" height="48" rx="8" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="480" y="364">{{ s.drv }}</text>
      <line marker-end="url(#sf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="430" x2="380" y1="335" y2="260"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="430" y="300">{{ s.e1 }}</text>
      <rect x="880" y="335" width="390" height="80" rx="10" fill="var(--vp-c-bg)"/><rect x="880" y="335" width="390" height="80" rx="10" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="1075" y="370">{{ s.infra }}</text>
      <path d="M150,260 L150,335" fill="none" stroke="var(--dc3-arrow)" stroke-width="1.2" stroke-dasharray="3,3" opacity="0.5"/>
      <path d="M370,260 L1075,335" fill="none" stroke="var(--dc3-arrow)" stroke-width="1.2" stroke-dasharray="3,3" opacity="0.5"/>
      <path d="M590,260 L1075,335" fill="none" stroke="var(--dc3-arrow)" stroke-width="1.2" stroke-dasharray="3,3" opacity="0.5"/>
      <path d="M810,260 L1075,335" fill="none" stroke="var(--dc3-arrow)" stroke-width="1.2" stroke-dasharray="3,3" opacity="0.5"/>
    </svg>
  </div></DiagramFrame>
</template>
