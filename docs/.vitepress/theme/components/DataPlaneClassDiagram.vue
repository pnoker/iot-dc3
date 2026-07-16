<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'PointValue 分层架构', rpv:'ReadPointValue', rpvT:'«驱动 读取原始»', rpvM:'+calculate() CalculatedPointValue', cpv:'CalculatedPointValue', cpvT:'«驱动 换算/投影»', cpvM:'+getFinalValue() / getNumericValue()', pv:'PointValue', pvT:'«驱动 发送 bean»', pvM:'+rawValue / calValue / numValue', pvbo:'PointValueBO', pvboT:'«消息 / 业务»', pvboM:'+tenantId / createTime', pvdo:'PointValueDO', pvdoT:'«持久 DB»', pvdoM:'+num_value DOUBLE', pvvo:'PointValueVO', pvvoT:'«API 响应»', pvvoM:'+hasLatestValue', e1:'calculate()', e2:'构造', e3:'经 RabbitMQ', e4:'落库', e5:'读 API'},
  en: {aria:'PointValue layered architecture', rpv:'ReadPointValue', rpvT:'«driver raw read»', rpvM:'+calculate() CalculatedPointValue', cpv:'CalculatedPointValue', cpvT:'«driver convert»', cpvM:'+getFinalValue() / getNumericValue()', pv:'PointValue', pvT:'«driver send bean»', pvM:'+rawValue / calValue / numValue', pvbo:'PointValueBO', pvboT:'«message / business»', pvboM:'+tenantId / createTime', pvdo:'PointValueDO', pvdoT:'«persisted DB»', pvdoM:'+num_value DOUBLE', pvvo:'PointValueVO', pvvoT:'«API response»', pvvoM:'+hasLatestValue', e1:'calculate()', e2:'construct', e3:'via RabbitMQ', e4:'persist', e5:'read API'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1320 240">
      <defs><marker id="dpc-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#dpc-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="220" x2="240" y1="115" y2="115"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="230" y="107">{{ s.e1 }}</text>
      <line marker-end="url(#dpc-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="430" x2="450" y1="115" y2="115"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="440" y="107">{{ s.e2 }}</text>
      <line marker-end="url(#dpc-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="630" x2="650" y1="115" y2="115"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="640" y="107">{{ s.e3 }}</text>
      <line marker-end="url(#dpc-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="830" x2="850" y1="115" y2="115"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="840" y="107">{{ s.e4 }}</text>
      <line marker-end="url(#dpc-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1030" x2="1050" y1="115" y2="115"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="1040" y="107">{{ s.e5 }}</text>
      <rect v-for="(c,i) in [{n:s.rpv,t:s.rpvT,m:s.rpvM},{n:s.cpv,t:s.cpvT,m:s.cpvM},{n:s.pv,t:s.pvT,m:s.pvM},{n:s.pvbo,t:s.pvboT,m:s.pvboM},{n:s.pvdo,t:s.pvdoT,m:s.pvdoM},{n:s.pvvo,t:s.pvvoT,m:s.pvvoM}]" :key="i" :x="20+i*210" y="60" width="200" height="120" rx="6" fill="var(--vp-c-bg)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <rect v-for="(c,i) in [0,1,2,3,4,5]" :key="'h'+i" :x="20+i*210" y="60" width="200" height="48" rx="6" :fill="['var(--dc3-amber-fill)','var(--dc3-amber-fill)','var(--dc3-bus-fill)','var(--dc3-be-fill)','var(--dc3-db-fill)','var(--dc3-fe-fill)'][i]" :stroke="['var(--dc3-amber-stroke)','var(--dc3-amber-stroke)','var(--dc3-bus-stroke)','var(--dc3-be-stroke)','var(--dc3-db-stroke)','var(--dc3-fe-stroke)'][i]" stroke-width="1.5"/>
      <text :x="120" y="80" fill="var(--dc3-text2)" font-size="8.5" font-style="italic" text-anchor="middle">{{ s.rpvT }}</text>
      <text :x="120" y="100" class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle">{{ s.rpv }}</text>
      <text :x="330" y="80" fill="var(--dc3-text2)" font-size="8.5" font-style="italic" text-anchor="middle">{{ s.cpvT }}</text>
      <text :x="330" y="100" class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle">{{ s.cpv }}</text>
      <text :x="540" y="80" fill="var(--dc3-text2)" font-size="8.5" font-style="italic" text-anchor="middle">{{ s.pvT }}</text>
      <text :x="540" y="100" class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle">{{ s.pv }}</text>
      <text :x="750" y="80" fill="var(--dc3-text2)" font-size="8.5" font-style="italic" text-anchor="middle">{{ s.pvboT }}</text>
      <text :x="750" y="100" class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle">{{ s.pvbo }}</text>
      <text :x="960" y="80" fill="var(--dc3-text2)" font-size="8.5" font-style="italic" text-anchor="middle">{{ s.pvdoT }}</text>
      <text :x="960" y="100" class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle">{{ s.pvdo }}</text>
      <text :x="1150" y="80" fill="var(--dc3-text2)" font-size="8.5" font-style="italic" text-anchor="middle">{{ s.pvvoT }}</text>
      <text :x="1150" y="100" class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle">{{ s.pvvo }}</text>
      <text :x="120" y="140" fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle">{{ s.rpvM }}</text>
      <text :x="330" y="140" fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle">{{ s.cpvM }}</text>
      <text :x="540" y="140" fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle">{{ s.pvM }}</text>
      <text :x="750" y="140" fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle">{{ s.pvboM }}</text>
      <text :x="960" y="140" fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle">{{ s.pvdoM }}</text>
      <text :x="1150" y="140" fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle">{{ s.pvvoM }}</text>
    </svg>
  </div></DiagramFrame>
</template>
