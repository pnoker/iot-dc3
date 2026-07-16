<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'运营路径导航', a:'接设备', b:'看数据', c:'下命令', d:'收告警', e:'AI 运营', a1:'device-onboarding', b1:'data-commands', c1:'data-commands', d1:'alarms', e1:'agentic', link:'对应页面'},
  en: {aria:'Operations path navigation', a:'Onboard device', b:'View data', c:'Send command', d:'Receive alarm', e:'AI ops', a1:'device-onboarding', b1:'data-commands', c1:'data-commands', d1:'alarms', e1:'agentic', link:'related page'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1200 200">
      <defs><marker id="opi-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#opi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="200" x2="250" y1="60" y2="60"/>
      <line marker-end="url(#opi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="410" x2="460" y1="60" y2="60"/>
      <line marker-end="url(#opi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="620" x2="670" y1="60" y2="60"/>
      <line marker-end="url(#opi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="830" x2="880" y1="60" y2="60"/>
      <line stroke="var(--dc3-arrow)" stroke-width="1" stroke-dasharray="3,3" opacity="0.5" x1="120" x2="120" y1="85" y2="130"/>
      <line stroke="var(--dc3-arrow)" stroke-width="1" stroke-dasharray="3,3" opacity="0.5" x1="330" x2="330" y1="85" y2="130"/>
      <line stroke="var(--dc3-arrow)" stroke-width="1" stroke-dasharray="3,3" opacity="0.5" x1="540" x2="540" y1="85" y2="130"/>
      <line stroke="var(--dc3-arrow)" stroke-width="1" stroke-dasharray="3,3" opacity="0.5" x1="750" x2="750" y1="85" y2="130"/>
      <line stroke="var(--dc3-arrow)" stroke-width="1" stroke-dasharray="3,3" opacity="0.5" x1="960" x2="960" y1="85" y2="130"/>
      <rect v-for="(c,i) in [['a','a1'],['b','b1'],['c','c1'],['d','d1'],['e','e1']]" :key="i" :x="40+i*210" y="35" width="160" height="50" rx="8" :fill="['var(--dc3-fe-fill)','var(--dc3-be-fill)','var(--dc3-bus-fill)','var(--dc3-amber-fill)','var(--dc3-fe-fill)'][i]" :stroke="['var(--dc3-fe-stroke)','var(--dc3-be-stroke)','var(--dc3-bus-stroke)','var(--dc3-amber-stroke)','var(--dc3-fe-stroke)'][i]" stroke-width="1.5"/>
      <text v-for="(c,i) in [['a','a1'],['b','b1'],['c','c1'],['d','d1'],['e','e1']]" :key="'t'+i" :x="120+i*210" y="66" class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle">{{ s[c[0]] }}</text>
      <text v-for="(c,i) in [['a','a1'],['b','b1'],['c','c1'],['d','d1'],['e','e1']]" :key="'l'+i" :x="120+i*210" y="150" fill="var(--dc3-text2)" font-size="9" text-anchor="middle" opacity="0.7">{{ s[c[1]] }}</text>
    </svg>
  </div></DiagramFrame>
</template>
