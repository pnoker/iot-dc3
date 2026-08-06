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
    aria: 'Maven 模块依赖',
    gw: '网关 dc3-gateway',
    centersT: '四个中心（部署单元）',
    auth: '鉴权中心 dc3-center-auth',
    mgr: '管理中心 dc3-center-manager',
    data: '数据中心 dc3-center-data',
    ai: '智能中心 dc3-center-agentic',
    facadeT: '跨服务契约（facade 三态）',
    fapi: 'dc3-common-facade-api · 接口',
    fgrpc: 'dc3-common-facade-grpc · gRPC 实现',
    flocal: 'dc3-common-facade-local-* · 本地实现',
    commonsT: '公共与契约库',
    api: 'dc3-api-* · protobuf 合约',
    common: 'dc3-common-constant / model / dal / postgres / rabbitmq',
    drv: '28 个协议驱动 dc3-driver-*',
    e1: 'gRPC / local',
    e2: 'protobuf 合约',
    e3: '获取元数据',
    e4: '值 / 命令 (RabbitMQ)'
  },
  en: {
    aria: 'Maven module dependencies',
    gw: 'Gateway dc3-gateway',
    centersT: 'Four centers (deploy units)',
    auth: 'Auth dc3-center-auth',
    mgr: 'Manager dc3-center-manager',
    data: 'Data dc3-center-data',
    ai: 'Agentic dc3-center-agentic',
    facadeT: 'Cross-service contract (facade tri-state)',
    fapi: 'dc3-common-facade-api · interface',
    fgrpc: 'dc3-common-facade-grpc · gRPC impl',
    flocal: 'dc3-common-facade-local-* · local impl',
    commonsT: 'Common & contract libs',
    api: 'dc3-api-* · protobuf contract',
    common: 'dc3-common-constant / model / dal / postgres / rabbitmq',
    drv: '28 protocol drivers dc3-driver-*',
    e1: 'gRPC / local',
    e2: 'protobuf contract',
    e3: 'fetch metadata',
    e4: 'value / command (RabbitMQ)'
  }
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 520">
        <defs>
          <marker id="mf-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/>
          </marker>
        </defs>
        <rect fill="var(--vp-c-bg)" height="46" rx="8" width="260" x="510" y="15"/>
        <rect fill="var(--dc3-be-fill)" height="46" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="260" x="510"
              y="15"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="640"
              y="44">{{ s.gw }}
        </text>
        <rect fill="var(--dc3-be-fill)" height="100" opacity="0.2" rx="12" stroke="var(--dc3-be-stroke)" stroke-dasharray="6,4" stroke-width="1"
              width="600" x="30" y="85"/>
        <text fill="var(--dc3-be-text)" font-size="10" font-weight="600" x="44" y="104">{{ s.centersT }}</text>
        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="135" x="50" y="115"/>
        <rect fill="var(--dc3-be-fill)" height="55" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="135" x="50"
              y="115"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="117" y="147">{{
            s.auth
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="135" x="200" y="115"/>
        <rect fill="var(--dc3-be-fill)" height="55" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="135" x="200"
              y="115"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="267" y="147">{{
            s.mgr
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="135" x="350" y="115"/>
        <rect fill="var(--dc3-be-fill)" height="55" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="135" x="350"
              y="115"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="417" y="147">{{
            s.data
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="120" x="500" y="115"/>
        <rect fill="var(--dc3-fe-fill)" height="55" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="120" x="500"
              y="115"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="560" y="147">{{
            s.ai
          }}
        </text>
        <rect fill="var(--dc3-amber-fill)" height="190" opacity="0.2" rx="12" stroke="var(--dc3-amber-stroke)" stroke-dasharray="6,4" stroke-width="1"
              width="590" x="660" y="85"/>
        <text fill="var(--dc3-amber-stroke)" font-size="10" font-weight="600" x="674" y="104">{{ s.facadeT }}</text>
        <rect fill="var(--vp-c-bg)" height="42" rx="6" width="380" x="680" y="115"/>
        <rect fill="var(--dc3-amber-fill)" height="42" rx="6" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="380"
              x="680" y="115"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="870" y="141">{{
            s.fapi
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="42" rx="6" width="380" x="680" y="167"/>
        <rect fill="var(--dc3-amber-fill)" height="42" rx="6" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="380"
              x="680" y="167"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="870" y="193">{{
            s.fgrpc
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="42" rx="6" width="380" x="680" y="219"/>
        <rect fill="var(--dc3-amber-fill)" height="42" rx="6" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="380"
              x="680" y="219"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="870" y="245">{{
            s.flocal
          }}
        </text>
        <rect fill="var(--dc3-db-fill)" height="100" opacity="0.18" rx="12" stroke="var(--dc3-db-stroke)" stroke-dasharray="6,4" stroke-width="1"
              width="600" x="30" y="210"/>
        <text fill="var(--dc3-db-text)" font-size="10" font-weight="600" x="44" y="229">{{ s.commonsT }}</text>
        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="260" x="50" y="240"/>
        <rect fill="var(--dc3-db-fill)" height="55" rx="8" stroke="var(--dc3-db-stroke)" stroke-width="1.5" width="260" x="50"
              y="240"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="180" y="272">{{
            s.api
          }}
        </text>
        <rect fill="var(--vp-c-bg)" height="55" rx="8" width="290" x="330" y="240"/>
        <rect fill="var(--dc3-db-fill)" height="55" rx="8" stroke="var(--dc3-db-stroke)" stroke-width="1.5" width="290" x="330"
              y="240"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="475" y="272">
          {{ s.common }}
        </text>
        <rect fill="var(--vp-c-bg)" height="60" rx="8" width="590" x="660" y="320"/>
        <rect fill="var(--dc3-bus-fill)" height="60" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="590" x="660"
              y="320"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="955" y="355">{{
            s.drv
          }}
        </text>
        <path d="M640,60 L640,85 L870,115" fill="none" marker-end="url(#mf-ah)" stroke="var(--dc3-arrow)"
              stroke-dasharray="5,4" stroke-width="1.5"/>
        <path d="M330,170 L870,115" fill="none" marker-end="url(#mf-ah)" opacity="0.5" stroke="var(--dc3-arrow)"
              stroke-width="1.2"/>
        <path d="M870,209 L310,240" fill="none" marker-end="url(#mf-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="560" y="232">{{ s.e2 }}</text>
        <path d="M475,295 L475,330 L660,350" fill="none" marker-end="url(#mf-ah)" opacity="0.5"
              stroke="var(--dc3-arrow)" stroke-width="1.2"/>
        <path d="M640,60 L475,240" fill="none" opacity="0.5" stroke="var(--dc3-arrow)" stroke-dasharray="3,3"
              stroke-width="1.2"/>
        <path d="M955,320 L955,275" fill="none" marker-end="url(#mf-ah)" stroke="var(--dc3-arrow)" stroke-dasharray="5,4"
              stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="1010" y="300">{{ s.e3 }}</text>
        <path d="M780,380 L420,170" fill="none" marker-end="url(#mf-ah)" stroke="var(--dc3-arrow)" stroke-dasharray="5,4"
              stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="560" y="300">{{ s.e4 }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
