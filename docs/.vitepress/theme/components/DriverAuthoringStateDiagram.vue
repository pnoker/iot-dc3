<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria: '驱动生命周期状态机', starting: 'STARTING', registering: 'REGISTERING', initial: 'INITIAL', scheduling: 'SCHEDULING', running: 'RUNNING',
    t1: '进程启动 DriverInitRunner.run()', t2: 'registerWithRetry()', tSelf1: '注册失败 · 指数退避 2s→30s, 最多 30 次', t3: '注册成功 initial()', t4: 'driverScheduleService.initialize()', t5: '读/自定义/健康任务装配完成', tSelf2: '周期 read() + 周期上报状态', tExit: '30 次仍失败, 抛异常退出'},
  en: {aria: 'Driver lifecycle state machine', starting: 'STARTING', registering: 'REGISTERING', initial: 'INITIAL', scheduling: 'SCHEDULING', running: 'RUNNING',
    t1: 'process starts DriverInitRunner.run()', t2: 'registerWithRetry()', tSelf1: 'registration failed · exponential backoff 2s→30s, up to 30 attempts', t3: 'registration succeeded initial()', t4: 'driverScheduleService.initialize()', t5: 'read/custom/health tasks wired up', tSelf2: 'periodic read() + periodic status report', tExit: 'still failing after 30 attempts, throw and exit'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1240 280">
      <defs><marker id="das-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <!-- start -> STARTING -->
      <circle cx="30" cy="140" r="9" fill="var(--dc3-arrow)"/>
      <line marker-end="url(#das-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="39" x2="60" y1="140" y2="140"/>
      <!-- STARTING -> REGISTERING -->
      <line marker-end="url(#das-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="190" x2="230" y1="140" y2="140"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="210" y="132">{{ s.t2 }}</text>
      <!-- REGISTERING self loop (above) -->
      <path d="M285,110 Q285,64 315,64 Q345,64 345,110" fill="none" marker-end="url(#das-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="315" y="56">{{ s.tSelf1 }}</text>
      <!-- REGISTERING -> exit (fail) -->
      <line marker-end="url(#das-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="315" x2="315" y1="170" y2="240"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="385" y="220">{{ s.tExit }}</text>
      <circle cx="315" cy="252" r="9" fill="none" stroke="var(--dc3-arrow)" stroke-width="1.8"/><circle cx="315" cy="252" r="4" fill="var(--dc3-arrow)"/>
      <!-- REGISTERING -> INITIAL -->
      <line marker-end="url(#das-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="400" x2="440" y1="140" y2="140"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="420" y="124">{{ s.t3 }}</text>
      <!-- INITIAL -> SCHEDULING -->
      <line marker-end="url(#das-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="570" x2="610" y1="140" y2="140"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="590" y="124">{{ s.t4 }}</text>
      <!-- SCHEDULING -> RUNNING -->
      <line marker-end="url(#das-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="760" x2="800" y1="140" y2="140"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="780" y="132">{{ s.t5 }}</text>
      <!-- RUNNING self loop (below) -->
      <path d="M850,170 Q850,216 880,216 Q910,216 910,170" fill="none" marker-end="url(#das-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="880" y="234">{{ s.tSelf2 }}</text>
      <!-- STARTING -->
      <rect x="60" y="116" width="130" height="48" rx="24" fill="var(--vp-c-bg)"/><rect x="60" y="116" width="130" height="48" rx="24" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="125" y="145">{{ s.starting }}</text>
      <!-- REGISTERING -->
      <rect x="230" y="110" width="170" height="60" rx="10" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="315" y="145">{{ s.registering }}</text>
      <!-- INITIAL -->
      <rect x="440" y="116" width="130" height="48" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="505" y="145">{{ s.initial }}</text>
      <!-- SCHEDULING -->
      <rect x="610" y="116" width="150" height="48" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="685" y="145">{{ s.scheduling }}</text>
      <!-- RUNNING (hero) -->
      <rect x="800" y="110" width="160" height="60" rx="10" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="14" font-weight="700" text-anchor="middle" x="880" y="145">{{ s.running }}</text>
    </svg>
  </div></DiagramFrame>
</template>
