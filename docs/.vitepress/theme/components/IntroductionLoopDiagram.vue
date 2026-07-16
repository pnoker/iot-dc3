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
    aria: '采集与命令闭环',
    dev: '现场设备', drv: '驱动 dc3-driver-*', dc: '数据中心',
    store: '时序存储', storeSub: 'TimescaleDB',
    ai: '智能中心 Agentic', aiSub: 'Spring AI + @Tool',
    e1: '采集', e2: '归一为 PointValue', e3: '持久化',
    e4: '读数据', e5: '下发命令', e6: '命令经 RabbitMQ', e7: '执行写入'
  },
  en: {
    aria: 'Collection and command loop',
    dev: 'Field Device', drv: 'Driver dc3-driver-*', dc: 'Data Center',
    store: 'Time-series store', storeSub: 'TimescaleDB',
    ai: 'Agentic Center', aiSub: 'Spring AI + @Tool',
    e1: 'collect', e2: 'normalize to PointValue', e3: 'persist',
    e4: 'read data', e5: 'issue command', e6: 'command via RabbitMQ', e7: 'write'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 430">
        <defs>
          <marker id="il-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="il-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="8"/>
          </filter>
        </defs>

        <!-- data up -->
        <line marker-end="url(#il-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="190" x2="290" y1="195" y2="195"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="240" y="187">{{ s.e1 }}</text>
        <line marker-end="url(#il-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="470" x2="560" y1="190" y2="190"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="515" y="182">{{ s.e2 }}</text>
        <line marker-end="url(#il-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="760" x2="850" y1="180" y2="180"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="805" y="172">{{ s.e3 }}</text>
        <!-- AI <-> DC -->
        <line marker-end="url(#il-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="620" x2="620" y1="330" y2="244"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="668" y="292">{{ s.e4 }}</text>
        <line marker-end="url(#il-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="700" x2="700" y1="330" y2="244"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="748" y="320">{{ s.e5 }}</text>
        <!-- command down (DC -> Driver -> Device) -->
        <path d="M660,244 Q660,210 470,225" fill="none" marker-end="url(#il-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="565" y="222">{{ s.e6 }}</text>
        <path d="M290,210 Q240,210 190,210" fill="none" marker-end="url(#il-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="240" y="232">{{ s.e7 }}</text>

        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="150" x="40" y="160"/>
        <rect fill="var(--dc3-ext-fill)" height="70" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="150" x="40" y="160"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="115" y="190">{{ s.dev }}</text>

        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="290" y="160"/>
        <rect fill="var(--dc3-bus-fill)" height="70" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="180" x="290" y="160"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="380" y="190">{{ s.drv }}</text>

        <rect fill="var(--dc3-be-stroke)" height="92" opacity="0.2" rx="14" width="200" x="560" y="150" filter="url(#il-glow)"/>
        <rect fill="var(--vp-c-bg)" height="92" rx="10" width="200" x="560" y="150"/>
        <rect fill="var(--dc3-be-fill)" height="92" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="2.5" width="200" x="560" y="150"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="14" font-weight="700" text-anchor="middle" x="660" y="190">{{ s.dc }}</text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="660" y="212">位号值 · 命令分发 · 告警</text>

        <path d="M850,130 a85,16 0 0 0 170,0 v100 a85,16 0 0 1 -170,0 z" fill="var(--vp-c-bg)"/>
        <path d="M850,130 a85,16 0 0 0 170,0 v100 a85,16 0 0 1 -170,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="935" cy="130" fill="none" rx="85" ry="16" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="935" y="180">{{ s.store }}</text>
        <text fill="var(--dc3-db-text)" font-size="9.5" text-anchor="middle" x="935" y="198">{{ s.storeSub }}</text>

        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="220" x="550" y="330"/>
        <rect fill="var(--dc3-fe-fill)" height="70" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="220" x="550" y="330"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="660" y="358">{{ s.ai }}</text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="660" y="378">{{ s.aiSub }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
