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
    aria: '平台架构总览',
    caller: '用户 / 第三方 / AI Agent', laneCaller: '调用方',
    gw: '网关 dc3-gateway', gwSub: '唯一对外 HTTP :8000',
    laneCenter: '中心服务（gRPC facade 互联，多租户）',
    auth: '鉴权中心', authSub: '认证 / 租户 / RBAC / OAuth',
    mgr: '管理中心', mgrSub: '驱动 / 模板 / 设备 / 位号',
    data: '数据中心', dataSub: '位号值 / 命令 / 告警',
    ai: '智能中心', aiSub: '会话 / 工具 / MCP',
    laneEdge: '接入层', drv: '协议驱动 dc3-driver-*',
    field: '现场设备 / 数据源', db: 'PostgreSQL + TimescaleDB',
    eRabbit: 'RabbitMQ'
  },
  en: {
    aria: 'Platform architecture overview',
    caller: 'User / 3rd-party / AI Agent', laneCaller: 'Callers',
    gw: 'Gateway dc3-gateway', gwSub: 'sole public HTTP :8000',
    laneCenter: 'Center services (gRPC facade mesh, multi-tenant)',
    auth: 'Auth Center', authSub: 'auth / tenant / RBAC / OAuth',
    mgr: 'Manager Center', mgrSub: 'driver / profile / device / point',
    data: 'Data Center', dataSub: 'point value / command / alarm',
    ai: 'Agentic Center', aiSub: 'chat / tools / MCP',
    laneEdge: 'Edge', drv: 'Protocol drivers dc3-driver-*',
    field: 'Field devices / sources', db: 'PostgreSQL + TimescaleDB',
    eRabbit: 'RabbitMQ'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1140 540">
        <defs>
          <marker id="ia-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
        </defs>

        <!-- caller lane -->
        <rect fill="var(--dc3-ext-fill)" height="56" opacity="0.4" rx="10" width="1120" x="10" y="20"/>
        <text fill="var(--dc3-text2)" font-size="10" font-weight="600" x="24" y="38">{{ s.laneCaller }}</text>
        <rect fill="var(--vp-c-bg)" height="40" rx="8" width="240" x="450" y="30"/>
        <rect fill="var(--dc3-ext-fill)" height="40" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="240" x="450" y="30"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="570" y="55">{{ s.caller }}</text>

        <!-- gateway -->
        <line marker-end="url(#ia-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="570" x2="570" y1="76" y2="108"/>
        <rect fill="var(--vp-c-bg)" height="56" rx="8" width="240" x="450" y="108"/>
        <rect fill="var(--dc3-be-fill)" height="56" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="240" x="450" y="108"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="570" y="130">{{ s.gw }}</text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="570" y="148">{{ s.gwSub }}</text>

        <!-- center lane -->
        <rect fill="var(--dc3-be-fill)" height="140" opacity="0.3" rx="10" width="1120" x="10" y="190"/>
        <text fill="var(--dc3-be-text)" font-size="10" font-weight="600" x="24" y="208">{{ s.laneCenter }}</text>
        <!-- GW -> 4 centers -->
        <line marker-end="url(#ia-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="570" x2="170" y1="164" y2="230"/>
        <line marker-end="url(#ia-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="570" x2="390" y1="164" y2="230"/>
        <line marker-end="url(#ia-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="570" x2="610" y1="164" y2="230"/>
        <line marker-end="url(#ia-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="570" x2="830" y1="164" y2="230"/>
        <!-- auth/mgr/data/ai -->
        <rect fill="var(--vp-c-bg)" height="80" rx="8" width="180" x="80" y="230"/>
        <rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="180" x="80" y="230"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="170" y="258">{{ s.auth }}</text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="170" y="280">{{ s.authSub }}</text>
        <rect fill="var(--vp-c-bg)" height="80" rx="8" width="180" x="300" y="230"/>
        <rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="180" x="300" y="230"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="390" y="258">{{ s.mgr }}</text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="390" y="280">{{ s.mgrSub }}</text>
        <rect fill="var(--vp-c-bg)" height="80" rx="8" width="180" x="520" y="230"/>
        <rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="2.5" width="180" x="520" y="230"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" font-weight="700" text-anchor="middle" x="610" y="258">{{ s.data }}</text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="610" y="280">{{ s.dataSub }}</text>
        <rect fill="var(--vp-c-bg)" height="80" rx="8" width="180" x="740" y="230"/>
        <rect fill="var(--dc3-fe-fill)" height="80" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="180" x="740" y="230"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="830" y="258">{{ s.ai }}</text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="830" y="280">{{ s.aiSub }}</text>

        <!-- edge lane -->
        <rect fill="var(--dc3-bus-fill)" height="120" opacity="0.35" rx="10" width="1120" x="10" y="360"/>
        <text fill="var(--dc3-bus-text)" font-size="10" font-weight="600" x="24" y="378">{{ s.laneEdge }}</text>
        <rect fill="var(--vp-c-bg)" height="56" rx="8" width="220" x="520" y="395"/>
        <rect fill="var(--dc3-bus-fill)" height="56" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="220" x="520" y="395"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="630" y="420">{{ s.drv }}</text>
        <rect fill="var(--vp-c-bg)" height="50" rx="8" width="180" x="270" y="398"/>
        <rect fill="var(--dc3-ext-fill)" height="50" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="180" x="270" y="398"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="360" y="427">{{ s.field }}</text>
        <!-- field -> drv -->
        <line marker-end="url(#ia-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="450" x2="520" y1="423" y2="423"/>
        <!-- drv <-> data via RabbitMQ -->
        <path d="M610,330 L610,360 L630,360 L630,395" fill="none" marker-end="url(#ia-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <path d="M650,395 L650,360 L670,360 L670,330" fill="none" marker-end="url(#ia-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5"/>
        <text fill="var(--dc3-arrow-label)" font-size="9.5" text-anchor="middle" x="640" y="354">{{ s.eRabbit }}</text>
        <!-- data -> DB -->
        <path d="M780,150 a80,15 0 0 0 160,0 v80 a80,15 0 0 1 -160,0 z" fill="var(--vp-c-bg)"/>
        <path d="M780,150 a80,15 0 0 0 160,0 v80 a80,15 0 0 1 -160,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <ellipse cx="860" cy="150" fill="none" rx="80" ry="15" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="860" y="198">{{ s.db }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
