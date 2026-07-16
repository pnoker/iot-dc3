<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria: '从模板派生新驱动五步', a: '拷贝模板', aSub: 'dc3-driver-virtual → dc3-driver-knx', b: '改 pom.xml', bSub: '继承 dc3-driver 父模块 · 加协议库依赖', c: '改 application.yml', cSub: 'dc3.driver code/name · driver/point-attribute', d: '实现 DriverCustomService', dSub: 'read() / write() / schedule()…', e: 'package + 运行', eSub: '自动注册到管理中心'},
  en: {aria: 'Five steps to derive a new driver from the template', a: 'copy template', aSub: 'dc3-driver-virtual → dc3-driver-knx', b: 'edit pom.xml', bSub: 'inherit dc3-driver parent · add protocol library', c: 'edit application.yml', cSub: 'dc3.driver code/name · driver/point-attribute', d: 'implement DriverCustomService', dSub: 'read() / write() / schedule()…', e: 'package + run', eSub: 'auto-register with management center'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 180">
      <defs><marker id="daf1-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#daf1-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="250" x2="290" y1="90" y2="90"/>
      <line marker-end="url(#daf1-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="510" x2="560" y1="90" y2="90"/>
      <line marker-end="url(#daf1-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="780" x2="810" y1="90" y2="90"/>
      <line marker-end="url(#daf1-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1030" x2="1060" y1="90" y2="90"/>
      <!-- A -->
      <rect x="40" y="55" width="210" height="70" rx="10" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.8"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="145" y="84">{{ s.a }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="145" y="104">{{ s.aSub }}</text>
      <!-- B -->
      <rect x="290" y="55" width="220" height="70" rx="10" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.8"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="400" y="84">{{ s.b }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="400" y="104">{{ s.bSub }}</text>
      <!-- C -->
      <rect x="560" y="55" width="220" height="70" rx="10" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.8"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="670" y="84">{{ s.c }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="670" y="104">{{ s.cSub }}</text>
      <!-- D -->
      <rect x="810" y="55" width="220" height="70" rx="10" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="920" y="84">{{ s.d }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="920" y="104">{{ s.dSub }}</text>
      <!-- E -->
      <rect x="1060" y="55" width="200" height="70" rx="10" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.8"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="1160" y="84">{{ s.e }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="1160" y="104">{{ s.eSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
