<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria: '读值与下命令双数据流', read: '数据流：查值（无副作用）', write: '命令流：下命令（异步，有副作用）',
    dev: '现场设备', drv: '驱动 Driver', data: '数据中心', dataSub: 'dc3-center-data', ts: 'TimescaleDB', tsSub: 'dc3_point_value', you: '你（HTTP）',
    eDrvData: 'dc3.e.value', eYouRead: 'POST /api/v3/data/point_value/latest · /list',
    eYouWrite: 'POST /api/v3/data/point_command/read · /write', eDataDrv: 'dc3.e.point_command', eDrvRes: 'dc3.e.point_command_result',
    ePoll: '轮询 GET .../point_command_history/get_by_command_id'},
  en: {aria: 'Read values and issue commands — dual data flows', read: 'Data flow: read values (no side effects)', write: 'Command flow: issue commands (async, with side effects)',
    dev: 'Field device', drv: 'Driver', data: 'Data Center', dataSub: 'dc3-center-data', ts: 'TimescaleDB', tsSub: 'dc3_point_value', you: 'You (HTTP)',
    eDrvData: 'dc3.e.value', eYouRead: 'POST /api/v3/data/point_value/latest · /list',
    eYouWrite: 'POST /api/v3/data/point_command/read · /write', eDataDrv: 'dc3.e.point_command', eDrvRes: 'dc3.e.point_command_result',
    ePoll: 'Poll GET .../point_command_history/get_by_command_id'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1000 450">
      <defs><marker id="dcf-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <!-- swimlane bands -->
      <rect x="20" y="46" width="960" height="158" rx="10" fill="var(--vp-c-bg-soft)" opacity="0.55" stroke="var(--vp-c-divider)" stroke-width="1"/>
      <rect x="20" y="230" width="960" height="180" rx="10" fill="var(--vp-c-bg-soft)" opacity="0.55" stroke="var(--vp-c-divider)" stroke-width="1"/>
      <text fill="var(--dc3-text2)" font-size="10.5" font-weight="700" x="34" y="64">{{ s.read }}</text>
      <text fill="var(--dc3-text2)" font-size="10.5" font-weight="700" x="34" y="250">{{ s.write }}</text>
      <!-- ===== READ lane ===== -->
      <line marker-end="url(#dcf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="200" x2="232" y1="122" y2="122"/>
      <line marker-end="url(#dcf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="365" x2="397" y1="122" y2="122"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="381" y="114">{{ s.eDrvData }}</text>
      <line marker-end="url(#dcf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="560" x2="588" y1="122" y2="122"/>
      <line marker-end="url(#dcf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="550" x2="550" y1="168" y2="152"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="640" y="164">{{ s.eYouRead }}</text>
      <!-- Dev1 -->
      <rect x="70" y="100" width="130" height="44" rx="8" fill="var(--vp-c-bg)"/><rect x="70" y="100" width="130" height="44" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="135" y="127">{{ s.dev }}</text>
      <!-- Drv1 -->
      <rect x="232" y="100" width="133" height="44" rx="8" fill="var(--vp-c-bg)"/><rect x="232" y="100" width="133" height="44" rx="8" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="298" y="127">{{ s.drv }}</text>
      <!-- Data1 hero -->
      <rect x="397" y="92" width="163" height="60" rx="10" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="478" y="116">{{ s.data }}</text>
      <text fill="var(--dc3-be-text)" font-size="9.5" text-anchor="middle" x="478" y="135">{{ s.dataSub }}</text>
      <!-- TS cylinder -->
      <rect x="590" y="106" width="110" height="34" fill="var(--dc3-db-fill)"/>
      <ellipse cx="645" cy="106" rx="55" ry="8" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <path d="M590,106 V140" stroke="var(--dc3-db-stroke)" stroke-width="1.5" fill="none"/><path d="M700,106 V140" stroke="var(--dc3-db-stroke)" stroke-width="1.5" fill="none"/>
      <ellipse cx="645" cy="140" rx="55" ry="8" fill="none" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-db-text)" font-size="10" font-weight="700" text-anchor="middle" x="645" y="120">{{ s.ts }}</text>
      <text fill="var(--dc3-db-text)" font-size="8.5" text-anchor="middle" x="645" y="133">{{ s.tsSub }}</text>
      <!-- You1 -->
      <rect x="475" y="168" width="150" height="32" rx="8" fill="var(--vp-c-bg)"/><rect x="475" y="168" width="150" height="32" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle" x="550" y="188">{{ s.you }}</text>
      <!-- ===== WRITE lane ===== -->
      <line marker-end="url(#dcf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="200" x2="232" y1="322" y2="322"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="216" y="314">{{ s.eYouWrite }}</text>
      <line marker-end="url(#dcf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="560" x2="428" y1="322" y2="322"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="494" y="314">{{ s.eDataDrv }}</text>
      <line marker-end="url(#dcf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="425" x2="425" y1="298" y2="296" style="display:none"/>
      <path d="M495,300 Q495,275 360,275 Q360,300 360,300" fill="none" marker-end="url(#dcf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="430" y="270">{{ s.eDrvRes }}</text>
      <path d="M135,344 Q135,390 320,390 Q320,344 320,344" fill="none" marker-end="url(#dcf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="225" y="385">{{ s.ePoll }}</text>
      <!-- Drv2 → Dev2 -->
      <line marker-end="url(#dcf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="560" x2="600" y1="322" y2="322"/>
      <!-- You2 -->
      <rect x="70" y="300" width="130" height="44" rx="8" fill="var(--vp-c-bg)"/><rect x="70" y="300" width="130" height="44" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="135" y="327">{{ s.you }}</text>
      <!-- Data2 hero -->
      <rect x="232" y="292" width="128" height="60" rx="10" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="296" y="316">{{ s.data }}</text>
      <text fill="var(--dc3-be-text)" font-size="9.5" text-anchor="middle" x="296" y="335">{{ s.dataSub }}</text>
      <!-- Drv2 -->
      <rect x="428" y="300" width="132" height="44" rx="8" fill="var(--vp-c-bg)"/><rect x="428" y="300" width="132" height="44" rx="8" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="494" y="327">{{ s.drv }}</text>
      <!-- Dev2 -->
      <rect x="600" y="300" width="130" height="44" rx="8" fill="var(--vp-c-bg)"/><rect x="600" y="300" width="130" height="44" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="665" y="327">{{ s.dev }}</text>
    </svg>
  </div></DiagramFrame>
</template>
