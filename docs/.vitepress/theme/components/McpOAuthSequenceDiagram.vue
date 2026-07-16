<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'MCP OAuth 2.1 授权时序', agent:'AI Agent', auth:'鉴权中心 dc3-center-auth', gw:'网关 dc3-gateway (/mcp)', backend:'后端 manager / data / agentic',
    m1:'GET /oauth2/authorize (Auth Code + PKCE)', m2:'authorization_code', m3:'POST /oauth2/token (code + code_verifier)', m4:'access_token + refresh_token',
    m5:'POST /mcp 携带 Bearer (tools/call)', m6:'introspect (gRPC) 校验 JWT + 连接', m7:'tenantId · principalId · connectionId · scopes · active', m8:'重新校验 RBAC ∩ 白名单 ∩ 风险 ∩ confirm',
    m9:'内部 POST + X-Auth-Principal + HMAC', m10:'统一响应 R', m11:'MCP CallToolResult'},
  en: {aria:'MCP OAuth 2.1 authorization sequence', agent:'AI Agent', auth:'Auth dc3-center-auth', gw:'Gateway dc3-gateway (/mcp)', backend:'Backend manager / data / agentic',
    m1:'GET /oauth2/authorize (Auth Code + PKCE)', m2:'authorization_code', m3:'POST /oauth2/token (code + code_verifier)', m4:'access_token + refresh_token',
    m5:'POST /mcp with Bearer (tools/call)', m6:'introspect (gRPC) verify JWT + connection', m7:'tenantId · principalId · connectionId · scopes · active', m8:'re-check RBAC ∩ allowlist ∩ risk ∩ confirm',
    m9:'internal POST + X-Auth-Principal + HMAC', m10:'unified R response', m11:'MCP CallToolResult'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const PX: Record<string, number> = {agent: 110, auth: 370, gw: 620, backend: 870}
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 980 480">
      <defs><marker id="mos-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line v-for="p in ['agent','auth','gw','backend']" :key="'l'+p" :x1="PX[p]" :x2="PX[p]" y1="70" y2="455" stroke="var(--dc3-divider)" stroke-width="1" stroke-dasharray="4,4"/>
      <rect v-for="(p,i) in ['agent','auth','gw','backend']" :key="'b'+p" :x="PX[p]-95" y="22" width="190" height="48" rx="8" :fill="['var(--dc3-ext-fill)','var(--dc3-rose-fill)','var(--dc3-be-fill)','var(--dc3-fe-fill)'][i]" :stroke="['var(--dc3-ext-stroke)','var(--dc3-rose-stroke)','var(--dc3-be-stroke)','var(--dc3-fe-stroke)'][i]" stroke-width="1.5" opacity="0.65"/>
      <text v-for="p in ['agent','auth','gw','backend']" :key="'t'+p" :x="PX[p]" y="51" class="d-name" fill="var(--dc3-box-name)" font-size="10" font-weight="700" text-anchor="middle">{{ s[p] }}</text>
      <line marker-end="url(#mos-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.agent" :x2="PX.auth" y1="95" y2="95"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.agent+PX.auth)/2" y="87">{{ s.m1 }}</text>
      <line marker-end="url(#mos-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.auth" :x2="PX.agent" y1="125" y2="125"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.auth+PX.agent)/2" y="117">{{ s.m2 }}</text>
      <line marker-end="url(#mos-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.agent" :x2="PX.auth" y1="158" y2="158"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.agent+PX.auth)/2" y="150">{{ s.m3 }}</text>
      <line marker-end="url(#mos-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.auth" :x2="PX.agent" y1="188" y2="188"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.auth+PX.agent)/2" y="180">{{ s.m4 }}</text>
      <line marker-end="url(#mos-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.agent" :x2="PX.gw" y1="225" y2="225"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.agent+PX.gw)/2" y="217">{{ s.m5 }}</text>
      <line marker-end="url(#mos-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.gw" :x2="PX.auth" y1="258" y2="258"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.gw+PX.auth)/2" y="250">{{ s.m6 }}</text>
      <line marker-end="url(#mos-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.auth" :x2="PX.gw" y1="288" y2="288"/><text fill="var(--dc3-arrow-label)" font-size="8" text-anchor="middle" :x="(PX.auth+PX.gw)/2" y="280">{{ s.m7 }}</text>
      <path :d="`M${PX.gw},305 L${PX.gw+50},305 L${PX.gw+50},325 L${PX.gw},325`" fill="none" marker-end="url(#mos-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
      <text :x="690" y="320" fill="var(--dc3-arrow-label)" font-size="8">{{ s.m8 }}</text>
      <line marker-end="url(#mos-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.gw" :x2="PX.backend" y1="355" y2="355"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.gw+PX.backend)/2" y="347">{{ s.m9 }}</text>
      <line marker-end="url(#mos-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.backend" :x2="PX.gw" y1="385" y2="385"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.backend+PX.gw)/2" y="377">{{ s.m10 }}</text>
      <path :d="`M${PX.gw},415 Q${(PX.gw+PX.agent)/2},450 ${PX.agent},425`" fill="none" marker-end="url(#mos-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.gw+PX.agent)/2" y="445">{{ s.m11 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
