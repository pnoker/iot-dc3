<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria: '驱动端命令消费处理流程', in: '收到命令 dc3.q.point_command.{service}', qExp: '已过期?', qDup: '去重命中?', expired: '标记 EXPIRED, ack', dup: '标记 DUPLICATE, ack', lock: '按设备加锁 DeviceLockManager (ReentrantLock)', exec: '执行 read() / write()', ok: '回结果 SUCCESS/FAILED', nack: 'nack 重入队 + 释放去重', fail: '回结果 FAILED + ack 避免死循环',
    lYes: '是', lNo: '否', lHit: '命中', lMiss: '未命中', lOk: '成功', lFirst: '异常, 首次投递', lRedeliver: '异常, 重投'},
  en: {aria: 'Driver-side command consumption flow', in: 'command received dc3.q.point_command.{service}', qExp: 'expired?', qDup: 'dedup hit?', expired: 'mark EXPIRED, ack', dup: 'mark DUPLICATE, ack', lock: 'lock per device DeviceLockManager (ReentrantLock)', exec: 'execute read() / write()', ok: 'return result SUCCESS/FAILED', nack: 'nack requeue + release dedup', fail: 'return result FAILED + ack avoid infinite loop',
    lYes: 'yes', lNo: 'no', lHit: 'hit', lMiss: 'miss', lOk: 'success', lFirst: 'exception, first delivery', lRedeliver: 'exception, redelivery'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1120 540">
      <defs><marker id="daf3-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <!-- In -> Exp -->
      <line marker-end="url(#daf3-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="560" x2="560" y1="46" y2="67"/>
      <!-- Exp -> Expired (left) -->
      <line marker-end="url(#daf3-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="490" x2="300" y1="95" y2="95"/><text fill="var(--dc3-arrow-label)" font-size="9" font-weight="600" text-anchor="middle" x="395" y="88">{{ s.lYes }}</text>
      <!-- Exp -> Dedup (down) -->
      <line marker-end="url(#daf3-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="560" x2="560" y1="123" y2="167"/><text fill="var(--dc3-arrow-label)" font-size="9" font-weight="600" text-anchor="start" x="568" y="150">{{ s.lNo }}</text>
      <!-- Dedup -> Dup (left) -->
      <line marker-end="url(#daf3-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="490" x2="300" y1="195" y2="195"/><text fill="var(--dc3-arrow-label)" font-size="9" font-weight="600" text-anchor="middle" x="395" y="188">{{ s.lHit }}</text>
      <!-- Dedup -> Lock (down) -->
      <line marker-end="url(#daf3-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="560" x2="560" y1="223" y2="255"/><text fill="var(--dc3-arrow-label)" font-size="9" font-weight="600" text-anchor="start" x="568" y="244">{{ s.lMiss }}</text>
      <!-- Lock -> Exec -->
      <line marker-end="url(#daf3-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="560" x2="560" y1="295" y2="325"/>
      <!-- Exec -> OK (left) -->
      <path d="M440,345 Q330,345 290,440" fill="none" marker-end="url(#daf3-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="9" font-weight="600" text-anchor="middle" x="320" y="390">{{ s.lOk }}</text>
      <!-- Exec -> Nack (down) -->
      <line marker-end="url(#daf3-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="560" x2="570" y1="365" y2="440"/><text fill="var(--dc3-arrow-label)" font-size="9" font-weight="600" text-anchor="start" x="578" y="408">{{ s.lFirst }}</text>
      <!-- Exec -> Fail (right) -->
      <path d="M680,345 Q800,345 860,440" fill="none" marker-end="url(#daf3-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/><text fill="var(--dc3-arrow-label)" font-size="9" font-weight="600" text-anchor="middle" x="815" y="390">{{ s.lRedeliver }}</text>
      <!-- In -->
      <rect x="400" y="10" width="320" height="36" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle" x="560" y="33">{{ s.in }}</text>
      <!-- Exp diamond -->
      <polygon points="560,67 630,95 560,123 490,95" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle" x="560" y="99">{{ s.qExp }}</text>
      <!-- Expired -->
      <rect x="120" y="72" width="180" height="46" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle" x="210" y="100">{{ s.expired }}</text>
      <!-- Dedup diamond -->
      <polygon points="560,167 630,195 560,223 490,195" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle" x="560" y="199">{{ s.qDup }}</text>
      <!-- Dup -->
      <rect x="120" y="172" width="180" height="46" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle" x="210" y="200">{{ s.dup }}</text>
      <!-- Lock -->
      <rect x="400" y="255" width="320" height="40" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle" x="560" y="280">{{ s.lock }}</text>
      <!-- Exec -->
      <rect x="440" y="325" width="240" height="40" rx="8" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.8"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle" x="560" y="350">{{ s.exec }}</text>
      <!-- OK -->
      <rect x="180" y="440" width="220" height="50" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-be-text)" font-size="11" font-weight="700" text-anchor="middle" x="290" y="470">{{ s.ok }}</text>
      <!-- Nack -->
      <rect x="470" y="440" width="200" height="50" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10" font-weight="700" text-anchor="middle" x="570" y="468">{{ s.nack }}</text>
      <!-- Fail -->
      <rect x="740" y="440" width="240" height="50" rx="8" fill="var(--dc3-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-rose-stroke)" font-size="10.5" font-weight="700" text-anchor="middle" x="860" y="470">{{ s.fail }}</text>
    </svg>
  </div></DiagramFrame>
</template>
