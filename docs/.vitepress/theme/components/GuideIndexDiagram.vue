<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'部署运维门户导航', start:'你在这', startSub:'部署运维门户', deploy:'部署模式与镜像源', deploySub:'compose 编排 / global·cn 镜像源',
    obs:'可观测性', obsSub:'Grafana / Prometheus / ELK', log:'日志规范', logSub:'控制台彩色 + 滚动 JSON',
    trouble:'故障排查', troubleSub:'端口 / DB / MQ / 401·403 / 驱动注册'},
  en: {aria:'Deploy & ops portal navigation', start:'You are here', startSub:'Deploy & ops portal', deploy:'Deploy mode & image source', deploySub:'compose / global·cn registry',
    obs:'Observability', obsSub:'Grafana / Prometheus / ELK', log:'Logging', logSub:'colored console + rolling JSON',
    trouble:'Troubleshooting', troubleSub:'ports / DB / MQ / 401·403 / driver register'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1200 320">
      <defs><marker id="gi-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#gi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="220" x2="290" y1="160" y2="160"/>
      <line marker-end="url(#gi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="510" x2="580" y1="140" y2="95"/>
      <line marker-end="url(#gi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="510" x2="580" y1="180" y2="225"/>
      <line marker-end="url(#gi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="800" x2="880" y1="95" y2="140"/>
      <line marker-end="url(#gi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="800" x2="880" y1="225" y2="180"/>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="200" x="20" y="115"/><rect fill="var(--dc3-amber-fill)" height="90" rx="10" stroke="var(--dc3-amber-stroke)" stroke-width="2" width="200" x="20" y="115"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="14" font-weight="700" text-anchor="middle" x="120" y="150">{{ s.start }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="120" y="172">{{ s.startSub }}</text>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="220" x="290" y="115"/><rect fill="var(--dc3-be-fill)" height="90" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="220" x="290" y="115"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="400" y="150">{{ s.deploy }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="400" y="172">{{ s.deploySub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="220" x="580" y="55"/><rect fill="var(--dc3-fe-fill)" height="80" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="220" x="580" y="55"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="690" y="85">{{ s.obs }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="690" y="105">{{ s.obsSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="220" x="580" y="185"/><rect fill="var(--dc3-fe-fill)" height="80" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="220" x="580" y="185"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="690" y="215">{{ s.log }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="690" y="235">{{ s.logSub }}</text>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="280" x="880" y="115"/><rect fill="var(--dc3-rose-fill)" height="90" rx="10" stroke="var(--dc3-rose-stroke)" stroke-width="2" width="280" x="880" y="115"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="1020" y="150">{{ s.trouble }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="1020" y="172">{{ s.troubleSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
