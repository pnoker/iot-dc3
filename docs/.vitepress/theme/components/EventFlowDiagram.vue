<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in hope that it will be useful,
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
    aria: '事件上报与告警评估链路',
    dev: '设备', devSub: 'Device',
    drv: '驱动', drvSub: 'eventReportSender',
    mq1: 'RabbitMQ', mq2: 'dc3.e.event',
    dc: '数据中心', dcSub: 'EventReportReceiver', dcSub2: '消费 + 评估',
    hist: 'dc3_event_history',
    rule: '告警规则引擎', ruleSub: 'Alarm rule engine',
    alarm: 'dc3_entity_alarm',
    dto: 'EventReportDTO', persist: '持久化', eval: '提交评估', match: '匹配'
  },
  en: {
    aria: 'Event reporting and alarm evaluation pipeline',
    dev: 'Device', devSub: 'field device',
    drv: 'Driver', drvSub: 'eventReportSender',
    mq1: 'RabbitMQ', mq2: 'dc3.e.event',
    dc: 'Data Center', dcSub: 'EventReportReceiver', dcSub2: 'consume + evaluate',
    hist: 'dc3_event_history',
    rule: 'Alarm Rule Engine', ruleSub: 'evaluates events',
    alarm: 'dc3_entity_alarm',
    dto: 'EventReportDTO', persist: 'persist', eval: 'submit to evaluate', match: 'matches'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 440">
        <defs>
          <marker id="ef-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
        </defs>

        <line marker-end="url(#ef-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="140" x2="190" y1="200" y2="200"/>
        <line marker-end="url(#ef-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="390" x2="440" y1="200" y2="200"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="415" y="192">{{ s.dto }}</text>
        <line marker-end="url(#ef-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="620" x2="660" y1="200" y2="200"/>
        <line marker-end="url(#ef-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="880" x2="930" y1="180" y2="190"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="905" y="172">{{ s.persist }}</text>
        <line marker-end="url(#ef-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="770" x2="770" y1="240" y2="320"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="816" y="284">{{ s.eval }}</text>
        <line marker-end="url(#ef-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="880" x2="930" y1="350" y2="350"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="905" y="342">{{ s.match }}</text>

        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="110" x="30" y="172"/>
        <rect fill="var(--dc3-ext-fill)" height="55" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="110" x="30" y="172"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="85" y="196">{{ s.dev }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="85" y="212">{{ s.devSub }}</text>

        <rect fill="var(--vp-c-bg)" height="65" rx="8" width="200" x="190" y="168"/>
        <rect fill="var(--dc3-bus-fill)" height="65" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="200" x="190" y="168"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="290" y="194">{{ s.drv }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="290" y="212">{{ s.drvSub }}</text>

        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="180" x="440" y="172"/>
        <rect fill="var(--dc3-bus-fill)" height="55" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="180" x="440" y="172"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="530" y="194">{{ s.mq1 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="530" y="212">{{ s.mq2 }}</text>

        <rect fill="var(--vp-c-bg)" height="75" rx="8" width="220" x="660" y="165"/>
        <rect fill="var(--dc3-be-fill)" height="75" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="220" x="660" y="165"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="770" y="192">{{ s.dc }}</text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="770" y="210">{{ s.dcSub }}</text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="770" y="226">{{ s.dcSub2 }}</text>

        <path d="M930,150 a75,15 0 0 0 150,0 v95 a75,15 0 0 1 -150,0 z" fill="var(--vp-c-bg)"/>
        <path d="M930,150 a75,15 0 0 0 150,0 v95 a75,15 0 0 1 -150,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="1005" cy="150" fill="none" rx="75" ry="15" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="1005" y="205">{{ s.hist }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="220" x="660" y="320"/>
        <rect fill="var(--dc3-amber-fill)" height="60" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="220" x="660" y="320"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="770" y="346">{{ s.rule }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="770" y="363">{{ s.ruleSub }}</text>

        <path d="M930,310 a75,15 0 0 0 150,0 v85 a75,15 0 0 1 -150,0 z" fill="var(--vp-c-bg)"/>
        <path d="M930,310 a75,15 0 0 0 150,0 v85 a75,15 0 0 1 -150,0 z" fill="var(--dc3-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
        <ellipse cx="1005" cy="310" fill="none" rx="75" ry="15" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="1005" y="360">{{ s.alarm }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
