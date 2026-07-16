<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'Point 分层架构类图', controller:'PointController', cM1:'+add(PointVO) String', cM2:'+listByProfileId(...) List~PointVO~',
    service:'PointService', sM1:'+add(PointBO) void', sM2:'+listByProfileId(...) List~PointBO~',
    manager:'PointManager', mM1:'+save(PointDO) boolean', mM2:'+list(...) List~PointDO~',
    builder:'PointBuilder', builderTag:'«MapStruct Mapper»', bM1:'+buildBOByVO(PointVO) PointBO', bM2:'+buildDOByBO(PointBO) PointDO', bM3:'+buildBOByDO(PointDO) PointBO', bM4:'+buildVOByBO(PointBO) PointVO',
    note:'@AfterMapping 处理枚举与 JSON 扩展', e1:'传 BO', e2:'传 DO', e3:'VO↔BO', e4:'BO↔DO'},
  en: {aria:'Point layered class diagram', controller:'PointController', cM1:'+add(PointVO) String', cM2:'+listByProfileId(...) List~PointVO~',
    service:'PointService', sM1:'+add(PointBO) void', sM2:'+listByProfileId(...) List~PointBO~',
    manager:'PointManager', mM1:'+save(PointDO) boolean', mM2:'+list(...) List~PointDO~',
    builder:'PointBuilder', builderTag:'«MapStruct Mapper»', bM1:'+buildBOByVO(PointVO) PointBO', bM2:'+buildDOByBO(PointBO) PointDO', bM3:'+buildBOByDO(PointDO) PointBO', bM4:'+buildVOByBO(PointBO) PointVO',
    note:'@AfterMapping handles enums & JSON extensions', e1:'passes BO', e2:'passes DO', e3:'VO↔BO', e4:'BO↔DO'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 400">
      <defs><marker id="cl-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker>
      <marker id="cl-dep" markerHeight="8" markerWidth="10" orient="auto" refX="9" refY="4"><path d="M0,0 L9,4 L0,8" fill="none" stroke="var(--dc3-arrow)" stroke-width="1.2"/></marker></defs>
      <!-- Controller -> Service (solid) -->
      <line marker-end="url(#cl-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="310" x2="360" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="335" y="92">{{ s.e1 }}</text>
      <!-- Service -> Manager (solid) -->
      <line marker-end="url(#cl-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="590" x2="640" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="615" y="92">{{ s.e2 }}</text>
      <!-- Controller ..> Builder (dashed) -->
      <path d="M230,150 Q500,290 870,170" fill="none" marker-end="url(#cl-dep)" stroke="var(--dc3-arrow)" stroke-width="1.3" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="450" y="275">{{ s.e3 }}</text>
      <!-- Service ..> Builder (dashed) -->
      <path d="M510,150 Q700,220 870,150" fill="none" marker-end="url(#cl-dep)" stroke="var(--dc3-arrow)" stroke-width="1.3" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="700" y="205">{{ s.e4 }}</text>
      <!-- note -->
      <rect x="870" y="240" width="260" height="40" rx="4" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1"/>
      <text fill="var(--dc3-box-name)" font-size="9.5" text-anchor="middle" x="1000" y="265">{{ s.note }}</text>
      <!-- Controller -->
      <rect x="40" y="50" width="270" height="100" rx="6" fill="var(--vp-c-bg)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <rect x="40" y="50" width="270" height="28" rx="6" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="175" y="69">{{ s.controller }}</text>
      <text fill="var(--dc3-text2)" font-size="9.5" x="52" y="96">{{ s.cM1 }}</text>
      <text fill="var(--dc3-text2)" font-size="9.5" x="52" y="124">{{ s.cM2 }}</text>
      <!-- Service -->
      <rect x="360" y="50" width="230" height="100" rx="6" fill="var(--vp-c-bg)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <rect x="360" y="50" width="230" height="28" rx="6" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="475" y="69">{{ s.service }}</text>
      <text fill="var(--dc3-text2)" font-size="9.5" x="372" y="96">{{ s.sM1 }}</text>
      <text fill="var(--dc3-text2)" font-size="9.5" x="372" y="124">{{ s.sM2 }}</text>
      <!-- Manager -->
      <rect x="640" y="50" width="230" height="100" rx="6" fill="var(--vp-c-bg)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <rect x="640" y="50" width="230" height="28" rx="6" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="755" y="69">{{ s.manager }}</text>
      <text fill="var(--dc3-text2)" font-size="9.5" x="652" y="96">{{ s.mM1 }}</text>
      <text fill="var(--dc3-text2)" font-size="9.5" x="652" y="124">{{ s.mM2 }}</text>
      <!-- Builder -->
      <rect x="870" y="40" width="270" height="135" rx="6" fill="var(--vp-c-bg)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <rect x="870" y="40" width="270" height="44" rx="6" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text fill="var(--dc3-amber-stroke)" font-size="9" font-style="italic" text-anchor="middle" x="1005" y="58">{{ s.builderTag }}</text>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="1005" y="76">{{ s.builder }}</text>
      <text fill="var(--dc3-text2)" font-size="9" x="882" y="103">{{ s.bM1 }}</text>
      <text fill="var(--dc3-text2)" font-size="9" x="882" y="121">{{ s.bM2 }}</text>
      <text fill="var(--dc3-text2)" font-size="9" x="882" y="139">{{ s.bM3 }}</text>
      <text fill="var(--dc3-text2)" font-size="9" x="882" y="157">{{ s.bM4 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
