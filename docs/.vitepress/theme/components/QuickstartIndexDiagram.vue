<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'本地开发启动流程', a:'make up-db', aSub:'起 PostgreSQL + RabbitMQ', b:'source dev.env.sh', bSub:'加载本地环境变量',
    c:'make package', cSub:'多模块构建', d:'make up-dev', dSub:'起本地开发栈', e:'make test', eSub:'跑单元测试', f:'接入第一个设备', fSub:'验证通过'},
  en: {aria:'Local dev startup flow', a:'make up-db', aSub:'start PostgreSQL + RabbitMQ', b:'source dev.env.sh', bSub:'load local env vars',
    c:'make package', cSub:'multi-module build', d:'make up-dev', dSub:'start local dev stack', e:'make test', eSub:'run unit tests', f:'Onboard first device', fSub:'verified'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 150">
      <defs><marker id="qi-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#qi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="190" x2="220" y1="75" y2="75"/>
      <line marker-end="url(#qi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="400" x2="430" y1="75" y2="75"/>
      <line marker-end="url(#qi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="610" x2="640" y1="75" y2="75"/>
      <line marker-end="url(#qi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="820" x2="850" y1="75" y2="75"/>
      <line marker-end="url(#qi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1030" x2="1060" y1="75" y2="75"/>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="190" x="10" y="35"/><rect fill="var(--dc3-amber-fill)" height="80" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="190" x="10" y="35"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="105" y="62">{{ s.a }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="105" y="82">{{ s.aSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="180" x="220" y="35"/><rect fill="var(--dc3-fe-fill)" height="80" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="220" y="35"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="310" y="62">{{ s.b }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="310" y="82">{{ s.bSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="180" x="430" y="35"/><rect fill="var(--dc3-amber-fill)" height="80" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="180" x="430" y="35"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="520" y="62">{{ s.c }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="520" y="82">{{ s.cSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="180" x="640" y="35"/><rect fill="var(--dc3-amber-fill)" height="80" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="180" x="640" y="35"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="730" y="62">{{ s.d }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="730" y="82">{{ s.dSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="180" x="850" y="35"/><rect fill="var(--dc3-fe-fill)" height="80" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="850" y="35"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="940" y="62">{{ s.e }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="940" y="82">{{ s.eSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="1060" y="35"/><rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="2" width="200" x="1060" y="35"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="1160" y="62">{{ s.f }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="1160" y="82">{{ s.fSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
