<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'环境变量两条路径', composeT:'Compose 路径', env:'.env (根目录)', envSub:'Compose 插值', yml:'docker-compose*.yml', ymlSub:'environment / env_file', ctn:'容器进程', ctnSub:'dc3-postgres:5432',
    localT:'本地源码路径', sh:'dc3/env/dev.env.sh', shSub:'source (导出)', devenv:'dc3/env/dev.env', devenvSub:'IDE EnvFile 插件', proc:'本地 Java 进程', procSub:'localhost:35432',
    field:'host 端口 35432 / 35672 / 31883', fieldSub:'宿主机发布', e1:'内部端口 5432 / 5672 / 1883'},
  en: {aria:'Env var two paths', composeT:'Compose path', env:'.env (root)', envSub:'Compose interpolation', yml:'docker-compose*.yml', ymlSub:'environment / env_file', ctn:'Container process', ctnSub:'dc3-postgres:5432',
    localT:'Local source path', sh:'dc3/env/dev.env.sh', shSub:'source (export)', devenv:'dc3/env/dev.env', devenvSub:'IDE EnvFile plugin', proc:'Local Java process', procSub:'localhost:35432',
    field:'host ports 35432 / 35672 / 31883', fieldSub:'host publish', e1:'internal ports 5432 / 5672 / 1883'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1240 380">
      <defs><marker id="en-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect fill="var(--dc3-be-fill)" height="300" opacity="0.25" rx="12" width="540" x="20" y="40" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-be-text)" font-size="11" font-weight="600" x="36" y="62">{{ s.composeT }}</text>
      <line marker-end="url(#en-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="270" x2="270" y1="120" y2="150"/>
      <line marker-end="url(#en-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="270" x2="270" y1="210" y2="240"/>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="240" x="150" y="60"/><rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="240" x="150" y="60"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="270" y="85">{{ s.env }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="270" y="103">{{ s.envSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="240" x="150" y="150"/><rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="240" x="150" y="150"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="270" y="175">{{ s.yml }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="270" y="193">{{ s.ymlSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="240" x="150" y="240"/><rect fill="var(--dc3-be-fill)" height="60" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="240" x="150" y="240"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="270" y="265">{{ s.ctn }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="270" y="283">{{ s.ctnSub }}</text>
      <rect fill="var(--dc3-amber-fill)" height="300" opacity="0.25" rx="12" width="540" x="600" y="40" stroke="var(--dc3-amber-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-amber-stroke)" font-size="11" font-weight="600" x="616" y="62">{{ s.localT }}</text>
      <line marker-end="url(#en-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="850" x2="850" y1="120" y2="180"/>
      <line marker-end="url(#en-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="850" x2="850" y1="150" y2="180"/>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="240" x="730" y="60"/><rect fill="var(--dc3-amber-fill)" height="60" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="240" x="730" y="60"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="850" y="85">{{ s.sh }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="850" y="103">{{ s.shSub }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="240" x="730" y="130"/><rect fill="var(--dc3-amber-fill)" height="50" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="240" x="730" y="130"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="850" y="153">{{ s.devenv }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="850" y="170">{{ s.devenvSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="240" x="730" y="220"/><rect fill="var(--dc3-be-fill)" height="60" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="240" x="730" y="220"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="850" y="245">{{ s.proc }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="850" y="263">{{ s.procSub }}</text>
      <line marker-end="url(#en-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" x1="390" x2="730" y1="270" y2="250"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="560" y="262">{{ s.e1 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
