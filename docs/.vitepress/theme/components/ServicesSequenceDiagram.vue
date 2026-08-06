<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'

const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {
    aria: '服务启动顺序',
    infra: 'PG / RabbitMQ',
    auth: '鉴权中心',
    mgr: '管理中心',
    data: '数据中心',
    ai: '智能中心',
    gw: '网关 dc3-gateway',
    drv: '驱动 dc3-driver-*',
    n0: 'pg_isready / rabbitmq-diagnostics ping 通过',
    m1: '依赖就绪，启动 Auth',
    n1: 'readiness 通过',
    m2: '启动 Manager',
    n2: 'readiness 通过',
    m3: '启动 Data（需 Auth + Manager）',
    n3: 'readiness 通过',
    m4: '启动 Agentic（需 Auth + Manager + Data）',
    n4: 'readiness 通过',
    m5: '四中心齐备，启动 Gateway',
    n5: 'readiness 通过，对外开放 :8000',
    m6: '驱动仅依赖 Manager，注册元数据后调度采集'
  },
  en: {
    aria: 'Service startup order',
    infra: 'PG / RabbitMQ',
    auth: 'Auth',
    mgr: 'Manager',
    data: 'Data',
    ai: 'Agentic',
    gw: 'Gateway dc3-gateway',
    drv: 'Driver dc3-driver-*',
    n0: 'pg_isready / rabbitmq-diagnostics ping ok',
    m1: 'deps ready, start Auth',
    n1: 'readiness ok',
    m2: 'start Manager',
    n2: 'readiness ok',
    m3: 'start Data (needs Auth + Manager)',
    n3: 'readiness ok',
    m4: 'start Agentic (needs Auth + Manager + Data)',
    n4: 'readiness ok',
    m5: 'all 4 centers ready, start Gateway',
    n5: 'readiness ok, opens :8000',
    m6: 'drivers depend only on Manager, register metadata then collect'
  }
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const PX: Record<string, number> = {infra: 105, auth: 275, mgr: 445, data: 615, ai: 785, gw: 955, drv: 1175}
const P = ['infra', 'auth', 'mgr', 'data', 'ai', 'gw', 'drv']
const fill = ['var(--dc3-db-fill)', 'var(--dc3-be-fill)', 'var(--dc3-be-fill)', 'var(--dc3-be-fill)', 'var(--dc3-fe-fill)', 'var(--dc3-be-fill)', 'var(--dc3-bus-fill)']
const stroke = ['var(--dc3-db-stroke)', 'var(--dc3-be-stroke)', 'var(--dc3-be-stroke)', 'var(--dc3-be-stroke)', 'var(--dc3-fe-stroke)', 'var(--dc3-be-stroke)', 'var(--dc3-bus-stroke)']
</script>
<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 560">
        <defs>
          <marker id="ssq-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/>
          </marker>
        </defs>
        <line v-for="p in P" :key="'l'+p" :x1="PX[p]" :x2="PX[p]" stroke="var(--dc3-divider)" stroke-dasharray="4,4" stroke-width="1"
              y1="70" y2="540"/>
        <rect v-for="(p,i) in P" :key="'b'+p" :fill="fill[i]" :stroke="stroke[i]" :x="PX[p]-78" height="48" opacity="0.65" rx="8"
              stroke-width="1.5" width="156" y="22"/>
        <text v-for="p in P" :key="'t'+p" :x="PX[p]" class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700"
              text-anchor="middle" y="51">{{ s[p] }}
        </text>
        <rect :x="PX.infra-78" fill="var(--dc3-amber-fill)" height="28" rx="4" stroke="var(--dc3-amber-stroke)" stroke-width="1"
              width="156" y="82"/>
        <text :x="PX.infra" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle" y="100">{{ s.n0 }}</text>
        <line :x1="PX.infra" :x2="PX.auth" marker-end="url(#ssq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"
              y1="125" y2="125"/>
        <text :x="(PX.infra+PX.auth)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="117">{{
            s.m1
          }}
        </text>
        <rect :x="PX.auth-78" fill="var(--dc3-amber-fill)" height="24" rx="4" stroke="var(--dc3-amber-stroke)" stroke-width="1"
              width="156" y="138"/>
        <text :x="PX.auth" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle" y="154">{{ s.n1 }}</text>
        <line :x1="PX.auth" :x2="PX.mgr" marker-end="url(#ssq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" y1="180"
              y2="180"/>
        <text :x="(PX.auth+PX.mgr)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="172">{{
            s.m2
          }}
        </text>
        <rect :x="PX.mgr-78" fill="var(--dc3-amber-fill)" height="24" rx="4" stroke="var(--dc3-amber-stroke)" stroke-width="1"
              width="156" y="193"/>
        <text :x="PX.mgr" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle" y="209">{{ s.n2 }}</text>
        <line :x1="PX.mgr" :x2="PX.data" marker-end="url(#ssq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" y1="235"
              y2="235"/>
        <text :x="(PX.mgr+PX.data)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="227">{{
            s.m3
          }}
        </text>
        <rect :x="PX.data-78" fill="var(--dc3-amber-fill)" height="24" rx="4" stroke="var(--dc3-amber-stroke)" stroke-width="1"
              width="156" y="248"/>
        <text :x="PX.data" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle" y="264">{{ s.n3 }}</text>
        <line :x1="PX.data" :x2="PX.ai" marker-end="url(#ssq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" y1="290"
              y2="290"/>
        <text :x="(PX.data+PX.ai)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="282">{{
            s.m4
          }}
        </text>
        <rect :x="PX.ai-78" fill="var(--dc3-amber-fill)" height="24" rx="4" stroke="var(--dc3-amber-stroke)" stroke-width="1"
              width="156" y="303"/>
        <text :x="PX.ai" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle" y="319">{{ s.n4 }}</text>
        <line :x1="PX.ai" :x2="PX.gw" marker-end="url(#ssq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" y1="345"
              y2="345"/>
        <text :x="(PX.ai+PX.gw)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="337">{{
            s.m5
          }}
        </text>
        <rect :x="PX.gw-78" fill="var(--dc3-amber-fill)" height="24" rx="4" stroke="var(--dc3-amber-stroke)" stroke-width="1"
              width="156" y="358"/>
        <text :x="PX.gw" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle" y="374">{{ s.n5 }}</text>
        <path :d="`M${PX.mgr},410 Q${(PX.mgr+PX.drv)/2},470 ${PX.drv},430`" fill="none" marker-end="url(#ssq-ah)"
              stroke="var(--dc3-arrow)" stroke-dasharray="5,4" stroke-width="1.5"/>
        <text :x="(PX.mgr+PX.drv)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="475">{{
            s.m6
          }}
        </text>
      </svg>
    </div>
  </DiagramFrame>
</template>
