<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria: '五类告警来源汇入统一运行告警表', rule: '规则引擎', ruleSub: 'dc3_rule 命中', offline: '离线超时', offlineSub: 'dc3_entity_state 租约过期', devRep: '设备上报', devSub: '随值更新内嵌', drvRep: '驱动上报', drvSub: 'source=3', evtRep: '事件上报', evtSub: 'dc3_event_history 触发规则',
    ea: 'dc3_entity_alarm', eaSub: '统一运行告警', notify: '通知链路', notifySub: 'email / SMS / webhook',
    l0: 'source=0 RULE', l1: 'source=1 STATE_TIMEOUT', l2: 'source=2 DEVICE_REPORT', l3: 'source=3 DRIVER_REPORT', l5: 'source=5 EVENT_REPORT'},
  en: {aria: 'Five alarm sources converge into the unified runtime alarm table', rule: 'Rule Engine', ruleSub: 'dc3_rule match', offline: 'Offline timeout', offlineSub: 'dc3_entity_state lease expired', devRep: 'Device report', devSub: 'embedded with value update', drvRep: 'Driver report', drvSub: 'source=3', evtRep: 'Event report', evtSub: 'dc3_event_history triggers rule',
    ea: 'dc3_entity_alarm', eaSub: 'unified runtime alarms', notify: 'Notification pipeline', notifySub: 'email / SMS / webhook',
    l0: 'source=0 RULE', l1: 'source=1 STATE_TIMEOUT', l2: 'source=2 DEVICE_REPORT', l3: 'source=3 DRIVER_REPORT', l5: 'source=5 EVENT_REPORT'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const CY = [60, 140, 220, 300, 380]
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1000 470">
      <defs><marker id="asf-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <!-- arrows: each source right edge (240) -> EA cylinder left (468) -->
      <line v-for="(c,i) in CY" :key="'a'+i" marker-end="url(#asf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="240" :x2="468" :y1="c" :y2="210"/>
      <text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="354" y="52">{{ s.l0 }}</text>
      <text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="354" y="132">{{ s.l1 }}</text>
      <text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="354" y="212">{{ s.l2 }}</text>
      <text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="354" y="292">{{ s.l3 }}</text>
      <text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="354" y="372">{{ s.l5 }}</text>
      <!-- EA -> Notify -->
      <line marker-end="url(#asf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="650" x2="800" y1="210" y2="210"/>
      <!-- EA cylinder -->
      <rect x="468" y="170" width="182" height="80" fill="var(--dc3-db-fill)"/>
      <ellipse cx="559" cy="170" rx="91" ry="10" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.8"/>
      <path d="M468,170 V250" stroke="var(--dc3-db-stroke)" stroke-width="1.8" fill="none"/><path d="M650,170 V250" stroke="var(--dc3-db-stroke)" stroke-width="1.8" fill="none"/>
      <ellipse cx="559" cy="250" rx="91" ry="10" fill="none" stroke="var(--dc3-db-stroke)" stroke-width="1.8"/>
      <text class="d-name" fill="var(--dc3-db-text)" font-size="12" font-weight="700" text-anchor="middle" x="559" y="206">{{ s.ea }}</text>
      <text fill="var(--dc3-db-text)" font-size="9.5" text-anchor="middle" x="559" y="226">{{ s.eaSub }}</text>
      <!-- Notify -->
      <rect x="800" y="180" width="160" height="60" rx="10" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="880" y="206">{{ s.notify }}</text>
      <text fill="var(--dc3-be-text)" font-size="9.5" text-anchor="middle" x="880" y="224">{{ s.notifySub }}</text>
      <!-- sources -->
      <rect x="30" y="34" width="210" height="52" rx="8" fill="var(--vp-c-bg)"/><rect x="30" y="34" width="210" height="52" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="135" y="56">{{ s.rule }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="135" y="74">{{ s.ruleSub }}</text>
      <rect x="30" y="114" width="210" height="52" rx="8" fill="var(--vp-c-bg)"/><rect x="30" y="114" width="210" height="52" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="135" y="136">{{ s.offline }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="135" y="154">{{ s.offlineSub }}</text>
      <rect x="30" y="194" width="210" height="52" rx="8" fill="var(--vp-c-bg)"/><rect x="30" y="194" width="210" height="52" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="135" y="216">{{ s.devRep }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="135" y="234">{{ s.devSub }}</text>
      <rect x="30" y="274" width="210" height="52" rx="8" fill="var(--vp-c-bg)"/><rect x="30" y="274" width="210" height="52" rx="8" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="135" y="296">{{ s.drvRep }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="135" y="314">{{ s.drvSub }}</text>
      <rect x="30" y="354" width="210" height="52" rx="8" fill="var(--vp-c-bg)"/><rect x="30" y="354" width="210" height="52" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="135" y="376">{{ s.evtRep }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="135" y="394">{{ s.evtSub }}</text>
    </svg>
  </div></DiagramFrame>
</template>
