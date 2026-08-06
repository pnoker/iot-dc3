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
    aria: 'CLI 接入链路',
    human: '人 / 运维',
    script: '脚本 / CI',
    aicoder: 'AI 编码工具',
    cli: 'dc3 CLI',
    gw: '网关 dc3-gateway :8000',
    rbac: 'RBAC + 租户校验 dc3-center-auth',
    svc: '管理 / 数据中心',
    e1: 'X-Auth-Tenant / Login / Token'
  },
  en: {
    aria: 'CLI access chain',
    human: 'Human / ops',
    script: 'Script / CI',
    aicoder: 'AI coding tool',
    cli: 'dc3 CLI',
    gw: 'Gateway dc3-gateway :8000',
    rbac: 'RBAC + tenant check dc3-center-auth',
    svc: 'Manager / Data Center',
    e1: 'X-Auth-Tenant / Login / Token'
  }
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 220">
        <defs>
          <marker id="aui-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/>
          </marker>
        </defs>
        <line marker-end="url(#aui-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="160" x2="220" y1="45" y2="90"/>
        <line marker-end="url(#aui-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="160" x2="220" y1="100"
              y2="100"/>
        <line marker-end="url(#aui-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="160" x2="220" y1="155"
              y2="115"/>
        <line marker-end="url(#aui-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="360" x2="430" y1="100"
              y2="100"/>
        <text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="395" y="92">{{ s.e1 }}</text>
        <line marker-end="url(#aui-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="610" x2="660" y1="100"
              y2="100"/>
        <line marker-end="url(#aui-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="860" x2="910" y1="100"
              y2="100"/>
        <rect fill="var(--vp-c-bg)" height="42" rx="8" width="140" x="20" y="25"/>
        <rect fill="var(--dc3-ext-fill)" height="42" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="140" x="20"
              y="25"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="90" y="51">{{
            s.human
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="42" rx="8" width="140" x="20" y="80"/>
        <rect fill="var(--dc3-ext-fill)" height="42" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="140" x="20"
              y="80"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="90" y="106">{{
            s.script
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="42" rx="8" width="140" x="20" y="135"/>
        <rect fill="var(--dc3-ext-fill)" height="42" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="140" x="20"
              y="135"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="90" y="161">
          {{ s.aicoder }}
        </text>
        <rect fill="var(--vp-c-bg)" height="90" rx="10" width="140" x="220" y="55"/>
        <rect fill="var(--dc3-amber-fill)" height="90" rx="10" stroke="var(--dc3-amber-stroke)" stroke-width="2" width="140"
              x="220" y="55"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="14" font-weight="700" text-anchor="middle" x="290"
              y="105">{{ s.cli }}
        </text>
        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="180" x="430" y="70"/>
        <rect fill="var(--dc3-be-fill)" height="60" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="180" x="430"
              y="70"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="520" y="105">{{
            s.gw
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="200" x="660" y="70"/>
        <rect fill="var(--dc3-rose-fill)" height="60" rx="8" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" width="200" x="660"
              y="70"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="760" y="105">{{
            s.rbac
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="150" x="910" y="70"/>
        <rect fill="var(--dc3-be-fill)" height="60" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="150" x="910"
              y="70"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="985" y="105">{{
            s.svc
          }}
        </text>
      </svg>
    </div>
  </DiagramFrame>
</template>
