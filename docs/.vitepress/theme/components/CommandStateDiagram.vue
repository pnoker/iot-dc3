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
    aria: '命令状态机',
    pending: 'PENDING',
    sent: 'SENT',
    success: 'SUCCESS',
    failed: 'FAILED',
    expired: 'EXPIRED',
    duplicate: 'DUPLICATE',
    timeout: 'TIMEOUT',
    dead: 'DEAD',
    t1: '落库',
    t2: '发布到 broker',
    t3: '执行成功',
    t4: '返回失败 / 重投异常',
    t5: 'now 超过 expireAt',
    t6: '去重命中',
    t7: '枚举预留未产生',
    t8: '进入死信 DLX',
    noteT: '枚举已预留 · 当前链路尚不产生'
  },
  en: {
    aria: 'Command state machine',
    pending: 'PENDING',
    sent: 'SENT',
    success: 'SUCCESS',
    failed: 'FAILED',
    expired: 'EXPIRED',
    duplicate: 'DUPLICATE',
    timeout: 'TIMEOUT',
    dead: 'DEAD',
    t1: 'persisted',
    t2: 'published to broker',
    t3: 'executed ok',
    t4: 'failed / retry error',
    t5: 'now past expireAt',
    t6: 'dedup hit',
    t7: 'enum reserved, not produced',
    t8: 'to dead-letter DLX',
    noteT: 'enum reserved · not produced yet'
  }
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 440">
        <defs>
          <marker id="st-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/>
          </marker>
        </defs>
        <!-- start -> PENDING -> SENT -->
        <circle cx="40" cy="215" fill="var(--dc3-arrow)" r="9"/>
        <line marker-end="url(#st-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="49" x2="100" y1="215" y2="215"/>
        <text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="74" y="207">{{ s.t1 }}</text>
        <line marker-end="url(#st-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="240" x2="300" y1="215"
              y2="215"/>
        <text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="270" y="207">{{ s.t2 }}</text>
        <!-- SENT -> 6 terminal states -->
        <line marker-end="url(#st-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="540" y1="200"
              y2="110"/>
        <text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="470" y="148">{{ s.t3 }}</text>
        <line marker-end="url(#st-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="540" y1="225"
              y2="220"/>
        <text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="470" y="240">{{ s.t5 }}</text>
        <line marker-end="url(#st-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="540" y1="240"
              y2="330"/>
        <text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="470" y="300">{{ s.t7 }}</text>
        <line marker-end="url(#st-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="820" y1="200"
              y2="110"/>
        <text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="700" y="130">{{ s.t4 }}</text>
        <line marker-end="url(#st-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="820" y1="230"
              y2="220"/>
        <text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="700" y="245">{{ s.t6 }}</text>
        <line marker-end="url(#st-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="440" x2="820" y1="245"
              y2="330"/>
        <text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" x="700" y="305">{{ s.t8 }}</text>
        <!-- PENDING -->
        <rect fill="var(--vp-c-bg)" height="50" rx="25" width="140" x="100" y="190"/>
        <rect fill="var(--dc3-amber-fill)" height="50" rx="25" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="140"
              x="100" y="190"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="170"
              y="221">{{ s.pending }}
        </text>
        <!-- SENT (hero) -->
        <rect fill="var(--dc3-be-fill)" height="70" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="2.5" width="140" x="300"
              y="180"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="14" font-weight="700" text-anchor="middle" x="370"
              y="215">{{ s.sent }}
        </text>
        <!-- terminals col1 -->
        <rect fill="var(--vp-c-bg)" height="46" rx="6" width="180" x="540" y="85"/>
        <rect fill="var(--dc3-be-fill)" height="46" rx="6" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="180" x="540"
              y="85"/>
        <text class="d-name" fill="var(--dc3-be-text)" font-size="12" font-weight="700" text-anchor="middle" x="630"
              y="113">{{ s.success }}
        </text>
        <rect fill="var(--vp-c-bg)" height="46" rx="6" width="180" x="540" y="197"/>
        <rect fill="var(--dc3-amber-fill)" height="46" rx="6" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="180"
              x="540" y="197"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="630"
              y="225">{{ s.expired }}
        </text>
        <rect fill="var(--vp-c-bg)" height="46" rx="6" width="180" x="540" y="307"/>
        <rect fill="var(--dc3-amber-fill)" height="46" rx="6" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="180"
              x="540" y="307"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="630"
              y="335">{{ s.timeout }}
        </text>
        <!-- terminals col2 -->
        <rect fill="var(--vp-c-bg)" height="46" rx="6" width="180" x="820" y="85"/>
        <rect fill="var(--dc3-rose-fill)" height="46" rx="6" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" width="180" x="820"
              y="85"/>
        <text class="d-name" fill="var(--dc3-rose-stroke)" font-size="12" font-weight="700" text-anchor="middle" x="910"
              y="113">{{ s.failed }}
        </text>
        <rect fill="var(--vp-c-bg)" height="46" rx="6" width="180" x="820" y="197"/>
        <rect fill="var(--dc3-amber-fill)" height="46" rx="6" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="180"
              x="820" y="197"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" font-weight="700" text-anchor="middle" x="910"
              y="225">{{ s.duplicate }}
        </text>
        <rect fill="var(--vp-c-bg)" height="46" rx="6" width="180" x="820" y="307"/>
        <rect fill="var(--dc3-rose-fill)" height="46" rx="6" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" width="180" x="820"
              y="307"/>
        <text class="d-name" fill="var(--dc3-rose-stroke)" font-size="12" font-weight="700" text-anchor="middle" x="910"
              y="335">{{ s.dead }}
        </text>
        <!-- note for TIMEOUT -->
        <rect fill="var(--dc3-amber-fill)" height="34" rx="4" stroke="var(--dc3-amber-stroke)" stroke-width="1" width="320"
              x="500" y="370"/>
        <text fill="var(--dc3-box-name)" font-size="9.5" text-anchor="middle" x="630" y="392">{{ s.noteT }} (TIMEOUT)
        </text>
      </svg>
    </div>
  </DiagramFrame>
</template>
