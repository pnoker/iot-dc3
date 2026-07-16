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
    aria: '属性声明与配置实例',
    driver: '驱动 Driver', driverSub: '启动注册声明',
    da: 'DriverAttribute', daSub: '驱动属性声明',
    pa: 'PointAttribute', paSub: '位号属性声明',
    dac: 'DriverAttributeConfig', dacSub: '连接配置值',
    pac: 'PointAttributeConfig', pacSub: '位号配置值',
    device: '设备 Device', deviceSub: '填配置值',
    point: '位号 Point', pointSub: '关联位号配置',
    reg: '注册', attrId: 'attributeId', fill: '填值', fillEach: '每位号填值', pointId: 'pointId'
  },
  en: {
    aria: 'Attribute declarations and config instances',
    driver: 'Driver', driverSub: 'registers on startup',
    da: 'DriverAttribute', daSub: 'driver attr decl',
    pa: 'PointAttribute', paSub: 'point attr decl',
    dac: 'DriverAttributeConfig', dacSub: 'connection config',
    pac: 'PointAttributeConfig', pacSub: 'per-point config',
    device: 'Device', deviceSub: 'fills values',
    point: 'Point', pointSub: 'links point config',
    reg: 'registers', attrId: 'attributeId', fill: 'fills', fillEach: 'fills per point', pointId: 'pointId'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1080 400">
        <defs>
          <marker id="ac-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="ac-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="7"/>
          </filter>
        </defs>

        <line marker-end="url(#ac-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="170" x2="280" y1="180" y2="100"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="205" y="126">{{ s.reg }}</text>
        <line marker-end="url(#ac-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="170" x2="280" y1="210" y2="300"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="205" y="272">{{ s.reg }}</text>
        <line marker-end="url(#ac-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" x1="480" x2="580" y1="100" y2="100"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="530" y="92">{{ s.attrId }}</text>
        <line marker-end="url(#ac-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" x1="480" x2="580" y1="300" y2="300"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="530" y="292">{{ s.attrId }}</text>
        <line marker-end="url(#ac-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="870" x2="810" y1="180" y2="110"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="860" y="140">{{ s.fill }}</text>
        <line marker-end="url(#ac-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="870" x2="810" y1="200" y2="290"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="860" y="252">{{ s.fillEach }}</text>
        <line marker-end="url(#ac-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4" x1="870" x2="812" y1="335" y2="320"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="846" y="340">{{ s.pointId }}</text>

        <rect fill="var(--dc3-bus-stroke)" height="90" opacity="0.2" rx="14" width="160" x="20" y="150" filter="url(#ac-glow)"/>
        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="140" x="30" y="160"/>
        <rect fill="var(--dc3-bus-fill)" height="70" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="2.5" width="140" x="30" y="160"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13.5" font-weight="700" text-anchor="middle" x="100" y="190">{{ s.driver }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="100" y="208">{{ s.driverSub }}</text>

        <rect fill="var(--vp-c-bg)" height="58" rx="8" width="200" x="280" y="72"/>
        <rect fill="var(--dc3-fe-fill)" height="58" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="200" x="280" y="72"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="380" y="97">{{ s.da }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="380" y="114">{{ s.daSub }}</text>

        <rect fill="var(--vp-c-bg)" height="58" rx="8" width="200" x="280" y="272"/>
        <rect fill="var(--dc3-fe-fill)" height="58" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="200" x="280" y="272"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="380" y="297">{{ s.pa }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="380" y="314">{{ s.paSub }}</text>

        <rect fill="var(--vp-c-bg)" height="58" rx="8" width="230" x="580" y="72"/>
        <rect fill="var(--dc3-amber-fill)" height="58" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="230" x="580" y="72"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="695" y="97">{{ s.dac }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="695" y="114">{{ s.dacSub }}</text>

        <rect fill="var(--vp-c-bg)" height="58" rx="8" width="230" x="580" y="272"/>
        <rect fill="var(--dc3-amber-fill)" height="58" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="230" x="580" y="272"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="695" y="297">{{ s.pac }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="695" y="314">{{ s.pacSub }}</text>

        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="170" x="870" y="150"/>
        <rect fill="var(--dc3-ext-fill)" height="70" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="170" x="870" y="150"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="955" y="180">{{ s.device }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="955" y="198">{{ s.deviceSub }}</text>

        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="170" x="870" y="305"/>
        <rect fill="var(--dc3-fe-fill)" height="55" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="170" x="870" y="305"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="955" y="328">{{ s.point }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="955" y="345">{{ s.pointSub }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
