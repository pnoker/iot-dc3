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
    aria: '驱动 Driver 在体系中的位置',
    driver: '驱动 Driver', driverSub1: 'dc3-driver-*', driverSub2: '协议适配',
    mgr: '管理中心 Manager', mgrSub: '注册入口',
    da: 'DriverAttribute', daSub: '驱动配置项',
    device: '设备 Device', deviceSub: '1..N',
    profile: '模板 Profile', profileSub: '归属根',
    point: '位号 Point', pointSub: '采集项',
    dac: 'DriverAttributeConfig', dacSub: '连接配置',
    pv: '位号值 PointValue',
    e1: '注册身份', e2: '注册声明', e3: '采集承载', e4: '归属', e5: '按声明填值', e6: '定义', e7: '采集翻译'
  },
  en: {
    aria: 'Where Driver sits in the model',
    driver: 'Driver', driverSub1: 'dc3-driver-*', driverSub2: 'protocol adapter',
    mgr: 'Manager Center', mgrSub: 'registry',
    da: 'DriverAttribute', daSub: 'driver config items',
    device: 'Device', deviceSub: '1..N',
    profile: 'Profile', profileSub: 'root',
    point: 'Point', pointSub: 'sampled item',
    dac: 'DriverAttributeConfig', dacSub: 'connection config',
    pv: 'PointValue',
    e1: 'registers identity', e2: 'declares attrs', e3: 'hosts', e4: 'belongs to', e5: 'fills per decl', e6: 'defines', e7: 'collects & translates'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1120 480">
        <defs>
          <marker id="drv-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="drv-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="7"/>
          </filter>
        </defs>

        <line marker-end="url(#drv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="230" x2="340" y1="215" y2="110"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="262" y="150">{{ s.e1 }}</text>
        <line marker-end="url(#drv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="230" x2="340" y1="240" y2="240"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="285" y="232">{{ s.e2 }}</text>
        <line marker-end="url(#drv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="230" x2="340" y1="265" y2="390"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="262" y="345">{{ s.e3 }}</text>
        <line marker-end="url(#drv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="520" x2="640" y1="380" y2="180"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="565" y="272">{{ s.e4 }}</text>
        <line marker-end="url(#drv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="520" x2="640" y1="400" y2="390"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="580" y="392">{{ s.e5 }}</text>
        <line marker-end="url(#drv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="725" x2="720" y1="212" y2="250"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="762" y="234">{{ s.e6 }}</text>
        <path d="M230,275 Q560,470 920,290" fill="none" marker-end="url(#drv-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="575" y="462">{{ s.e7 }}</text>

        <rect fill="var(--dc3-bus-stroke)" height="110" opacity="0.2" rx="14" width="200" x="40" y="180" filter="url(#drv-glow)"/>
        <rect fill="var(--vp-c-bg)" height="90" rx="10" width="180" x="50" y="190"/>
        <rect fill="var(--dc3-bus-fill)" height="90" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="2.5" width="180" x="50" y="190"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="15.5" font-weight="700" text-anchor="middle" x="140" y="225">{{ s.driver }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="140" y="246">{{ s.driverSub1 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="140" y="262">{{ s.driverSub2 }}</text>

        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="200" x="340" y="80"/>
        <rect fill="var(--dc3-be-fill)" height="62" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="200" x="340" y="80"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="440" y="108">{{ s.mgr }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="440" y="126">{{ s.mgrSub }}</text>

        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="220" x="340" y="210"/>
        <rect fill="var(--dc3-fe-fill)" height="62" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="220" x="340" y="210"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="450" y="237">{{ s.da }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="450" y="255">{{ s.daSub }}</text>

        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="180" x="340" y="360"/>
        <rect fill="var(--dc3-ext-fill)" height="62" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="180" x="340" y="360"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="430" y="387">{{ s.device }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="430" y="405">{{ s.deviceSub }}</text>

        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="170" x="640" y="150"/>
        <rect fill="var(--dc3-be-fill)" height="62" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="170" x="640" y="150"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="725" y="178">{{ s.profile }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="725" y="196">{{ s.profileSub }}</text>

        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="160" x="640" y="250"/>
        <rect fill="var(--dc3-fe-fill)" height="62" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="160" x="640" y="250"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="720" y="278">{{ s.point }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="720" y="296">{{ s.pointSub }}</text>

        <rect fill="var(--vp-c-bg)" height="62" rx="8" width="220" x="640" y="360"/>
        <rect fill="var(--dc3-amber-fill)" height="62" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="220" x="640" y="360"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="750" y="387">{{ s.dac }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="750" y="405">{{ s.dacSub }}</text>

        <path d="M920,200 a80,16 0 0 0 160,0 v100 a80,16 0 0 1 -160,0 z" fill="var(--vp-c-bg)"/>
        <path d="M920,200 a80,16 0 0 0 160,0 v100 a80,16 0 0 1 -160,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="1000" cy="200" fill="none" rx="80" ry="16" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="1000" y="255">{{ s.pv }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
