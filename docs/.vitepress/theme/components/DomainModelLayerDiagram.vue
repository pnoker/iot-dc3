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
    aria: 'Param / Attribute / Config 三层',
    bizT: '业务层 · Param（模板模型里定义）',
    cp: 'CommandParam',
    cpSub: '命令的输入输出参数',
    ep: 'EventParam',
    epSub: '事件携带的参数',
    protoT: '协议层 · Attribute（驱动启动注册）',
    attr: 'DriverAttribute / PointAttribute / CommandAttribute / EventAttribute',
    attrSub: '驱动有哪些配置项 · 来自 application.yml',
    instT: '实例层 · Config（用户为设备填值）',
    pac: 'PointAttributeConfig 等',
    pacSub: 'attributeId + deviceId + pointId + configValue',
    e1: '属性被实例化'
  },
  en: {
    aria: 'Param / Attribute / Config three layers',
    bizT: 'Business · Param (defined in profile model)',
    cp: 'CommandParam',
    cpSub: 'command input/output params',
    ep: 'EventParam',
    epSub: 'event-carried params',
    protoT: 'Protocol · Attribute (registered on driver startup)',
    attr: 'DriverAttribute / PointAttribute / CommandAttribute / EventAttribute',
    attrSub: 'driver config items · from application.yml',
    instT: 'Instance · Config (user fills per device)',
    pac: 'PointAttributeConfig etc.',
    pacSub: 'attributeId + deviceId + pointId + configValue',
    e1: 'attribute instantiated'
  }
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1200 440">
        <defs>
          <marker id="ly-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/>
          </marker>
        </defs>
        <!-- biz layer -->
        <rect fill="var(--dc3-fe-fill)" height="100" opacity="0.25" rx="12" stroke="var(--dc3-fe-stroke)" stroke-dasharray="6,4" stroke-width="1"
              width="1140" x="30" y="30"/>
        <text fill="var(--dc3-fe-stroke)" font-size="11" font-weight="600" x="46" y="52">{{ s.bizT }}</text>
        <rect fill="var(--vp-c-bg)" height="56" rx="8" width="320" x="60" y="60"/>
        <rect fill="var(--dc3-fe-fill)" height="56" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="320" x="60"
              y="60"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="220" y="83">{{
            s.cp
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="220" y="102">{{ s.cpSub }}</text>
        <rect fill="var(--vp-c-bg)" height="56" rx="8" width="320" x="820" y="60"/>
        <rect fill="var(--dc3-fe-fill)" height="56" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="320" x="820"
              y="60"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="980" y="83">{{
            s.ep
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="980" y="102">{{ s.epSub }}</text>
        <!-- proto layer -->
        <rect fill="var(--dc3-bus-fill)" height="100" opacity="0.25" rx="12" stroke="var(--dc3-bus-stroke)" stroke-dasharray="6,4" stroke-width="1"
              width="1140" x="30" y="160"/>
        <text fill="var(--dc3-bus-text)" font-size="11" font-weight="600" x="46" y="182">{{ s.protoT }}</text>
        <rect fill="var(--vp-c-bg)" height="56" rx="8" width="720" x="240" y="190"/>
        <rect fill="var(--dc3-bus-fill)" height="56" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="720" x="240"
              y="190"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="600" y="213">{{
            s.attr
          }}
        </text>
        <text fill="var(--dc3-bus-text)" font-size="9" text-anchor="middle" x="600" y="232">{{ s.attrSub }}</text>
        <!-- inst layer -->
        <rect fill="var(--dc3-amber-fill)" height="100" opacity="0.28" rx="12" stroke="var(--dc3-amber-stroke)" stroke-dasharray="6,4" stroke-width="1"
              width="1140" x="30" y="290"/>
        <text fill="var(--dc3-amber-stroke)" font-size="11" font-weight="600" x="46" y="312">{{ s.instT }}</text>
        <rect fill="var(--vp-c-bg)" height="56" rx="8" width="600" x="300" y="320"/>
        <rect fill="var(--dc3-amber-fill)" height="56" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="600"
              x="300" y="320"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="600" y="343">{{
            s.pac
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="600" y="362">{{ s.pacSub }}</text>
        <!-- proto -> inst -->
        <path d="M600,260 L600,320" fill="none" marker-end="url(#ly-ah)" stroke="var(--dc3-arrow)" stroke-dasharray="5,4"
              stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="660" y="294">{{ s.e1 }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
