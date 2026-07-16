<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria: '规则触发后的通知送达流程', change: 'dc3_rule_state.entity_state_flag 变化', svc: 'AlarmRuleTriggerService.processXxx()', ins1: 'dc3_entity_alarm', ins1Sub: 'INSERT', ins2: 'dc3_notify_history', ins2Sub: 'INSERT · 事务内同步落 pending', send: 'NotifyTaskSender 经 RabbitMQ 发布 NotifyTaskDTO（异步）', bind: 'dc3_notify → dc3_notify_channel_bind → dc3_notify_channel', deliver: '渠道送达 email / SMS / webhook', status: '回写 status_flag · 0pending→1sent→2success / 3failed→4retry'},
  en: {aria: 'Notification delivery flow after a rule fires', change: 'dc3_rule_state.entity_state_flag changes', svc: 'AlarmRuleTriggerService.processXxx()', ins1: 'dc3_entity_alarm', ins1Sub: 'INSERT', ins2: 'dc3_notify_history', ins2Sub: 'INSERT · synchronous pending within transaction', send: 'NotifyTaskSender publishes NotifyTaskDTO via RabbitMQ (async)', bind: 'dc3_notify → dc3_notify_channel_bind → dc3_notify_channel', deliver: 'Channel delivery email / SMS / webhook', status: 'write back status_flag · 0pending→1sent→2success / 3failed→4retry'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 900 560">
      <defs><marker id="anf-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <!-- Change -> Svc -->
      <line marker-end="url(#anf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="450" x2="450" y1="56" y2="84"/>
      <!-- Svc -> Ins1 / Ins2 -->
      <path d="M380,130 Q300,150 300,176" fill="none" marker-end="url(#anf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="end" x="330" y="156">{{ s.ins1Sub }}</text>
      <path d="M520,130 Q600,150 600,176" fill="none" marker-end="url(#anf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="start" x="570" y="156">{{ s.ins2Sub }}</text>
      <!-- Ins2 -> Send -->
      <path d="M600,250 Q600,280 450,280 Q450,294 450,300" fill="none" marker-end="url(#anf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
      <!-- Send -> Bind -->
      <line marker-end="url(#anf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="450" x2="450" y1="352" y2="378"/>
      <!-- Bind -> Deliver -->
      <line marker-end="url(#anf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="450" x2="450" y1="420" y2="448"/>
      <!-- Deliver -> Status -->
      <line marker-end="url(#anf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="450" x2="450" y1="490" y2="516"/>
      <!-- Change -->
      <rect x="300" y="20" width="300" height="36" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle" x="450" y="43">{{ s.change }}</text>
      <!-- Svc -->
      <rect x="290" y="84" width="320" height="46" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="450" y="112">{{ s.svc }}</text>
      <!-- Ins1 cylinder -->
      <rect x="225" y="184" width="150" height="60" fill="var(--dc3-db-fill)"/>
      <ellipse cx="300" cy="184" rx="75" ry="8" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <path d="M225,184 V244" stroke="var(--dc3-db-stroke)" stroke-width="1.5" fill="none"/><path d="M375,184 V244" stroke="var(--dc3-db-stroke)" stroke-width="1.5" fill="none"/>
      <ellipse cx="300" cy="244" rx="75" ry="8" fill="none" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-db-text)" font-size="10.5" font-weight="700" text-anchor="middle" x="300" y="216">{{ s.ins1 }}</text>
      <!-- Ins2 cylinder -->
      <rect x="525" y="184" width="150" height="60" fill="var(--dc3-db-fill)"/>
      <ellipse cx="600" cy="184" rx="75" ry="8" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <path d="M525,184 V244" stroke="var(--dc3-db-stroke)" stroke-width="1.5" fill="none"/><path d="M675,184 V244" stroke="var(--dc3-db-stroke)" stroke-width="1.5" fill="none"/>
      <ellipse cx="600" cy="244" rx="75" ry="8" fill="none" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-db-text)" font-size="10.5" font-weight="700" text-anchor="middle" x="600" y="216">{{ s.ins2 }}</text>
      <!-- Send -->
      <rect x="250" y="300" width="400" height="52" rx="8" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.8"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle" x="450" y="322">{{ s.send }}</text>
      <!-- Bind -->
      <rect x="230" y="378" width="440" height="42" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle" x="450" y="404">{{ s.bind }}</text>
      <!-- Deliver -->
      <rect x="270" y="448" width="360" height="42" rx="8" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle" x="450" y="474">{{ s.deliver }}</text>
      <!-- Status -->
      <rect x="240" y="516" width="420" height="38" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle" x="450" y="540">{{ s.status }}</text>
    </svg>
  </div></DiagramFrame>
</template>
