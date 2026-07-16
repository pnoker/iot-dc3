<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'服务启动顺序', infra:'PG / RabbitMQ', auth:'鉴权中心', mgr:'管理中心', data:'数据中心', ai:'智能中心', gw:'网关 dc3-gateway', drv:'驱动 dc3-driver-*',
    n0:'pg_isready / rabbitmq-diagnostics ping 通过', m1:'依赖就绪，启动 Auth', n1:'readiness 通过', m2:'启动 Manager', n2:'readiness 通过', m3:'启动 Data（需 Auth + Manager）', n3:'readiness 通过', m4:'启动 Agentic（需 Auth + Manager + Data）', n4:'readiness 通过', m5:'四中心齐备，启动 Gateway', n5:'readiness 通过，对外开放 :8000', m6:'驱动仅依赖 Manager，注册元数据后调度采集'},
  en: {aria:'Service startup order', infra:'PG / RabbitMQ', auth:'Auth', mgr:'Manager', data:'Data', ai:'Agentic', gw:'Gateway dc3-gateway', drv:'Driver dc3-driver-*',
    n0:'pg_isready / rabbitmq-diagnostics ping ok', m1:'deps ready, start Auth', n1:'readiness ok', m2:'start Manager', n2:'readiness ok', m3:'start Data (needs Auth + Manager)', n3:'readiness ok', m4:'start Agentic (needs Auth + Manager + Data)', n4:'readiness ok', m5:'all 4 centers ready, start Gateway', n5:'readiness ok, opens :8000', m6:'drivers depend only on Manager, register metadata then collect'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const PX: Record<string, number> = {infra: 105, auth: 275, mgr: 445, data: 615, ai: 785, gw: 955, drv: 1175}
const P = ['infra','auth','mgr','data','ai','gw','drv']
const fill = ['var(--dc3-db-fill)','var(--dc3-be-fill)','var(--dc3-be-fill)','var(--dc3-be-fill)','var(--dc3-fe-fill)','var(--dc3-be-fill)','var(--dc3-bus-fill)']
const stroke = ['var(--dc3-db-stroke)','var(--dc3-be-stroke)','var(--dc3-be-stroke)','var(--dc3-be-stroke)','var(--dc3-fe-stroke)','var(--dc3-be-stroke)','var(--dc3-bus-stroke)']
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 560">
      <defs><marker id="ssq-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line v-for="p in P" :key="'l'+p" :x1="PX[p]" :x2="PX[p]" y1="70" y2="540" stroke="var(--dc3-divider)" stroke-width="1" stroke-dasharray="4,4"/>
      <rect v-for="(p,i) in P" :key="'b'+p" :x="PX[p]-78" y="22" width="156" height="48" rx="8" :fill="fill[i]" :stroke="stroke[i]" stroke-width="1.5" opacity="0.65"/>
      <text v-for="p in P" :key="'t'+p" :x="PX[p]" y="51" class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle">{{ s[p] }}</text>
      <rect :x="PX.infra-78" y="82" width="156" height="28" rx="4" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1"/>
      <text :x="PX.infra" y="100" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle">{{ s.n0 }}</text>
      <line marker-end="url(#ssq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.infra" :x2="PX.auth" y1="125" y2="125"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.infra+PX.auth)/2" y="117">{{ s.m1 }}</text>
      <rect :x="PX.auth-78" y="138" width="156" height="24" rx="4" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1"/>
      <text :x="PX.auth" y="154" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle">{{ s.n1 }}</text>
      <line marker-end="url(#ssq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.auth" :x2="PX.mgr" y1="180" y2="180"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.auth+PX.mgr)/2" y="172">{{ s.m2 }}</text>
      <rect :x="PX.mgr-78" y="193" width="156" height="24" rx="4" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1"/>
      <text :x="PX.mgr" y="209" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle">{{ s.n2 }}</text>
      <line marker-end="url(#ssq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.mgr" :x2="PX.data" y1="235" y2="235"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.mgr+PX.data)/2" y="227">{{ s.m3 }}</text>
      <rect :x="PX.data-78" y="248" width="156" height="24" rx="4" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1"/>
      <text :x="PX.data" y="264" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle">{{ s.n3 }}</text>
      <line marker-end="url(#ssq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.data" :x2="PX.ai" y1="290" y2="290"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.data+PX.ai)/2" y="282">{{ s.m4 }}</text>
      <rect :x="PX.ai-78" y="303" width="156" height="24" rx="4" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1"/>
      <text :x="PX.ai" y="319" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle">{{ s.n4 }}</text>
      <line marker-end="url(#ssq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.ai" :x2="PX.gw" y1="345" y2="345"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.ai+PX.gw)/2" y="337">{{ s.m5 }}</text>
      <rect :x="PX.gw-78" y="358" width="156" height="24" rx="4" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1"/>
      <text :x="PX.gw" y="374" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle">{{ s.n5 }}</text>
      <path :d="`M${PX.mgr},410 Q${(PX.mgr+PX.drv)/2},470 ${PX.drv},430`" fill="none" marker-end="url(#ssq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.mgr+PX.drv)/2" y="475">{{ s.m6 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
