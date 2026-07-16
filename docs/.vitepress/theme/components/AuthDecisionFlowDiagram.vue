<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'权限判定链', p:'principal', pSub:'tenantId : principalId', rpb:'dc3_role_principal_bind', rpbSub:'租户内（含 tenant_id）', rrb:'dc3_role_resource_bind', rrbSub:'全局（无 tenant_id）', res:'dc3_resource', resSub:'resource_code = service:domain:scope', cache:'权限集缓存', cacheSub:'key=(tenantId:principalId) · TTL 5 分钟', decision:'@PreAuthorize 判定', allow:'放行', deny:'fail-closed → 403', e1:'命中权限码', e2:'查不到 / 加载失败'},
  en: {aria:'Permission decision chain', p:'principal', pSub:'tenantId : principalId', rpb:'dc3_role_principal_bind', rpbSub:'tenant-scoped (has tenant_id)', rrb:'dc3_role_resource_bind', rrbSub:'global (no tenant_id)', res:'dc3_resource', resSub:'resource_code = service:domain:scope', cache:'permission cache', cacheSub:'key=(tenantId:principalId) · TTL 5 min', decision:'@PreAuthorize', allow:'allow', deny:'fail-closed → 403', e1:'code matched', e2:'not found / load failed'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1400 300">
      <defs><marker id="afw-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#afw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="160" x2="200" y1="150" y2="150"/>
      <line marker-end="url(#afw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="390" x2="420" y1="150" y2="150"/>
      <line marker-end="url(#afw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="630" x2="670" y1="150" y2="150"/>
      <line marker-end="url(#afw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="880" x2="920" y1="150" y2="150"/>
      <line marker-end="url(#afw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1120" x2="1160" y1="150" y2="150"/>
      <line marker-end="url(#afw-ah)" stroke="var(--dc3-be-stroke)" stroke-width="1.5" x1="1280" x2="1310" y1="135" y2="95"/><text fill="var(--dc3-be-text)" font-size="9.5" font-weight="600" text-anchor="middle" x="1320" y="80">{{ s.e1 }}</text>
      <line marker-end="url(#afw-ah)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" x1="1280" x2="1310" y1="165" y2="205"/><text fill="var(--dc3-rose-stroke)" font-size="9.5" font-weight="600" text-anchor="middle" x="1320" y="230">{{ s.e2 }}</text>
      <rect x="20" y="115" width="140" height="70" rx="8" fill="var(--vp-c-bg)"/><rect x="20" y="115" width="140" height="70" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="90" y="145">{{ s.p }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="90" y="165">{{ s.pSub }}</text>
      <rect x="200" y="125" width="190" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="200" y="125" width="190" height="50" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="295" y="146">{{ s.rpb }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="295" y="162">{{ s.rpbSub }}</text>
      <rect x="420" y="125" width="210" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="420" y="125" width="210" height="50" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="525" y="146">{{ s.rrb }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="525" y="162">{{ s.rrbSub }}</text>
      <rect x="670" y="125" width="210" height="50" rx="8" fill="var(--vp-c-bg)"/><rect x="670" y="125" width="210" height="50" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="775" y="146">{{ s.res }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="775" y="162">{{ s.resSub }}</text>
      <rect x="920" y="120" width="200" height="60" rx="8" fill="var(--vp-c-bg)"/><rect x="920" y="120" width="200" height="60" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="1020" y="143">{{ s.cache }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="1020" y="161">{{ s.cacheSub }}</text>
      <polygon points="1220,150 1280,110 1340,150 1280,190" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="1280" y="154">{{ s.decision }}</text>
      <rect x="1310" y="65" width="80" height="40" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-be-text)" font-size="12" font-weight="700" text-anchor="middle" x="1350" y="90">{{ s.allow }}</text>
      <rect x="1310" y="195" width="80" height="40" rx="8" fill="var(--dc3-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-rose-stroke)" font-size="12" font-weight="700" text-anchor="middle" x="1350" y="220">{{ s.deny }}</text>
    </svg>
  </div></DiagramFrame>
</template>
