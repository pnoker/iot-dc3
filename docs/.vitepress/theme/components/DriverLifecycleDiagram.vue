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
    aria: '驱动启动注册与心跳租约',
    start: '驱动进程启动', startSub: 'startup',
    run: 'DriverInitRunner.run()', runSub: '注册 + 心跳编排',
    mgr: '管理中心 Manager', mgrSub: '注册入口',
    meta1: 'dc3_driver /', meta2: 'dc3_driver_attribute',
    hb: 'DriverHealth', hbSub: '周期心跳',
    state: 'dc3_entity_state', stateSub: 'driver=3 · online',
    off: '判定离线 offline',
    e1: 'RegisterBO 带退避重试', e2: '落库', e3: '周期心跳', e4: '续租 45s', e5: '租约到期未续'
  },
  en: {
    aria: 'Driver startup registration and heartbeat lease',
    start: 'Driver process starts', startSub: 'startup',
    run: 'DriverInitRunner.run()', runSub: 'register + heartbeat orchestration',
    mgr: 'Manager Center', mgrSub: 'registry',
    meta1: 'dc3_driver /', meta2: 'dc3_driver_attribute',
    hb: 'DriverHealth', hbSub: 'periodic heartbeat',
    state: 'dc3_entity_state', stateSub: 'driver=3 · online',
    off: 'marked offline',
    e1: 'RegisterBO w/ backoff retry', e2: 'persist', e3: 'periodic heartbeat', e4: 'renew lease 45s', e5: 'lease expired'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1100 520">
        <defs>
          <marker id="dl-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <filter id="dl-glow" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="7"/>
          </filter>
        </defs>

        <line marker-end="url(#dl-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="180" x2="250" y1="257" y2="255"/>
        <line marker-end="url(#dl-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="480" x2="560" y1="230" y2="115"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="553" y="166">{{ s.e1 }}</text>
        <line marker-end="url(#dl-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="770" x2="850" y1="115" y2="115"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="810" y="107">{{ s.e2 }}</text>
        <line marker-end="url(#dl-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="480" x2="560" y1="275" y2="390"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="553" y="346">{{ s.e3 }}</text>
        <line marker-end="url(#dl-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="780" x2="850" y1="390" y2="390"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="815" y="382">{{ s.e4 }}</text>
        <line marker-end="url(#dl-ah)" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" stroke-dasharray="5,4" x1="950" x2="930" y1="455" y2="470"/>
        <text fill="var(--dc3-rose-stroke)" font-size="11" text-anchor="middle" x="1018" y="468">{{ s.e5 }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="150" x="30" y="225"/>
        <rect fill="var(--dc3-ext-fill)" height="60" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="150" x="30" y="225"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="105" y="251">{{ s.start }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="105" y="269">{{ s.startSub }}</text>

        <rect fill="var(--dc3-be-stroke)" height="105" opacity="0.2" rx="14" width="250" x="240" y="200" filter="url(#dl-glow)"/>
        <rect fill="var(--vp-c-bg)" height="85" rx="10" width="230" x="250" y="210"/>
        <rect fill="var(--dc3-be-fill)" height="85" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="2.5" width="230" x="250" y="210"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="365" y="245">{{ s.run }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="365" y="266">{{ s.runSub }}</text>

        <rect fill="var(--vp-c-bg)" height="65" rx="8" width="210" x="560" y="82"/>
        <rect fill="var(--dc3-be-fill)" height="65" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="210" x="560" y="82"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="665" y="110">{{ s.mgr }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="665" y="128">{{ s.mgrSub }}</text>

        <path d="M850,65 a100,16 0 0 0 200,0 v95 a100,16 0 0 1 -200,0 z" fill="var(--vp-c-bg)"/>
        <path d="M850,65 a100,16 0 0 0 200,0 v95 a100,16 0 0 1 -200,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="950" cy="65" fill="none" rx="100" ry="16" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="950" y="113">{{ s.meta1 }}</text>
        <text fill="var(--dc3-db-text)" font-size="10.5" text-anchor="middle" x="950" y="130">{{ s.meta2 }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="220" x="560" y="360"/>
        <rect fill="var(--dc3-amber-fill)" height="60" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="220" x="560" y="360"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="670" y="387">{{ s.hb }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="670" y="405">{{ s.hbSub }}</text>

        <path d="M850,335 a100,16 0 0 0 200,0 v110 a100,16 0 0 1 -200,0 z" fill="var(--vp-c-bg)"/>
        <path d="M850,335 a100,16 0 0 0 200,0 v110 a100,16 0 0 1 -200,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="950" cy="335" fill="none" rx="100" ry="16" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="950" y="390">{{ s.state }}</text>
        <text fill="var(--dc3-db-text)" font-size="9.5" text-anchor="middle" x="950" y="408">{{ s.stateSub }}</text>

        <rect fill="var(--vp-c-bg)" height="45" rx="8" width="240" x="810" y="470"/>
        <rect fill="var(--dc3-rose-fill)" height="45" rx="8" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" width="240" x="810" y="470"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="930" y="498">{{ s.off }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
