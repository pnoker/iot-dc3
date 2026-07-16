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
    aria: '模板与其它概念的关系',
    device: '设备 Device', deviceSub: '设备实例',
    profile: '模板 Profile', profileSub: '能力模板 · 归属根', plus: '物模型+',
    point: '位号 Point', pointSub: '数据点 / 控制点',
    command: '指令 Command', commandSub: '动作型能力',
    event: '事件 Event', eventSub: '上报型能力',
    driver: '驱动 Driver', driverSub: '协议连接',
    fk: 'profileId 单一外键', agg: '聚合', connect: 'connect'
  },
  en: {
    aria: 'How Profile relates to other concepts',
    device: 'Device', deviceSub: 'device instance',
    profile: 'Profile', profileSub: 'capability template · root', plus: 'Thing Model+',
    point: 'Point', pointSub: 'data / control point',
    command: 'Command', commandSub: 'action capability',
    event: 'Event', eventSub: 'report capability',
    driver: 'Driver', driverSub: 'protocol link',
    fk: 'profileId (single FK)', agg: 'aggregates', connect: 'connect'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1000 440">
        <defs>
          <marker id="prd-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="prd-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="6"/>
          </filter>
        </defs>

        <!-- edges (behind boxes) -->
        <line marker-end="url(#prd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="210" x2="376" y1="222" y2="222"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="293" y="214">{{ s.fk }}</text>
        <line marker-end="url(#prd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="584" x2="756" y1="200" y2="108"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="650" y="146">{{ s.agg }}</text>
        <line marker-end="url(#prd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="584" x2="756" y1="222" y2="222"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="670" y="214">{{ s.agg }}</text>
        <line marker-end="url(#prd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="584" x2="756" y1="244" y2="336"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="650" y="300">{{ s.agg }}</text>
        <path d="M 135 254 L 135 362 Q 135 362 155 362 L 376 362" fill="none" marker-end="url(#prd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="252" y="354">{{ s.connect }}</text>

        <!-- device -->
        <rect fill="var(--vp-c-bg)" height="64" rx="8" width="150" x="60" y="190"/>
        <rect fill="var(--dc3-ext-fill)" height="64" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="150" x="60" y="190"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13.5" text-anchor="middle" x="135" y="216">{{ s.device }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="135" y="235">{{ s.deviceSub }}</text>

        <!-- profile (hero) -->
        <rect fill="var(--dc3-be-stroke)" height="84" opacity="0.28" rx="12" width="204" x="380" y="180" filter="url(#prd-glow)"/>
        <rect fill="var(--vp-c-bg)" height="84" rx="10" width="204" x="380" y="180"/>
        <rect fill="var(--dc3-be-fill)" height="84" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="2.5" width="204" x="380" y="180"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="16" font-weight="700" text-anchor="middle" x="482" y="213">{{ s.profile }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="482" y="233">{{ s.profileSub }}</text>
        <text fill="var(--dc3-be-text)" font-size="9" font-weight="600" text-anchor="middle" x="482" y="250">{{ s.plus }}</text>

        <!-- point / command / event -->
        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="184" x="760" y="72"/>
        <rect fill="var(--dc3-fe-fill)" height="62" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="184" x="760" y="72"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="852" y="100">{{ s.point }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="852" y="118">{{ s.pointSub }}</text>

        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="184" x="760" y="192"/>
        <rect fill="var(--dc3-fe-fill)" height="62" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="184" x="760" y="192"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="852" y="220">{{ s.command }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="852" y="238">{{ s.commandSub }}</text>

        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="184" x="760" y="312"/>
        <rect fill="var(--dc3-fe-fill)" height="62" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="184" x="760" y="312"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="852" y="340">{{ s.event }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="852" y="358">{{ s.eventSub }}</text>

        <!-- driver -->
        <rect fill="var(--vp-c-bg)" height="64" rx="8" width="204" x="380" y="330"/>
        <rect fill="var(--dc3-bus-fill)" height="64" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="204" x="380" y="330"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13.5" text-anchor="middle" x="482" y="358">{{ s.driver }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="482" y="377">{{ s.driverSub }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
