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
    gw: 'Gateway',
    cloud: 'Cloud Server',
    secLabel: 'Security & Load',
    sec1: 'Load Balancing',
    sec2: 'Security Services Suite',
    sec3: 'Token',
    sec4: 'Encryption',
    depLabel: 'Deployment & System',
    dep1: 'Management Platform',
    dep2: 'Container',
    dep3: 'Fast Deployment',
    dep4: 'System Services',
    dep5a: 'Device',
    dep5b: 'System',
    dep5c: 'Monitoring Mgmt',
    L1: 'Application Layer',
    L1a: 'Client Registration & Mgmt',
    L1b: 'Data Open API',
    L1c: 'Data Distribution',
    L1d: 'Extension Module',
    L2: 'Platform Layer',
    L2a: 'Task Scheduling',
    L2b: 'Alerting',
    L2c: 'Notification',
    L2d: 'Logging',
    L2e: 'Message',
    L3: 'Network Layer',
    L3a: 'Protocol Gateway',
    L3b: 'MQTT Broker',
    L3c: 'Message Routing',
    L3d: 'RabbitMQ',
    L3e: 'Driver Access',
    L4: 'Perception Layer',
    L4a: 'Sensor',
    L4b: 'Actuator',
    L4c: 'PLC Controller',
    L4d: 'Smart Meter',
    L4e: 'RFID',
    L4f: 'Field Device…',
    bot1: 'SDK Fast Development',
    bot2: 'Deployment & Ops',
    legTitle: 'Legend',
    leg1: 'Entry / Exit',
    leg2: 'Service / Component',
    leg3: 'Data / Dependency Flow',
    leg4: 'External Access',
    leg5: 'Service Group',
    legApp: 'App',
    legPlat: 'Platform',
    legNet: 'Network',
    legPerc: 'Perception',
    legSec: 'Security',
    legDep: 'Deploy',
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-arch-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1400 695" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <marker id="ad-arrow-down" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
          <polygon fill="var(--ad-blue)" points="0 0, 10 3.5, 0 7"/>
        </marker>
        <marker id="ad-arrow-up" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
          <polygon fill="var(--ad-blue)" points="0 0, 10 3.5, 0 7"/>
        </marker>
        <marker id="ad-arrow-data" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
          <polygon fill="var(--ad-blue-light)" points="0 0, 10 3.5, 0 7"/>
        </marker>
        <pattern id="ad-grid" height="40" patternUnits="userSpaceOnUse" width="40">
          <path d="M 40 0 L 0 0 0 40" fill="none" stroke="var(--dc3-grid)" stroke-width="0.5"/>
        </pattern>
        <linearGradient id="ad-banner-grad" x1="0%" x2="100%" y1="0%" y2="0%">
          <stop offset="0%" stop-color="var(--ad-blue-dark)"/>
          <stop offset="100%" stop-color="var(--ad-blue)"/>
        </linearGradient>
      </defs>

      <!-- Background -->
      <rect fill="var(--ad-bg)" height="100%" width="100%"/>
      <rect fill="url(#ad-grid)" height="100%" width="100%"/>

      <g transform="translate(0, -57)">

        <!-- ============================================================
             TOP BANNERS
             ============================================================ -->
        <!-- Gateway -->
        <rect fill="url(#ad-banner-grad)" height="34" rx="8" width="420" x="260" y="85"/>
        <g fill="none" stroke="var(--ad-icon-stroke)" stroke-linecap="round" stroke-linejoin="round"
           stroke-width="1.5" transform="translate(282, 93)">
          <circle cx="7" cy="4" r="2.5"/>
          <circle cx="7" cy="14" r="2.5"/>
          <circle cx="16" cy="9" r="2.5"/>
          <line x1="7" x2="7" y1="6.5" y2="11.5"/>
          <line x1="9" x2="14" y1="7.5" y2="9"/>
          <line x1="9" x2="14" y1="10.5" y2="9"/>
        </g>
        <text fill="var(--ad-text-bright)" font-size="13" font-weight="700" text-anchor="middle" x="470" y="107">{{
            s.gw
          }}
        </text>
        <line marker-end="url(#ad-arrow-down)" stroke="var(--ad-blue)" stroke-width="2" x1="470" x2="470" y1="119"
              y2="133"/>

        <!-- Cloud Server -->
        <rect fill="url(#ad-banner-grad)" height="34" rx="8" width="420" x="720" y="85"/>
        <g fill="var(--ad-icon-stroke)" stroke="var(--ad-icon-stroke)" stroke-linejoin="round" stroke-width="1"
           transform="translate(744, 93)">
          <path
              d="M4 12 C2 12 2 9 3.5 8.5 C3.5 6.5 6.5 5.5 8 7 C9 4.5 13 4.5 15 7 C17 6.5 18 8.5 16.5 10.5 C17.5 11 17 12 15 12 Z"/>
        </g>
        <text fill="var(--ad-text-bright)" font-size="13" font-weight="700" text-anchor="middle" x="930" y="107">
          {{ s.cloud }}
        </text>
        <line marker-end="url(#ad-arrow-down)" stroke="var(--ad-blue)" stroke-width="2" x1="930" x2="930" y1="119"
              y2="133"/>

        <!-- ============================================================
             LEFT SIDEBAR — Security & Load
             ============================================================ -->
        <rect fill="var(--ad-rose-fill)" height="560" rx="10" stroke="var(--dc3-rose-stroke)" stroke-dasharray="6,3" stroke-width="1" width="195"
              x="28" y="85"/>

        <rect fill="var(--ad-rose-box)" height="52" rx="6" stroke="var(--ad-rose-box-stroke)" stroke-width="0.5" width="165" x="43"
              y="110"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="125" y="141">{{
            s.sec1
          }}
        </text>

        <rect fill="var(--ad-rose-box)" height="52" rx="6" stroke="var(--ad-rose-box-stroke)" stroke-width="0.5" width="165" x="43"
              y="212"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="125" y="243">{{
            s.sec2
          }}
        </text>

        <rect fill="var(--ad-rose-box)" height="52" rx="6" stroke="var(--ad-rose-box-stroke)" stroke-width="0.5" width="165" x="43"
              y="314"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="125" y="345">{{
            s.sec3
          }}
        </text>

        <rect fill="var(--ad-rose-box)" height="52" rx="6" stroke="var(--ad-rose-box-stroke)" stroke-width="0.5" width="165" x="43"
              y="416"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="125" y="447">{{
            s.sec4
          }}
        </text>

        <text fill="var(--dc3-rose-stroke)" font-size="12" font-weight="700" text-anchor="middle" x="125" y="636">
          {{ s.secLabel }}
        </text>

        <!-- ============================================================
             RIGHT SIDEBAR — Deployment & System
             ============================================================ -->
        <rect fill="var(--ad-amber-fill)" height="560" rx="10" stroke="var(--dc3-amber-stroke)" stroke-dasharray="6,3" stroke-width="1"
              width="195" x="1177" y="85"/>

        <rect fill="var(--ad-amber-box)" height="48" rx="6" stroke="var(--ad-amber-box-stroke)" stroke-width="0.5" width="165"
              x="1192" y="110"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="1274" y="139">{{
            s.dep1
          }}
        </text>

        <rect fill="var(--ad-amber-box)" height="48" rx="6" stroke="var(--ad-amber-box-stroke)" stroke-width="0.5" width="165"
              x="1192" y="198"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="1274" y="227">{{
            s.dep2
          }}
        </text>

        <rect fill="var(--ad-amber-box)" height="48" rx="6" stroke="var(--ad-amber-box-stroke)" stroke-width="0.5" width="165"
              x="1192" y="286"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="1274" y="315">{{
            s.dep3
          }}
        </text>

        <rect fill="var(--ad-amber-box)" height="48" rx="6" stroke="var(--ad-amber-box-stroke)" stroke-width="0.5" width="165"
              x="1192" y="374"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="1274" y="403">{{
            s.dep4
          }}
        </text>

        <rect fill="var(--ad-amber-box)" height="52" rx="6" stroke="var(--ad-amber-box-stroke)" stroke-width="0.5" width="165"
              x="1192" y="462"/>
        <text fill="var(--ad-text)" font-size="9" font-weight="600" text-anchor="middle" x="1274" y="478">{{
            s.dep5a
          }}
        </text>
        <text fill="var(--ad-text)" font-size="9" font-weight="600" text-anchor="middle" x="1274" y="492">{{
            s.dep5b
          }}
        </text>
        <text fill="var(--ad-text)" font-size="9" font-weight="600" text-anchor="middle" x="1274" y="506">{{
            s.dep5c
          }}
        </text>

        <text fill="var(--dc3-amber-stroke)" font-size="12" font-weight="700" text-anchor="middle" x="1274" y="636">
          {{ s.depLabel }}
        </text>

        <!-- ============================================================
             CENTER — Layered Architecture
             ============================================================ -->

        <!-- 1. 应用层 -->
        <rect fill="var(--ad-app-fill)" height="72" rx="6" stroke="var(--dc3-fe-stroke)" stroke-dasharray="4,4" stroke-width="0.8" width="880"
              x="260" y="140"/>

        <rect fill="var(--ad-app-box)" height="40" rx="5" stroke="var(--dc3-fe-stroke)" stroke-width="0.5" width="205" x="266"
              y="154"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="368" y="179">{{
            s.L1a
          }}
        </text>

        <rect fill="var(--ad-app-box)" height="40" rx="5" stroke="var(--dc3-fe-stroke)" stroke-width="0.5" width="205" x="487"
              y="154"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="589" y="179">{{
            s.L1b
          }}
        </text>

        <rect fill="var(--ad-app-box)" height="40" rx="5" stroke="var(--dc3-fe-stroke)" stroke-width="0.5" width="205" x="708"
              y="154"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="810" y="179">{{
            s.L1c
          }}
        </text>

        <rect fill="var(--ad-app-box)" height="40" rx="5" stroke="var(--dc3-fe-stroke)" stroke-width="0.5" width="205" x="929"
              y="154"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="1031" y="179">{{
            s.L1d
          }}
        </text>

        <text fill="var(--dc3-fe-stroke)" font-size="13" font-weight="700" text-anchor="middle" x="700" y="230">{{
            s.L1
          }}
        </text>

        <!-- 2. 平台层 -->
        <rect fill="var(--ad-plat-mgmt-fill)" height="72" rx="6" stroke="var(--ad-plat-mgmt-stroke)" stroke-dasharray="4,4" stroke-width="0.8"
              width="880" x="260" y="260"/>

        <rect fill="var(--ad-plat-mgmt-box)" height="40" rx="5" stroke="var(--ad-plat-mgmt-stroke)" stroke-width="0.5" width="160"
              x="268" y="274"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="348" y="299">{{
            s.L2a
          }}
        </text>

        <rect fill="var(--ad-plat-mgmt-box)" height="40" rx="5" stroke="var(--ad-plat-mgmt-stroke)" stroke-width="0.5" width="160"
              x="444" y="274"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="524" y="299">{{
            s.L2b
          }}
        </text>

        <rect fill="var(--ad-plat-mgmt-box)" height="40" rx="5" stroke="var(--ad-plat-mgmt-stroke)" stroke-width="0.5" width="160"
              x="620" y="274"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="700" y="299">{{
            s.L2c
          }}
        </text>

        <rect fill="var(--ad-plat-mgmt-box)" height="40" rx="5" stroke="var(--ad-plat-mgmt-stroke)" stroke-width="0.5" width="160"
              x="796" y="274"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="876" y="299">{{
            s.L2d
          }}
        </text>

        <rect fill="var(--ad-plat-mgmt-box)" height="40" rx="5" stroke="var(--ad-plat-mgmt-stroke)" stroke-width="0.5" width="162"
              x="972" y="274"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="1053" y="299">{{
            s.L2e
          }}
        </text>

        <text fill="var(--ad-plat-mgmt-stroke)" font-size="13" font-weight="700" text-anchor="middle" x="700" y="350">
          {{ s.L2 }}
        </text>

        <!-- 3. 网络层 -->
        <rect fill="var(--ad-plat-data-fill)" height="72" rx="6" stroke="var(--ad-plat-data-stroke)" stroke-dasharray="4,4" stroke-width="0.8"
              width="880" x="260" y="380"/>

        <rect fill="var(--ad-plat-data-box)" height="40" rx="5" stroke="var(--ad-plat-data-stroke)" stroke-width="0.5" width="160"
              x="268" y="394"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="348" y="419">{{
            s.L3a
          }}
        </text>

        <rect fill="var(--ad-plat-data-box)" height="40" rx="5" stroke="var(--ad-plat-data-stroke)" stroke-width="0.5" width="160"
              x="444" y="394"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="524" y="419">{{
            s.L3b
          }}
        </text>

        <rect fill="var(--ad-plat-data-box)" height="40" rx="5" stroke="var(--ad-plat-data-stroke)" stroke-width="0.5" width="160"
              x="620" y="394"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="700" y="419">{{
            s.L3c
          }}
        </text>

        <rect fill="var(--ad-plat-data-box)" height="40" rx="5" stroke="var(--ad-plat-data-stroke)" stroke-width="0.5" width="160"
              x="796" y="394"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="876" y="419">{{
            s.L3d
          }}
        </text>

        <rect fill="var(--ad-plat-data-box)" height="40" rx="5" stroke="var(--ad-plat-data-stroke)" stroke-width="0.5" width="162"
              x="972" y="394"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="1053" y="419">{{
            s.L3e
          }}
        </text>

        <text fill="var(--ad-plat-data-stroke)" font-size="13" font-weight="700" text-anchor="middle" x="700" y="470">
          {{ s.L3 }}
        </text>

        <!-- 4. 感知层 -->
        <rect fill="var(--ad-net-fill)" height="72" rx="6" stroke="var(--dc3-be-stroke)" stroke-dasharray="4,4" stroke-width="0.8" width="880"
              x="260" y="500"/>

        <rect fill="var(--ad-net-box)" height="40" rx="5" stroke="var(--dc3-be-stroke)" stroke-width="0.5" width="132" x="273"
              y="514"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="339" y="539">{{
            s.L4a
          }}
        </text>

        <rect fill="var(--ad-net-box)" height="40" rx="5" stroke="var(--dc3-be-stroke)" stroke-width="0.5" width="132" x="419"
              y="514"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="485" y="539">{{
            s.L4b
          }}
        </text>

        <rect fill="var(--ad-net-box)" height="40" rx="5" stroke="var(--dc3-be-stroke)" stroke-width="0.5" width="132" x="565"
              y="514"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="631" y="539">{{
            s.L4c
          }}
        </text>

        <rect fill="var(--ad-net-box)" height="40" rx="5" stroke="var(--dc3-be-stroke)" stroke-width="0.5" width="132" x="711"
              y="514"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="777" y="539">{{
            s.L4d
          }}
        </text>

        <rect fill="var(--ad-net-box)" height="40" rx="5" stroke="var(--dc3-be-stroke)" stroke-width="0.5" width="132" x="857"
              y="514"/>
        <text fill="var(--ad-text)" font-size="10" font-weight="600" text-anchor="middle" x="923" y="539">{{
            s.L4e
          }}
        </text>

        <rect fill="var(--ad-net-box)" height="40" rx="5" stroke="var(--dc3-be-stroke)" stroke-width="0.5" width="132" x="1003"
              y="514"/>
        <text fill="var(--ad-text)" font-size="9" font-weight="600" text-anchor="middle" x="1069" y="539">{{
            s.L4f
          }}
        </text>

        <text fill="var(--dc3-be-stroke)" font-size="13" font-weight="700" text-anchor="middle" x="700" y="590">{{
            s.L4
          }}
        </text>

        <!-- ============================================================
             LAYER 3 → LAYER 4 CONNECTIONS
             ============================================================ -->
        <line stroke="var(--ad-blue-light)" stroke-width="1.5" x1="524" x2="524" y1="434" y2="484"/>
        <line stroke="var(--ad-blue-light)" stroke-width="1.5" x1="876" x2="876" y1="434" y2="484"/>

        <line stroke="var(--ad-blue-light)" stroke-width="1" x1="339" x2="1069" y1="484" y2="484"/>

        <line marker-end="url(#ad-arrow-data)" stroke="var(--ad-blue-light)" stroke-width="1" x1="339" x2="339" y1="484"
              y2="512"/>
        <line marker-end="url(#ad-arrow-data)" stroke="var(--ad-blue-light)" stroke-width="1" x1="485" x2="485" y1="484"
              y2="512"/>
        <line marker-end="url(#ad-arrow-data)" stroke="var(--ad-blue-light)" stroke-width="1" x1="631" x2="631" y1="484"
              y2="512"/>
        <line marker-end="url(#ad-arrow-data)" stroke="var(--ad-blue-light)" stroke-width="1" x1="777" x2="777" y1="484"
              y2="512"/>
        <line marker-end="url(#ad-arrow-data)" stroke="var(--ad-blue-light)" stroke-width="1" x1="923" x2="923" y1="484"
              y2="512"/>
        <line marker-end="url(#ad-arrow-data)" stroke="var(--ad-blue-light)" stroke-width="1" x1="1069" x2="1069" y1="484"
              y2="512"/>

        <!-- ============================================================
             BOTTOM BANNERS
             ============================================================ -->
        <rect fill="url(#ad-banner-grad)" height="34" rx="8" width="420" x="260" y="611"/>
        <g fill="none" stroke="var(--ad-icon-stroke)" stroke-linecap="round" stroke-linejoin="round"
           stroke-width="1.5" transform="translate(282, 619)">
          <rect height="14" rx="2" width="15" x="2" y="2"/>
          <rect height="6" width="7" x="6" y="6"/>
          <line x1="2" x2="0" y1="6" y2="6"/>
          <line x1="2" x2="0" y1="12" y2="12"/>
          <line x1="17" x2="19" y1="6" y2="6"/>
          <line x1="17" x2="19" y1="12" y2="12"/>
          <line x1="6" x2="6" y1="2" y2="0"/>
          <line x1="13" x2="13" y1="2" y2="0"/>
          <line x1="6" x2="6" y1="16" y2="18"/>
          <line x1="13" x2="13" y1="16" y2="18"/>
        </g>
        <text fill="var(--ad-text-bright)" font-size="12" font-weight="700" text-anchor="middle" x="470" y="633">
          {{ s.bot1 }}
        </text>
        <line marker-end="url(#ad-arrow-up)" stroke="var(--ad-blue)" stroke-width="2" x1="470" x2="470" y1="611"
              y2="598"/>

        <rect fill="url(#ad-banner-grad)" height="34" rx="8" width="420" x="720" y="611"/>
        <g fill="none" stroke="var(--ad-icon-stroke)" stroke-linecap="round" stroke-linejoin="round"
           stroke-width="1.5" transform="translate(742, 619)">
          <circle cx="10" cy="9" r="5.5"/>
          <circle cx="10" cy="9" fill="var(--ad-icon-stroke)" r="1.8"/>
          <line x1="10" x2="10" y1="1" y2="3"/>
          <line x1="10" x2="10" y1="15" y2="17"/>
          <line x1="2" x2="4" y1="9" y2="9"/>
          <line x1="16" x2="18" y1="9" y2="9"/>
          <line x1="4.5" x2="6" y1="3.5" y2="5"/>
          <line x1="14" x2="15.5" y1="13" y2="14.5"/>
          <line x1="15.5" x2="14" y1="3.5" y2="5"/>
          <line x1="6" x2="4.5" y1="13" y2="14.5"/>
        </g>
        <text fill="var(--ad-text-bright)" font-size="12" font-weight="700" text-anchor="middle" x="930" y="633">
          {{ s.bot2 }}
        </text>
        <line marker-end="url(#ad-arrow-up)" stroke="var(--ad-blue)" stroke-width="2" x1="930" x2="930" y1="611"
              y2="598"/>

        <!-- ============================================================
             LEGEND
             ============================================================ -->
        <text fill="var(--ad-text-bright)" font-size="11" font-weight="700" x="60" y="700">{{ s.legTitle }}</text>

        <rect fill="url(#ad-banner-grad)" height="10" rx="2" width="18" x="60" y="712"/>
        <text fill="var(--ad-legend-text)" font-size="9" x="84" y="721">{{ s.leg1 }}</text>

        <rect fill="var(--ad-app-box)" height="10" rx="2" stroke="var(--dc3-fe-stroke)" stroke-width="0.5" width="18" x="210"
              y="712"/>
        <text fill="var(--ad-legend-text)" font-size="9" x="234" y="721">{{ s.leg2 }}</text>

        <line marker-end="url(#ad-arrow-data)" stroke="var(--ad-blue-light)" stroke-width="1.5" x1="390" x2="408" y1="717"
              y2="717"/>
        <text fill="var(--ad-legend-text)" font-size="9" x="414" y="721">{{ s.leg3 }}</text>

        <line marker-end="url(#ad-arrow-down)" stroke="var(--ad-blue)" stroke-width="2" x1="570" x2="588" y1="717"
              y2="717"/>
        <text fill="var(--ad-legend-text)" font-size="9" x="594" y="721">{{ s.leg4 }}</text>

        <rect fill="var(--ad-rose-box)" height="10" rx="2" stroke="var(--dc3-rose-stroke)" stroke-dasharray="4,3" stroke-width="1" width="18"
              x="750" y="712"/>
        <text fill="var(--ad-legend-text)" font-size="9" x="774" y="721">{{ s.leg5 }}</text>

        <rect fill="var(--dc3-fe-stroke)" height="8" rx="2" width="8" x="910" y="714"/>
        <text fill="var(--ad-legend-text)" font-size="9" x="924" y="722">{{ s.legApp }}</text>
        <rect fill="var(--ad-plat-mgmt-stroke)" height="8" rx="2" width="8" x="956" y="714"/>
        <text fill="var(--ad-legend-text)" font-size="9" x="970" y="722">{{ s.legPlat }}</text>
        <rect fill="var(--ad-plat-data-stroke)" height="8" rx="2" width="8" x="1012" y="714"/>
        <text fill="var(--ad-legend-text)" font-size="9" x="1026" y="722">{{ s.legNet }}</text>
        <rect fill="var(--dc3-be-stroke)" height="8" rx="2" width="8" x="1068" y="714"/>
        <text fill="var(--ad-legend-text)" font-size="9" x="1082" y="722">{{ s.legPerc }}</text>
        <rect fill="var(--dc3-rose-stroke)" height="8" rx="2" width="8" x="1132" y="714"/>
        <text fill="var(--ad-legend-text)" font-size="9" x="1146" y="722">{{ s.legSec }}</text>
        <rect fill="var(--dc3-amber-stroke)" height="8" rx="2" width="8" x="1208" y="714"/>
        <text fill="var(--ad-legend-text)" font-size="9" x="1222" y="722">{{ s.legDep }}</text>

      </g>
    </svg>
    </div>
  </DiagramFrame>
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
