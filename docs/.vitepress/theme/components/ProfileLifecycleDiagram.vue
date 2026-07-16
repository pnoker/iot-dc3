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
    aria: '模板的生命周期',
    t1: '建模', d1a: '建 Profile', d1b: '加 Point / Command / Event',
    t2: '复用', d2a: '多个 Device', d2b: '绑定同一 profileId',
    t3: '运行', d3a: '设备按模板', d3b: '采集 / 下令 / 上报',
    t4: '演进', d4a: '改能力', d4b: 'version 递增'
  },
  en: {
    aria: 'Profile lifecycle',
    t1: 'Model', d1a: 'Create Profile', d1b: 'add Point / Command / Event',
    t2: 'Reuse', d2a: 'Many Devices', d2b: 'bind one profileId',
    t3: 'Run', d3a: 'Per template', d3b: 'sample / command / report',
    t4: 'Evolve', d4a: 'Change capabilities', d4b: 'bump version'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1000 220">
        <defs>
          <marker id="pld-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
        </defs>

        <!-- arrows -->
        <line marker-end="url(#pld-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="240" x2="273" y1="125" y2="125"/>
        <line marker-end="url(#pld-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="485" x2="518" y1="125" y2="125"/>
        <line marker-end="url(#pld-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="730" x2="763" y1="125" y2="125"/>

        <!-- stage 1 建模 (cyan) -->
        <rect fill="var(--dc3-fe-fill)" height="110" rx="10" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="210" x="30" y="70"/>
        <circle cx="56" cy="96" fill="none" r="13" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
        <text fill="var(--dc3-fe-stroke)" font-size="13" font-weight="700" text-anchor="middle" x="56" y="101">1</text>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="14.5" font-weight="600" text-anchor="middle" x="146" y="101">{{ s.t1 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="135" y="133">{{ s.d1a }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="135" y="151">{{ s.d1b }}</text>

        <!-- stage 2 复用 (emerald) -->
        <rect fill="var(--dc3-be-fill)" height="110" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="210" x="275" y="70"/>
        <circle cx="301" cy="96" fill="none" r="13" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
        <text fill="var(--dc3-be-stroke)" font-size="13" font-weight="700" text-anchor="middle" x="301" y="101">2</text>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="14.5" font-weight="600" text-anchor="middle" x="391" y="101">{{ s.t2 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="380" y="133">{{ s.d2a }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="380" y="151">{{ s.d2b }}</text>

        <!-- stage 3 运行 (amber) -->
        <rect fill="var(--dc3-amber-fill)" height="110" rx="10" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="210" x="520" y="70"/>
        <circle cx="546" cy="96" fill="none" r="13" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
        <text fill="var(--dc3-amber-stroke)" font-size="13" font-weight="700" text-anchor="middle" x="546" y="101">3</text>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="14.5" font-weight="600" text-anchor="middle" x="636" y="101">{{ s.t3 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="625" y="133">{{ s.d3a }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="625" y="151">{{ s.d3b }}</text>

        <!-- stage 4 演进 (violet) -->
        <rect fill="var(--dc3-db-fill)" height="110" rx="10" stroke="var(--dc3-db-stroke)" stroke-width="1.5" width="210" x="765" y="70"/>
        <circle cx="791" cy="96" fill="none" r="13" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text fill="var(--dc3-db-stroke)" font-size="13" font-weight="700" text-anchor="middle" x="791" y="101">4</text>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="14.5" font-weight="600" text-anchor="middle" x="881" y="101">{{ s.t4 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="870" y="133">{{ s.d4a }}</text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="870" y="151">{{ s.d4b }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
