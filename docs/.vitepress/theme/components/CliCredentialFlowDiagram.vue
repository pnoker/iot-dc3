<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'密码来源决策', start:'需要密码（续期 / 登录）', kc:'keychain 可用？', enc:'加密文件 (AES-256-GCM)', envv:'环境变量 DC3_PASSWORD', prompt:'交互式 prompt 兜底', use:'用该密码走 salt / generate', yes:'命中', no:'不可用 / 未命中'},
  en: {aria:'Credential source decision', start:'Need password (renew / login)', kc:'keychain available?', enc:'Encrypted file (AES-256-GCM)', envv:'Env var DC3_PASSWORD', prompt:'Interactive prompt fallback', use:'Use password for salt / generate', yes:'hit', no:'unavailable / miss'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 340">
      <defs><marker id="ccf-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#ccf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="200" x2="280" y1="170" y2="170"/>
      <line marker-end="url(#ccf-ah)" stroke="var(--dc3-be-stroke)" stroke-width="1.5" x1="420" x2="900" y1="140" y2="100"/><text fill="var(--dc3-be-text)" font-size="9" font-weight="600" x="600" y="115">{{ s.yes }}</text>
      <line marker-end="url(#ccf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="420" x2="500" y1="200" y2="200"/><text fill="var(--dc3-arrow-label)" font-size="9" x="445" y="195">{{ s.no }}</text>
      <line marker-end="url(#ccf-ah)" stroke="var(--dc3-be-stroke)" stroke-width="1.5" x1="640" x2="900" y1="170" y2="115"/><text fill="var(--dc3-be-text)" font-size="9" font-weight="600" x="780" y="145">{{ s.yes }}</text>
      <line marker-end="url(#ccf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="640" x2="720" y1="225" y2="225"/><text fill="var(--dc3-arrow-label)" font-size="9" x="665" y="220">{{ s.no }}</text>
      <line marker-end="url(#ccf-ah)" stroke="var(--dc3-be-stroke)" stroke-width="1.5" x1="860" x2="900" y1="200" y2="130"/><text fill="var(--dc3-be-text)" font-size="9" font-weight="600" x="890" y="175">{{ s.yes }}</text>
      <line marker-end="url(#ccf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="860" x2="900" y1="250" y2="250"/><text fill="var(--dc3-arrow-label)" font-size="9" x="875" y="240">{{ s.no }}</text>
      <line marker-end="url(#ccf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1000" x2="980" y1="250" y2="230"/>
      <rect x="20" y="145" width="180" height="50" rx="25" fill="var(--vp-c-bg)"/><rect x="20" y="145" width="180" height="50" rx="25" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="110" y="175">{{ s.start }}</text>
      <polygon points="350,170 420,130 420,210" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text fill="var(--dc3-box-name)" font-size="10" text-anchor="middle" x="385" y="173">{{ s.kc }}</text>
      <polygon points="570,200 640,165 640,240" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text fill="var(--dc3-box-name)" font-size="9.5" text-anchor="middle" x="605" y="205">{{ s.enc }}</text>
      <polygon points="790,225 860,190 860,265" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text fill="var(--dc3-box-name)" font-size="9.5" text-anchor="middle" x="825" y="230">{{ s.envv }}</text>
      <rect x="860" y="245" width="140" height="40" rx="8" fill="var(--vp-c-bg)"/><rect x="860" y="245" width="140" height="40" rx="8" fill="var(--dc3-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10" text-anchor="middle" x="930" y="270">{{ s.prompt }}</text>
      <rect x="900" y="80" width="170" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="900" y="80" width="170" height="50" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-be-text)" font-size="11" font-weight="700" text-anchor="middle" x="985" y="110">{{ s.use }}</text>
    </svg>
  </div></DiagramFrame>
</template>
