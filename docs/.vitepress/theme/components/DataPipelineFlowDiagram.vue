<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'IoT 数据管道', dev:'现场设备', devSub:'传感器 / 执行器', acq:'采集端', acqSub:'驱动 / 网关',
    bus:'消息总线', busSub:'队列 / Topic', stream:'流处理', streamSub:'实时聚合 / 告警判定',
    sink:'消费落库', tsdb:'时序数据库', tsdbSub:'超表 / 分区', query:'查询层', querySub:'仪表盘 / API / AI',
    e1:'批量推送 (背压)', e2:'降采样 / 保留'},
  en: {aria:'IoT data pipeline', dev:'Field device', devSub:'sensor / actuator', acq:'Acquisition', acqSub:'driver / gateway',
    bus:'Message bus', busSub:'queue / Topic', stream:'Stream processing', streamSub:'real-time aggregate / alarm',
    sink:'Consume & persist', tsdb:'Time-series DB', tsdbSub:'hypertable / partition', query:'Query layer', querySub:'dashboard / API / AI',
    e1:'batch push (backpressure)', e2:'downsample / retain'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 400">
      <defs><marker id="df-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <!-- main spine: Dev -> Acq -> Bus -->
      <line marker-end="url(#df-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="180" x2="230" y1="210" y2="210"/>
      <line marker-end="url(#df-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="400" x2="460" y1="210" y2="210"/><text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="430" y="202">{{ s.e1 }}</text>
      <!-- Bus -> Stream / Sink -->
      <line marker-end="url(#df-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="610" x2="690" y1="185" y2="110"/>
      <line marker-end="url(#df-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="610" x2="690" y1="235" y2="310"/>
      <!-- Sink -> TSDB -->
      <line marker-end="url(#df-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="850" x2="900" y1="320" y2="320"/>
      <!-- Stream -> Query (horizontal top) -->
      <line marker-end="url(#df-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="860" x2="1080" y1="110" y2="110"/>
      <!-- TSDB -> Query (diagonal up) -->
      <path d="M980,275 Q1040,190 1080,130" fill="none" marker-end="url(#df-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="1055" y="200">{{ s.e2 }}</text>
      <!-- Dev -->
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="150" x="30" y="175"/><rect fill="var(--dc3-ext-fill)" height="70" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="150" x="30" y="175"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="105" y="202">{{ s.dev }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="105" y="220">{{ s.devSub }}</text>
      <!-- Acq -->
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="170" x="230" y="175"/><rect fill="var(--dc3-amber-fill)" height="70" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="170" x="230" y="175"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="315" y="202">{{ s.acq }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="315" y="220">{{ s.acqSub }}</text>
      <!-- Bus cylinder -->
      <path d="M460,165 a75,14 0 0 0 150,0 v90 a75,14 0 0 1 -150,0 z" fill="var(--vp-c-bg)"/>
      <path d="M460,165 a75,14 0 0 0 150,0 v90 a75,14 0 0 1 -150,0 z" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <ellipse cx="535" cy="165" fill="none" rx="75" ry="14" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="535" y="210">{{ s.bus }}</text><text fill="var(--dc3-bus-text)" font-size="8.5" text-anchor="middle" x="535" y="228">{{ s.busSub }}</text>
      <!-- Stream -->
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="170" x="690" y="75"/><rect fill="var(--dc3-amber-fill)" height="70" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="170" x="690" y="75"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="775" y="102">{{ s.stream }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="775" y="120">{{ s.streamSub }}</text>
      <!-- Sink -->
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="160" x="690" y="290"/><rect fill="var(--dc3-amber-fill)" height="60" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="160" x="690" y="290"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="770" y="325">{{ s.sink }}</text>
      <!-- TSDB cylinder -->
      <path d="M900,275 a80,14 0 0 0 160,0 v90 a80,14 0 0 1 -160,0 z" fill="var(--vp-c-bg)"/>
      <path d="M900,275 a80,14 0 0 0 160,0 v90 a80,14 0 0 1 -160,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <ellipse cx="980" cy="275" fill="none" rx="80" ry="14" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="980" y="320">{{ s.tsdb }}</text><text fill="var(--dc3-db-text)" font-size="8.5" text-anchor="middle" x="980" y="338">{{ s.tsdbSub }}</text>
      <!-- Query -->
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="1080" y="75"/><rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="180" x="1080" y="75"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="1170" y="102">{{ s.query }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="1170" y="120">{{ s.querySub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
