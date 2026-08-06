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
    aria: '平台架构总览',
    client: '调用方 API / Web / CLI / AI Agent',
    platT: '平台层（gRPC facade 互联，tenantId 贯穿）',
    gw: '网关 dc3-gateway :8000',
    gwSub: '唯一 HTTP 入口 + HMAC 签名',
    auth: '鉴权中心',
    mgr: '管理中心',
    data: '数据中心',
    ai: '智能中心',
    edgeT: '接入层 · 南向',
    drv: '协议驱动 dc3-driver-* (28 个)',
    field: '现场设备 / 数据源',
    storeT: '存储与消息',
    mq: 'RabbitMQ（异步解耦）',
    pg: 'PostgreSQL + TimescaleDB',
    e1: 'gRPC facade',
    e2: '上行值 / 下行命令'
  },
  en: {
    aria: 'Platform architecture overview',
    client: 'Caller API / Web / CLI / AI Agent',
    platT: 'Platform (gRPC facade mesh, tenantId throughout)',
    gw: 'Gateway dc3-gateway :8000',
    gwSub: 'sole HTTP entry + HMAC sign',
    auth: 'Auth Center',
    mgr: 'Manager Center',
    data: 'Data Center',
    ai: 'Agentic Center',
    edgeT: 'Edge · southbound',
    drv: 'Protocol drivers dc3-driver-* (28)',
    field: 'Field devices / sources',
    storeT: 'Storage & messaging',
    mq: 'RabbitMQ (async decoupling)',
    pg: 'PostgreSQL + TimescaleDB',
    e1: 'gRPC facade',
    e2: 'uplink values / downlink commands'
  }
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 560">
        <defs>
          <marker id="aif-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/>
          </marker>
        </defs>
        <rect fill="var(--vp-c-bg)" height="45" rx="8" width="240" x="520" y="15"/>
        <rect fill="var(--dc3-ext-fill)" height="45" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="240" x="520"
              y="15"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="640" y="43">{{
            s.client
          }}
        </text>
        <rect fill="var(--dc3-be-fill)" height="170" opacity="0.2" rx="12" stroke="var(--dc3-be-stroke)" stroke-dasharray="6,4" stroke-width="1"
              width="1220" x="30" y="80"/>
        <text fill="var(--dc3-be-text)" font-size="11" font-weight="600" x="46" y="102">{{ s.platT }}</text>
        <line marker-end="url(#aif-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="640" x2="180" y1="60"
              y2="115"/>
        <path d="M290,140 L330,140" fill="none" marker-end="url(#aif-ah)" stroke="var(--dc3-arrow)" stroke-dasharray="5,4"
              stroke-width="1.5"/>
        <path d="M290,150 L540,150" fill="none" marker-end="url(#aif-ah)" stroke="var(--dc3-arrow)" stroke-dasharray="5,4"
              stroke-width="1.5"/>
        <path d="M290,160 L750,160" fill="none" marker-end="url(#aif-ah)" stroke="var(--dc3-arrow)" stroke-dasharray="5,4"
              stroke-width="1.5"/>
        <path d="M290,170 L960,170" fill="none" marker-end="url(#aif-ah)" stroke="var(--dc3-arrow)" stroke-dasharray="5,4"
              stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="320" y="205">{{ s.e1 }}</text>
        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="230" x="60" y="115"/>
        <rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="2" width="230" x="60"
              y="115"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="175"
              y="142">{{ s.gw }}
        </text>
        <text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="175" y="162">{{ s.gwSub }}</text>
        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="190" x="330" y="115"/>
        <rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="190" x="330"
              y="115"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="425" y="155">{{
            s.auth
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="190" x="540" y="115"/>
        <rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="190" x="540"
              y="115"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="635" y="155">{{
            s.mgr
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="190" x="750" y="115"/>
        <rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="2" width="190" x="750"
              y="115"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="845"
              y="155">{{ s.data }}
        </text>
        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="190" x="960" y="115"/>
        <rect fill="var(--dc3-fe-fill)" height="70" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="190" x="960"
              y="115"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="1055" y="155">{{
            s.ai
          }}
        </text>
        <rect fill="var(--dc3-bus-fill)" height="120" opacity="0.2" rx="12" stroke="var(--dc3-bus-stroke)" stroke-dasharray="6,4" stroke-width="1"
              width="1220" x="30" y="280"/>
        <text fill="var(--dc3-bus-text)" font-size="11" font-weight="600" x="46" y="302">{{ s.edgeT }}</text>
        <line marker-end="url(#aif-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="270" x2="370" y1="345"
              y2="345"/>
        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="210" x="60" y="315"/>
        <rect fill="var(--dc3-ext-fill)" height="55" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="210" x="60"
              y="315"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="165" y="347">{{
            s.field
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="280" x="370" y="315"/>
        <rect fill="var(--dc3-bus-fill)" height="55" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="280" x="370"
              y="315"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="510" y="347">{{
            s.drv
          }}
        </text>
        <rect fill="var(--dc3-db-fill)" height="115" opacity="0.18" rx="12" stroke="var(--dc3-db-stroke)" stroke-dasharray="6,4" stroke-width="1"
              width="1220" x="30" y="425"/>
        <text fill="var(--dc3-db-text)" font-size="11" font-weight="600" x="46" y="447">{{ s.storeT }}</text>
        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="220" x="370" y="460"/>
        <rect fill="var(--dc3-bus-fill)" height="55" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="220" x="370"
              y="460"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="480" y="492">{{
            s.mq
          }}
        </text>
        <path d="M780,455 a90,15 0 0 0 200,0 v60 a90,15 0 0 1 -200,0 z" fill="var(--vp-c-bg)"/>
        <path d="M780,455 a90,15 0 0 0 200,0 v60 a90,15 0 0 1 -200,0 z" fill="var(--dc3-db-fill)"
              stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="880" cy="455" fill="none" rx="90" ry="15" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="880" y="495">{{
            s.pg
          }}
        </text>
        <path d="M510,370 L510,460" fill="none" marker-end="url(#aif-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <path d="M470,460 L470,370" fill="none" marker-end="url(#aif-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="560" y="420">{{ s.e2 }}</text>
        <path d="M590,487 L750,170" fill="none" marker-end="url(#aif-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <path d="M845,185 L845,440" fill="none" marker-end="url(#aif-ah)" stroke="var(--dc3-arrow)" stroke-dasharray="5,4"
              stroke-width="1.5"/>
        <path d="M425,185 L425,440" fill="none" opacity="0.5" stroke="var(--dc3-arrow)" stroke-dasharray="3,3"
              stroke-width="1.2"/>
        <path d="M635,185 L635,440" fill="none" opacity="0.5" stroke="var(--dc3-arrow)" stroke-dasharray="3,3"
              stroke-width="1.2"/>
        <path d="M1055,185 L1055,440" fill="none" opacity="0.5" stroke="var(--dc3-arrow)" stroke-dasharray="3,3"
              stroke-width="1.2"/>
      </svg>
    </div>
  </DiagramFrame>
</template>
