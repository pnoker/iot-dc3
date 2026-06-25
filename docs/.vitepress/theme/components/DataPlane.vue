<!--
  Copyright 2016-present the IoT DC3 original author or authors.
  Licensed under the GNU Affero General Public License v3.0.

  数据平面：位号值从现场设备到落库与缓存的链路。
  纯内联 SVG，颜色走 .dc3-diagram CSS 变量随明暗主题切换，文案由 lang prop 切中英。
-->
<script setup lang="ts">
import {computed} from 'vue'

const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})

const DICT = {
  zh: {
    aria: '数据平面链路：设备到落库',
    dev: '现场设备', devSub: 'PLC · 传感器 · 电表',
    drv: '驱动 SDK', drvSub: '采集并归一为 PointValue',
    bus: 'RabbitMQ 消息总线',
    busL1: 'exchange dc3.e.value',
    busL2: 'queue dc3.q.value.point',
    busL3: 'TTL 7 天 + 死信 DLX',
    rcv: 'PointValueReceiver', rcvSub: '数据中心 · @RabbitListener · 手动 ack',
    ts: 'TimescaleDB', tsSub: 'dc3_point_value（时序超表）',
    cache: 'Caffeine 缓存', cacheSub: '每个位号的最新值',
    e1: '采集', e2: '异步投递', e3: '消费', e4: '写入历史', e5: '更新最新值'
  },
  en: {
    aria: 'Data plane: device to storage',
    dev: 'Field Devices', devSub: 'PLC · Sensor · Meter',
    drv: 'Driver SDK', drvSub: 'Collect & normalize to PointValue',
    bus: 'RabbitMQ Message Bus',
    busL1: 'exchange dc3.e.value',
    busL2: 'queue dc3.q.value.point',
    busL3: 'TTL 7d + dead-letter DLX',
    rcv: 'PointValueReceiver', rcvSub: 'Data Center · @RabbitListener · manual ack',
    ts: 'TimescaleDB', tsSub: 'dc3_point_value (hypertable)',
    cache: 'Caffeine Cache', cacheSub: 'latest value per point',
    e1: 'collect', e2: 'async publish', e3: 'consume', e4: 'write history', e5: 'update latest'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <div class="dc3-diagram">
    <svg viewBox="0 0 1180 300" role="img" :aria-label="s.aria">
      <defs>
        <marker id="dp-ah" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="var(--dc3-arrow)"/>
        </marker>
      </defs>

      <!-- arrows -->
      <line x1="172" y1="150" x2="210" y2="150" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dp-ah)"/>
      <text x="178" y="142" fill="var(--dc3-arrow-label)" font-size="10">{{ s.e1 }}</text>
      <line x1="374" y1="150" x2="412" y2="150" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dp-ah)"/>
      <text x="372" y="142" fill="var(--dc3-arrow-label)" font-size="10">{{ s.e2 }}</text>
      <line x1="640" y1="150" x2="678" y2="150" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dp-ah)"/>
      <text x="640" y="142" fill="var(--dc3-arrow-label)" font-size="10">{{ s.e3 }}</text>
      <line x1="872" y1="138" x2="910" y2="92" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dp-ah)"/>
      <text x="876" y="116" fill="var(--dc3-arrow-label)" font-size="10">{{ s.e4 }}</text>
      <line x1="872" y1="164" x2="910" y2="212" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dp-ah)"/>
      <text x="876" y="200" fill="var(--dc3-arrow-label)" font-size="10">{{ s.e5 }}</text>

      <!-- field devices -->
      <rect x="22" y="116" width="150" height="68" rx="9" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text x="97" y="146" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.dev }}</text>
      <text x="97" y="165" fill="var(--dc3-text2)" font-size="10" text-anchor="middle">{{ s.devSub }}</text>

      <!-- driver sdk -->
      <rect x="212" y="116" width="162" height="68" rx="9" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="293" y="146" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.drv }}</text>
      <text x="293" y="165" fill="var(--dc3-text2)" font-size="10" text-anchor="middle">{{ s.drvSub }}</text>

      <!-- rabbitmq -->
      <rect x="414" y="98" width="226" height="104" rx="10" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text x="527" y="124" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.bus }}</text>
      <text x="527" y="146" fill="var(--dc3-bus-text)" font-size="10" text-anchor="middle">{{ s.busL1 }}</text>
      <text x="527" y="163" fill="var(--dc3-bus-text)" font-size="10" text-anchor="middle">{{ s.busL2 }}</text>
      <text x="527" y="182" fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle">{{ s.busL3 }}</text>

      <!-- receiver -->
      <rect x="680" y="116" width="192" height="68" rx="9" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="776" y="146" class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle">{{ s.rcv }}</text>
      <text x="776" y="165" fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle">{{ s.rcvSub }}</text>

      <!-- timescaledb -->
      <rect x="912" y="40" width="246" height="70" rx="9" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text x="1035" y="71" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.ts }}</text>
      <text x="1035" y="90" fill="var(--dc3-db-text)" font-size="10" text-anchor="middle">{{ s.tsSub }}</text>

      <!-- caffeine cache -->
      <rect x="912" y="190" width="246" height="70" rx="9" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text x="1035" y="221" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.cache }}</text>
      <text x="1035" y="240" fill="var(--dc3-text2)" font-size="10" text-anchor="middle">{{ s.cacheSub }}</text>
    </svg>
  </div>
</template>
