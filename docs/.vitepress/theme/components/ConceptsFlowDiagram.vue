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
    aria: '数据流与命令流',
    lane1: '数据流（南向 → 北向）', lane2: '命令流（北向 → 南向）',
    dev: '设备', drv: '驱动', mq: 'RabbitMQ', dc: '数据中心', ts: '时序存储',
    caller: '调用方 / Web / AI', gw: '网关', dev2: '设备'
  },
  en: {
    aria: 'Data flow and command flow',
    lane1: 'Data flow (south → north)', lane2: 'Command flow (north → south)',
    dev: 'Device', drv: 'Driver', mq: 'RabbitMQ', dc: 'Data Center', ts: 'Time-series store',
    caller: 'Caller / Web / AI', gw: 'Gateway', dev2: 'Device'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 380">
        <defs>
          <marker id="cf2-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
        </defs>

        <!-- lane backgrounds -->
        <rect fill="var(--dc3-be-fill)" height="120" opacity="0.35" rx="10" width="1160" x="10" y="40"/>
        <text fill="var(--dc3-be-text)" font-size="11" font-weight="600" x="26" y="62">{{ s.lane1 }}</text>
        <rect fill="var(--dc3-amber-fill)" height="120" opacity="0.4" rx="10" width="1160" x="10" y="220"/>
        <text fill="var(--dc3-amber-stroke)" font-size="11" font-weight="600" x="26" y="242">{{ s.lane2 }}</text>

        <!-- data flow arrows -->
        <line marker-end="url(#cf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="170" x2="250" y1="105" y2="105"/>
        <line marker-end="url(#cf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="410" x2="490" y1="105" y2="105"/>
        <line marker-end="url(#cf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="650" x2="730" y1="105" y2="105"/>
        <line marker-end="url(#cf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="890" x2="970" y1="105" y2="105"/>
        <!-- data flow nodes -->
        <rect fill="var(--vp-c-bg)" height="50" rx="8" width="140" x="30" y="80"/>
        <rect fill="var(--dc3-ext-fill)" height="50" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="140" x="30" y="80"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="100" y="110">{{ s.dev }}</text>
        <rect fill="var(--vp-c-bg)" height="50" rx="8" width="140" x="270" y="80"/>
        <rect fill="var(--dc3-bus-fill)" height="50" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="140" x="270" y="80"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="340" y="110">{{ s.drv }}</text>
        <rect fill="var(--vp-c-bg)" height="50" rx="8" width="140" x="510" y="80"/>
        <rect fill="var(--dc3-bus-fill)" height="50" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="140" x="510" y="80"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="580" y="110">{{ s.mq }}</text>
        <rect fill="var(--vp-c-bg)" height="50" rx="8" width="140" x="750" y="80"/>
        <rect fill="var(--dc3-be-fill)" height="50" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="140" x="750" y="80"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="820" y="110">{{ s.dc }}</text>
        <path d="M980,88 a65,12 0 0 0 130,0 v44 a65,12 0 0 1 -130,0 z" fill="var(--vp-c-bg)"/>
        <path d="M980,88 a65,12 0 0 0 130,0 v44 a65,12 0 0 1 -130,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="1045" cy="88" fill="none" rx="65" ry="12" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="1045" y="118">{{ s.ts }}</text>

        <!-- command flow arrows -->
        <line marker-end="url(#cf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="160" x2="230" y1="285" y2="285"/>
        <line marker-end="url(#cf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="370" x2="450" y1="285" y2="285"/>
        <line marker-end="url(#cf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="590" x2="670" y1="285" y2="285"/>
        <line marker-end="url(#cf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="810" x2="890" y1="285" y2="285"/>
        <line marker-end="url(#cf2-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1030" x2="1110" y1="285" y2="285"/>
        <!-- command flow nodes -->
        <rect fill="var(--vp-c-bg)" height="50" rx="8" width="150" x="20" y="260"/>
        <rect fill="var(--dc3-ext-fill)" height="50" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="150" x="20" y="260"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="95" y="290">{{ s.caller }}</text>
        <rect fill="var(--vp-c-bg)" height="50" rx="8" width="130" x="240" y="260"/>
        <rect fill="var(--dc3-be-fill)" height="50" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="130" x="240" y="260"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="305" y="290">{{ s.gw }}</text>
        <rect fill="var(--vp-c-bg)" height="50" rx="8" width="130" x="460" y="260"/>
        <rect fill="var(--dc3-be-fill)" height="50" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="130" x="460" y="260"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="525" y="290">{{ s.dc }}</text>
        <rect fill="var(--vp-c-bg)" height="50" rx="8" width="130" x="680" y="260"/>
        <rect fill="var(--dc3-bus-fill)" height="50" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="130" x="680" y="260"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="745" y="290">{{ s.mq }}</text>
        <rect fill="var(--vp-c-bg)" height="50" rx="8" width="130" x="900" y="260"/>
        <rect fill="var(--dc3-bus-fill)" height="50" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="130" x="900" y="260"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="965" y="290">{{ s.drv }}</text>
        <rect fill="var(--vp-c-bg)" height="50" rx="8" width="60" x="1110" y="260"/>
        <rect fill="var(--dc3-ext-fill)" height="50" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="60" x="1110" y="260"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="1140" y="290">{{ s.dev2 }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
