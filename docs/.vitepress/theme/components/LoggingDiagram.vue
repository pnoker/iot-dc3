<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'日志链路', app:'业务代码', appSub:'log.info(事件, 参数...)', mdc:'MDC 上下文槽位', mdcSub:'encoder 预留 mdc provider',
    console:'CONSOLE Appender', consoleSub:'彩色文本', file:'FILE Appender', fileSub:'JSON (logstash encoder)',
    dev:'本地终端 / podman logs', roll:'滚动文件 *.gz', rollSub:'应用内 logback 轮转', collect:'日志采集', collectSub:'ELK / Loki (可选)'},
  en: {aria:'Logging pipeline', app:'Business code', appSub:'log.info(event, args...)', mdc:'MDC context slots', mdcSub:'encoder mdc provider',
    console:'CONSOLE Appender', consoleSub:'colored text', file:'FILE Appender', fileSub:'JSON (logstash encoder)',
    dev:'Local terminal / podman logs', roll:'Rolling file *.gz', rollSub:'in-app logback rotation', collect:'Log collection', collectSub:'ELK / Loki (optional)'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1200 340">
      <defs><marker id="lg-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#lg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="220" x2="280" y1="170" y2="170"/>
      <line marker-end="url(#lg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="480" x2="540" y1="150" y2="95"/>
      <line marker-end="url(#lg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="480" x2="540" y1="190" y2="245"/>
      <line marker-end="url(#lg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="740" x2="800" y1="95" y2="95"/>
      <line marker-end="url(#lg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="740" x2="800" y1="245" y2="170"/>
      <line marker-end="url(#lg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="740" x2="800" y1="260" y2="280"/>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="20" y="130"/><rect fill="var(--dc3-fe-fill)" height="80" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="200" x="20" y="130"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="120" y="160">{{ s.app }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="120" y="180">{{ s.appSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="280" y="130"/><rect fill="var(--dc3-amber-fill)" height="80" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="2" width="200" x="280" y="130"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="380" y="160">{{ s.mdc }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="380" y="180">{{ s.mdcSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="200" x="540" y="60"/><rect fill="var(--dc3-bus-fill)" height="70" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="200" x="540" y="60"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="640" y="88">{{ s.console }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="640" y="108">{{ s.consoleSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="200" x="540" y="210"/><rect fill="var(--dc3-bus-fill)" height="70" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="200" x="540" y="210"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="640" y="238">{{ s.file }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="640" y="258">{{ s.fileSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="180" x="800" y="65"/><rect fill="var(--dc3-ext-fill)" height="60" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="180" x="800" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="890" y="100">{{ s.dev }}</text>
      <rect fill="var(--vp-c-bg)" height="55" rx="8" width="180" x="800" y="143"/><rect fill="var(--dc3-bus-fill)" height="55" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="180" x="800" y="143"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="890" y="170">{{ s.roll }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="890" y="186">{{ s.rollSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="180" x="800" y="250"/><rect fill="var(--dc3-be-fill)" height="60" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="180" x="800" y="250"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="890" y="278">{{ s.collect }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="890" y="296">{{ s.collectSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
