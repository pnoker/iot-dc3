<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'

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
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 300">
        <defs>
          <marker id="dp-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
        </defs>

        <!-- arrows -->
        <line marker-end="url(#dp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="172" x2="210" y1="150"
              y2="150"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" x="178" y="142">{{ s.e1 }}</text>
        <line marker-end="url(#dp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="374" x2="412" y1="150"
              y2="150"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" x="372" y="142">{{ s.e2 }}</text>
        <line marker-end="url(#dp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="640" x2="678" y1="150"
              y2="150"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" x="640" y="142">{{ s.e3 }}</text>
        <line marker-end="url(#dp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="872" x2="910" y1="138" y2="92"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" x="876" y="116">{{ s.e4 }}</text>
        <line marker-end="url(#dp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="872" x2="910" y1="164"
              y2="212"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" x="876" y="200">{{ s.e5 }}</text>

        <!-- field devices -->
        <rect fill="var(--dc3-ext-fill)" height="68" rx="9" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"
              width="150"
              x="22"
              y="116"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="97" y="146">{{
          s.dev
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="97" y="165">{{ s.devSub }}</text>

        <!-- driver sdk -->
        <rect fill="var(--dc3-be-fill)" height="68" rx="9" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="162"
              x="212"
              y="116"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="293" y="146">{{
          s.drv
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="293" y="165">{{ s.drvSub }}</text>

        <!-- rabbitmq -->
        <rect fill="var(--dc3-bus-fill)" height="104" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"
              width="226" x="414"
              y="98"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="527" y="124">{{
          s.bus
          }}
        </text>
        <text fill="var(--dc3-bus-text)" font-size="10" text-anchor="middle" x="527" y="146">{{ s.busL1 }}</text>
        <text fill="var(--dc3-bus-text)" font-size="10" text-anchor="middle" x="527" y="163">{{ s.busL2 }}</text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="527" y="182">{{ s.busL3 }}</text>

        <!-- receiver -->
        <rect fill="var(--dc3-be-fill)" height="68" rx="9" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="192"
              x="680"
              y="116"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="776" y="146">{{
          s.rcv
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="776" y="165">{{ s.rcvSub }}</text>

        <!-- timescaledb -->
        <rect fill="var(--dc3-db-fill)" height="70" rx="9" stroke="var(--dc3-db-stroke)" stroke-width="1.5" width="246"
              x="912"
              y="40"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="1035" y="71">{{
          s.ts
          }}
        </text>
        <text fill="var(--dc3-db-text)" font-size="10" text-anchor="middle" x="1035" y="90">{{ s.tsSub }}</text>

        <!-- caffeine cache -->
        <rect fill="var(--dc3-amber-fill)" height="70" rx="9" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"
              width="246" x="912"
              y="190"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="1035" y="221">{{
          s.cache
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="1035" y="240">{{ s.cacheSub }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
