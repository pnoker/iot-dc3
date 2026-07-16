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
    aria: '请求的租户校验',
    req: '请求（令牌绑定 tenantId）',
    ctrl: '控制器 requireTenant()', ctrlSub: '比对实体 tenantId',
    nf: 'NotFoundException → 404',
    ft: 'filterTenant()', ftSub: '剔除非本租户条目',
    e1: '不一致 / 不存在', e2: '批量'
  },
  en: {
    aria: 'Per-request tenant check',
    req: 'Request (token bound to tenantId)',
    ctrl: 'Controller requireTenant()', ctrlSub: 'compare entity tenantId',
    nf: 'NotFoundException → 404',
    ft: 'filterTenant()', ftSub: 'drop non-tenant entries',
    e1: 'mismatch / absent', e2: 'bulk'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 980 320">
        <defs>
          <marker id="ta-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="ta-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="7"/>
          </filter>
        </defs>

        <line marker-end="url(#ta-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="230" x2="310" y1="180" y2="180"/>
        <line marker-end="url(#ta-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="560" x2="640" y1="160" y2="100"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="610" y="120">{{ s.e1 }}</text>
        <line marker-end="url(#ta-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="560" x2="640" y1="210" y2="260"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="610" y="248">{{ s.e2 }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="200" x="30" y="150"/>
        <rect fill="var(--dc3-ext-fill)" height="60" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="200" x="30" y="150"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="130" y="178">{{ s.req }}</text>

        <rect fill="var(--dc3-be-stroke)" height="90" opacity="0.2" rx="14" width="250" x="310" y="135" filter="url(#ta-glow)"/>
        <rect fill="var(--vp-c-bg)" height="80" rx="10" width="230" x="320" y="140"/>
        <rect fill="var(--dc3-be-fill)" height="80" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="2.5" width="230" x="320" y="140"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="435" y="175">{{ s.ctrl }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="435" y="196">{{ s.ctrlSub }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="240" x="640" y="70"/>
        <rect fill="var(--dc3-rose-fill)" height="60" rx="8" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" width="240" x="640" y="70"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="760" y="105">{{ s.nf }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="250" x="640" y="240"/>
        <rect fill="var(--dc3-amber-fill)" height="60" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="250" x="640" y="240"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="765" y="267">{{ s.ft }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="765" y="285">{{ s.ftSub }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
