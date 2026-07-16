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
    aria: '租户与成员、角色',
    tenant: '租户 Tenant', tenantSub: 'tenant_id',
    owned: '设备 / 位号 / 数据',
    tm: '租户成员 TenantMembership',
    principal: '主体 Principal', principalSub: 'USER / SERVICE_ACCOUNT / SYSTEM',
    role: '角色 Role → 资源码',
    e1: '拥有', e2: '经成员关系', e3: 'N:1', e4: '租户内绑定'
  },
  en: {
    aria: 'Tenant, membership, and roles',
    tenant: 'Tenant', tenantSub: 'tenant_id',
    owned: 'Device / Point / Data',
    tm: 'TenantMembership',
    principal: 'Principal', principalSub: 'USER / SERVICE_ACCOUNT / SYSTEM',
    role: 'Role → resource code',
    e1: 'owns', e2: 'via membership', e3: 'N:1', e4: 'bound within tenant'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 360">
        <defs>
          <marker id="tr-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="tr-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="7"/>
          </filter>
        </defs>

        <line marker-end="url(#tr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="210" x2="310" y1="170" y2="90"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="265" y="120">{{ s.e1 }}</text>
        <line marker-end="url(#tr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="210" x2="310" y1="215" y2="265"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="265" y="248">{{ s.e2 }}</text>
        <line marker-end="url(#tr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="530" x2="600" y1="280" y2="280"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="565" y="272">{{ s.e3 }}</text>
        <line marker-end="url(#tr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="800" x2="880" y1="280" y2="280"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="840" y="272">{{ s.e4 }}</text>

        <rect fill="var(--dc3-rose-stroke)" height="110" opacity="0.2" rx="14" width="180" x="30" y="125" filter="url(#tr-glow)"/>
        <rect fill="var(--vp-c-bg)" height="90" rx="10" width="160" x="40" y="135"/>
        <rect fill="var(--dc3-rose-fill)" height="90" rx="10" stroke="var(--dc3-rose-stroke)" stroke-width="2.5" width="160" x="40" y="135"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="15" font-weight="700" text-anchor="middle" x="120" y="172">{{ s.tenant }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="120" y="196">{{ s.tenantSub }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="210" x="310" y="60"/>
        <rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="210" x="310" y="60"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="415" y="87">{{ s.owned }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="415" y="105">{{ s.tenantSub }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="220" x="310" y="250"/>
        <rect fill="var(--dc3-amber-fill)" height="60" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="220" x="310" y="250"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="420" y="285">{{ s.tm }}</text>

        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="200" x="600" y="245"/>
        <rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="200" x="600" y="245"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="700" y="272">{{ s.principal }}</text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="700" y="294">{{ s.principalSub }}</text>

        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="880" y="245"/>
        <rect fill="var(--dc3-db-fill)" height="70" rx="8" stroke="var(--dc3-db-stroke)" stroke-width="1.5" width="180" x="880" y="245"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="970" y="285">{{ s.role }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
