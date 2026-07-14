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
    aria: '命令平面链路：客户端到现场设备的命令下发与回执',
    client: '客户端', clientSub: 'Web · CLI · AI Agent',
    gw: 'dc3-gateway', gwSub: 'API 网关 · 注入租户/principal',
    data: 'dc3-center-data', dataSub1: 'PointCommandServiceImpl', dataSub2: '校验 · 落库 PENDING → SENT',
    bus: 'RabbitMQ', busSub1: 'exchange dc3.e.point_command', busSub2: 'result 经 dc3.e.point_command_result',
    drv: 'dc3-driver-*', drvSub1: 'PointCommandReceiver', drvSub2: 'expireAt 预检 · 去重 · 加锁',
    dev: '现场设备', devSub: 'PLC · 传感器 · 电表',
    down: '▼ 命令下发', up: '▲ 结果回执',
    note1: 'PointCommandDTO.expireAt 默认 now + 10s',
    note2: '写失败 responseValue = null（不回显值）',
    note3: '类型 READ(0) / READ_BATCH(1) / WRITE(2) / WRITE_BATCH(3) / CONFIG(4)'
  },
  en: {
    aria: 'Command plane: command dispatch and receipt from client to field device',
    client: 'Client', clientSub: 'Web · CLI · AI Agent',
    gw: 'dc3-gateway', gwSub: 'API Gateway · inject tenant/principal',
    data: 'dc3-center-data', dataSub1: 'PointCommandServiceImpl', dataSub2: 'validate · persist PENDING → SENT',
    bus: 'RabbitMQ', busSub1: 'exchange dc3.e.point_command', busSub2: 'result via dc3.e.point_command_result',
    drv: 'dc3-driver-*', drvSub1: 'PointCommandReceiver', drvSub2: 'expireAt precheck · dedup · lock',
    dev: 'Field Device', devSub: 'PLC · Sensor · Meter',
    down: '▼ command dispatch', up: '▲ result receipt',
    note1: 'PointCommandDTO.expireAt defaults to now + 10s',
    note2: 'on write failure responseValue = null (no echo)',
    note3: 'type READ(0) / READ_BATCH(1) / WRITE(2) / WRITE_BATCH(3) / CONFIG(4)'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 320">
        <defs>
          <marker id="cp-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
        </defs>

        <!-- direction legend (top) -->
        <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="476" x2="512" y1="42" y2="42"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="end" x="468" y="46">{{ s.down }}</text>
        <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="640" x2="604" y1="42" y2="42"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="start" x="648" y="46">{{ s.up }}</text>

        <!-- arrows: each gap has ▼ command dispatch (top, rightward) + ▲ result receipt (bottom, leftward) -->
        <!-- client <-> gateway -->
        <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="178" x2="216" y1="124"
              y2="124"/>
        <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="216" x2="178" y1="152"
              y2="152"/>
        <!-- gateway <-> center-data -->
        <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="374" x2="412" y1="124"
              y2="124"/>
        <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="412" x2="374" y1="152"
              y2="152"/>
        <!-- center-data <-> rabbitmq -->
        <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="582" x2="620" y1="124"
              y2="124"/>
        <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="620" x2="582" y1="152"
              y2="152"/>
        <!-- rabbitmq <-> driver -->
        <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="790" x2="828" y1="124"
              y2="124"/>
        <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="828" x2="790" y1="152"
              y2="152"/>
        <!-- driver <-> device -->
        <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="986" x2="1024" y1="124"
              y2="124"/>
        <line marker-end="url(#cp-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1024" x2="986" y1="152"
              y2="152"/>

        <!-- client -->
        <rect fill="var(--dc3-fe-fill)" height="80" rx="9" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="158"
              x="20"
              y="98"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="99" y="133">{{
          s.client
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="99" y="152">{{ s.clientSub }}</text>

        <!-- gateway -->
        <rect fill="var(--dc3-be-fill)" height="80" rx="9" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="158"
              x="216"
              y="98"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="295" y="133">{{
          s.gw
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="295" y="152">{{ s.gwSub }}</text>

        <!-- center-data -->
        <rect fill="var(--dc3-be-fill)" height="96" rx="9" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="170"
              x="412"
              y="90"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="497" y="120">{{
          s.data
          }}
        </text>
        <text fill="var(--dc3-be-text)" font-size="9.5" text-anchor="middle" x="497" y="140">{{ s.dataSub1 }}</text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="497" y="157">{{ s.dataSub2 }}</text>

        <!-- rabbitmq -->
        <rect fill="var(--dc3-bus-fill)" height="96" rx="9" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"
              width="170"
              x="620"
              y="90"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="705" y="120">{{
          s.bus
          }}
        </text>
        <text fill="var(--dc3-bus-text)" font-size="9" text-anchor="middle" x="705" y="141">{{ s.busSub1 }}</text>
        <text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="705" y="157">{{ s.busSub2 }}</text>

        <!-- driver -->
        <rect fill="var(--dc3-be-fill)" height="96" rx="9" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="158"
              x="828"
              y="90"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="907" y="120">{{
          s.drv
          }}
        </text>
        <text fill="var(--dc3-be-text)" font-size="9" text-anchor="middle" x="907" y="140">{{ s.drvSub1 }}</text>
        <text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="907" y="157">{{ s.drvSub2 }}</text>

        <!-- field device -->
        <rect fill="var(--dc3-ext-fill)" height="80" rx="9" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"
              width="136"
              x="1024"
              y="98"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="1092" y="133">{{
          s.dev
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="1092" y="152">{{ s.devSub }}</text>

        <!-- key facts (verified against command-plane.md) -->
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="590" y="240">{{ s.note1 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="590" y="262">{{ s.note2 }}</text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="590" y="290">{{ s.note3 }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
