<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'SQL Server 驱动读写', drv:'dc3-driver-sqlserver', drvSub:'JDBC 客户端',
    db:'SQL Server 实例', dbSub:'业务表 / 历史表', pv:'位号值 PointValue',
    e1:'readQuery (SELECT)', e2:'writeQuery (UPDATE/INSERT, ?)', e3:'结果第一行第一列'},
  en: {aria:'SQL Server driver read/write', drv:'dc3-driver-sqlserver', drvSub:'JDBC client',
    db:'SQL Server instance', dbSub:'business / history tables', pv:'PointValue',
    e1:'readQuery (SELECT)', e2:'writeQuery (UPDATE/INSERT, ?)', e3:'first row, first column'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1040 300">
      <defs><marker id="ss-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#ss-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="260" x2="400" y1="130" y2="130"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="330" y="122">{{ s.e1 }}</text>
      <line marker-end="url(#ss-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" x1="260" x2="400" y1="170" y2="180"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="330" y="192">{{ s.e2 }}</text>
      <line marker-end="url(#ss-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="600" x2="760" y1="150" y2="150"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="680" y="142">{{ s.e3 }}</text>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="220" x="40" y="105"/>
      <rect fill="var(--dc3-bus-fill)" height="90" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="2" width="220" x="40" y="105"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="150" y="142">{{ s.drv }}</text><text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="150" y="164">{{ s.drvSub }}</text>
      <path d="M400,100 a100,16 0 0 0 200,0 v100 a100,16 0 0 1 -200,0 z" fill="var(--vp-c-bg)"/>
      <path d="M400,100 a100,16 0 0 0 200,0 v100 a100,16 0 0 1 -200,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <ellipse cx="500" cy="100" fill="none" rx="100" ry="16" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="500" y="152">{{ s.db }}</text><text fill="var(--dc3-db-text)" font-size="9.5" text-anchor="middle" x="500" y="170">{{ s.dbSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="200" x="760" y="120"/>
      <rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="200" x="760" y="120"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="860" y="156">{{ s.pv }}</text>
    </svg>
  </div></DiagramFrame>
</template>
