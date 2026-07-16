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
    aria: '指令 Command 在体系中的位置',
    device: '设备 Device', deviceSub: '调用方',
    profile: '模板 Profile', profileSub: '定义者',
    command: '指令 Command', commandSub1: '动作型能力', commandSub2: '下发 / 控制',
    param: '指令参数', paramSub: 'CommandParam',
    hist: 'dc3_command_history',
    driver: '驱动 Driver', driverSub: '执行',
    defines: '定义', has: '含', call: '调用 CommandCall', exec: '经驱动执行'
  },
  en: {
    aria: 'Where Command sits in the model',
    device: 'Device', deviceSub: 'caller',
    profile: 'Profile', profileSub: 'defines it',
    command: 'Command', commandSub1: 'action capability', commandSub2: 'issue / control',
    param: 'Command Param', paramSub: 'CommandParam',
    hist: 'dc3_command_history',
    driver: 'Driver', driverSub: 'executes',
    defines: 'defines', has: 'has', call: 'invokes CommandCall', exec: 'via driver'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1040 440">
        <defs>
          <marker id="cr-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="cr-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="7"/>
          </filter>
        </defs>

        <line marker-end="url(#cr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="200" x2="340" y1="215" y2="205"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="270" y="200">{{ s.defines }}</text>
        <line marker-end="url(#cr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="540" x2="680" y1="165" y2="85"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="625" y="116">{{ s.has }}</text>
        <path d="M190,74 Q430,28 690,200" fill="none" marker-end="url(#cr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="440" y="42">{{ s.call }}</text>
        <line marker-end="url(#cr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="780" x2="780" y1="285" y2="330"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="860" y="312">{{ s.exec }}</text>

        <rect fill="var(--vp-c-bg)" height="58" rx="8" width="150" x="40" y="45"/>
        <rect fill="var(--dc3-ext-fill)" height="58" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="150" x="40" y="45"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="115" y="70">{{ s.device }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="115" y="87">{{ s.deviceSub }}</text>

        <rect fill="var(--vp-c-bg)" height="68" rx="8" width="160" x="40" y="180"/>
        <rect fill="var(--dc3-be-fill)" height="68" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="160" x="40" y="180"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="120" y="210">{{ s.profile }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="120" y="228">{{ s.profileSub }}</text>

        <rect fill="var(--dc3-fe-stroke)" height="115" opacity="0.22" rx="14" width="220" x="330" y="145" filter="url(#cr-glow)"/>
        <rect fill="var(--vp-c-bg)" height="95" rx="10" width="200" x="340" y="155"/>
        <rect fill="var(--dc3-fe-fill)" height="95" rx="10" stroke="var(--dc3-fe-stroke)" stroke-width="2.5" width="200" x="340" y="155"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="16" font-weight="700" text-anchor="middle" x="440" y="190">{{ s.command }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="440" y="212">{{ s.commandSub1 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="440" y="228">{{ s.commandSub2 }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="190" x="680" y="55"/>
        <rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="190" x="680" y="55"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="775" y="82">{{ s.param }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="775" y="99">{{ s.paramSub }}</text>

        <path d="M690,175 a80,15 0 0 0 180,0 v95 a80,15 0 0 1 -180,0 z" fill="var(--vp-c-bg)"/>
        <path d="M690,175 a80,15 0 0 0 180,0 v95 a80,15 0 0 1 -180,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="780" cy="175" fill="none" rx="80" ry="15" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="780" y="225">{{ s.hist }}</text>

        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="190" x="680" y="330"/>
        <rect fill="var(--dc3-bus-fill)" height="62" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="190" x="680" y="330"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="775" y="357">{{ s.driver }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="775" y="374">{{ s.driverSub }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
