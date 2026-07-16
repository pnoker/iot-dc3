<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'请求鉴权与签名校验时序', client:'调用方', gw:'网关 AuthenticGatewayFilter', backend:'后端 GatewayJwtConverter',
    m1:'请求 + X-Auth-Tenant / Login / Token', m2:'解析身份 → PrincipalHeader(tenantId, principalId)', m3:'序列化 X-Auth-Principal (JSON)', m4:'HMAC-SHA256 签名 → X-Auth-Sign',
    m5:'透传 X-Auth-Principal + X-Auth-Sign', m6:'验签 verify(principal, sign)', m7:'提取 principal → @PreAuthorize 判定', m8:'返回统一响应 R'},
  en: {aria:'Auth & signature verification sequence', client:'Caller', gw:'Gateway AuthenticGatewayFilter', backend:'Backend GatewayJwtConverter',
    m1:'request + X-Auth-Tenant / Login / Token', m2:'parse → PrincipalHeader(tenantId, principalId)', m3:'serialize X-Auth-Principal (JSON)', m4:'HMAC-SHA256 sign → X-Auth-Sign',
    m5:'forward X-Auth-Principal + X-Auth-Sign', m6:'verify(principal, sign)', m7:'extract principal → @PreAuthorize', m8:'return unified R'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const PX: Record<string, number> = {client: 140, gw: 450, backend: 800}
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 940 520">
      <defs><marker id="asq-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line v-for="p in ['client','gw','backend']" :key="'l'+p" :x1="PX[p]" :x2="PX[p]" y1="70" y2="480" stroke="var(--dc3-divider)" stroke-width="1" stroke-dasharray="4,4"/>
      <rect v-for="p in ['client','gw','backend']" :key="'b'+p" :x="PX[p]-110" y="22" width="220" height="48" rx="8" fill="var(--dc3-be-fill)" :stroke="p==='gw' ? 'var(--dc3-amber-stroke)' : 'var(--dc3-be-stroke)'" stroke-width="1.5" opacity="0.6"/>
      <text v-for="p in ['client','gw','backend']" :key="'t'+p" :x="PX[p]" y="51" class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle">{{ s[p] }}</text>
      <line marker-end="url(#asq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.client" :x2="PX.gw" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" :x="(PX.client+PX.gw)/2" y="92">{{ s.m1 }}</text>
      <path :d="`M${PX.gw},118 L${PX.gw+55},118 L${PX.gw+55},140 L${PX.gw},140`" fill="none" marker-end="url(#asq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="9" :x="PX.gw+62" y="133">{{ s.m2 }}</text>
      <path :d="`M${PX.gw},158 L${PX.gw+55},158 L${PX.gw+55},180 L${PX.gw},180`" fill="none" marker-end="url(#asq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="9" :x="PX.gw+62" y="173">{{ s.m3 }}</text>
      <path :d="`M${PX.gw},198 L${PX.gw+55},198 L${PX.gw+55},220 L${PX.gw},220`" fill="none" marker-end="url(#asq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="9" :x="PX.gw+62" y="213">{{ s.m4 }}</text>
      <line marker-end="url(#asq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.gw" :x2="PX.backend" y1="248" y2="248"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" :x="(PX.gw+PX.backend)/2" y="240">{{ s.m5 }}</text>
      <path :d="`M${PX.backend},266 L${PX.backend+55},266 L${PX.backend+55},288 L${PX.backend},288`" fill="none" marker-end="url(#asq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="9" :x="PX.backend+62" y="281">{{ s.m6 }}</text>
      <path :d="`M${PX.backend},306 L${PX.backend+55},306 L${PX.backend+55},328 L${PX.backend},328`" fill="none" marker-end="url(#asq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="9" :x="PX.backend+62" y="321">{{ s.m7 }}</text>
      <path :d="`M${PX.backend},355 Q${(PX.backend+PX.client)/2},420 ${PX.client},395`" fill="none" marker-end="url(#asq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" :x="(PX.backend+PX.client)/2" y="430">{{ s.m8 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
