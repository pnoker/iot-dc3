<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'CLI 登录取 token', cli:'dc3 (CLI)', gw:'网关 dc3-gateway', auth:'鉴权中心 dc3-center-auth', m1:'POST /api/v3/auth/token/salt (tenant, name)', m2:'转发取盐请求', m3:'salt（建议 5 分钟内使用）', m4:'POST generate (tenant, name, salt, 密码)', m5:'转发生成 token', m6:'JWT 访问令牌（12h）', m7:'解析 iat/exp → ~/.dc3/tokens.json (0600)'},
  en: {aria:'CLI login for token', cli:'dc3 (CLI)', gw:'Gateway dc3-gateway', auth:'Auth dc3-center-auth', m1:'POST /api/v3/auth/token/salt (tenant, name)', m2:'forward salt request', m3:'salt (use within 5 min)', m4:'POST generate (tenant, name, salt, password)', m5:'forward token gen', m6:'JWT access token (12h)', m7:'parse iat/exp → ~/.dc3/tokens.json (0600)'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const PX: Record<string, number> = {cli: 140, gw: 460, auth: 790}
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 930 360">
      <defs><marker id="clsq-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line v-for="p in ['cli','gw','auth']" :key="'l'+p" :x1="PX[p]" :x2="PX[p]" y1="70" y2="340" stroke="var(--dc3-divider)" stroke-width="1" stroke-dasharray="4,4"/>
      <rect v-for="(p,i) in ['cli','gw','auth']" :key="'b'+p" :x="PX[p]-110" y="22" width="220" height="48" rx="8" :fill="['var(--dc3-amber-fill)','var(--dc3-be-fill)','var(--dc3-rose-fill)'][i]" :stroke="['var(--dc3-amber-stroke)','var(--dc3-be-stroke)','var(--dc3-rose-stroke)'][i]" stroke-width="1.5" opacity="0.65"/>
      <text v-for="p in ['cli','gw','auth']" :key="'t'+p" :x="PX[p]" y="51" class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle">{{ s[p] }}</text>
      <line marker-end="url(#clsq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.cli" :x2="PX.gw" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.cli+PX.gw)/2" y="92">{{ s.m1 }}</text>
      <line marker-end="url(#clsq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.gw" :x2="PX.auth" y1="130" y2="130"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.gw+PX.auth)/2" y="122">{{ s.m2 }}</text>
      <line marker-end="url(#clsq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.auth" :x2="PX.cli" y1="165" y2="165"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.auth+PX.cli)/2" y="157">{{ s.m3 }}</text>
      <line marker-end="url(#clsq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.cli" :x2="PX.gw" y1="205" y2="205"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.cli+PX.gw)/2" y="197">{{ s.m4 }}</text>
      <line marker-end="url(#clsq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.gw" :x2="PX.auth" y1="235" y2="235"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.gw+PX.auth)/2" y="227">{{ s.m5 }}</text>
      <line marker-end="url(#clsq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.auth" :x2="PX.cli" y1="270" y2="270"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.auth+PX.cli)/2" y="262">{{ s.m6 }}</text>
      <path :d="`M${PX.cli},295 L${PX.cli+50},295 L${PX.cli+50},315 L${PX.cli},315`" fill="none" marker-end="url(#clsq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="8.5" :x="PX.cli+58" y="310">{{ s.m7 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
