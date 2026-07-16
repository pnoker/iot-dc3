<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'MCP 工具四层过滤', token:'access_token (JWT: principal / scope / tenant / connection_id)', verify:'网关内省校验 introspect (gRPC)', catalog:'工具目录 dc3_mcp_tool_catalog (~330+)',
    f1:'① principal RBAC', f1S:'PermissionProvider.listPermissionCodes', f2:'② MCP 连接白名单', f2S:'dc3_mcp_connection_tool enable_flag=0', f3:'③ 风险策略', f3S:'HIGH 默认隐藏，除非显式开启', f4:'④ OAuth scope', f4S:'mcp:tools:list / call / call:high',
    visible:'可见 / 可调工具集（交集）'},
  en: {aria:'MCP tool four-layer filter', token:'access_token (JWT: principal / scope / tenant / connection_id)', verify:'gateway introspect (gRPC)', catalog:'tool catalog dc3_mcp_tool_catalog (~330+)',
    f1:'① principal RBAC', f1S:'PermissionProvider.listPermissionCodes', f2:'② MCP connection allowlist', f2S:'dc3_mcp_connection_tool enable_flag=0', f3:'③ risk policy', f3S:'HIGH hidden unless explicit', f4:'④ OAuth scope', f4S:'mcp:tools:list / call / call:high',
    visible:'visible / callable tool set (intersection)'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 280">
      <defs><marker id="mff-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="160" x2="160" y1="70" y2="90"/>
      <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="160" x2="160" y1="135" y2="155"/>
      <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="310" x2="390" y1="180" y2="180"/>
      <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="550" x2="570" y1="180" y2="180"/>
      <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="730" x2="750" y1="180" y2="180"/>
      <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="910" x2="930" y1="180" y2="180"/>
      <line marker-end="url(#mff-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1090" x2="1110" y1="180" y2="180"/>
      <rect x="20" y="25" width="280" height="46" rx="8" fill="var(--vp-c-bg)"/><rect x="20" y="25" width="280" height="46" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10" text-anchor="middle" x="160" y="53">{{ s.token }}</text>
      <rect x="20" y="90" width="280" height="46" rx="8" fill="var(--vp-c-bg)"/><rect x="20" y="90" width="280" height="46" rx="8" fill="var(--dc3-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="160" y="118">{{ s.verify }}</text>
      <rect x="20" y="155" width="290" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="20" y="155" width="290" height="50" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="165" y="185">{{ s.catalog }}</text>
      <rect v-for="(f,i) in [['f1','f1S'],['f2','f2S'],['f3','f3S'],['f4','f4S']]" :key="i" :x="390+i*180" y="140" width="160" height="80" rx="8" fill="var(--vp-c-bg)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <rect v-for="(f,i) in [0,1,2,3]" :key="'h'+i" :x="390+i*180" y="140" width="160" height="30" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text v-for="(f,i) in [['f1','f1S'],['f2','f2S'],['f3','f3S'],['f4','f4S']]" :key="'t'+i" :x="470+i*180" y="160" class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle">{{ s[f[0]] }}</text>
      <text v-for="(f,i) in [['f1','f1S'],['f2','f2S'],['f3','f3S'],['f4','f4S']]" :key="'s'+i" :x="470+i*180" y="195" fill="var(--dc3-text2)" font-size="8" text-anchor="middle">{{ s[f[1]] }}</text>
      <rect x="1110" y="140" width="160" height="80" rx="8" fill="var(--vp-c-bg)"/><rect x="1110" y="140" width="160" height="80" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle" x="1190" y="185">{{ s.visible }}</text>
    </svg>
  </div></DiagramFrame>
</template>
