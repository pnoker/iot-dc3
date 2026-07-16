<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'测试金字塔', utT:'单元 · 最多 · 最快', ut:'*Test.java (Surefire)', utS:'孤立业务逻辑 · 不启动 Spring', itT:'集成 · 中量 · 中速', it:'*IT.java (Failsafe)', itS:'DAL / gRPC InProcess / 消息契约 · 真实 PG/MQ', e2eT:'E2E · 最少 · 最慢', e2e:'dc3-e2e', e2eS:'端到端业务链路 · 命令/事件/时序表', done:'合并前 make coverage 聚合 JaCoCo', e1:'make test', e2:'make test-it（需容器）', e3:'make test-e2e（DC3_E2E=true + 起栈）'},
  en: {aria:'Test pyramid', utT:'Unit · most · fastest', ut:'*Test.java (Surefire)', utS:'isolated logic · no Spring context', itT:'Integration · medium · medium', it:'*IT.java (Failsafe)', itS:'DAL / gRPC InProcess / contracts · real PG/MQ', e2eT:'E2E · fewest · slowest', e2e:'dc3-e2e', e2eS:'end-to-end business chain · command/event/timeseries', done:'before merge: make coverage aggregates JaCoCo', e1:'make test', e2:'make test-it (needs container runtime)', e3:'make test-e2e (DC3_E2E=true + stack up)'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1100 340">
      <defs><marker id="tst-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect x="30" y="30" width="1040" height="80" rx="12" fill="var(--dc3-rose-fill)" opacity="0.25" stroke="var(--dc3-rose-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-rose-stroke)" font-size="11" font-weight="600" x="46" y="50">{{ s.e2eT }}</text>
      <rect x="380" y="55" width="340" height="45" rx="8" fill="var(--vp-c-bg)"/><rect x="380" y="55" width="340" height="45" rx="8" fill="var(--dc3-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="550" y="76">{{ s.e2e }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="550" y="92">{{ s.e2eS }}</text>
      <rect x="30" y="130" width="1040" height="80" rx="12" fill="var(--dc3-amber-fill)" opacity="0.25" stroke="var(--dc3-amber-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-amber-stroke)" font-size="11" font-weight="600" x="46" y="150">{{ s.itT }}</text>
      <rect x="300" y="155" width="500" height="45" rx="8" fill="var(--vp-c-bg)"/><rect x="300" y="155" width="500" height="45" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="550" y="176">{{ s.it }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="550" y="192">{{ s.itS }}</text>
      <rect x="30" y="230" width="1040" height="80" rx="12" fill="var(--dc3-be-fill)" opacity="0.25" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-be-text)" font-size="11" font-weight="600" x="46" y="250">{{ s.utT }}</text>
      <rect x="250" y="255" width="600" height="45" rx="8" fill="var(--vp-c-bg)"/><rect x="250" y="255" width="600" height="45" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="550" y="276">{{ s.ut }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="550" y="292">{{ s.utS }}</text>
      <line marker-end="url(#tst-ah)" stroke="var(--dc3-arrow)" stroke-width="1.2" opacity="0.6" x1="550" x2="550" y1="255" y2="200"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="620" y="230">{{ s.e1 }}</text>
      <line marker-end="url(#tst-ah)" stroke="var(--dc3-arrow)" stroke-width="1.2" opacity="0.6" x1="550" x2="550" y1="155" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="620" y="130">{{ s.e2 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
