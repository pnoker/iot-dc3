<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'主体 / 成员 / 凭据领域', tenant:'租户 Tenant', principal:'主体 Principal', principalSub:'USER / SERVICE_ACCOUNT / SYSTEM', membership:'租户成员 TenantMembership', membershipSub:'ACTIVE / SUSPENDED / INVITED', credential:'本地凭据 LocalCredential', credentialSub:'ARGON2ID / BCRYPT 哈希', e1:'包含成员', e2:'属于（可多租户）', e3:'挂载凭据'},
  en: {aria:'Principal / membership / credential', tenant:'Tenant', principal:'Principal', principalSub:'USER / SERVICE_ACCOUNT / SYSTEM', membership:'TenantMembership', membershipSub:'ACTIVE / SUSPENDED / INVITED', credential:'LocalCredential', credentialSub:'ARGON2ID / BCRYPT hash', e1:'has members', e2:'belongs to (multi-tenant)', e3:'mounts credential'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 340">
      <defs><marker id="aer-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker>
      <filter id="aer-g" x="-40%" y="-40%" width="180%" height="180%"><feGaussianBlur stdDeviation="7"/></filter></defs>
      <line marker-end="url(#aer-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="250" x2="300" y1="116" y2="230"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="230" y="180">{{ s.e1 }}</text>
      <line marker-end="url(#aer-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="420" y1="215" y2="245"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="370" y="235">{{ s.e2 }}</text>
      <line marker-end="url(#aer-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="650" x2="780" y1="150" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="730" y="115">{{ s.e3 }}</text>
      <rect x="80" y="60" width="200" height="56" rx="8" fill="var(--vp-c-bg)"/><rect x="80" y="60" width="200" height="56" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="180" y="93">{{ s.tenant }}</text>
      <rect x="780" y="60" width="220" height="56" rx="8" fill="var(--vp-c-bg)"/><rect x="780" y="60" width="220" height="56" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="890" y="85">{{ s.credential }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="890" y="103">{{ s.credentialSub }}</text>
      <rect x="420" y="120" width="240" height="100" rx="14" fill="var(--dc3-be-stroke)" opacity="0.2" filter="url(#aer-g)"/>
      <rect x="430" y="130" width="220" height="80" rx="10" fill="var(--vp-c-bg)"/><rect x="430" y="130" width="220" height="80" rx="10" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="14" font-weight="700" text-anchor="middle" x="540" y="162">{{ s.principal }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="540" y="184">{{ s.principalSub }}</text>
      <rect x="180" y="230" width="260" height="70" rx="8" fill="var(--vp-c-bg)"/><rect x="180" y="230" width="260" height="70" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="310" y="258">{{ s.membership }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="310" y="278">{{ s.membershipSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
