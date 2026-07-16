<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'取盐换 token 时序', client:'客户端', gw:'网关 dc3-gateway', auth:'鉴权中心 dc3-center-auth',
    m1:'取盐 (tenant, name)', m2:'转发取盐请求', m3:'salt 字符串 (5 分钟内使用)', note1:'密码无需本地哈希，明文提交 (依赖 HTTPS)', m4:'换 token (tenant, name, salt, 明文密码)', m5:'转发生成 token', m6:'access token (12h)', m7:'POST /api/v3/... 带 X-Auth-*', m8:'校验鉴权头 + 注入 principal', m9:'租户隔离 + 权限校验', m10:'统一响应 R'},
  en: {aria:'Salt & token sequence', client:'Client', gw:'Gateway dc3-gateway', auth:'Auth dc3-center-auth',
    m1:'get salt (tenant, name)', m2:'forward salt request', m3:'salt string (use within 5 min)', note1:'no local hash; plaintext over HTTPS', m4:'exchange token (tenant, name, salt, password)', m5:'forward token gen', m6:'access token (12h)', m7:'POST /api/v3/... with X-Auth-*', m8:'verify auth headers + inject principal', m9:'tenant isolation + permission', m10:'unified R'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const PX: Record<string, number> = {client: 160, gw: 480, auth: 820}
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 980 460">
      <defs><marker id="ads-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line v-for="p in ['client','gw','auth']" :key="'l'+p" :x1="PX[p]" :x2="PX[p]" y1="70" y2="440" stroke="var(--dc3-divider)" stroke-width="1" stroke-dasharray="4,4"/>
      <rect v-for="(p,i) in ['client','gw','auth']" :key="'b'+p" :x="PX[p]-120" y="22" width="240" height="48" rx="8" :fill="['var(--dc3-ext-fill)','var(--dc3-be-fill)','var(--dc3-rose-fill)'][i]" :stroke="['var(--dc3-ext-stroke)','var(--dc3-be-stroke)','var(--dc3-rose-stroke)'][i]" stroke-width="1.5" opacity="0.65"/>
      <text v-for="p in ['client','gw','auth']" :key="'t'+p" :x="PX[p]" y="51" class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle">{{ s[p] }}</text>
      <line marker-end="url(#ads-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.client" :x2="PX.gw" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.client+PX.gw)/2" y="92">{{ s.m1 }}</text>
      <line marker-end="url(#ads-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.gw" :x2="PX.auth" y1="130" y2="130"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.gw+PX.auth)/2" y="122">{{ s.m2 }}</text>
      <line marker-end="url(#ads-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.auth" :x2="PX.client" y1="160" y2="160"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.auth+PX.client)/2" y="152">{{ s.m3 }}</text>
      <rect :x="PX.client-100" y="175" width="200" height="24" rx="4" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1"/>
      <text :x="PX.client" y="192" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle">{{ s.note1 }}</text>
      <line marker-end="url(#ads-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.client" :x2="PX.gw" y1="220" y2="220"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.client+PX.gw)/2" y="212">{{ s.m4 }}</text>
      <line marker-end="url(#ads-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.gw" :x2="PX.auth" y1="250" y2="250"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.gw+PX.auth)/2" y="242">{{ s.m5 }}</text>
      <line marker-end="url(#ads-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.auth" :x2="PX.client" y1="280" y2="280"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.auth+PX.client)/2" y="272">{{ s.m6 }}</text>
      <line marker-end="url(#ads-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.client" :x2="PX.gw" y1="320" y2="320"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.client+PX.gw)/2" y="312">{{ s.m7 }}</text>
      <path :d="`M${PX.gw},335 L${PX.gw+50},335 L${PX.gw+50},355 L${PX.gw},355`" fill="none" marker-end="url(#ads-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="8.5" :x="PX.gw+58" y="350">{{ s.m8 }}</text>
      <line marker-end="url(#ads-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.gw" :x2="PX.auth" y1="385" y2="385"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.gw+PX.auth)/2" y="377">{{ s.m9 }}</text>
      <path :d="`M${PX.auth},410 Q${(PX.auth+PX.client)/2},440 ${PX.client},415`" fill="none" marker-end="url(#ads-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.auth+PX.client)/2" y="438">{{ s.m10 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
