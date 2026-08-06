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
    aria: '枚举在分层间的转换时序',
    db: '数据库 dc3_point',
    do: 'PointDO',
    b: 'PointBuilder',
    bo: 'PointBO',
    vo: 'PointVO',
    m1: 'rwFlag = (Byte) 2',
    note1: '字段是裸 Byte · @EnumValue index',
    m2: 'buildBOByDO(PointDO)',
    m3: '@AfterMapping: RwTypeEnum.ofIndex((byte)2)',
    m4: 'rwFlag = READ_WRITE',
    note2: '业务层是域枚举',
    m5: 'buildVOByBO(PointBO)',
    m6: 'rwFlag = READ_WRITE'
  },
  en: {
    aria: 'Enum conversion across layers',
    db: 'DB dc3_point',
    do: 'PointDO',
    b: 'PointBuilder',
    bo: 'PointBO',
    vo: 'PointVO',
    m1: 'rwFlag = (Byte) 2',
    note1: 'raw Byte field · @EnumValue index',
    m2: 'buildBOByDO(PointDO)',
    m3: '@AfterMapping: RwTypeEnum.ofIndex((byte)2)',
    m4: 'rwFlag = READ_WRITE',
    note2: 'domain enum in business layer',
    m5: 'buildVOByBO(PointBO)',
    m6: 'rwFlag = READ_WRITE'
  }
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const PX: Record<string, number> = {db: 130, do: 340, b: 550, bo: 760, vo: 970}
</script>
<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" font-family="inherit" role="img" viewBox="0 0 1100 460">
        <defs>
          <marker id="sq-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/>
          </marker>
        </defs>
        <!-- lifelines -->
        <line v-for="p in ['db','do','b','bo','vo']" :key="p" :x1="PX[p]" :x2="PX[p]" stroke="var(--dc3-divider)" stroke-dasharray="4,4"
              stroke-width="1" y1="70" y2="420"/>
        <!-- participants -->
        <rect v-for="p in ['db','do','b','bo','vo']" :key="p" :x="PX[p]-80" fill="var(--vp-c-bg)" height="48" rx="8" width="160"
              y="22"/>
        <rect v-for="p in ['db','do','b','bo','vo']" :key="p" :fill="p==='b' ? 'var(--dc3-amber-fill)' : 'var(--dc3-be-fill)'" :stroke="p==='b' ? 'var(--dc3-amber-stroke)' : 'var(--dc3-be-stroke)'" :x="PX[p]-80" height="48" opacity="0.6"
              rx="8"
              stroke-width="1.5" width="160" y="22"/>
        <text v-for="p in ['db','do','b','bo','vo']" :key="'t'+p" :x="PX[p]" class="d-name" fill="var(--dc3-box-name)"
              font-size="12" font-weight="700" text-anchor="middle" y="51">{{ s[p] }}
        </text>
        <!-- msg 1: db -> do -->
        <line :x1="PX.db" :x2="PX.do" marker-end="url(#sq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" y1="100"
              y2="100"/>
        <text :x="(PX.db+PX.do)/2" fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" y="92">{{
            s.m1
          }}
        </text>
        <!-- note over DO -->
        <rect :x="PX.do-90" fill="var(--dc3-amber-fill)" height="34" rx="4" stroke="var(--dc3-amber-stroke)" stroke-width="1"
              width="180" y="112"/>
        <text :x="PX.do" fill="var(--dc3-box-name)" font-size="9.5" text-anchor="middle" y="133">{{ s.note1 }}</text>
        <!-- msg 2: do -> b -->
        <line :x1="PX.do" :x2="PX.b" marker-end="url(#sq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" y1="165"
              y2="165"/>
        <text :x="(PX.do+PX.b)/2" fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" y="157">{{
            s.m2
          }}
        </text>
        <!-- msg 3: b -> b (self) -->
        <path :d="`M${PX.b},180 L${PX.b+60},180 L${PX.b+60},202 L${PX.b},202`" fill="none" marker-end="url(#sq-ah)"
              stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text :x="PX.b+66" fill="var(--dc3-arrow-label)" font-size="9" y="195">{{ s.m3 }}</text>
        <!-- msg 4: b -> bo -->
        <line :x1="PX.b" :x2="PX.bo" marker-end="url(#sq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" y1="232"
              y2="232"/>
        <text :x="(PX.b+PX.bo)/2" fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" y="224">{{
            s.m4
          }}
        </text>
        <!-- note over BO -->
        <rect :x="PX.bo-90" fill="var(--dc3-amber-fill)" height="34" rx="4" stroke="var(--dc3-amber-stroke)" stroke-width="1"
              width="180" y="244"/>
        <text :x="PX.bo" fill="var(--dc3-box-name)" font-size="9.5" text-anchor="middle" y="265">{{ s.note2 }}</text>
        <!-- msg 5: bo -> b -->
        <line :x1="PX.bo" :x2="PX.b" marker-end="url(#sq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" y1="300"
              y2="300"/>
        <text :x="(PX.bo+PX.b)/2" fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" y="292">{{
            s.m5
          }}
        </text>
        <!-- msg 6: b -> vo -->
        <line :x1="PX.b" :x2="PX.vo" marker-end="url(#sq-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" y1="345"
              y2="345"/>
        <text :x="(PX.b+PX.vo)/2" fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" y="337">{{
            s.m6
          }}
        </text>
      </svg>
    </div>
  </DiagramFrame>
</template>
