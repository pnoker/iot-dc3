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
    aria: '设备 Device 在体系中的位置',
    profile: '模板 Profile', profileSub: '能力模型',
    driver: '驱动 Driver', driverSub: '通信通道',
    tenant: '租户 Tenant', tenantSub: '数据隔离',
    device: '设备 Device', deviceSub1: '现场设备的平台镜像', deviceSub2: '绑定 Profile + Driver',
    point: '位号 Point', pointSub: '采集 / 写入',
    pv: '位号值 PointValue',
    event: '事件 Event', eventSub: '主动上报',
    e1: '能力模型', e2: '通信通道', e3: '隔离',
    e4: 'profileId 取得', e5: '运行时产生', e6: '主动上报'
  },
  en: {
    aria: 'Where Device sits in the model',
    profile: 'Profile', profileSub: 'capability model',
    driver: 'Driver', driverSub: 'comm channel',
    tenant: 'Tenant', tenantSub: 'data isolation',
    device: 'Device', deviceSub1: 'platform mirror of a device', deviceSub2: 'binds Profile + Driver',
    point: 'Point', pointSub: 'sample / write',
    pv: 'PointValue',
    event: 'Event', eventSub: 'reported',
    e1: 'capability model', e2: 'comm channel', e3: 'isolates',
    e4: 'via profileId', e5: 'produces', e6: 'reports'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1100 460">
        <defs>
          <marker id="dr-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="dr-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="7"/>
          </filter>
        </defs>

        <!-- edges -->
        <line marker-end="url(#dr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="210" x2="400" y1="90" y2="195"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="300" y="132">{{ s.e1 }}</text>
        <line marker-end="url(#dr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="210" x2="400" y1="230" y2="230"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="305" y="222">{{ s.e2 }}</text>
        <line marker-end="url(#dr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="210" x2="400" y1="370" y2="265"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="300" y="328">{{ s.e3 }}</text>
        <line marker-end="url(#dr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="620" x2="760" y1="200" y2="110"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="700" y="142">{{ s.e4 }}</text>
        <line marker-end="url(#dr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="620" x2="760" y1="235" y2="235"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="690" y="227">{{ s.e5 }}</text>
        <line marker-end="url(#dr-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="620" x2="760" y1="265" y2="360"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="700" y="328">{{ s.e6 }}</text>

        <!-- profile / driver / tenant -->
        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="170" x="40" y="60"/>
        <rect fill="var(--dc3-be-fill)" height="62" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="170" x="40" y="60"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="125" y="88">{{ s.profile }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="125" y="105">{{ s.profileSub }}</text>

        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="170" x="40" y="200"/>
        <rect fill="var(--dc3-bus-fill)" height="62" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="170" x="40" y="200"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="125" y="228">{{ s.driver }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="125" y="245">{{ s.driverSub }}</text>

        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="170" x="40" y="340"/>
        <rect fill="var(--dc3-rose-fill)" height="62" rx="8" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" width="170" x="40" y="340"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="125" y="368">{{ s.tenant }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="125" y="385">{{ s.tenantSub }}</text>

        <!-- device HERO -->
        <rect fill="var(--dc3-ext-stroke)" height="110" opacity="0.22" rx="14" width="230" x="390" y="150" filter="url(#dr-glow)"/>
        <rect fill="var(--vp-c-bg)" height="90" rx="10" width="210" x="400" y="160"/>
        <rect fill="var(--dc3-ext-fill)" height="90" rx="10" stroke="var(--dc3-ext-stroke)" stroke-width="2.5" width="210" x="400" y="160"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="16" font-weight="700" text-anchor="middle" x="505" y="194">{{ s.device }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="505" y="214">{{ s.deviceSub1 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="505" y="230">{{ s.deviceSub2 }}</text>

        <!-- point / pointvalue / event -->
        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="180" x="760" y="80"/>
        <rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="760" y="80"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="850" y="106">{{ s.point }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="850" y="123">{{ s.pointSub }}</text>

        <path d="M760,195 a90,16 0 0 0 180,0 v80 a90,16 0 0 1 -180,0 z" fill="var(--vp-c-bg)"/>
        <path d="M760,195 a90,16 0 0 0 180,0 v80 a90,16 0 0 1 -180,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="850" cy="195" fill="none" rx="90" ry="16" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="850" y="240">{{ s.pv }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="180" x="760" y="330"/>
        <rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="760" y="330"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="850" y="356">{{ s.event }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="850" y="373">{{ s.eventSub }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
