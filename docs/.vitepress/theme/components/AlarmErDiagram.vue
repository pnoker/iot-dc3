<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria: '告警与通知领域模型', bindSub: '绑定表（M:N 关联）',
    e1: '运行状态跟踪', e2: '触发产生告警', e3: 'notify_id 绑定通知', e4: '绑定渠道', e5: '被绑定', e6: '产生送达记录', e7: '经此渠道送达', e8: 'alarm_id 关联', e9: '事件触发告警'},
  en: {aria: 'Alarm and notification domain model', bindSub: 'bind table (M:N join)',
    e1: 'runtime state tracking', e2: 'triggers alarms', e3: 'notify_id binds notification', e4: 'binds channel', e5: 'is bound', e6: 'produces delivery records', e7: 'delivered via this channel', e8: 'alarm_id link', e9: 'event triggers alarm'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
type Role = 'be' | 'amber' | 'db' | 'ext' | 'fe'
type Ent = { k: string; x: number; y: number; role: Role; attrs: string[] }
const ENT: Ent[] = [
  {k: 'RULE_STATE', x: 40, y: 40, role: 'amber', attrs: ['long id', 'long rule_id', 'string fingerprint', 'byte entity_state_flag', 'int trigger_count', 'long alarm_id']},
  {k: 'RULE', x: 340, y: 40, role: 'be', attrs: ['long id', 'string rule_code', 'byte alarm_target_type_flag', 'long notify_id', 'long message_id', 'byte enable_flag']},
  {k: 'NOTIFY', x: 640, y: 40, role: 'be', attrs: ['long id', 'string notify_code', 'long notify_interval', 'byte auto_confirm_flag']},
  {k: 'NOTIFY_CHANNEL_BIND', x: 940, y: 40, role: 'fe', attrs: []},
  {k: 'NOTIFY_CHANNEL', x: 940, y: 300, role: 'ext', attrs: ['long id', 'string channel_code', 'byte channel_type_flag']},
  {k: 'EVENT_HISTORY', x: 40, y: 470, role: 'fe', attrs: ['long id', 'long event_id', 'byte event_type_flag', 'byte acknowledge_flag']},
  {k: 'ENTITY_ALARM', x: 340, y: 440, role: 'db', attrs: ['long id', 'byte alarm_source_flag', 'byte alarm_target_type_flag', 'long rule_id', 'long rule_state_id']},
  {k: 'NOTIFY_HISTORY', x: 940, y: 440, role: 'db', attrs: ['long id', 'long alarm_id', 'long channel_id', 'byte status_flag', 'int retry_count']}
]
const W = 220
const headH = 26
const lineH = 15
function h(e: Ent) { return e.attrs.length === 0 ? headH + 22 : headH + e.attrs.length * lineH + 8 }
function fill(r: Role) { return `var(--dc3-${r}-fill)` }
function stroke(r: Role) { return `var(--dc3-${r}-stroke)` }
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1300 620">
      <defs><marker id="aer-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <!-- relationships -->
      <line marker-end="url(#aer-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="260" x2="340" y1="101" y2="101"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="300" y="94">{{ s.e1 }}</text>
      <line marker-end="url(#aer-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="450" x2="450" y1="162" y2="440"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="start" x="458" y="300">{{ s.e2 }}</text>
      <line marker-end="url(#aer-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="560" x2="640" y1="86" y2="86"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="600" y="79">{{ s.e3 }}</text>
      <line marker-end="url(#aer-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="860" x2="940" y1="78" y2="65"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="900" y="58">{{ s.e4 }}</text>
      <line marker-end="url(#aer-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1050" x2="1050" y1="300" y2="92"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="start" x="1058" y="200">{{ s.e5 }}</text>
      <path d="M820,132 L940,450" fill="none" marker-end="url(#aer-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="start" x="848" y="290">{{ s.e6 }}</text>
      <line marker-end="url(#aer-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1050" x2="1050" y1="377" y2="440"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="start" x="1058" y="412">{{ s.e7 }}</text>
      <line marker-end="url(#aer-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="560" x2="940" y1="493" y2="493"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="750" y="486">{{ s.e8 }}</text>
      <line marker-end="url(#aer-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="260" x2="340" y1="510" y2="495"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="300" y="525">{{ s.e9 }}</text>
      <!-- entities -->
      <g v-for="e in ENT" :key="e.k">
        <rect :x="e.x" :y="e.y" :width="W" :height="h(e)" rx="8" :fill="fill(e.role)" :stroke="stroke(e.role)" stroke-width="1.5"/>
        <rect :x="e.x" :y="e.y" :width="W" :height="headH" rx="8" :fill="stroke(e.role)" opacity="0.85"/>
        <rect :x="e.x" :y="e.y + headH - 8" :width="W" height="8" :fill="stroke(e.role)" opacity="0.85"/>
        <text class="d-name" fill="#ffffff" font-size="12" font-weight="700" text-anchor="middle" :x="e.x + W / 2" :y="e.y + 17.5">{{ e.k }}</text>
        <template v-if="e.attrs.length">
          <text v-for="(a, i) in e.attrs" :key="e.k + i" fill="var(--dc3-text2)" font-size="8.5" :x="e.x + 12" :y="e.y + headH + 18 + i * lineH">{{ a }}</text>
        </template>
        <text v-else fill="var(--dc3-text2)" font-size="9" font-style="italic" text-anchor="middle" :x="e.x + W / 2" :y="e.y + headH + 16">{{ s.bindSub }}</text>
      </g>
    </svg>
  </div></DiagramFrame>
</template>
