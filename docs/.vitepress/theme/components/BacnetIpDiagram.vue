<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'BACnet 设备/对象/属性寻址', drv:'dc3-driver-bacnet-ip', drvSub:'本地设备 LocalDevice',
    dev:'远端设备', devSub:'设备实例号', obj:'对象 Object', objSub:'objectType + objectInstance',
    prop:'属性 Property', propSub:'propertyId · 如 PRESENT_VALUE',
    e1:'广播发现 remoteDeviceId', e2:'objectType + instance', e3:'propertyId 读/写'},
  en: {aria:'BACnet device/object/property addressing', drv:'dc3-driver-bacnet-ip', drvSub:'LocalDevice',
    dev:'Remote Device', devSub:'device instance', obj:'Object', objSub:'objectType + objectInstance',
    prop:'Property', propSub:'propertyId · e.g. PRESENT_VALUE',
    e1:'broadcast discover remoteDeviceId', e2:'objectType + instance', e3:'propertyId read/write'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 220">
      <defs><marker id="bi-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#bi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="240" x2="350" y1="110" y2="110"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="295" y="102">{{ s.e1 }}</text>
      <line marker-end="url(#bi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="550" x2="660" y1="110" y2="110"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="605" y="102">{{ s.e2 }}</text>
      <line marker-end="url(#bi-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="860" x2="970" y1="110" y2="110"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="915" y="102">{{ s.e3 }}</text>
      <rect fill="var(--vp-c-bg)" height="90" rx="10" width="220" x="20" y="65"/>
      <rect fill="var(--dc3-bus-fill)" height="90" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="2" width="220" x="20" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="130" y="100">{{ s.drv }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="130" y="122">{{ s.drvSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="350" y="70"/>
      <rect fill="var(--dc3-ext-fill)" height="80" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="200" x="350" y="70"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="450" y="100">{{ s.dev }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="450" y="120">{{ s.devSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="660" y="70"/>
      <rect fill="var(--dc3-fe-fill)" height="80" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="200" x="660" y="70"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="760" y="100">{{ s.obj }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="760" y="120">{{ s.objSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="210" x="970" y="70"/>
      <rect fill="var(--dc3-fe-fill)" height="80" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="210" x="970" y="70"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="1075" y="100">{{ s.prop }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="1075" y="120">{{ s.propSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
