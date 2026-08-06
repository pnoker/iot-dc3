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
    aria: 'MCP 工具四层过滤',
    token: 'access_token (JWT: principal / scope / tenant / connection_id)',
    verify: '网关内省校验 introspect (gRPC)',
    catalog: '工具目录 dc3_mcp_tool_catalog (~330+)',
    f1: '① principal RBAC',
    f1S: 'PermissionProvider.listPermissionCodes',
    f2: '② MCP 连接白名单',
    f2S: 'dc3_mcp_connection_tool enable_flag=0',
    f3: '③ 风险策略',
    f3S: 'HIGH 默认隐藏，除非显式开启',
    f4: '④ OAuth scope',
    f4S: 'mcp:tools:list / call / call:high',
    visible: '可见 / 可调工具集（交集）'
  },
  en: {
    aria: 'MCP tool four-layer filter',
    token: 'access_token (JWT: principal / scope / tenant / connection_id)',
    verify: 'gateway introspect (gRPC)',
    catalog: 'tool catalog dc3_mcp_tool_catalog (~330+)',
    f1: '① principal RBAC',
    f1S: 'PermissionProvider.listPermissionCodes',
    f2: '② MCP connection allowlist',
    f2S: 'dc3_mcp_connection_tool enable_flag=0',
    f3: '③ risk policy',
    f3S: 'HIGH hidden unless explicit',
    f4: '④ OAuth scope',
    f4S: 'mcp:tools:list / call / call:high',
    visible: 'visible / callable tool set (intersection)'
  }
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 280">
        <defs>
          <marker id="mff-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/>
          </marker>
        </defs>
        <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="160" x2="160" y1="70" y2="90"/>
        <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="160" x2="160" y1="135"
              y2="155"/>
        <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="310" x2="390" y1="180"
              y2="180"/>
        <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="550" x2="570" y1="180"
              y2="180"/>
        <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="730" x2="750" y1="180"
              y2="180"/>
        <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="910" x2="930" y1="180"
              y2="180"/>
        <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1090" x2="1110" y1="180"
              y2="180"/>
        <rect fill="var(--vp-c-bg)" height="46" rx="8" width="280" x="20" y="25"/>
        <rect fill="var(--dc3-ext-fill)" height="46" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="280" x="20"
              y="25"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="10" text-anchor="middle" x="160" y="53">{{
            s.token
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="46" rx="8" width="280" x="20" y="90"/>
        <rect fill="var(--dc3-rose-fill)" height="46" rx="8" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" width="280" x="20"
              y="90"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="160" y="118">{{
            s.verify
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="50" rx="8" width="290" x="20" y="155"/>
        <rect fill="var(--dc3-be-fill)" height="50" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="2" width="290" x="20"
              y="155"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="165" y="185">{{
            s.catalog
          }}
        </text>
        <rect v-for="(f,i) in [['f1','f1S'],['f2','f2S'],['f3','f3S'],['f4','f4S']]" :key="i" :x="390+i*180" fill="var(--vp-c-bg)"
              height="80" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="160" y="140"/>
        <rect v-for="(f,i) in [0,1,2,3]" :key="'h'+i" :x="390+i*180" fill="var(--dc3-amber-fill)" height="30" rx="8" stroke="var(--dc3-amber-stroke)"
              stroke-width="1.5" width="160" y="140"/>
        <text v-for="(f,i) in [['f1','f1S'],['f2','f2S'],['f3','f3S'],['f4','f4S']]" :key="'t'+i" :x="470+i*180" class="d-name"
              fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle" y="160">
          {{ s[f[0]] }}
        </text>
        <text v-for="(f,i) in [['f1','f1S'],['f2','f2S'],['f3','f3S'],['f4','f4S']]" :key="'s'+i" :x="470+i*180" fill="var(--dc3-text2)"
              font-size="8" text-anchor="middle" y="195">{{ s[f[1]] }}
        </text>
        <rect fill="var(--vp-c-bg)" height="80" rx="8" width="160" x="1110" y="140"/>
        <rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="2.5" width="160" x="1110"
              y="140"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle" x="1190"
              y="185">{{ s.visible }}
        </text>
      </svg>
    </div>
  </DiagramFrame>
</template>
