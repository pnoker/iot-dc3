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
    aria: '位号 Point 在体系中的位置',
    device: '设备 Device', deviceSub: '设备实例',
    profile: '模板 Profile', profileSub: '归属根',
    point: '位号 Point', pointSub: '数据项 / 控制点',
    command: '指令 Command', commandSub: '兄弟能力',
    pvName: '位号值', pvSub: 'PointValue',
    belongs: '归属', defines: '定义', fk: 'point_id', runtime: '运行态取值'
  },
  en: {
    aria: 'Where Point sits in the model',
    device: 'Device', deviceSub: 'device instance',
    profile: 'Profile', profileSub: 'root',
    point: 'Point', pointSub: 'data / control point',
    command: 'Command', commandSub: 'sibling capability',
    pvName: 'PointValue', pvSub: 'value object',
    belongs: 'belongs to', defines: 'defines', fk: 'point_id', runtime: 'runtime value'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1000 420">
        <defs>
          <marker id="ptr-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="ptr-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="6"/>
          </filter>
        </defs>

        <!-- edges (behind boxes) -->
        <line marker-end="url(#ptr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="190" x2="240" y1="215" y2="215"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="215" y="207">{{ s.belongs }}</text>
        <line marker-end="url(#ptr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="410" x2="500" y1="190" y2="120"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="445" y="150">{{ s.defines }}</text>
        <line marker-end="url(#ptr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="410" x2="500" y1="230" y2="235"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="455" y="227">{{ s.defines }}</text>
        <line marker-end="url(#ptr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" x1="690" x2="800" y1="120" y2="195"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="762" y="148">{{ s.fk }}</text>
        <path d="M120,247 Q120,362 500,362 Q820,362 868,282" fill="none" marker-end="url(#ptr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="430" y="354">{{ s.runtime }}</text>

        <!-- device -->
        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="140" x="50" y="185"/>
        <rect fill="var(--dc3-ext-fill)" height="62" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="140" x="50" y="185"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="120" y="211">{{ s.device }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="120" y="229">{{ s.deviceSub }}</text>

        <!-- profile -->
        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="170" x="240" y="180"/>
        <rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="170" x="240" y="180"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="325" y="210">{{ s.profile }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="325" y="228">{{ s.profileSub }}</text>

        <!-- point (HERO) -->
        <rect fill="var(--dc3-fe-stroke)" height="90" opacity="0.28" rx="14" width="210" x="490" y="40" filter="url(#ptr-glow)"/>
        <rect fill="var(--vp-c-bg)" height="70" rx="10" width="190" x="500" y="50"/>
        <rect fill="var(--dc3-fe-fill)" height="70" rx="10" stroke="var(--dc3-fe-stroke)" stroke-width="2.5" width="190" x="500" y="50"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="16" font-weight="700" text-anchor="middle" x="595" y="82">{{ s.point }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="595" y="102">{{ s.pointSub }}</text>

        <!-- command -->
        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="190" x="500" y="205"/>
        <rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="190" x="500" y="205"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="595" y="231">{{ s.command }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="595" y="249">{{ s.commandSub }}</text>

        <!-- pointvalue (cylinder) -->
        <path d="M800,170 a70,16 0 0 0 140,0 v110 a70,16 0 0 1 -140,0 z" fill="var(--vp-c-bg)"/>
        <path d="M800,170 a70,16 0 0 0 140,0 v110 a70,16 0 0 1 -140,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="870" cy="170" fill="none" rx="70" ry="16" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="870" y="226">{{ s.pvName }}</text>
        <text fill="var(--dc3-db-text)" font-size="9.5" text-anchor="middle" x="870" y="244">{{ s.pvSub }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
