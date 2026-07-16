<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria: '驱动属性从配置到注册到渲染', yml: 'application.yml', ymlSub: 'dc3.driver.*-attribute', props: 'DriverProperties', propsSub: 'SDK 绑定', reg: 'RegisterBO', regSub: 'code/name/service/tenant + 属性定义', mgr: '管理中心', mgrSub: 'dc3-center-manager', db: 'dc3_driver_attribute / dc3_point_attribute', ui: '管理侧渲染', uiSub: '设备/位号配置表单', grpc: 'gRPC'},
  en: {aria: 'Driver attributes from config to registry to rendering', yml: 'application.yml', ymlSub: 'dc3.driver.*-attribute', props: 'DriverProperties', propsSub: 'SDK binding', reg: 'RegisterBO', regSub: 'code/name/service/tenant + attribute defs', mgr: 'Management Center', mgrSub: 'dc3-center-manager', db: 'dc3_driver_attribute / dc3_point_attribute', ui: 'management-side rendering', uiSub: 'device/point config forms', grpc: 'gRPC'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1240 320">
      <defs><marker id="daf2-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#daf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="220" x2="270" y1="150" y2="150"/>
      <line marker-end="url(#daf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="450" x2="510" y1="150" y2="150"/>
      <line marker-end="url(#daf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="730" x2="790" y1="150" y2="150"/><text fill="var(--dc3-arrow-label)" font-size="9.5" font-weight="700" text-anchor="middle" x="760" y="142">{{ s.grpc }}</text>
      <path d="M920,130 Q1010,110 1070,90" fill="none" marker-end="url(#daf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
      <path d="M920,170 Q1010,195 1020,225" fill="none" marker-end="url(#daf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
      <!-- YML -->
      <rect x="40" y="120" width="180" height="60" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="130" y="146">{{ s.yml }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="130" y="164">{{ s.ymlSub }}</text>
      <!-- Props -->
      <rect x="270" y="120" width="180" height="60" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="360" y="146">{{ s.props }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="360" y="164">{{ s.propsSub }}</text>
      <!-- Reg -->
      <rect x="510" y="118" width="220" height="64" rx="8" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.8"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="620" y="144">{{ s.reg }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="620" y="164">{{ s.regSub }}</text>
      <!-- MGR hero -->
      <rect x="790" y="115" width="130" height="70" rx="10" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="855" y="144">{{ s.mgr }}</text><text fill="var(--dc3-be-text)" font-size="9" text-anchor="middle" x="855" y="164">{{ s.mgrSub }}</text>
      <!-- DB cylinder -->
      <rect x="1010" y="62" width="220" height="56" fill="var(--dc3-db-fill)"/>
      <ellipse cx="1120" cy="62" rx="110" ry="9" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <path d="M1010,62 V118" stroke="var(--dc3-db-stroke)" stroke-width="1.5" fill="none"/><path d="M1230,62 V118" stroke="var(--dc3-db-stroke)" stroke-width="1.5" fill="none"/>
      <ellipse cx="1120" cy="118" rx="110" ry="9" fill="none" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-db-text)" font-size="10.5" font-weight="700" text-anchor="middle" x="1120" y="92">{{ s.db }}</text>
      <!-- UI -->
      <rect x="1020" y="225" width="200" height="60" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="1120" y="251">{{ s.ui }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="1120" y="269">{{ s.uiSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
