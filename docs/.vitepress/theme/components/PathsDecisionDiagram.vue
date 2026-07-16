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
    aria: '学习路径决策树',
    start: '你想做什么？',
    q1: '评估平台？', q2: '接入设备 / 运维？', q3: '二次开发？', q4: '自动化 / 接 AI？',
    yes: '是', no: '否',
    p1: '评估路径', p1d: '定位→概念→架构→demo',
    p2: '运营路径', p2d: '概念→首设备→接入→命令→告警',
    p3: '开发路径', p3d: '架构→平面→领域模型→驱动',
    p4: '集成路径', p4d: 'CLI→MCP→Agentic',
    p5: '贡献路径', p5d: '规范→测试→贡献指南'
  },
  en: {
    aria: 'Learning path decision tree',
    start: 'What do you want to do?',
    q1: 'Evaluating?', q2: 'Onboarding / ops?', q3: 'Custom dev?', q4: 'Automation / AI?',
    yes: 'yes', no: 'no',
    p1: 'Evaluate path', p1d: 'positioning→concepts→arch→demo',
    p2: 'Operate path', p2d: 'concepts→first device→onboard→command→alarm',
    p3: 'Develop path', p3d: 'arch→planes→domain model→driver',
    p4: 'Integrate path', p4d: 'CLI→MCP→Agentic',
    p5: 'Contribute path', p5d: 'norms→tests→contributing'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 620">
        <defs>
          <marker id="pd-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
        </defs>

        <!-- start -->
        <rect fill="var(--vp-c-bg)" height="50" rx="25" width="200" x="490" y="20"/>
        <rect fill="var(--dc3-ext-fill)" height="50" rx="25" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="200" x="490" y="20"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="590" y="51">{{ s.start }}</text>

        <!-- vertical spine of diamonds -->
        <line marker-end="url(#pd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="590" x2="590" y1="70" y2="100"/>
        <!-- diamond Q1 -->
        <polygon fill="var(--dc3-amber-fill)" points="590,100 700,150 590,200 480,150" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
        <text fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="590" y="154">{{ s.q1 }}</text>
        <line marker-end="url(#pd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="590" x2="590" y1="200" y2="230"/>
        <polygon fill="var(--dc3-amber-fill)" points="590,230 700,280 590,330 480,280" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
        <text fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="590" y="284">{{ s.q2 }}</text>
        <line marker-end="url(#pd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="590" x2="590" y1="330" y2="360"/>
        <polygon fill="var(--dc3-amber-fill)" points="590,360 700,410 590,460 480,410" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
        <text fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="590" y="414">{{ s.q3 }}</text>
        <line marker-end="url(#pd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="590" x2="590" y1="460" y2="490"/>
        <polygon fill="var(--dc3-amber-fill)" points="590,490 700,540 590,590 480,540" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
        <text fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="590" y="544">{{ s.q4 }}</text>

        <!-- yes branches (left) -->
        <path d="M480,150 L300,150" fill="none" marker-end="url(#pd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-be-text)" font-size="10" font-weight="600" x="385" y="143">{{ s.yes }}</text>
        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="220" x="60" y="119"/>
        <rect fill="var(--dc3-be-fill)" height="62" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="220" x="60" y="119"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="170" y="146">{{ s.p1 }}</text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="170" y="165">{{ s.p1d }}</text>

        <path d="M480,280 L300,280" fill="none" marker-end="url(#pd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-be-text)" font-size="10" font-weight="600" x="385" y="273">{{ s.yes }}</text>
        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="220" x="60" y="249"/>
        <rect fill="var(--dc3-be-fill)" height="62" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="220" x="60" y="249"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="170" y="276">{{ s.p2 }}</text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="170" y="295">{{ s.p2d }}</text>

        <path d="M480,410 L300,410" fill="none" marker-end="url(#pd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-be-text)" font-size="10" font-weight="600" x="385" y="403">{{ s.yes }}</text>
        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="220" x="60" y="379"/>
        <rect fill="var(--dc3-be-fill)" height="62" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="220" x="60" y="379"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="170" y="406">{{ s.p3 }}</text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="170" y="425">{{ s.p3d }}</text>

        <path d="M480,540 L300,540" fill="none" marker-end="url(#pd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-be-text)" font-size="10" font-weight="600" x="385" y="533">{{ s.yes }}</text>
        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="220" x="60" y="509"/>
        <rect fill="var(--dc3-be-fill)" height="62" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="220" x="60" y="509"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="170" y="536">{{ s.p4 }}</text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="170" y="555">{{ s.p4d }}</text>

        <!-- Q4 no -> contribute (right bottom) -->
        <path d="M700,540 L860,540" fill="none" marker-end="url(#pd-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" x="755" y="533">{{ s.no }}</text>
        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="220" x="860" y="509"/>
        <rect fill="var(--dc3-rose-fill)" height="62" rx="8" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" width="220" x="860" y="509"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="970" y="536">{{ s.p5 }}</text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="970" y="555">{{ s.p5d }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
