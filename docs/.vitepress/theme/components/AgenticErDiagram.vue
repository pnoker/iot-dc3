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
    aria: '会话持久化模型',
    session: 'SESSION 会话',
    sessionSub: 'conversation_id / title / session_ext / tenant_id',
    message: 'MESSAGE 消息',
    messageSub: 'role(user/assistant) / content / model / index',
    attachment: 'ATTACHMENT 附件',
    attachmentSub: 'file_name / content_type / size / file_path',
    e1: '包含多轮消息',
    e2: '挂载附件'
  },
  en: {
    aria: 'Chat persistence model',
    session: 'SESSION',
    sessionSub: 'conversation_id / title / session_ext / tenant_id',
    message: 'MESSAGE',
    messageSub: 'role(user/assistant) / content / model / index',
    attachment: 'ATTACHMENT',
    attachmentSub: 'file_name / content_type / size / file_path',
    e1: 'has messages',
    e2: 'has attachments'
  }
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1120 320">
        <defs>
          <marker id="ager-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/>
          </marker>
          <filter id="ager-g" height="180%" width="180%" x="-40%" y="-40%">
            <feGaussianBlur stdDeviation="7"/>
          </filter>
        </defs>
        <line marker-end="url(#ager-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="410" x2="290" y1="160"
              y2="160"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="350" y="152">{{ s.e1 }}</text>
        <line marker-end="url(#ager-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="690" x2="790" y1="160"
              y2="160"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="740" y="152">{{ s.e2 }}</text>
        <rect fill="var(--vp-c-bg)" height="90" rx="8" width="230" x="60" y="115"/>
        <rect fill="var(--dc3-fe-fill)" height="90" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="230" x="60"
              y="115"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="175"
              y="145">{{ s.message }}
        </text>
        <text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="175" y="175">{{ s.messageSub }}</text>
        <rect fill="var(--dc3-be-stroke)" filter="url(#ager-g)" height="130" opacity="0.2" rx="14" width="290" x="400"
              y="95"/>
        <rect fill="var(--vp-c-bg)" height="110" rx="10" width="270" x="410" y="105"/>
        <rect fill="var(--dc3-be-fill)" height="110" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="2.5" width="270" x="410"
              y="105"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="14" font-weight="700" text-anchor="middle" x="545"
              y="138">{{ s.session }}
        </text>
        <text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="545" y="175">{{ s.sessionSub }}</text>
        <rect fill="var(--vp-c-bg)" height="90" rx="8" width="270" x="790" y="115"/>
        <rect fill="var(--dc3-amber-fill)" height="90" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="270"
              x="790" y="115"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="925"
              y="145">{{ s.attachment }}
        </text>
        <text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="925" y="175">{{ s.attachmentSub }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
