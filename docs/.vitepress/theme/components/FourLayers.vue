<!--
  Copyright 2016-present the IoT DC3 original author or authors.
  Licensed under the GNU Affero General Public License v3.0.

  IoT four-layer reference architecture ↔ IoT DC3 implementation mapping (Perception / Network / Platform / Application + cross-cutting Security).
  Pure inline SVG; colors use the .dc3-diagram CSS variables to switch with the light/dark theme, and the text switches between Chinese and English via the lang prop.
-->
<script setup lang="ts">
import {computed} from 'vue'

const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})

const DICT = {
  zh: {
    aria: '物联网四层参考架构与 DC3 映射',
    dcLabel: '在 IoT DC3 中',
    up: '数据上行', down: '命令下行',
    appName: '应用层 Application', appSub: '运营 · 告警 · 数据分析 · AIoT', appDc: '运营 · Agentic 中心 · MCP',
    platName: '平台层 Platform', platSub: '设备管理 · 数据存储 · 规则与计算', platDc: '中心服务 · 数据平面 · TimescaleDB',
    netName: '网络层 Network', netSub: '现场总线 · IoT 协议 · 无线广域', netDc: '28 协议驱动 · 网关 · RabbitMQ',
    percName: '感知层 Perception', percSub: '传感测量 · 自动识别 · 执行器', percDc: '物模型 Profile · 设备 Device · 位号 Point',
    secName: '安全（贯穿四层）', secSub: '设备 · 通信 · 平台 · 数据安全', secDc: 'DC3：鉴权 · 租户 · RBAC · TLS'
  },
  en: {
    aria: 'IoT four-layer reference architecture mapped to DC3',
    dcLabel: 'In IoT DC3',
    up: 'data up', down: 'command down',
    appName: 'Application', appSub: 'Operations · Alarms · Analytics · AIoT', appDc: 'Operations · Agentic Center · MCP',
    platName: 'Platform', platSub: 'Device mgmt · Storage · Rules & compute', platDc: 'Center services · Data plane · TimescaleDB',
    netName: 'Network', netSub: 'Fieldbus · IoT protocols · Wireless/WAN', netDc: '28 protocol drivers · Gateway · RabbitMQ',
    percName: 'Perception', percSub: 'Sensing · Auto-ID · Actuators', percDc: 'Profile · Device · Point',
    secName: 'Security (cross-cutting)', secSub: 'Device · Comms · Platform · Data', secDc: 'DC3: Auth · Tenant · RBAC · TLS'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <div class="dc3-diagram">
    <svg viewBox="0 0 1180 470" role="img" :aria-label="s.aria">
      <defs>
        <marker id="fl-ah" markerWidth="9" markerHeight="7" refX="8" refY="3.5" orient="auto">
          <polygon points="0 0, 9 3.5, 0 7" fill="var(--dc3-arrow)"/>
        </marker>
      </defs>

      <!-- left direction axis: data up / command down -->
      <line x1="30" y1="404" x2="30" y2="60" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#fl-ah)"/>
      <text x="22" y="240" fill="var(--dc3-arrow-label)" font-size="11" transform="rotate(-90 22 240)" text-anchor="middle">{{ s.up }}</text>
      <line x1="52" y1="60" x2="52" y2="404" stroke="var(--dc3-arrow)" stroke-width="1.2" stroke-dasharray="5,4" marker-end="url(#fl-ah)"/>
      <text x="62" y="240" fill="var(--dc3-arrow-label)" font-size="11" transform="rotate(-90 62 240)" text-anchor="middle">{{ s.down }}</text>

      <!-- application layer -->
      <rect x="84" y="48" width="852" height="84" rx="10" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text x="108" y="82" class="d-name" fill="var(--dc3-box-name)" font-size="15">{{ s.appName }}</text>
      <text x="108" y="106" fill="var(--dc3-text2)" font-size="11">{{ s.appSub }}</text>
      <line x1="556" y1="62" x2="556" y2="118" stroke="var(--dc3-db-stroke)" stroke-width="1" stroke-dasharray="3,3" opacity="0.6"/>
      <text x="576" y="80" fill="var(--dc3-text2)" font-size="10">{{ s.dcLabel }}</text>
      <text x="576" y="104" fill="var(--dc3-db-text)" font-size="12.5">{{ s.appDc }}</text>

      <!-- platform layer -->
      <rect x="84" y="144" width="852" height="84" rx="10" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="108" y="178" class="d-name" fill="var(--dc3-box-name)" font-size="15">{{ s.platName }}</text>
      <text x="108" y="202" fill="var(--dc3-text2)" font-size="11">{{ s.platSub }}</text>
      <line x1="556" y1="158" x2="556" y2="214" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="3,3" opacity="0.6"/>
      <text x="576" y="176" fill="var(--dc3-text2)" font-size="10">{{ s.dcLabel }}</text>
      <text x="576" y="200" fill="var(--dc3-be-text)" font-size="12.5">{{ s.platDc }}</text>

      <!-- network layer -->
      <rect x="84" y="240" width="852" height="84" rx="10" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text x="108" y="274" class="d-name" fill="var(--dc3-box-name)" font-size="15">{{ s.netName }}</text>
      <text x="108" y="298" fill="var(--dc3-text2)" font-size="11">{{ s.netSub }}</text>
      <line x1="556" y1="254" x2="556" y2="310" stroke="var(--dc3-bus-stroke)" stroke-width="1" stroke-dasharray="3,3" opacity="0.6"/>
      <text x="576" y="272" fill="var(--dc3-text2)" font-size="10">{{ s.dcLabel }}</text>
      <text x="576" y="296" fill="var(--dc3-bus-text)" font-size="12.5">{{ s.netDc }}</text>

      <!-- perception layer -->
      <rect x="84" y="336" width="852" height="84" rx="10" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text x="108" y="370" class="d-name" fill="var(--dc3-box-name)" font-size="15">{{ s.percName }}</text>
      <text x="108" y="394" fill="var(--dc3-text2)" font-size="11">{{ s.percSub }}</text>
      <line x1="556" y1="350" x2="556" y2="406" stroke="var(--dc3-fe-stroke)" stroke-width="1" stroke-dasharray="3,3" opacity="0.6"/>
      <text x="576" y="368" fill="var(--dc3-text2)" font-size="10">{{ s.dcLabel }}</text>
      <text x="576" y="392" fill="var(--dc3-fe-text, var(--dc3-fe-stroke))" font-size="12.5">{{ s.percDc }}</text>

      <!-- security: cross-cutting across all four layers -->
      <rect x="952" y="48" width="200" height="372" rx="10" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" stroke-dasharray="8,4"/>
      <text x="1052" y="210" class="d-name" fill="var(--dc3-box-name)" font-size="14" text-anchor="middle">{{ s.secName }}</text>
      <text x="1052" y="234" fill="var(--dc3-text2)" font-size="10.5" text-anchor="middle">{{ s.secSub }}</text>
      <text x="1052" y="262" fill="var(--dc3-amber-stroke)" font-size="11" text-anchor="middle">{{ s.secDc }}</text>
    </svg>
  </div>
</template>
