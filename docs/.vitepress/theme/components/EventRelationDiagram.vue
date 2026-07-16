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
    aria: '事件 Event 在体系中的位置',
    device: '设备 Device', deviceSub: '上报方',
    profile: '模板 Profile', profileSub: '定义者',
    event: '事件 Event', eventSub1: '上报型能力', eventSub2: '故障 / 状态',
    param: '事件参数', paramSub: 'EventParam',
    hist: 'dc3_event_history', alarm: 'dc3_entity_alarm',
    defines: '定义', has: '含', report: '上报实例', hit: '命中规则'
  },
  en: {
    aria: 'Where Event sits in the model',
    device: 'Device', deviceSub: 'reporter',
    profile: 'Profile', profileSub: 'defines it',
    event: 'Event', eventSub1: 'report capability', eventSub2: 'fault / status',
    param: 'Event Param', paramSub: 'EventParam',
    hist: 'dc3_event_history', alarm: 'dc3_entity_alarm',
    defines: 'defines', has: 'has', report: 'reports instance', hit: 'matches rule'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1040 440">
        <defs>
          <marker id="er-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="er-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="7"/>
          </filter>
        </defs>

        <line marker-end="url(#er-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="200" x2="330" y1="215" y2="205"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="265" y="200">{{ s.defines }}</text>
        <line marker-end="url(#er-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="530" x2="660" y1="165" y2="85"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="610" y="118">{{ s.has }}</text>
        <path d="M190,74 Q430,28 670,205" fill="none" marker-end="url(#er-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="430" y="42">{{ s.report }}</text>
        <line marker-end="url(#er-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="755" x2="755" y1="285" y2="340"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="838" y="316">{{ s.hit }}</text>

        <rect fill="var(--vp-c-bg)" height="58" rx="8" width="150" x="40" y="45"/>
        <rect fill="var(--dc3-ext-fill)" height="58" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="150" x="40" y="45"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="115" y="70">{{ s.device }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="115" y="87">{{ s.deviceSub }}</text>

        <rect fill="var(--vp-c-bg)" height="68" rx="8" width="160" x="40" y="180"/>
        <rect fill="var(--dc3-be-fill)" height="68" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="160" x="40" y="180"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="120" y="210">{{ s.profile }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="120" y="228">{{ s.profileSub }}</text>

        <rect fill="var(--dc3-fe-stroke)" height="115" opacity="0.22" rx="14" width="220" x="320" y="145" filter="url(#er-glow)"/>
        <rect fill="var(--vp-c-bg)" height="95" rx="10" width="200" x="330" y="155"/>
        <rect fill="var(--dc3-fe-fill)" height="95" rx="10" stroke="var(--dc3-fe-stroke)" stroke-width="2.5" width="200" x="330" y="155"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="16" font-weight="700" text-anchor="middle" x="430" y="190">{{ s.event }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="430" y="212">{{ s.eventSub1 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="430" y="228">{{ s.eventSub2 }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="190" x="660" y="55"/>
        <rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="190" x="660" y="55"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="755" y="82">{{ s.param }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="755" y="99">{{ s.paramSub }}</text>

        <path d="M670,175 a80,15 0 0 0 180,0 v95 a80,15 0 0 1 -180,0 z" fill="var(--vp-c-bg)"/>
        <path d="M670,175 a80,15 0 0 0 180,0 v95 a80,15 0 0 1 -180,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="760" cy="175" fill="none" rx="80" ry="15" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="760" y="225">{{ s.hist }}</text>

        <path d="M670,340 a85,15 0 0 0 170,0 v60 a85,15 0 0 1 -170,0 z" fill="var(--vp-c-bg)"/>
        <path d="M670,340 a85,15 0 0 0 170,0 v60 a85,15 0 0 1 -170,0 z" fill="var(--dc3-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
        <ellipse cx="755" cy="340" fill="none" rx="85" ry="15" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="755" y="378">{{ s.alarm }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
