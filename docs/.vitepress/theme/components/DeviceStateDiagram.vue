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
    aria: '设备在线判定：续租 + 扫描',
    driver: '驱动 DeviceHealth', driverSub: '周期心跳',
    dc: '数据中心', dcSub: 'DeviceStateDTO',
    state: 'dc3_entity_state', stateSub: 'expire_time · online/offline',
    scan: '扫描器 device_scan', scanSub: '周期 tick',
    renew: '续租', upsert: 'upsert expire_time',
    query: '查 expire_time ≤ now', expire: '过期 → 离线'
  },
  en: {
    aria: 'Device online verdict: renew + scan',
    driver: 'Driver DeviceHealth', driverSub: 'periodic heartbeat',
    dc: 'Data Center', dcSub: 'DeviceStateDTO',
    state: 'dc3_entity_state', stateSub: 'expire_time · online/offline',
    scan: 'Scanner device_scan', scanSub: 'periodic tick',
    renew: 'renew', upsert: 'upsert expire_time',
    query: 'where expire_time ≤ now', expire: 'expired → offline'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1000 360">
        <defs>
          <marker id="ds-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="ds-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="7"/>
          </filter>
        </defs>

        <!-- top chain -->
        <line marker-end="url(#ds-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="240" x2="340" y1="90" y2="90"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="290" y="82">{{ s.renew }}</text>
        <line marker-end="url(#ds-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="540" x2="680" y1="90" y2="95"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="610" y="82">{{ s.upsert }}</text>
        <!-- scanner -> state -->
        <line marker-end="url(#ds-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="580" x2="800" y1="250" y2="165"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="720" y="198">{{ s.query }}</text>
        <line marker-end="url(#ds-ah)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" stroke-dasharray="5,4" x1="460" x2="740" y1="250" y2="150"/>
        <text fill="var(--dc3-rose-stroke)" font-size="11" text-anchor="middle" x="558" y="232">{{ s.expire }}</text>

        <!-- driver -->
        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="200" x="40" y="60"/>
        <rect fill="var(--dc3-bus-fill)" height="62" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="200" x="40" y="60"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="140" y="88">{{ s.driver }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="140" y="105">{{ s.driverSub }}</text>

        <!-- data center -->
        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="200" x="340" y="55"/>
        <rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="200" x="340" y="55"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="440" y="84">{{ s.dc }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="440" y="103">{{ s.dcSub }}</text>

        <!-- entity_state HERO cylinder -->
        <rect fill="var(--dc3-db-stroke)" height="130" opacity="0.18" rx="14" width="240" x="660" y="35" filter="url(#ds-glow)"/>
        <path d="M690,60 a90,16 0 0 0 180,0 v90 a90,16 0 0 1 -180,0 z" fill="var(--vp-c-bg)"/>
        <path d="M690,60 a90,16 0 0 0 180,0 v90 a90,16 0 0 1 -180,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="2"/>
        <ellipse cx="780" cy="60" fill="none" rx="90" ry="16" stroke="var(--dc3-db-stroke)" stroke-width="2"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13.5" font-weight="700" text-anchor="middle" x="780" y="108">{{ s.state }}</text>
        <text fill="var(--dc3-db-text)" font-size="9.5" text-anchor="middle" x="780" y="126">{{ s.stateSub }}</text>

        <!-- scanner -->
        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="240" x="340" y="225"/>
        <rect fill="var(--dc3-amber-fill)" height="62" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="240" x="340" y="225"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="460" y="252">{{ s.scan }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="460" y="270">{{ s.scanSub }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
