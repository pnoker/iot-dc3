<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria: '命令状态机', pending: 'PENDING', sent: 'SENT', success: 'SUCCESS', failed: 'FAILED', expired: 'EXPIRED', duplicate: 'DUPLICATE', timeout: 'TIMEOUT', dead: 'DEAD',
    t1: '发布确认 (broker ACK)', t2: '驱动确认成功', t3: '驱动报告失败', t4: '应用级超时', t5: '消费时已过 expireAt', t6: '命中驱动去重缓存', t7: '进入死信队列 (DLX)'},
  en: {aria: 'Command state machine', pending: 'PENDING', sent: 'SENT', success: 'SUCCESS', failed: 'FAILED', expired: 'EXPIRED', duplicate: 'DUPLICATE', timeout: 'TIMEOUT', dead: 'DEAD',
    t1: 'publisher confirm (broker ACK)', t2: 'driver confirms success', t3: 'driver reports failure', t4: 'application-level timeout', t5: 'expireAt already passed at consume', t6: 'hit driver dedup cache', t7: 'routed to dead-letter queue (DLX)'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 440">
      <defs><marker id="dcs-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <!-- start -> PENDING -> SENT -->
      <circle cx="40" cy="215" r="9" fill="var(--dc3-arrow)"/>
      <line marker-end="url(#dcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="49" x2="100" y1="215" y2="215"/>
      <line marker-end="url(#dcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="240" x2="300" y1="215" y2="215"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="270" y="207">{{ s.t1 }}</text>
      <!-- SENT -> 6 terminal states -->
      <line marker-end="url(#dcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="540" y1="200" y2="110"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="468" y="148">{{ s.t2 }}</text>
      <line marker-end="url(#dcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="540" y1="222" y2="220"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="468" y="240">{{ s.t4 }}</text>
      <line marker-end="url(#dcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="540" y1="240" y2="330"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="468" y="300">{{ s.t5 }}</text>
      <line marker-end="url(#dcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="820" y1="200" y2="110"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="700" y="128">{{ s.t3 }}</text>
      <line marker-end="url(#dcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="820" y1="228" y2="220"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="700" y="245">{{ s.t6 }}</text>
      <line marker-end="url(#dcs-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="820" y1="245" y2="330"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="700" y="305">{{ s.t7 }}</text>
      <!-- PENDING -->
      <rect x="100" y="190" width="140" height="50" rx="25" fill="var(--vp-c-bg)"/><rect x="100" y="190" width="140" height="50" rx="25" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="170" y="221">{{ s.pending }}</text>
      <!-- SENT (hero) -->
      <rect x="300" y="180" width="140" height="70" rx="10" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="14" font-weight="700" text-anchor="middle" x="370" y="220">{{ s.sent }}</text>
      <!-- terminals col1 -->
      <rect x="540" y="85" width="180" height="46" rx="6" fill="var(--vp-c-bg)"/><rect x="540" y="85" width="180" height="46" rx="6" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-be-text)" font-size="12" font-weight="700" text-anchor="middle" x="630" y="113">{{ s.success }}</text>
      <rect x="540" y="197" width="180" height="46" rx="6" fill="var(--vp-c-bg)"/><rect x="540" y="197" width="180" height="46" rx="6" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="630" y="225">{{ s.timeout }}</text>
      <rect x="540" y="307" width="180" height="46" rx="6" fill="var(--vp-c-bg)"/><rect x="540" y="307" width="180" height="46" rx="6" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="630" y="335">{{ s.expired }}</text>
      <!-- terminals col2 -->
      <rect x="820" y="85" width="180" height="46" rx="6" fill="var(--vp-c-bg)"/><rect x="820" y="85" width="180" height="46" rx="6" fill="var(--dc3-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-rose-stroke)" font-size="12" font-weight="700" text-anchor="middle" x="910" y="113">{{ s.failed }}</text>
      <rect x="820" y="197" width="180" height="46" rx="6" fill="var(--vp-c-bg)"/><rect x="820" y="197" width="180" height="46" rx="6" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="910" y="225">{{ s.duplicate }}</text>
      <rect x="820" y="307" width="180" height="46" rx="6" fill="var(--vp-c-bg)"/><rect x="820" y="307" width="180" height="46" rx="6" fill="var(--dc3-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-rose-stroke)" font-size="12" font-weight="700" text-anchor="middle" x="910" y="335">{{ s.dead }}</text>
    </svg>
  </div></DiagramFrame>
</template>
