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

const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})

const DICT = {
  zh: {
    aria: 'IoT DC3 系统架构分层图',
    // top banners
    gw: '网关', cloud: '云服务器',
    // left sidebar — 安全与负载
    secLabel: '安全与负载',
    sec1: '负载均衡', sec2: '安全服务套件', sec3: 'Token', sec4: '加密',
    // right sidebar — 部署与系统
    depLabel: '部署与系统',
    dep1: '管理平台', dep2: '容器', dep3: '快速部署', dep4: '系统服务',
    dep5a: '设备', dep5b: '系统', dep5c: '监控管理',
    // center layer 1 — 应用层
    L1: '应用层', L1a: '客户端注册与管理', L1b: '数据开放API', L1c: '数据分发', L1d: '扩展模块',
    // center layer 2 — 平台层
    L2: '平台层', L2a: '任务调度', L2b: '告警', L2c: '通知', L2d: '日志', L2e: '消息',
    // center layer 3 — 网络层
    L3: '网络层', L3a: '协议网关', L3b: 'MQTT Broker', L3c: '消息路由', L3d: 'RabbitMQ', L3e: '驱动接入',
    // center layer 4 — 感知层
    L4: '感知层', L4a: '传感器', L4b: '执行器', L4c: 'PLC 控制器', L4d: '智能仪表', L4e: 'RFID', L4f: '现场设备…',
    // bottom banners
    bot1: 'SDK 快速开发', bot2: '部署与运维',
    // legend
    legTitle: '图例',
    leg1: '入口 / 出口', leg2: '服务 / 组件', leg3: '数据 / 依赖流',
    leg4: '外部访问', leg5: '服务组',
    legApp: '应用', legPlat: '平台', legNet: '网络', legPerc: '感知', legSec: '安全', legDep: '部署',
  },
  en: {
    aria: 'IoT DC3 system architecture layered diagram',
    gw: 'Gateway', cloud: 'Cloud Server',
    secLabel: 'Security & Load',
    sec1: 'Load Balancing', sec2: 'Security Services Suite', sec3: 'Token', sec4: 'Encryption',
    depLabel: 'Deployment & System',
    dep1: 'Management Platform', dep2: 'Container', dep3: 'Fast Deployment', dep4: 'System Services',
    dep5a: 'Device', dep5b: 'System', dep5c: 'Monitoring Mgmt',
    L1: 'Application Layer', L1a: 'Client Registration & Mgmt', L1b: 'Data Open API', L1c: 'Data Distribution', L1d: 'Extension Module',
    L2: 'Platform Layer', L2a: 'Task Scheduling', L2b: 'Alerting', L2c: 'Notification', L2d: 'Logging', L2e: 'Message',
    L3: 'Network Layer', L3a: 'Protocol Gateway', L3b: 'MQTT Broker', L3c: 'Message Routing', L3d: 'RabbitMQ', L3e: 'Driver Access',
    L4: 'Perception Layer', L4a: 'Sensor', L4b: 'Actuator', L4c: 'PLC Controller', L4d: 'Smart Meter', L4e: 'RFID', L4f: 'Field Device…',
    bot1: 'SDK Fast Development', bot2: 'Deployment & Ops',
    legTitle: 'Legend',
    leg1: 'Entry / Exit', leg2: 'Service / Component', leg3: 'Data / Dependency Flow',
    leg4: 'External Access', leg5: 'Service Group',
    legApp: 'App', legPlat: 'Platform', legNet: 'Network', legPerc: 'Perception', legSec: 'Security', legDep: 'Deploy',
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <div class="dc3-arch-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1400 695" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <marker id="ad-arrow-down" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="var(--ad-blue)"/>
        </marker>
        <marker id="ad-arrow-up" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="var(--ad-blue)"/>
        </marker>
        <marker id="ad-arrow-data" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="var(--ad-blue-light)"/>
        </marker>
        <pattern id="ad-grid" width="40" height="40" patternUnits="userSpaceOnUse">
          <path d="M 40 0 L 0 0 0 40" fill="none" stroke="var(--dc3-grid)" stroke-width="0.5"/>
        </pattern>
        <linearGradient id="ad-banner-grad" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="var(--ad-blue-dark)"/>
          <stop offset="100%" stop-color="var(--ad-blue)"/>
        </linearGradient>
      </defs>

      <!-- Background -->
      <rect width="100%" height="100%" fill="var(--ad-bg)"/>
      <rect width="100%" height="100%" fill="url(#ad-grid)"/>

      <g transform="translate(0, -57)">

        <!-- ============================================================
             TOP BANNERS
             ============================================================ -->
        <!-- Gateway -->
        <rect x="260" y="85" width="420" height="34" rx="8" fill="url(#ad-banner-grad)"/>
        <g transform="translate(282, 93)" fill="none" stroke="var(--ad-icon-stroke)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="7" cy="4" r="2.5"/>
          <circle cx="7" cy="14" r="2.5"/>
          <circle cx="16" cy="9" r="2.5"/>
          <line x1="7" y1="6.5" x2="7" y2="11.5"/>
          <line x1="9" y1="7.5" x2="14" y2="9"/>
          <line x1="9" y1="10.5" x2="14" y2="9"/>
        </g>
        <text x="470" y="107" fill="var(--ad-text-bright)" font-size="13" font-weight="700" text-anchor="middle">{{ s.gw }}</text>
        <line x1="470" y1="119" x2="470" y2="133" stroke="var(--ad-blue)" stroke-width="2" marker-end="url(#ad-arrow-down)"/>

        <!-- Cloud Server -->
        <rect x="720" y="85" width="420" height="34" rx="8" fill="url(#ad-banner-grad)"/>
        <g transform="translate(744, 93)" fill="var(--ad-icon-stroke)" stroke="var(--ad-icon-stroke)" stroke-width="1" stroke-linejoin="round">
          <path d="M4 12 C2 12 2 9 3.5 8.5 C3.5 6.5 6.5 5.5 8 7 C9 4.5 13 4.5 15 7 C17 6.5 18 8.5 16.5 10.5 C17.5 11 17 12 15 12 Z"/>
        </g>
        <text x="930" y="107" fill="var(--ad-text-bright)" font-size="13" font-weight="700" text-anchor="middle">{{ s.cloud }}</text>
        <line x1="930" y1="119" x2="930" y2="133" stroke="var(--ad-blue)" stroke-width="2" marker-end="url(#ad-arrow-down)"/>

        <!-- ============================================================
             LEFT SIDEBAR — Security & Load
             ============================================================ -->
        <rect x="28" y="85" width="195" height="560" rx="10" fill="var(--ad-rose-fill)" stroke="var(--dc3-rose-stroke)" stroke-width="1" stroke-dasharray="6,3"/>

        <rect x="43" y="110" width="165" height="52" rx="6" fill="var(--ad-rose-box)" stroke="var(--ad-rose-box-stroke)" stroke-width="0.5"/>
        <text x="125" y="141" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.sec1 }}</text>

        <rect x="43" y="212" width="165" height="52" rx="6" fill="var(--ad-rose-box)" stroke="var(--ad-rose-box-stroke)" stroke-width="0.5"/>
        <text x="125" y="243" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.sec2 }}</text>

        <rect x="43" y="314" width="165" height="52" rx="6" fill="var(--ad-rose-box)" stroke="var(--ad-rose-box-stroke)" stroke-width="0.5"/>
        <text x="125" y="345" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.sec3 }}</text>

        <rect x="43" y="416" width="165" height="52" rx="6" fill="var(--ad-rose-box)" stroke="var(--ad-rose-box-stroke)" stroke-width="0.5"/>
        <text x="125" y="447" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.sec4 }}</text>

        <text x="125" y="636" fill="var(--dc3-rose-stroke)" font-size="12" font-weight="700" text-anchor="middle">{{ s.secLabel }}</text>

        <!-- ============================================================
             RIGHT SIDEBAR — Deployment & System
             ============================================================ -->
        <rect x="1177" y="85" width="195" height="560" rx="10" fill="var(--ad-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1" stroke-dasharray="6,3"/>

        <rect x="1192" y="110" width="165" height="48" rx="6" fill="var(--ad-amber-box)" stroke="var(--ad-amber-box-stroke)" stroke-width="0.5"/>
        <text x="1274" y="139" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.dep1 }}</text>

        <rect x="1192" y="198" width="165" height="48" rx="6" fill="var(--ad-amber-box)" stroke="var(--ad-amber-box-stroke)" stroke-width="0.5"/>
        <text x="1274" y="227" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.dep2 }}</text>

        <rect x="1192" y="286" width="165" height="48" rx="6" fill="var(--ad-amber-box)" stroke="var(--ad-amber-box-stroke)" stroke-width="0.5"/>
        <text x="1274" y="315" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.dep3 }}</text>

        <rect x="1192" y="374" width="165" height="48" rx="6" fill="var(--ad-amber-box)" stroke="var(--ad-amber-box-stroke)" stroke-width="0.5"/>
        <text x="1274" y="403" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.dep4 }}</text>

        <rect x="1192" y="462" width="165" height="52" rx="6" fill="var(--ad-amber-box)" stroke="var(--ad-amber-box-stroke)" stroke-width="0.5"/>
        <text x="1274" y="478" fill="var(--ad-text)" font-size="9" font-weight="600" text-anchor="middle">{{ s.dep5a }}</text>
        <text x="1274" y="492" fill="var(--ad-text)" font-size="9" font-weight="600" text-anchor="middle">{{ s.dep5b }}</text>
        <text x="1274" y="506" fill="var(--ad-text)" font-size="9" font-weight="600" text-anchor="middle">{{ s.dep5c }}</text>

        <text x="1274" y="636" fill="var(--dc3-amber-stroke)" font-size="12" font-weight="700" text-anchor="middle">{{ s.depLabel }}</text>

        <!-- ============================================================
             CENTER — Layered Architecture
             ============================================================ -->

        <!-- 1. 应用层 -->
        <rect x="260" y="140" width="880" height="72" rx="6" fill="var(--ad-app-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="0.8" stroke-dasharray="4,4"/>

        <rect x="266" y="154" width="205" height="40" rx="5" fill="var(--ad-app-box)" stroke="var(--dc3-fe-stroke)" stroke-width="0.5"/>
        <text x="368" y="179" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L1a }}</text>

        <rect x="487" y="154" width="205" height="40" rx="5" fill="var(--ad-app-box)" stroke="var(--dc3-fe-stroke)" stroke-width="0.5"/>
        <text x="589" y="179" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L1b }}</text>

        <rect x="708" y="154" width="205" height="40" rx="5" fill="var(--ad-app-box)" stroke="var(--dc3-fe-stroke)" stroke-width="0.5"/>
        <text x="810" y="179" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L1c }}</text>

        <rect x="929" y="154" width="205" height="40" rx="5" fill="var(--ad-app-box)" stroke="var(--dc3-fe-stroke)" stroke-width="0.5"/>
        <text x="1031" y="179" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L1d }}</text>

        <text x="700" y="230" fill="var(--dc3-fe-stroke)" font-size="13" font-weight="700" text-anchor="middle">{{ s.L1 }}</text>

        <!-- 2. 平台层 -->
        <rect x="260" y="260" width="880" height="72" rx="6" fill="var(--ad-plat-mgmt-fill)" stroke="var(--ad-plat-mgmt-stroke)" stroke-width="0.8" stroke-dasharray="4,4"/>

        <rect x="268" y="274" width="160" height="40" rx="5" fill="var(--ad-plat-mgmt-box)" stroke="var(--ad-plat-mgmt-stroke)" stroke-width="0.5"/>
        <text x="348" y="299" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L2a }}</text>

        <rect x="444" y="274" width="160" height="40" rx="5" fill="var(--ad-plat-mgmt-box)" stroke="var(--ad-plat-mgmt-stroke)" stroke-width="0.5"/>
        <text x="524" y="299" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L2b }}</text>

        <rect x="620" y="274" width="160" height="40" rx="5" fill="var(--ad-plat-mgmt-box)" stroke="var(--ad-plat-mgmt-stroke)" stroke-width="0.5"/>
        <text x="700" y="299" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L2c }}</text>

        <rect x="796" y="274" width="160" height="40" rx="5" fill="var(--ad-plat-mgmt-box)" stroke="var(--ad-plat-mgmt-stroke)" stroke-width="0.5"/>
        <text x="876" y="299" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L2d }}</text>

        <rect x="972" y="274" width="162" height="40" rx="5" fill="var(--ad-plat-mgmt-box)" stroke="var(--ad-plat-mgmt-stroke)" stroke-width="0.5"/>
        <text x="1053" y="299" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L2e }}</text>

        <text x="700" y="350" fill="var(--ad-plat-mgmt-stroke)" font-size="13" font-weight="700" text-anchor="middle">{{ s.L2 }}</text>

        <!-- 3. 网络层 -->
        <rect x="260" y="380" width="880" height="72" rx="6" fill="var(--ad-plat-data-fill)" stroke="var(--ad-plat-data-stroke)" stroke-width="0.8" stroke-dasharray="4,4"/>

        <rect x="268" y="394" width="160" height="40" rx="5" fill="var(--ad-plat-data-box)" stroke="var(--ad-plat-data-stroke)" stroke-width="0.5"/>
        <text x="348" y="419" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L3a }}</text>

        <rect x="444" y="394" width="160" height="40" rx="5" fill="var(--ad-plat-data-box)" stroke="var(--ad-plat-data-stroke)" stroke-width="0.5"/>
        <text x="524" y="419" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L3b }}</text>

        <rect x="620" y="394" width="160" height="40" rx="5" fill="var(--ad-plat-data-box)" stroke="var(--ad-plat-data-stroke)" stroke-width="0.5"/>
        <text x="700" y="419" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L3c }}</text>

        <rect x="796" y="394" width="160" height="40" rx="5" fill="var(--ad-plat-data-box)" stroke="var(--ad-plat-data-stroke)" stroke-width="0.5"/>
        <text x="876" y="419" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L3d }}</text>

        <rect x="972" y="394" width="162" height="40" rx="5" fill="var(--ad-plat-data-box)" stroke="var(--ad-plat-data-stroke)" stroke-width="0.5"/>
        <text x="1053" y="419" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L3e }}</text>

        <text x="700" y="470" fill="var(--ad-plat-data-stroke)" font-size="13" font-weight="700" text-anchor="middle">{{ s.L3 }}</text>

        <!-- 4. 感知层 -->
        <rect x="260" y="500" width="880" height="72" rx="6" fill="var(--ad-net-fill)" stroke="var(--dc3-be-stroke)" stroke-width="0.8" stroke-dasharray="4,4"/>

        <rect x="273" y="514" width="132" height="40" rx="5" fill="var(--ad-net-box)" stroke="var(--dc3-be-stroke)" stroke-width="0.5"/>
        <text x="339" y="539" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L4a }}</text>

        <rect x="419" y="514" width="132" height="40" rx="5" fill="var(--ad-net-box)" stroke="var(--dc3-be-stroke)" stroke-width="0.5"/>
        <text x="485" y="539" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L4b }}</text>

        <rect x="565" y="514" width="132" height="40" rx="5" fill="var(--ad-net-box)" stroke="var(--dc3-be-stroke)" stroke-width="0.5"/>
        <text x="631" y="539" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L4c }}</text>

        <rect x="711" y="514" width="132" height="40" rx="5" fill="var(--ad-net-box)" stroke="var(--dc3-be-stroke)" stroke-width="0.5"/>
        <text x="777" y="539" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L4d }}</text>

        <rect x="857" y="514" width="132" height="40" rx="5" fill="var(--ad-net-box)" stroke="var(--dc3-be-stroke)" stroke-width="0.5"/>
        <text x="923" y="539" fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle">{{ s.L4e }}</text>

        <rect x="1003" y="514" width="132" height="40" rx="5" fill="var(--ad-net-box)" stroke="var(--dc3-be-stroke)" stroke-width="0.5"/>
        <text x="1069" y="539" fill="var(--ad-text)" font-size="9" font-weight="600" text-anchor="middle">{{ s.L4f }}</text>

        <text x="700" y="590" fill="var(--dc3-be-stroke)" font-size="13" font-weight="700" text-anchor="middle">{{ s.L4 }}</text>

        <!-- ============================================================
             LAYER 3 → LAYER 4 CONNECTIONS
             ============================================================ -->
        <line x1="524" y1="434" x2="524" y2="484" stroke="var(--ad-blue-light)" stroke-width="1.5"/>
        <line x1="876" y1="434" x2="876" y2="484" stroke="var(--ad-blue-light)" stroke-width="1.5"/>

        <line x1="339" y1="484" x2="1069" y2="484" stroke="var(--ad-blue-light)" stroke-width="1"/>

        <line x1="339" y1="484" x2="339" y2="512" stroke="var(--ad-blue-light)" stroke-width="1" marker-end="url(#ad-arrow-data)"/>
        <line x1="485" y1="484" x2="485" y2="512" stroke="var(--ad-blue-light)" stroke-width="1" marker-end="url(#ad-arrow-data)"/>
        <line x1="631" y1="484" x2="631" y2="512" stroke="var(--ad-blue-light)" stroke-width="1" marker-end="url(#ad-arrow-data)"/>
        <line x1="777" y1="484" x2="777" y2="512" stroke="var(--ad-blue-light)" stroke-width="1" marker-end="url(#ad-arrow-data)"/>
        <line x1="923" y1="484" x2="923" y2="512" stroke="var(--ad-blue-light)" stroke-width="1" marker-end="url(#ad-arrow-data)"/>
        <line x1="1069" y1="484" x2="1069" y2="512" stroke="var(--ad-blue-light)" stroke-width="1" marker-end="url(#ad-arrow-data)"/>

        <!-- ============================================================
             BOTTOM BANNERS
             ============================================================ -->
        <rect x="260" y="611" width="420" height="34" rx="8" fill="url(#ad-banner-grad)"/>
        <g transform="translate(282, 619)" fill="none" stroke="var(--ad-icon-stroke)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <rect x="2" y="2" width="15" height="14" rx="2"/>
          <rect x="6" y="6" width="7" height="6"/>
          <line x1="2" y1="6" x2="0" y2="6"/>
          <line x1="2" y1="12" x2="0" y2="12"/>
          <line x1="17" y1="6" x2="19" y2="6"/>
          <line x1="17" y1="12" x2="19" y2="12"/>
          <line x1="6" y1="2" x2="6" y2="0"/>
          <line x1="13" y1="2" x2="13" y2="0"/>
          <line x1="6" y1="16" x2="6" y2="18"/>
          <line x1="13" y1="16" x2="13" y2="18"/>
        </g>
        <text x="470" y="633" fill="var(--ad-text-bright)" font-size="12" font-weight="700" text-anchor="middle">{{ s.bot1 }}</text>
        <line x1="470" y1="611" x2="470" y2="598" stroke="var(--ad-blue)" stroke-width="2" marker-end="url(#ad-arrow-up)"/>

        <rect x="720" y="611" width="420" height="34" rx="8" fill="url(#ad-banner-grad)"/>
        <g transform="translate(742, 619)" fill="none" stroke="var(--ad-icon-stroke)" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="10" cy="9" r="5.5"/>
          <circle cx="10" cy="9" r="1.8" fill="var(--ad-icon-stroke)"/>
          <line x1="10" y1="1" x2="10" y2="3"/>
          <line x1="10" y1="15" x2="10" y2="17"/>
          <line x1="2" y1="9" x2="4" y2="9"/>
          <line x1="16" y1="9" x2="18" y2="9"/>
          <line x1="4.5" y1="3.5" x2="6" y2="5"/>
          <line x1="14" y1="13" x2="15.5" y2="14.5"/>
          <line x1="15.5" y1="3.5" x2="14" y2="5"/>
          <line x1="6" y1="13" x2="4.5" y2="14.5"/>
        </g>
        <text x="930" y="633" fill="var(--ad-text-bright)" font-size="12" font-weight="700" text-anchor="middle">{{ s.bot2 }}</text>
        <line x1="930" y1="611" x2="930" y2="598" stroke="var(--ad-blue)" stroke-width="2" marker-end="url(#ad-arrow-up)"/>

        <!-- ============================================================
             LEGEND
             ============================================================ -->
        <text x="60" y="700" fill="var(--ad-text-bright)" font-size="11" font-weight="700">{{ s.legTitle }}</text>

        <rect x="60" y="712" width="18" height="10" rx="2" fill="url(#ad-banner-grad)"/>
        <text x="84" y="721" fill="var(--ad-legend-text)" font-size="9">{{ s.leg1 }}</text>

        <rect x="210" y="712" width="18" height="10" rx="2" fill="var(--ad-app-box)" stroke="var(--dc3-fe-stroke)" stroke-width="0.5"/>
        <text x="234" y="721" fill="var(--ad-legend-text)" font-size="9">{{ s.leg2 }}</text>

        <line x1="390" y1="717" x2="408" y2="717" stroke="var(--ad-blue-light)" stroke-width="1.5" marker-end="url(#ad-arrow-data)"/>
        <text x="414" y="721" fill="var(--ad-legend-text)" font-size="9">{{ s.leg3 }}</text>

        <line x1="570" y1="717" x2="588" y2="717" stroke="var(--ad-blue)" stroke-width="2" marker-end="url(#ad-arrow-down)"/>
        <text x="594" y="721" fill="var(--ad-legend-text)" font-size="9">{{ s.leg4 }}</text>

        <rect x="750" y="712" width="18" height="10" rx="2" fill="var(--ad-rose-box)" stroke="var(--dc3-rose-stroke)" stroke-width="1" stroke-dasharray="4,3"/>
        <text x="774" y="721" fill="var(--ad-legend-text)" font-size="9">{{ s.leg5 }}</text>

        <rect x="910" y="714" width="8" height="8" rx="2" fill="var(--dc3-fe-stroke)"/>
        <text x="924" y="722" fill="var(--ad-legend-text)" font-size="9">{{ s.legApp }}</text>
        <rect x="956" y="714" width="8" height="8" rx="2" fill="var(--ad-plat-mgmt-stroke)"/>
        <text x="970" y="722" fill="var(--ad-legend-text)" font-size="9">{{ s.legPlat }}</text>
        <rect x="1012" y="714" width="8" height="8" rx="2" fill="var(--ad-plat-data-stroke)"/>
        <text x="1026" y="722" fill="var(--ad-legend-text)" font-size="9">{{ s.legNet }}</text>
        <rect x="1068" y="714" width="8" height="8" rx="2" fill="var(--dc3-be-stroke)"/>
        <text x="1082" y="722" fill="var(--ad-legend-text)" font-size="9">{{ s.legPerc }}</text>
        <rect x="1132" y="714" width="8" height="8" rx="2" fill="var(--dc3-rose-stroke)"/>
        <text x="1146" y="722" fill="var(--ad-legend-text)" font-size="9">{{ s.legSec }}</text>
        <rect x="1208" y="714" width="8" height="8" rx="2" fill="var(--dc3-amber-stroke)"/>
        <text x="1222" y="722" fill="var(--ad-legend-text)" font-size="9">{{ s.legDep }}</text>

      </g>
    </svg>
  </div>
</template>

<style>
/* light theme (default) */
.dc3-arch-diagram {
  --ad-bg: transparent;
  --ad-text: #334155;
  --ad-text-bright: #f8fafc;
  --ad-icon-stroke: #f8fafc;
  --ad-legend-text: #64748b;
  --ad-blue-dark: #1d4ed8;
  --ad-blue: #2563eb;
  --ad-blue-light: #3b82f6;

  /* layer 1 — 应用层 cyan */
  --ad-app-fill: rgba(8, 145, 178, 0.05);
  --ad-app-box: rgba(8, 145, 178, 0.07);

  /* layer 2 — 平台层(管理) sky */
  --ad-plat-mgmt-stroke: #0284c7;
  --ad-plat-mgmt-fill: rgba(2, 132, 199, 0.05);
  --ad-plat-mgmt-box: rgba(2, 132, 199, 0.07);

  /* layer 3 — 平台层(数据) indigo */
  --ad-plat-data-stroke: #6366f1;
  --ad-plat-data-fill: rgba(99, 102, 241, 0.05);
  --ad-plat-data-box: rgba(99, 102, 241, 0.07);

  /* layer 4 — 网络层 green (reuses dc3-be-stroke) */
  --ad-net-fill: rgba(5, 150, 105, 0.05);
  --ad-net-box: rgba(5, 150, 105, 0.07);

  /* left sidebar — 安全 rose */
  --ad-rose-fill: rgba(225, 29, 72, 0.03);
  --ad-rose-box: rgba(225, 29, 72, 0.06);
  --ad-rose-box-stroke: rgba(225, 29, 72, 0.2);

  /* right sidebar — 部署 amber */
  --ad-amber-fill: rgba(217, 119, 6, 0.03);
  --ad-amber-box: rgba(217, 119, 6, 0.06);
  --ad-amber-box-stroke: rgba(217, 119, 6, 0.2);

  /* reuse from Architecture.vue */
  --dc3-grid: #e2e8f0;
  --dc3-fe-stroke: #0891b2;
  --dc3-be-stroke: #059669;
  --dc3-rose-stroke: #e11d48;
  --dc3-amber-stroke: #d97706;

  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  padding: 16px;
  margin: 16px 0 8px;
  overflow-x: auto;
}

/* dark theme */
.dark .dc3-arch-diagram {
  --ad-bg: #020617;
  --ad-text: #e2e8f0;
  --ad-text-bright: #ffffff;
  --ad-icon-stroke: #ffffff;
  --ad-legend-text: #94a3b8;
  --ad-blue-dark: #2563eb;
  --ad-blue: #3b82f6;
  --ad-blue-light: #60a5fa;

  --ad-app-fill: rgba(34, 211, 238, 0.06);
  --ad-app-box: rgba(34, 211, 238, 0.08);

  --ad-plat-mgmt-stroke: #38bdf8;
  --ad-plat-mgmt-fill: rgba(56, 189, 248, 0.06);
  --ad-plat-mgmt-box: rgba(56, 189, 248, 0.08);

  --ad-plat-data-stroke: #818cf8;
  --ad-plat-data-fill: rgba(129, 140, 248, 0.06);
  --ad-plat-data-box: rgba(129, 140, 248, 0.08);

  --ad-net-fill: rgba(52, 211, 153, 0.06);
  --ad-net-box: rgba(52, 211, 153, 0.08);

  --ad-rose-fill: rgba(251, 113, 133, 0.04);
  --ad-rose-box: rgba(251, 113, 133, 0.08);
  --ad-rose-box-stroke: rgba(251, 113, 133, 0.3);

  --ad-amber-fill: rgba(251, 191, 36, 0.04);
  --ad-amber-box: rgba(251, 191, 36, 0.08);
  --ad-amber-box-stroke: rgba(251, 191, 36, 0.3);

  --dc3-grid: #1e293b;
  --dc3-fe-stroke: #22d3ee;
  --dc3-be-stroke: #34d399;
  --dc3-rose-stroke: #fb7185;
  --dc3-amber-stroke: #fbbf24;
}

.dc3-arch-diagram svg {
  width: 100%;
  height: auto;
  display: block;
  font-family: 'JetBrains Mono', ui-monospace, 'SFMono-Regular', Consolas, monospace;
}
</style>
