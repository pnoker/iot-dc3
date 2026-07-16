<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'设备标识与归属', phy:'现场设备', phySub:'PLC / 电表 / 温控器', dev:'设备 Device', devSub:'deviceId 唯一标识',
    tenant:'租户 Tenant', tenantSub:'tenantId 隔离边界', data:'位号 / 位号值 / 指令 / 事件',
    e1:'接入登记', e2:'归属', e3:'拥有'},
  en: {aria:'Device identity and ownership', phy:'Field device', phySub:'PLC / meter / thermostat', dev:'Device', devSub:'deviceId unique id',
    tenant:'Tenant', tenantSub:'tenantId isolation boundary', data:'Point / PointValue / Command / Event',
    e1:'onboard register', e2:'belongs to', e3:'owns'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1140 160">
      <defs><marker id="ie-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#ie-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="230" x2="290" y1="85" y2="85"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="260" y="77">{{ s.e1 }}</text>
      <line marker-end="url(#ie-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="490" x2="550" y1="85" y2="85"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="520" y="77">{{ s.e2 }}</text>
      <line marker-end="url(#ie-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="750" x2="810" y1="85" y2="85"/><text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="780" y="77">{{ s.e3 }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="220" x="10" y="45"/><rect fill="var(--dc3-ext-fill)" height="80" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="220" x="10" y="45"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="120" y="78">{{ s.phy }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="120" y="98">{{ s.phySub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="290" y="45"/><rect fill="var(--dc3-fe-fill)" height="80" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="2" width="200" x="290" y="45"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="390" y="78">{{ s.dev }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="390" y="98">{{ s.devSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="550" y="45"/><rect fill="var(--dc3-rose-fill)" height="80" rx="8" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" width="200" x="550" y="45"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="650" y="78">{{ s.tenant }}</text><text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="650" y="98">{{ s.tenantSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="320" x="810" y="45"/><rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="320" x="810" y="45"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="970" y="88">{{ s.data }}</text>
    </svg>
  </div></DiagramFrame>
</template>
