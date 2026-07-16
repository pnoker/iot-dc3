<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'命令分发与结果回流', data:'数据中心 dc3-center-data', ex:'命令交换机 dc3.e.point_command', q:'命令队列 point_command.{svc}', qSub:'TTL 30s + DLX',
    driver:'驱动 PointCommandReceiver', dlx:'死信交换机', dlq:'死信队列 point_command_dead', rex:'结果交换机 dc3.e.point_command_result', rq:'结果队列 point_command_result', rqSub:'TTL 60s', recv:'结果接收器',
    e1:'routing {svc}', e2:'超时 / reject', e3:'routing result {svc}'},
  en: {aria:'Command dispatch & result return', data:'Data Center dc3-center-data', ex:'cmd exchange dc3.e.point_command', q:'cmd queue point_command.{svc}', qSub:'TTL 30s + DLX',
    driver:'Driver PointCommandReceiver', dlx:'dead-letter exchange', dlq:'dead-letter queue point_command_dead', rex:'result exchange dc3.e.point_command_result', rq:'result queue point_command_result', rqSub:'TTL 60s', recv:'Result receiver',
    e1:'routing {svc}', e2:'timeout / reject', e3:'routing result {svc}'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1300 340">
      <defs><marker id="cfw-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#cfw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="150" x2="200" y1="120" y2="120"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="175" y="112">{{ s.e1 }}</text>
      <line marker-end="url(#cfw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="320" x2="370" y1="120" y2="120"/>
      <line marker-end="url(#cfw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="520" x2="570" y1="120" y2="120"/>
      <path d="M450,158 L450,200 L370,225" fill="none" marker-end="url(#cfw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="490" y="195">{{ s.e2 }}</text>
      <line marker-end="url(#cfw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="320" x2="370" y1="245" y2="245"/>
      <line marker-end="url(#cfw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="700" x2="750" y1="120" y2="120"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="725" y="112">{{ s.e3 }}</text>
      <line marker-end="url(#cfw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="870" x2="920" y1="120" y2="120"/>
      <line marker-end="url(#cfw-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1070" x2="1120" y1="120" y2="120"/>
      <!-- data -->
      <rect x="20" y="92" width="130" height="56" rx="8" fill="var(--vp-c-bg)"/><rect x="20" y="92" width="130" height="56" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="85" y="125">{{ s.data }}</text>
      <!-- ex -->
      <path d="M200,95 a60,11 0 0 0 120,0 v50 a60,11 0 0 1 -120,0 z" fill="var(--vp-c-bg)"/><path d="M200,95 a60,11 0 0 0 120,0 v50 a60,11 0 0 1 -120,0 z" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <ellipse cx="260" cy="95" fill="none" rx="60" ry="11" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="9" text-anchor="middle" x="260" y="128">{{ s.ex }}</text>
      <!-- q -->
      <path d="M370,95 a75,11 0 0 0 150,0 v50 a75,11 0 0 1 -150,0 z" fill="var(--vp-c-bg)"/><path d="M370,95 a75,11 0 0 0 150,0 v50 a75,11 0 0 1 -150,0 z" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <ellipse cx="445" cy="95" fill="none" rx="75" ry="11" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="9" text-anchor="middle" x="445" y="124">{{ s.q }}</text><text fill="var(--dc3-bus-text)" font-size="8" text-anchor="middle" x="445" y="140">{{ s.qSub }}</text>
      <!-- driver -->
      <rect x="570" y="92" width="130" height="56" rx="8" fill="var(--vp-c-bg)"/><rect x="570" y="92" width="130" height="56" rx="8" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="9.5" text-anchor="middle" x="635" y="125">{{ s.driver }}</text>
      <!-- dlx -->
      <path d="M220,220 a55,10 0 0 0 110,0 v44 a55,10 0 0 1 -110,0 z" fill="var(--vp-c-bg)"/><path d="M220,220 a55,10 0 0 0 110,0 v44 a55,10 0 0 1 -110,0 z" fill="var(--dc3-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
      <ellipse cx="275" cy="220" fill="none" rx="55" ry="10" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-rose-stroke)" font-size="9" text-anchor="middle" x="275" y="248">{{ s.dlx }}</text>
      <!-- dlq -->
      <rect x="370" y="222" width="150" height="44" rx="6" fill="var(--vp-c-bg)"/><rect x="370" y="222" width="150" height="44" rx="6" fill="var(--dc3-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-rose-stroke)" font-size="9" text-anchor="middle" x="445" y="248">{{ s.dlq }}</text>
      <!-- rex -->
      <path d="M750,95 a60,11 0 0 0 120,0 v50 a60,11 0 0 1 -120,0 z" fill="var(--vp-c-bg)"/><path d="M750,95 a60,11 0 0 0 120,0 v50 a60,11 0 0 1 -120,0 z" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <ellipse cx="810" cy="95" fill="none" rx="60" ry="11" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="9" text-anchor="middle" x="810" y="128">{{ s.rex }}</text>
      <!-- rq -->
      <path d="M920,95 a75,11 0 0 0 150,0 v50 a75,11 0 0 1 -150,0 z" fill="var(--vp-c-bg)"/><path d="M920,95 a75,11 0 0 0 150,0 v50 a75,11 0 0 1 -150,0 z" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <ellipse cx="995" cy="95" fill="none" rx="75" ry="11" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="9" text-anchor="middle" x="995" y="124">{{ s.rq }}</text><text fill="var(--dc3-bus-text)" font-size="8" text-anchor="middle" x="995" y="140">{{ s.rqSub }}</text>
      <!-- recv -->
      <rect x="1120" y="92" width="160" height="56" rx="8" fill="var(--vp-c-bg)"/><rect x="1120" y="92" width="160" height="56" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="1200" y="125">{{ s.recv }}</text>
    </svg>
  </div></DiagramFrame>
</template>
