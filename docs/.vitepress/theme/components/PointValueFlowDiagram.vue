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
    aria: '位号值数据管道：采集→换算→落库→查询',
    dev: '设备', devSub: 'Device',
    drv: '驱动', drvSub: '采集 + 换算',
    mq1: 'RabbitMQ', mq2: '位号值队列',
    dc: '数据中心', dcSub: '消费 + 落库',
    ts1: 'TimescaleDB', ts2: 'dc3_point_value',
    app: '应用 / 看板', appSub: 'App / dashboard',
    e1: 'raw → cal → num', e2: '持久化', e3: 'latest / 聚合查询'
  },
  en: {
    aria: 'Point value pipeline: collect → convert → store → query',
    dev: 'Device', devSub: 'field device',
    drv: 'Driver', drvSub: 'collect & convert',
    mq1: 'RabbitMQ', mq2: 'point-value queue',
    dc: 'Data Center', dcSub: 'consume & persist',
    ts1: 'TimescaleDB', ts2: 'dc3_point_value',
    app: 'App / Dashboard', appSub: 'queries',
    e1: 'raw → cal → num', e2: 'persist', e3: 'latest / aggregate query'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1100 400">
        <defs>
          <marker id="pvf-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="pvf-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="8"/>
          </filter>
        </defs>

        <line marker-end="url(#pvf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="140" x2="200" y1="185" y2="182"/>
        <line marker-end="url(#pvf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="400" x2="450" y1="182" y2="182"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="425" y="174">{{ s.e1 }}</text>
        <line marker-end="url(#pvf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="650" x2="700" y1="182" y2="180"/>
        <line marker-end="url(#pvf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="900" x2="960" y1="165" y2="160"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="930" y="156">{{ s.e2 }}</text>
        <path d="M1020,265 Q1020,332 800,330" fill="none" marker-end="url(#pvf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="930" y="324">{{ s.e3 }}</text>

        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="110" x="30" y="160"/>
        <rect fill="var(--dc3-ext-fill)" height="55" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="110" x="30" y="160"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="85" y="184">{{ s.dev }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="85" y="201">{{ s.devSub }}</text>

        <rect fill="var(--vp-c-bg)" height="65" rx="8" width="200" x="200" y="150"/>
        <rect fill="var(--dc3-bus-fill)" height="65" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="200" x="200" y="150"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="300" y="176">{{ s.drv }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="300" y="194">{{ s.drvSub }}</text>

        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="200" x="450" y="155"/>
        <rect fill="var(--dc3-bus-fill)" height="55" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="200" x="450" y="155"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="550" y="178">{{ s.mq1 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="550" y="195">{{ s.mq2 }}</text>

        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="200" x="700" y="145"/>
        <rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="200" x="700" y="145"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="800" y="172">{{ s.dc }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="800" y="190">{{ s.dcSub }}</text>

        <rect fill="var(--dc3-db-stroke)" height="190" opacity="0.16" rx="14" width="150" x="945" y="80" filter="url(#pvf-glow)"/>
        <path d="M960,120 a65,14 0 0 0 120,0 v140 a65,14 0 0 1 -120,0 z" fill="var(--vp-c-bg)"/>
        <path d="M960,120 a65,14 0 0 0 120,0 v140 a65,14 0 0 1 -120,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="2.5"/>
        <ellipse cx="1020" cy="120" fill="none" rx="65" ry="14" stroke="var(--dc3-db-stroke)" stroke-width="2.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="1020" y="186">{{ s.ts1 }}</text>
        <text fill="var(--dc3-db-text)" font-size="9" text-anchor="middle" x="1020" y="203">{{ s.ts2 }}</text>

        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="200" x="700" y="300"/>
        <rect fill="var(--dc3-fe-fill)" height="55" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="200" x="700" y="300"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="800" y="324">{{ s.app }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="800" y="341">{{ s.appSub }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
