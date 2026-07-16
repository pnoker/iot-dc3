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
    aria: '位号值 PointValue 在体系中的位置',
    profile: '模板 Profile', profileSub: '定义者',
    point: '位号 Point', pointSub: '定义项',
    device: '设备 Device', deviceSub: '产生方',
    driver: '驱动 Driver', driverSub: '采集方',
    pv1: '位号值', pv2: 'PointValue', pv3: 'dc3_point_value',
    defines: '定义', belongs: '归属', produce: '产生', collect: '采集', read: '取数'
  },
  en: {
    aria: 'Where PointValue sits in the model',
    profile: 'Profile', profileSub: 'defines it',
    point: 'Point', pointSub: 'defined item',
    device: 'Device', deviceSub: 'producer',
    driver: 'Driver', driverSub: 'collector',
    pv1: 'PointValue', pv2: 'value object', pv3: 'dc3_point_value',
    defines: 'defines', belongs: 'belongs to', produce: 'produces', collect: 'collects', read: 'reads'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1040 420">
        <defs>
          <marker id="pvr-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="pvr-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="8"/>
          </filter>
        </defs>

        <line marker-end="url(#pvr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="200" x2="300" y1="90" y2="85"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="250" y="78">{{ s.defines }}</text>
        <line marker-end="url(#pvr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="120" x2="120" y1="180" y2="120"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="162" y="155">{{ s.belongs }}</text>
        <line marker-end="url(#pvr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="190" x2="610" y1="210" y2="200"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="400" y="194">{{ s.produce }}</text>
        <line marker-end="url(#pvr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="190" x2="610" y1="330" y2="235"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="360" y="296">{{ s.collect }}</text>
        <line marker-end="url(#pvr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="460" x2="610" y1="90" y2="150"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="545" y="112">{{ s.read }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="160" x="40" y="60"/>
        <rect fill="var(--dc3-be-fill)" height="60" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="160" x="40" y="60"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="120" y="87">{{ s.profile }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="120" y="105">{{ s.profileSub }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="160" x="300" y="55"/>
        <rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="160" x="300" y="55"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="380" y="82">{{ s.point }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="380" y="100">{{ s.pointSub }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="150" x="40" y="180"/>
        <rect fill="var(--dc3-ext-fill)" height="60" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="150" x="40" y="180"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="115" y="207">{{ s.device }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="115" y="225">{{ s.deviceSub }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="150" x="40" y="300"/>
        <rect fill="var(--dc3-bus-fill)" height="60" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="150" x="40" y="300"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="115" y="327">{{ s.driver }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="115" y="345">{{ s.driverSub }}</text>

        <rect fill="var(--dc3-db-stroke)" height="200" opacity="0.16" rx="16" width="220" x="600" y="100" filter="url(#pvr-glow)"/>
        <path d="M620,130 a90,18 0 0 0 180,0 v150 a90,18 0 0 1 -180,0 z" fill="var(--vp-c-bg)"/>
        <path d="M620,130 a90,18 0 0 0 180,0 v150 a90,18 0 0 1 -180,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="2.5"/>
        <ellipse cx="710" cy="130" fill="none" rx="90" ry="18" stroke="var(--dc3-db-stroke)" stroke-width="2.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="14" font-weight="700" text-anchor="middle" x="710" y="200">{{ s.pv1 }}</text>
        <text fill="var(--dc3-db-text)" font-size="11" text-anchor="middle" x="710" y="220">{{ s.pv2 }}</text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="710" y="240">{{ s.pv3 }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
