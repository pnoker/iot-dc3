<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'MCP HIGH 风险两阶段确认', agent:'AI Agent', gw:'网关 /mcp', auth:'鉴权中心',
    m1:'tools/call (HIGH 工具，无 confirmId)', m2:'授权决策', m3:'CONFIRM_REQUIRED + confirmId (TTL PT5M)', m4:'confirmId', m5:'tools/call + confirmId + idempotency_key', m6:'校验（未过期 / digest 一致 / 一次性消费）', m7:'AUTHORIZED', m8:'执行并返回结果'},
  en: {aria:'MCP HIGH-risk two-phase confirm', agent:'AI Agent', gw:'Gateway /mcp', auth:'Auth',
    m1:'tools/call (HIGH tool, no confirmId)', m2:'authorize decision', m3:'CONFIRM_REQUIRED + confirmId (TTL PT5M)', m4:'confirmId', m5:'tools/call + confirmId + idempotency_key', m6:'verify (not expired / digest match / single-use)', m7:'AUTHORIZED', m8:'execute & return'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const PX: Record<string, number> = {agent: 160, gw: 480, auth: 780}
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 940 420">
      <defs><marker id="mcs-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line v-for="p in ['agent','gw','auth']" :key="'l'+p" :x1="PX[p]" :x2="PX[p]" y1="70" y2="395" stroke="var(--dc3-divider)" stroke-width="1" stroke-dasharray="4,4"/>
      <rect v-for="(p,i) in ['agent','gw','auth']" :key="'b'+p" :x="PX[p]-110" y="22" width="220" height="48" rx="8" :fill="['var(--dc3-ext-fill)','var(--dc3-be-fill)','var(--dc3-rose-fill)'][i]" :stroke="['var(--dc3-ext-stroke)','var(--dc3-be-stroke)','var(--dc3-rose-stroke)'][i]" stroke-width="1.5" opacity="0.65"/>
      <text v-for="p in ['agent','gw','auth']" :key="'t'+p" :x="PX[p]" y="51" class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle">{{ s[p] }}</text>
      <line marker-end="url(#mcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.agent" :x2="PX.gw" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.agent+PX.gw)/2" y="92">{{ s.m1 }}</text>
      <line marker-end="url(#mcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.gw" :x2="PX.auth" y1="135" y2="135"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.gw+PX.auth)/2" y="127">{{ s.m2 }}</text>
      <line marker-end="url(#mcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.auth" :x2="PX.gw" y1="165" y2="165"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.auth+PX.gw)/2" y="157">{{ s.m3 }}</text>
      <line marker-end="url(#mcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.gw" :x2="PX.agent" y1="195" y2="195"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.gw+PX.agent)/2" y="187">{{ s.m4 }}</text>
      <line marker-end="url(#mcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.agent" :x2="PX.gw" y1="235" y2="235"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.agent+PX.gw)/2" y="227">{{ s.m5 }}</text>
      <line marker-end="url(#mcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" :x1="PX.gw" :x2="PX.auth" y1="270" y2="270"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.gw+PX.auth)/2" y="262">{{ s.m6 }}</text>
      <line marker-end="url(#mcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" :x1="PX.auth" :x2="PX.gw" y1="300" y2="300"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.auth+PX.gw)/2" y="292">{{ s.m7 }}</text>
      <path :d="`M${PX.gw},335 Q${(PX.gw+PX.agent)/2},375 ${PX.agent},345`" fill="none" marker-end="url(#mcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" :x="(PX.gw+PX.agent)/2" y="372">{{ s.m8 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
