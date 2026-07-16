<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'变更日志生成流程', commits:'git 提交历史', commitsS:'Conventional Commits', gen:'changelog.py', genS:'解析类型/作用域并按版本聚合', change:'CHANGE.md', changeS:'分版本分类的清单', page:'本页《完整变更清单》', e1:'make changelog', e2:'覆盖写回', e3:'VitePress @include'},
  en: {aria:'Changelog generation flow', commits:'git commit history', commitsS:'Conventional Commits', gen:'changelog.py', genS:'parse type/scope, aggregate by version', change:'CHANGE.md', changeS:'versioned & categorized list', page:'This page (full changelog)', e1:'make changelog', e2:'overwrite write-back', e3:'VitePress @include'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 160">
      <defs><marker id="clg-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#clg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="240" x2="310" y1="80" y2="80"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="275" y="72">{{ s.e1 }}</text>
      <line marker-end="url(#clg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="520" x2="590" y1="80" y2="80"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="555" y="72">{{ s.e2 }}</text>
      <line marker-end="url(#clg-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="780" x2="850" y1="80" y2="80"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="815" y="72">{{ s.e3 }}</text>
      <rect x="20" y="45" width="220" height="70" rx="8" fill="var(--vp-c-bg)"/><rect x="20" y="45" width="220" height="70" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="130" y="72">{{ s.commits }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="130" y="92">{{ s.commitsS }}</text>
      <rect x="310" y="45" width="210" height="70" rx="8" fill="var(--vp-c-bg)"/><rect x="310" y="45" width="210" height="70" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="415" y="72">{{ s.gen }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="415" y="92">{{ s.genS }}</text>
      <rect x="590" y="45" width="190" height="70" rx="8" fill="var(--vp-c-bg)"/><rect x="590" y="45" width="190" height="70" rx="8" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="685" y="72">{{ s.change }}</text><text fill="var(--dc3-db-text)" font-size="9" text-anchor="middle" x="685" y="92">{{ s.changeS }}</text>
      <rect x="850" y="45" width="210" height="70" rx="8" fill="var(--vp-c-bg)"/><rect x="850" y="45" width="210" height="70" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="955" y="80">{{ s.page }}</text>
    </svg>
  </div></DiagramFrame>
</template>
