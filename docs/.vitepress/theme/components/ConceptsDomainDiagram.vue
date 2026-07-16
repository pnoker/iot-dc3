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
    aria: 'DC3 全局对象模型',
    point: '位号 Point', command: '指令 Command', event: '事件 Event',
    profile: '模板 Profile', device: '设备 Device', driver: '驱动 Driver', pv: '位号值 PointValue',
    e1: '包含位号', e2: '包含命令', e3: '包含事件',
    e4: '绑定一个模板', e5: '由一个驱动接入', e6: '产生值', e7: '归属'
  },
  en: {
    aria: 'DC3 global object model',
    point: 'Point', command: 'Command', event: 'Event',
    profile: 'Profile', device: 'Device', driver: 'Driver', pv: 'PointValue',
    e1: 'has points', e2: 'has commands', e3: 'has events',
    e4: 'binds one profile', e5: 'via one driver', e6: 'produces values', e7: 'owns'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1100 560">
        <defs>
          <marker id="cd-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="cd-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="8"/>
          </filter>
        </defs>

        <!-- Profile -> Point/Command/Event -->
        <line marker-end="url(#cd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="390" x2="220" y1="250" y2="68"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="285" y="150">{{ s.e1 }}</text>
        <line marker-end="url(#cd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="390" x2="220" y1="275" y2="218"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="300" y="250">{{ s.e2 }}</text>
        <line marker-end="url(#cd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="390" x2="220" y1="300" y2="368"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="285" y="350">{{ s.e3 }}</text>
        <!-- Device -> Profile / Driver / PointValue -->
        <line marker-end="url(#cd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="700" x2="600" y1="125" y2="235"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="675" y="170">{{ s.e4 }}</text>
        <line marker-end="url(#cd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="790" x2="790" y1="125" y2="210"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="845" y="172">{{ s.e5 }}</text>
        <line marker-end="url(#cd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="890" x2="890" y1="125" y2="400"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="935" y="265">{{ s.e7 }}</text>
        <!-- Point -> PointValue (top arc) -->
        <path d="M140,68 Q470,8 810,455" fill="none" marker-end="url(#cd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="470" y="22">{{ s.e6 }}</text>

        <!-- Point / Command / Event -->
        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="160" x="60" y="40"/>
        <rect fill="var(--dc3-fe-fill)" height="55" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="160" x="60" y="40"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="140" y="73">{{ s.point }}</text>
        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="160" x="60" y="190"/>
        <rect fill="var(--dc3-fe-fill)" height="55" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="160" x="60" y="190"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="140" y="223">{{ s.command }}</text>
        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="160" x="60" y="340"/>
        <rect fill="var(--dc3-fe-fill)" height="55" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="160" x="60" y="340"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="140" y="373">{{ s.event }}</text>

        <!-- Profile HERO -->
        <rect fill="var(--dc3-be-stroke)" height="110" opacity="0.2" rx="14" width="220" x="380" y="215" filter="url(#cd-glow)"/>
        <rect fill="var(--vp-c-bg)" height="92" rx="10" width="200" x="390" y="224"/>
        <rect fill="var(--dc3-be-fill)" height="92" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="2.5" width="200" x="390" y="224"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="16" font-weight="700" text-anchor="middle" x="490" y="262">{{ s.profile }}</text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="490" y="284">能力模板 · 归属根</text>

        <!-- Device / Driver -->
        <rect fill="var(--vp-c-bg)" height="65" rx="8" width="180" x="700" y="60"/>
        <rect fill="var(--dc3-ext-fill)" height="65" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="180" x="700" y="60"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="790" y="90">{{ s.device }}</text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="790" y="108">profileId + driverId</text>
        <rect fill="var(--vp-c-bg)" height="65" rx="8" width="180" x="700" y="210"/>
        <rect fill="var(--dc3-bus-fill)" height="65" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="180" x="700" y="210"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="790" y="240">{{ s.driver }}</text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="790" y="258">协议适配</text>

        <!-- PointValue cylinder -->
        <path d="M810,455 a90,16 0 0 0 180,0 v75 a90,16 0 0 1 -180,0 z" fill="var(--vp-c-bg)"/>
        <path d="M810,455 a90,16 0 0 0 180,0 v75 a90,16 0 0 1 -180,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="900" cy="455" fill="none" rx="90" ry="16" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="900" y="500">{{ s.pv }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
