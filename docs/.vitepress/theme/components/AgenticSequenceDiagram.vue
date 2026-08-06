<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'

const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {
    aria: '对话与工具调用时序',
    user: '用户',
    chat: 'ChatClient dc3-center-agentic',
    tool: '内置工具 @Tool',
    backend: '数据中心 dc3-center-data',
    m1: '提问（查温度并把风机关掉）',
    m2: 'getLatestPointValue()',
    m3: '读位号最新值（租户隔离）',
    m4: 'PointValueBO',
    m5: '返回工具结果',
    m6: '拟调用 writePointValue（写命令）',
    note1: '写工具不直接执行，生成待确认 Action',
    m7: 'pendingConfirmation + actionId',
    m8: 'POST /action/confirm',
    m9: '执行写命令',
    m10: 'submitWrite (facade gRPC)',
    m11: '命令受理结果',
    m12: '返回执行结果',
    m13: '自然语言回答（已读值 + 已下发）'
  },
  en: {
    aria: 'Chat & tool-call sequence',
    user: 'User',
    chat: 'ChatClient dc3-center-agentic',
    tool: 'Built-in @Tool',
    backend: 'Data Center dc3-center-data',
    m1: 'ask (read temp and turn off fan)',
    m2: 'getLatestPointValue()',
    m3: 'read latest value (tenant-scoped)',
    m4: 'PointValueBO',
    m5: 'tool result',
    m6: 'call writePointValue (write cmd)',
    note1: 'write tool does not execute directly; creates a pending Action',
    m7: 'pendingConfirmation + actionId',
    m8: 'POST /action/confirm',
    m9: 'execute write command',
    m10: 'submitWrite (facade gRPC)',
    m11: 'command accepted',
    m12: 'execution result',
    m13: 'natural-language answer (read + commanded)'
  }
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const PX: Record<string, number> = {user: 120, chat: 400, tool: 680, backend: 960}
</script>
<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 640">
        <defs>
          <marker id="agsq-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/>
          </marker>
        </defs>
        <line v-for="p in ['user','chat','tool','backend']" :key="'l'+p" :x1="PX[p]" :x2="PX[p]" stroke="var(--dc3-divider)" stroke-dasharray="4,4"
              stroke-width="1" y1="70" y2="610"/>
        <rect v-for="(p,i) in ['user','chat','tool','backend']" :key="'b'+p" :fill="['var(--dc3-ext-fill)','var(--dc3-fe-fill)','var(--dc3-amber-fill)','var(--dc3-be-fill)'][i]" :stroke="['var(--dc3-ext-stroke)','var(--dc3-fe-stroke)','var(--dc3-amber-stroke)','var(--dc3-be-stroke)'][i]" :x="PX[p]-110"
              height="48" opacity="0.65"
              rx="8"
              stroke-width="1.5"
              width="220" y="22"/>
        <text v-for="p in ['user','chat','tool','backend']" :key="'t'+p" :x="PX[p]" class="d-name" fill="var(--dc3-box-name)"
              font-size="10.5" font-weight="700" text-anchor="middle" y="51">{{ s[p] }}
        </text>
        <line :x1="PX.user" :x2="PX.chat" marker-end="url(#agsq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"
              y1="100" y2="100"/>
        <text :x="(PX.user+PX.chat)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="92">{{
            s.m1
          }}
        </text>
        <line :x1="PX.chat" :x2="PX.tool" marker-end="url(#agsq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"
              y1="135" y2="135"/>
        <text :x="(PX.chat+PX.tool)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="127">{{
            s.m2
          }}
        </text>
        <line :x1="PX.tool" :x2="PX.backend" marker-end="url(#agsq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"
              y1="170" y2="170"/>
        <text :x="(PX.tool+PX.backend)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="162">
          {{ s.m3 }}
        </text>
        <line :x1="PX.backend" :x2="PX.tool" marker-end="url(#agsq-ah)" stroke="var(--dc3-arrow)"
              stroke-dasharray="5,4" stroke-width="1.5" y1="200" y2="200"/>
        <text :x="(PX.backend+PX.tool)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="192">
          {{ s.m4 }}
        </text>
        <line :x1="PX.tool" :x2="PX.chat" marker-end="url(#agsq-ah)" stroke="var(--dc3-arrow)"
              stroke-dasharray="5,4" stroke-width="1.5" y1="230" y2="230"/>
        <text :x="(PX.tool+PX.chat)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="222">{{
            s.m5
          }}
        </text>
        <rect :x="PX.chat-120" fill="var(--dc3-amber-fill)" height="30" rx="4" stroke="var(--dc3-amber-stroke)" stroke-width="1"
              width="240" y="250"/>
        <text :x="PX.chat" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle" y="270">{{ s.note1 }}</text>
        <line :x1="PX.chat" :x2="PX.user" marker-end="url(#agsq-ah)" stroke="var(--dc3-arrow)"
              stroke-dasharray="5,4" stroke-width="1.5" y1="300" y2="300"/>
        <text :x="(PX.chat+PX.user)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="292">{{
            s.m7
          }}
        </text>
        <line :x1="PX.user" :x2="PX.chat" marker-end="url(#agsq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"
              y1="335" y2="335"/>
        <text :x="(PX.user+PX.chat)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="327">{{
            s.m8
          }}
        </text>
        <line :x1="PX.chat" :x2="PX.tool" marker-end="url(#agsq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"
              y1="370" y2="370"/>
        <text :x="(PX.chat+PX.tool)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="362">{{
            s.m9
          }}
        </text>
        <line :x1="PX.tool" :x2="PX.backend" marker-end="url(#agsq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"
              y1="405" y2="405"/>
        <text :x="(PX.tool+PX.backend)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="397">
          {{ s.m10 }}
        </text>
        <line :x1="PX.backend" :x2="PX.tool" marker-end="url(#agsq-ah)" stroke="var(--dc3-arrow)"
              stroke-dasharray="5,4" stroke-width="1.5" y1="435" y2="435"/>
        <text :x="(PX.backend+PX.tool)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="427">
          {{ s.m11 }}
        </text>
        <line :x1="PX.tool" :x2="PX.chat" marker-end="url(#agsq-ah)" stroke="var(--dc3-arrow)"
              stroke-dasharray="5,4" stroke-width="1.5" y1="465" y2="465"/>
        <text :x="(PX.tool+PX.chat)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="457">{{
            s.m12
          }}
        </text>
        <path :d="`M${PX.chat},500 Q${(PX.chat+PX.user)/2},545 ${PX.user},510`" fill="none" marker-end="url(#agsq-ah)"
              stroke="var(--dc3-arrow)" stroke-dasharray="5,4" stroke-width="1.5"/>
        <text :x="(PX.chat+PX.user)/2" fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" y="540">{{
            s.m13
          }}
        </text>
      </svg>
    </div>
  </DiagramFrame>
</template>
