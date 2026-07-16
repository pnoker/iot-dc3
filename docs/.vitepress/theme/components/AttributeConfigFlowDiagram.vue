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
    aria: '属性配置流转：声明→落库→填值→下发',
    yml: 'application.yml', ymlSub: '属性定义',
    mgr: 'Manager 落库', mgrSub1: 'Attribute 表', mgrSub2: '声明的 schema',
    ui: '配置矩阵', uiSub: '设备编辑页 · 列=Attribute',
    cfg: 'Config 表', cfgSub: 'configValue',
    rt: '驱动组装协议报文', rtSub: '运行时使用',
    e1: '驱动启动上报', e2: '加载', e3: '填值保存', e4: '运行时下发'
  },
  en: {
    aria: 'Attribute config flow: declare → store → fill → dispatch',
    yml: 'application.yml', ymlSub: 'attr definitions',
    mgr: 'Manager stores', mgrSub1: 'Attribute table', mgrSub2: 'declared schema',
    ui: 'Config matrix', uiSub: 'device edit page · cols=Attribute',
    cfg: 'Config table', cfgSub: 'configValue',
    rt: 'Driver assembles protocol frame', rtSub: 'used at runtime',
    e1: 'reported on startup', e2: 'loads', e3: 'save filled values', e4: 'dispatched at runtime'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 340">
        <defs>
          <marker id="acf-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
        </defs>

        <line marker-end="url(#acf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="220" x2="290" y1="155" y2="155"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="255" y="147">{{ s.e1 }}</text>
        <line marker-end="url(#acf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="520" x2="560" y1="150" y2="155"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="540" y="142">{{ s.e2 }}</text>
        <line marker-end="url(#acf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="780" x2="830" y1="155" y2="155"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="805" y="147">{{ s.e3 }}</text>
        <line marker-end="url(#acf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="940" x2="940" y1="205" y2="260"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" x="1005" y="238">{{ s.e4 }}</text>

        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="200" x="20" y="120"/>
        <rect fill="var(--dc3-amber-fill)" height="70" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="200" x="20" y="120"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="120" y="148">{{ s.yml }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="120" y="166">{{ s.ymlSub }}</text>

        <rect fill="var(--vp-c-bg)" height="80" rx="8" width="230" x="290" y="115"/>
        <rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="230" x="290" y="115"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="405" y="145">{{ s.mgr }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="405" y="163">{{ s.mgrSub1 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="405" y="180">{{ s.mgrSub2 }}</text>

        <rect fill="var(--vp-c-bg)" height="70" rx="8" width="220" x="560" y="120"/>
        <rect fill="var(--dc3-fe-fill)" height="70" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="220" x="560" y="120"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="670" y="148">{{ s.ui }}</text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="670" y="166">{{ s.uiSub }}</text>

        <path d="M830,110 a100,16 0 0 0 200,0 v90 a100,16 0 0 1 -200,0 z" fill="var(--vp-c-bg)"/>
        <path d="M830,110 a100,16 0 0 0 200,0 v90 a100,16 0 0 1 -200,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="930" cy="110" fill="none" rx="100" ry="16" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="930" y="160">{{ s.cfg }}</text>
        <text fill="var(--dc3-db-text)" font-size="9.5" text-anchor="middle" x="930" y="178">{{ s.cfgSub }}</text>

        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="220" x="830" y="260"/>
        <rect fill="var(--dc3-bus-fill)" height="60" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="220" x="830" y="260"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="940" y="286">{{ s.rt }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="940" y="303">{{ s.rtSub }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
