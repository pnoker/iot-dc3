<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'开发分层 VO/BO/DO + facade', client:'客户端 (VO 请求/响应)', ctrl:'Controller 只认 VO', svc:'Service 只认 BO', mgr:'Manager / Mapper 只认 DO', db:'PostgreSQL', fac:'*Facade 接口', grpc:'facade-grpc', local:'facade-local-*', other:'其他中心服务', e1:'MapStruct *Builder', e2:'本服务数据', e3:'跨服务必须经 facade', e4:'分布式 grpc', e5:'单体 local', e6:'gRPC'},
  en: {aria:'Dev layering VO/BO/DO + facade', client:'Client (VO req/resp)', ctrl:'Controller (VO only)', svc:'Service (BO only)', mgr:'Manager / Mapper (DO only)', db:'PostgreSQL', fac:'*Facade interface', grpc:'facade-grpc', local:'facade-local-*', other:'Other center service', e1:'MapStruct *Builder', e2:'in-process data', e3:'cross-service via facade', e4:'distributed grpc', e5:'monolith local', e6:'gRPC'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 380">
      <defs><marker id="di2-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#di2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="180" x2="220" y1="100" y2="100"/>
      <line marker-end="url(#di2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="420" x2="460" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="440" y="92">{{ s.e1 }}</text>
      <line marker-end="url(#di2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="660" x2="700" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="680" y="92">{{ s.e2 }}</text>
      <line marker-end="url(#di2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="900" x2="960" y1="100" y2="100"/>
      <line marker-end="url(#di2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="660" x2="700" y1="200" y2="200"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="680" y="192">{{ s.e3 }}</text>
      <line marker-end="url(#di2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" x1="920" x2="960" y1="175" y2="120"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="960" y="142">{{ s.e4 }}</text>
      <line marker-end="url(#di2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" x1="920" x2="960" y1="225" y2="280"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="960" y="260">{{ s.e5 }}</text>
      <line marker-end="url(#di2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1120" x2="1160" y1="120" y2="120"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="1140" y="112">{{ s.e6 }}</text>
      <rect x="20" y="75" width="160" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="20" y="75" width="160" height="50" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="100" y="104">{{ s.client }}</text>
      <rect x="220" y="70" width="200" height="60" rx="8" fill="var(--vp-c-bg)"/><rect x="220" y="70" width="200" height="60" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="320" y="105">{{ s.ctrl }}</text>
      <rect x="460" y="70" width="200" height="60" rx="8" fill="var(--vp-c-bg)"/><rect x="460" y="70" width="200" height="60" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="560" y="105">{{ s.svc }}</text>
      <rect x="700" y="70" width="200" height="60" rx="8" fill="var(--vp-c-bg)"/><rect x="700" y="70" width="200" height="60" rx="8" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="800" y="105">{{ s.mgr }}</text>
      <path d="M960,80 a70,12 0 0 0 140,0 v40 a70,12 0 0 1 -140,0 z" fill="var(--vp-c-bg)"/><path d="M960,80 a70,12 0 0 0 140,0 v40 a70,12 0 0 1 -140,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <ellipse cx="1030" cy="80" fill="none" rx="70" ry="12" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="1030" y="110">{{ s.db }}</text>
      <rect x="700" y="170" width="220" height="60" rx="8" fill="var(--vp-c-bg)"/><rect x="700" y="170" width="220" height="60" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="810" y="205">{{ s.fac }}</text>
      <rect x="960" y="90" width="160" height="60" rx="8" fill="var(--vp-c-bg)"/><rect x="960" y="90" width="160" height="60" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="1040" y="125">{{ s.grpc }}</text>
      <rect x="960" y="255" width="160" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="960" y="255" width="160" height="50" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="1040" y="285">{{ s.local }}</text>
      <rect x="1160" y="90" width="110" height="60" rx="8" fill="var(--vp-c-bg)"/><rect x="1160" y="90" width="110" height="60" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="9.5" text-anchor="middle" x="1215" y="125">{{ s.other }}</text>
    </svg>
  </div></DiagramFrame>
</template>
