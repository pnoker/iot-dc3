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
    aria: '物联网四层参考架构与 DC3 映射',
    dcLabel: '在 IoT DC3 中',
    up: '数据上行',
    down: '命令下行',
    appName: '应用层 Application',
    appSub: '运营 · 告警 · 数据分析 · AIoT',
    appDc: '运营 · Agentic 中心 · MCP',
    platName: '平台层 Platform',
    platSub: '设备管理 · 数据存储 · 规则与计算',
    platDc: '中心服务 · 数据平面 · TimescaleDB',
    netName: '网络层 Network',
    netSub: '现场总线 · IoT 协议 · 无线广域',
    netDc: '28 协议驱动 · 网关 · RabbitMQ',
    percName: '感知层 Perception',
    percSub: '传感测量 · 自动识别 · 执行器',
    percDc: '物模型 Profile · 设备 Device · 位号 Point',
    secName: '安全（贯穿四层）',
    secSub: '设备 · 通信 · 平台 · 数据安全',
    secDc: 'DC3：鉴权 · 租户 · RBAC · TLS'
  },
  en: {
    aria: 'IoT four-layer reference architecture mapped to DC3',
    dcLabel: 'In IoT DC3',
    up: 'data up',
    down: 'command down',
    appName: 'Application',
    appSub: 'Operations · Alarms · Analytics · AIoT',
    appDc: 'Operations · Agentic Center · MCP',
    platName: 'Platform',
    platSub: 'Device mgmt · Storage · Rules & compute',
    platDc: 'Center services · Data plane · TimescaleDB',
    netName: 'Network',
    netSub: 'Fieldbus · IoT protocols · Wireless/WAN',
    netDc: '28 protocol drivers · Gateway · RabbitMQ',
    percName: 'Perception',
    percSub: 'Sensing · Auto-ID · Actuators',
    percDc: 'Profile · Device · Point',
    secName: 'Security (cross-cutting)',
    secSub: 'Device · Comms · Platform · Data',
    secDc: 'DC3: Auth · Tenant · RBAC · TLS'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 470">
        <defs>
          <marker id="fl-ah" markerHeight="7" markerWidth="9" orient="auto" refX="8" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 9 3.5, 0 7"/>
          </marker>
        </defs>

        <!-- left direction axis: data up / command down -->
        <line marker-end="url(#fl-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="30" x2="30" y1="404" y2="60"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" transform="rotate(-90 22 240)" x="22"
              y="240">{{ s.up }}
        </text>
        <line marker-end="url(#fl-ah)" stroke="var(--dc3-arrow)" stroke-dasharray="5,4" stroke-width="1.2" x1="52"
              x2="52"
              y1="60"
              y2="404"/>
        <text fill="var(--dc3-arrow-label)" font-size="11" text-anchor="middle" transform="rotate(-90 62 240)" x="62"
              y="240">{{ s.down }}
        </text>

        <!-- application layer -->
        <rect fill="var(--dc3-db-fill)" height="84" rx="10" stroke="var(--dc3-db-stroke)" stroke-width="1.5" width="852"
              x="84"
              y="48"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="15" x="108" y="82">{{ s.appName }}</text>
        <text fill="var(--dc3-text2)" font-size="11" x="108" y="106">{{ s.appSub }}</text>
        <line opacity="0.6" stroke="var(--dc3-db-stroke)" stroke-dasharray="3,3" stroke-width="1" x1="556" x2="556"
              y1="62"
              y2="118"/>
        <text fill="var(--dc3-text2)" font-size="10" x="576" y="80">{{ s.dcLabel }}</text>
        <text fill="var(--dc3-db-text)" font-size="12.5" x="576" y="104">{{ s.appDc }}</text>

        <!-- platform layer -->
        <rect fill="var(--dc3-be-fill)" height="84" rx="10" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="852"
              x="84"
              y="144"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="15" x="108" y="178">{{ s.platName }}</text>
        <text fill="var(--dc3-text2)" font-size="11" x="108" y="202">{{ s.platSub }}</text>
        <line opacity="0.6" stroke="var(--dc3-be-stroke)" stroke-dasharray="3,3" stroke-width="1" x1="556" x2="556"
              y1="158"
              y2="214"/>
        <text fill="var(--dc3-text2)" font-size="10" x="576" y="176">{{ s.dcLabel }}</text>
        <text fill="var(--dc3-be-text)" font-size="12.5" x="576" y="200">{{ s.platDc }}</text>

        <!-- network layer -->
        <rect fill="var(--dc3-bus-fill)" height="84" rx="10" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"
              width="852"
              x="84"
              y="240"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="15" x="108" y="274">{{ s.netName }}</text>
        <text fill="var(--dc3-text2)" font-size="11" x="108" y="298">{{ s.netSub }}</text>
        <line opacity="0.6" stroke="var(--dc3-bus-stroke)" stroke-dasharray="3,3" stroke-width="1" x1="556" x2="556"
              y1="254"
              y2="310"/>
        <text fill="var(--dc3-text2)" font-size="10" x="576" y="272">{{ s.dcLabel }}</text>
        <text fill="var(--dc3-bus-text)" font-size="12.5" x="576" y="296">{{ s.netDc }}</text>

        <!-- perception layer -->
        <rect fill="var(--dc3-fe-fill)" height="84" rx="10" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="852"
              x="84"
              y="336"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="15" x="108" y="370">{{ s.percName }}</text>
        <text fill="var(--dc3-text2)" font-size="11" x="108" y="394">{{ s.percSub }}</text>
        <line opacity="0.6" stroke="var(--dc3-fe-stroke)" stroke-dasharray="3,3" stroke-width="1" x1="556" x2="556"
              y1="350"
              y2="406"/>
        <text fill="var(--dc3-text2)" font-size="10" x="576" y="368">{{ s.dcLabel }}</text>
        <text fill="var(--dc3-fe-text, var(--dc3-fe-stroke))" font-size="12.5" x="576" y="392">{{ s.percDc }}</text>

        <!-- security: cross-cutting across all four layers -->
        <rect fill="var(--dc3-amber-fill)" height="372" rx="10" stroke="var(--dc3-amber-stroke)" stroke-dasharray="8,4"
              stroke-width="1.5"
              width="200" x="952" y="48"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="14" text-anchor="middle" x="1052" y="210">{{
            s.secName
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="10.5" text-anchor="middle" x="1052" y="234">{{ s.secSub }}</text>
        <text fill="var(--dc3-amber-stroke)" font-size="11" text-anchor="middle" x="1052" y="262">{{ s.secDc }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
