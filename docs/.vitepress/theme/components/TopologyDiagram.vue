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
    centerRegion: '中心服务 Center Services · gRPC 互联 (Facade: api / grpc / local)',
    driverRegion: '驱动层 Driver Layer · 28+ 协议驱动 (gRPC ↔ manager, RabbitMQ 收发)',
    obsRegion: '可选运维 Observability',
    webName: 'Web 控制台', webSub: 'Vue3 · Vite8 · Element Plus · :8080',
    cliName: 'dc3-cli', cliSub: 'TypeScript CLI (HTTP)',
    agentName: 'AI Agent / MCP Client', agentSub: '对话 · 工具调用 · OAuth2',
    gwName: 'dc3-gateway · API 网关  :8000',
    gwSub: 'Spring Cloud Gateway (WebFlux) · 统一路由 + 鉴权过滤 + 限流',
    authSub: '认证 · 租户 · RBAC', mgrSub: 'Driver/Device/Point/Profile',
    dataSub: '点位值 · 命令 · 历史', agSub: 'AI 工具调用 · 会话',
    busName: 'RabbitMQ 消息总线',
    busSub: 'point_value · point_command · result · event · notify    |    MQTT :1883',
    modbusSub: 'TCP / RTU', opcName: 'OPC', opcSub: 'UA / DA',
    s7Name: 'S7 PLC', s7Sub: 'Siemens', mqttName: 'MQTT', mqttSub: 'Broker',
    moreName: '+ 24 协议', moreSub: 'BACnet·SNMP·IEC104·CoAP…',
    devName: '现场设备 Field Devices',
    devSub: 'PLC · 传感器 · 电表 · 网关 · 仪表  (Modbus / OPC / S7 / MQTT)',
    pgTitle: 'PostgreSQL 持久层',
    pgL1: '• TimescaleDB — 点位值 / 历史 (时序)',
    pgL2: '• pgvector — AI 向量检索 / 记忆',
    pgL3: '• AGE — 图数据 (设备关系)',
    pgSchemaLabel: 'schema:',
    pgPort: ':35432 → 5432',
    elkName: 'ELK 日志管道', elkSub: 'ES · Logstash · Kibana :5601 · APM',
    promName: 'Prometheus + Grafana', promSub: '指标采集 · 监控仪表盘 :3000',
    httpApi: 'HTTP /api', jwtVerify: 'JWT校验 gRPC',
    cmdDown: '命令▼', dataUp: '▲数据', writeDown: '写▼', readUp: '▲读',
    metricsLogs: 'metrics/logs',
    legAccess: '接入层', legService: '服务 / 驱动', legBus: '消息总线',
    legData: '数据存储', legOps: '运维 (可选)', legDevice: '现场设备 / 外部',
    legAuth: '鉴权流', legPlane: '▼ 命令下行   ▲ 数据上行',
    aria: 'IoT DC3 整体服务拓扑：一个网关、四个中心服务与一组协议驱动，经 RabbitMQ 消息总线串联数据上行与命令下行，PostgreSQL 持久化，并可选 ELK 与 Prometheus 运维'
  },
  en: {
    centerRegion: 'Center Services · gRPC interconnect (Facade: api / grpc / local)',
    driverRegion: 'Driver Layer · 28+ protocol drivers (gRPC ↔ manager, via RabbitMQ)',
    obsRegion: 'Observability (optional)',
    webName: 'Web Console', webSub: 'Vue3 · Vite8 · Element Plus · :8080',
    cliName: 'dc3-cli', cliSub: 'TypeScript CLI (HTTP)',
    agentName: 'AI Agent / MCP Client', agentSub: 'Chat · Tool calls · OAuth2',
    gwName: 'dc3-gateway · API Gateway  :8000',
    gwSub: 'Spring Cloud Gateway (WebFlux) · routing + auth filter + rate limit',
    authSub: 'Auth · Tenant · RBAC', mgrSub: 'Driver/Device/Point/Profile',
    dataSub: 'Point value · Command · History', agSub: 'AI tool calls · Session',
    busName: 'RabbitMQ Message Bus',
    busSub: 'point_value · point_command · result · event · notify    |    MQTT :1883',
    modbusSub: 'TCP / RTU', opcName: 'OPC', opcSub: 'UA / DA',
    s7Name: 'S7 PLC', s7Sub: 'Siemens', mqttName: 'MQTT', mqttSub: 'Broker',
    moreName: '+ 24 protocols', moreSub: 'BACnet·SNMP·IEC104·CoAP…',
    devName: 'Field Devices',
    devSub: 'PLC · Sensor · Meter · Gateway  (Modbus / OPC / S7 / MQTT)',
    pgTitle: 'PostgreSQL Storage',
    pgL1: '• TimescaleDB — point values / history (time-series)',
    pgL2: '• pgvector — AI vector search / memory',
    pgL3: '• AGE — graph (device relations)',
    pgSchemaLabel: 'schema:',
    pgPort: ':35432 → 5432',
    elkName: 'ELK Log Pipeline', elkSub: 'ES · Logstash · Kibana :5601 · APM',
    promName: 'Prometheus + Grafana', promSub: 'Metrics · Dashboards :3000',
    httpApi: 'HTTP /api', jwtVerify: 'JWT verify gRPC',
    cmdDown: 'Cmd▼', dataUp: '▲Data', writeDown: 'Write▼', readUp: '▲Read',
    metricsLogs: 'metrics/logs',
    legAccess: 'Access', legService: 'Services / Drivers', legBus: 'Message Bus',
    legData: 'Data Store', legOps: 'Ops (optional)', legDevice: 'Devices / External',
    legAuth: 'Auth flow', legPlane: '▼ Command down   ▲ Data up',
    aria: 'IoT DC3 end-to-end service topology: one gateway, four center services and a set of protocol drivers linked by the RabbitMQ message bus for data uplink and command downlink, with PostgreSQL storage and optional ELK and Prometheus ops'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <DiagramFrame>
    <div class="dc3-arch">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1200 768">
        <defs>
          <marker id="dc3-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <marker id="dc3-ah-amber" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-amber-stroke)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <marker id="dc3-ah-rose" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-rose-stroke)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <pattern id="dc3-grid" height="40" patternUnits="userSpaceOnUse" width="40">
            <path d="M 40 0 L 0 0 0 40" fill="none" stroke="var(--dc3-grid)" stroke-width="0.5"/>
          </pattern>
        </defs>

        <rect fill="url(#dc3-grid)" height="100%" width="100%"/>

        <!-- regions -->
        <rect fill="var(--dc3-region-be)" height="108" rx="12" stroke="var(--dc3-be-stroke)" stroke-dasharray="6,3"
              stroke-width="1" width="756"
              x="52" y="262"/>
        <text fill="var(--dc3-be-stroke)" font-size="10" font-weight="600" x="64" y="280">{{ s.centerRegion }}</text>
        <rect fill="var(--dc3-region-be)" height="108" rx="12" stroke="var(--dc3-be-stroke)" stroke-dasharray="6,3"
              stroke-width="1" width="756"
              x="52" y="496"/>
        <text fill="var(--dc3-be-stroke)" font-size="10" font-weight="600" x="64" y="514">{{ s.driverRegion }}</text>
        <rect fill="var(--dc3-region-amber)" height="200" rx="12" stroke="var(--dc3-amber-stroke)"
              stroke-dasharray="6,3"
              stroke-width="1"
              width="300" x="840" y="502"/>
        <text fill="var(--dc3-amber-stroke)" font-size="10" font-weight="600" x="852" y="520">{{ s.obsRegion }}</text>

        <!-- arrows -->
        <line marker-end="url(#dc3-ah)" stroke="var(--dc3-arrow)" stroke-width="1" x1="175" x2="175" y1="134" y2="171"/>
        <line marker-end="url(#dc3-ah)" stroke="var(--dc3-arrow)" stroke-width="1" x1="412" x2="412" y1="134" y2="171"/>
        <line marker-end="url(#dc3-ah)" stroke="var(--dc3-arrow)" stroke-width="1" x1="667" x2="667" y1="134" y2="171"/>
        <line marker-end="url(#dc3-ah)" stroke="var(--dc3-arrow)" stroke-width="1" x1="415" x2="415" y1="232" y2="289"/>
        <text fill="var(--dc3-arrow-label)" font-size="9" x="423" y="252">{{ s.httpApi }}</text>
        <line marker-end="url(#dc3-ah-rose)" stroke="var(--dc3-rose-stroke)" stroke-dasharray="4,4" stroke-width="0.8"
              x1="120" x2="120"
              y1="232" y2="289"/>
        <text fill="var(--dc3-rose-stroke)" font-size="8" x="58" y="258">{{ s.jwtVerify }}</text>
        <line marker-end="url(#dc3-ah)" stroke="var(--dc3-arrow)" stroke-width="1" x1="398" x2="398" y1="372" y2="406"/>
        <text fill="var(--dc3-arrow-label)" font-size="8" x="352" y="392">{{ s.cmdDown }}</text>
        <line marker-end="url(#dc3-ah)" stroke="var(--dc3-arrow)" stroke-width="1" x1="462" x2="462" y1="406" y2="374"/>
        <text fill="var(--dc3-arrow-label)" font-size="8" x="470" y="392">{{ s.dataUp }}</text>
        <line marker-end="url(#dc3-ah)" stroke="var(--dc3-arrow)" stroke-width="1" x1="398" x2="398" y1="466" y2="494"/>
        <line marker-end="url(#dc3-ah)" stroke="var(--dc3-arrow)" stroke-width="1" x1="462" x2="462" y1="494" y2="468"/>
        <line marker-end="url(#dc3-ah)" stroke="var(--dc3-arrow)" stroke-width="1" x1="398" x2="398" y1="604" y2="642"/>
        <text fill="var(--dc3-arrow-label)" font-size="8" x="352" y="626">{{ s.writeDown }}</text>
        <line marker-end="url(#dc3-ah)" stroke="var(--dc3-arrow)" stroke-width="1" x1="462" x2="462" y1="642" y2="606"/>
        <text fill="var(--dc3-arrow-label)" font-size="8" x="470" y="626">{{ s.readUp }}</text>
        <line marker-end="url(#dc3-ah)" marker-start="url(#dc3-ah)" stroke="var(--dc3-arrow)" stroke-width="1" x1="808"
              x2="838" y1="330"
              y2="330"/>
        <text fill="var(--dc3-arrow-label)" font-size="8" x="812" y="322">JDBC</text>
        <line marker-end="url(#dc3-ah-amber)" stroke="var(--dc3-amber-stroke)" stroke-dasharray="4,4" stroke-width="0.8"
              x1="980" x2="980"
              y1="466" y2="501"/>
        <text fill="var(--dc3-amber-stroke)" font-size="8" x="986" y="487">{{ s.metricsLogs }}</text>

        <!-- Band 1: clients -->
        <rect fill="var(--dc3-fe-fill)" height="64" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="230"
              x="60"
              y="70"/>
        <text fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle" x="175" y="98">{{
          s.webName
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="175" y="116">{{ s.webSub }}</text>
        <rect fill="var(--dc3-fe-fill)" height="64" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="215"
              x="305"
              y="70"/>
        <text fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle" x="412" y="98">{{
          s.cliName
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="412" y="116">{{ s.cliSub }}</text>
        <rect fill="var(--dc3-fe-fill)" height="64" rx="6" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="265"
              x="535"
              y="70"/>
        <text fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle" x="667" y="98">
          {{ s.agentName }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="667" y="116">{{ s.agentSub }}</text>

        <!-- Band 2: gateway -->
        <rect fill="var(--dc3-be-fill)" height="58" rx="6" stroke="var(--dc3-be-stroke)" stroke-width="1" width="740"
              x="60"
              y="174"/>
        <text fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle" x="430" y="200">{{
          s.gwName
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="430" y="218">{{ s.gwSub }}</text>

        <!-- Band 3: center services -->
        <rect fill="var(--dc3-be-fill)" height="62" rx="6" stroke="var(--dc3-be-stroke)" stroke-width="1" width="176"
              x="60"
              y="292"/>
        <text fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle" x="148" y="316">auth
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="148" y="332">{{ s.authSub }}</text>
        <text fill="var(--dc3-be-text)" font-size="8" text-anchor="middle" x="148" y="346">:8300 / gRPC 9300</text>
        <rect fill="var(--dc3-be-fill)" height="62" rx="6" stroke="var(--dc3-be-stroke)" stroke-width="1" width="176"
              x="246"
              y="292"/>
        <text fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle" x="334" y="316">manager
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="334" y="332">{{ s.mgrSub }}</text>
        <text fill="var(--dc3-be-text)" font-size="8" text-anchor="middle" x="334" y="346">:8400 / gRPC 9400</text>
        <rect fill="var(--dc3-be-fill)" height="62" rx="6" stroke="var(--dc3-be-stroke)" stroke-width="1" width="176"
              x="432"
              y="292"/>
        <text fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle" x="520" y="316">data
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="520" y="332">{{ s.dataSub }}</text>
        <text fill="var(--dc3-be-text)" font-size="8" text-anchor="middle" x="520" y="346">:8500 / gRPC 9500</text>
        <rect fill="var(--dc3-be-fill)" height="62" rx="6" stroke="var(--dc3-be-stroke)" stroke-width="1" width="182"
              x="618"
              y="292"/>
        <text fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle" x="709" y="316">agentic
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="709" y="332">{{ s.agSub }}</text>
        <text fill="var(--dc3-be-text)" font-size="8" text-anchor="middle" x="709" y="346">:8600 · Spring AI</text>

        <!-- Band 4: message bus -->
        <rect fill="var(--dc3-bus-fill)" height="56" rx="6" stroke="var(--dc3-bus-stroke)" stroke-width="1" width="740"
              x="60"
              y="410"/>
        <text fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle" x="430" y="435">
          {{ s.busName }}
        </text>
        <text fill="var(--dc3-bus-text)" font-size="9" text-anchor="middle" x="430" y="453">{{ s.busSub }}</text>

        <!-- Band 5: drivers -->
        <rect fill="var(--dc3-be-fill)" height="62" rx="6" stroke="var(--dc3-be-stroke)" stroke-width="1" width="150"
              x="60"
              y="526"/>
        <text fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle" x="135" y="552">Modbus
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="135" y="570">{{ s.modbusSub }}</text>
        <rect fill="var(--dc3-be-fill)" height="62" rx="6" stroke="var(--dc3-be-stroke)" stroke-width="1" width="140"
              x="220"
              y="526"/>
        <text fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle" x="290" y="552">
          {{ s.opcName }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="290" y="570">{{ s.opcSub }}</text>
        <rect fill="var(--dc3-be-fill)" height="62" rx="6" stroke="var(--dc3-be-stroke)" stroke-width="1" width="120"
              x="370"
              y="526"/>
        <text fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle" x="430" y="552">{{
          s.s7Name
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="430" y="570">{{ s.s7Sub }}</text>
        <rect fill="var(--dc3-be-fill)" height="62" rx="6" stroke="var(--dc3-be-stroke)" stroke-width="1" width="120"
              x="500"
              y="526"/>
        <text fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle" x="560" y="552">
          {{ s.mqttName }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="560" y="570">{{ s.mqttSub }}</text>
        <rect fill="var(--dc3-ext-fill)" height="62" rx="6" stroke="var(--dc3-ext-stroke)" stroke-width="1" width="170"
              x="630"
              y="526"/>
        <text fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle" x="715" y="552">
          {{ s.moreName }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="715" y="570">{{ s.moreSub }}</text>

        <!-- Band 6: devices -->
        <rect fill="var(--dc3-ext-fill)" height="58" rx="6" stroke="var(--dc3-ext-stroke)" stroke-width="1" width="740"
              x="60"
              y="644"/>
        <text fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle" x="430" y="670">
          {{ s.devName }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="430" y="688">{{ s.devSub }}</text>

        <!-- PostgreSQL -->
        <rect fill="var(--dc3-db-fill)" height="204" rx="6" stroke="var(--dc3-db-stroke)" stroke-width="1" width="300"
              x="840"
              y="262"/>
        <text fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle" x="990" y="290">
          {{ s.pgTitle }}
        </text>
        <text fill="var(--dc3-db-text)" font-size="9" x="858" y="318">{{ s.pgL1 }}</text>
        <text fill="var(--dc3-db-text)" font-size="9" x="858" y="338">{{ s.pgL2 }}</text>
        <text fill="var(--dc3-db-text)" font-size="9" x="858" y="358">{{ s.pgL3 }}</text>
        <text fill="var(--dc3-text2)" font-size="9" x="858" y="384">{{ s.pgSchemaLabel }}</text>
        <text fill="var(--dc3-text2)" font-size="8" x="858" y="402">dc3_auth · dc3_manager · dc3_data</text>
        <text fill="var(--dc3-text2)" font-size="8" x="858" y="416">dc3_agentic · dc3_common</text>
        <text fill="var(--dc3-db-stroke)" font-size="9" text-anchor="middle" x="990" y="446">{{ s.pgPort }}</text>

        <!-- Observability -->
        <rect fill="var(--dc3-amber-fill)" height="44" rx="6" stroke="var(--dc3-amber-stroke)" stroke-width="1"
              width="276" x="852"
              y="528"/>
        <text fill="var(--dc3-box-name)" font-size="10" font-weight="600" text-anchor="middle" x="990" y="548">EMQX ·
          MQTT
          Broker
        </text>
        <text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="990" y="562">:31883 · Dashboard :18083
        </text>
        <rect fill="var(--dc3-amber-fill)" height="44" rx="6" stroke="var(--dc3-amber-stroke)" stroke-width="1"
              width="276" x="852"
              y="584"/>
        <text fill="var(--dc3-box-name)" font-size="10" font-weight="600" text-anchor="middle" x="990" y="604">
          {{ s.elkName }}
        </text>
        <text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="990" y="618">{{ s.elkSub }}</text>
        <rect fill="var(--dc3-amber-fill)" height="44" rx="6" stroke="var(--dc3-amber-stroke)" stroke-width="1"
              width="276" x="852"
              y="640"/>
        <text fill="var(--dc3-box-name)" font-size="10" font-weight="600" text-anchor="middle" x="990" y="660">
          {{ s.promName }}
        </text>
        <text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="990" y="674">{{ s.promSub }}</text>

        <!-- legend -->
        <rect fill="var(--dc3-fe-fill)" height="11" rx="2" stroke="var(--dc3-fe-stroke)" stroke-width="1" width="16"
              x="60"
              y="724"/>
        <text fill="var(--dc3-text2)" font-size="9" x="82" y="733">{{ s.legAccess }}</text>
        <rect fill="var(--dc3-be-fill)" height="11" rx="2" stroke="var(--dc3-be-stroke)" stroke-width="1" width="16"
              x="160"
              y="724"/>
        <text fill="var(--dc3-text2)" font-size="9" x="182" y="733">{{ s.legService }}</text>
        <rect fill="var(--dc3-bus-fill)" height="11" rx="2" stroke="var(--dc3-bus-stroke)" stroke-width="1" width="16"
              x="300"
              y="724"/>
        <text fill="var(--dc3-text2)" font-size="9" x="322" y="733">{{ s.legBus }}</text>
        <rect fill="var(--dc3-db-fill)" height="11" rx="2" stroke="var(--dc3-db-stroke)" stroke-width="1" width="16"
              x="430"
              y="724"/>
        <text fill="var(--dc3-text2)" font-size="9" x="452" y="733">{{ s.legData }}</text>
        <rect fill="var(--dc3-amber-fill)" height="11" rx="2" stroke="var(--dc3-amber-stroke)" stroke-width="1"
              width="16"
              x="560"
              y="724"/>
        <text fill="var(--dc3-text2)" font-size="9" x="582" y="733">{{ s.legOps }}</text>
        <rect fill="var(--dc3-ext-fill)" height="11" rx="2" stroke="var(--dc3-ext-stroke)" stroke-width="1" width="16"
              x="700"
              y="724"/>
        <text fill="var(--dc3-text2)" font-size="9" x="722" y="733">{{ s.legDevice }}</text>
        <line stroke="var(--dc3-rose-stroke)" stroke-dasharray="4,3" stroke-width="0.8" x1="870" x2="886" y1="730"
              y2="730"/>
        <text fill="var(--dc3-text2)" font-size="9" x="892" y="733">{{ s.legAuth }}</text>
        <text fill="var(--dc3-arrow-label)" font-size="9" x="960" y="733">{{ s.legPlane }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>

<style>
/* light theme (default) */
.dc3-arch {
  --dc3-grid: #cbd5e1;
  --dc3-text2: #475569;
  --dc3-box-name: #0f172a;
  --dc3-fe-fill: #ecfeff;
  --dc3-fe-stroke: #0891b2;
  --dc3-be-fill: #ecfdf5;
  --dc3-be-stroke: #059669;
  --dc3-be-text: #047857;
  --dc3-db-fill: #f5f3ff;
  --dc3-db-stroke: #7c3aed;
  --dc3-db-text: #6d28d9;
  --dc3-amber-fill: #fffbeb;
  --dc3-amber-stroke: #d97706;
  --dc3-rose-stroke: #e11d48;
  --dc3-bus-fill: #fff7ed;
  --dc3-bus-stroke: #ea580c;
  --dc3-bus-text: #c2410c;
  --dc3-ext-fill: #f1f5f9;
  --dc3-ext-stroke: #64748b;
  --dc3-arrow: #94a3b8;
  --dc3-arrow-label: #64748b;
  --dc3-region-be: rgba(5, 150, 105, 0.05);
  --dc3-region-amber: rgba(217, 119, 6, 0.06);

  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
  border-radius: 12px;
  padding: 16px;
  margin: 16px 0 8px;
  overflow-x: auto;
}

/* dark theme */
.dark .dc3-arch {
  --dc3-grid: #1e293b;
  --dc3-text2: #94a3b8;
  --dc3-box-name: #ffffff;
  --dc3-fe-fill: rgba(8, 51, 68, 0.25);
  --dc3-fe-stroke: #22d3ee;
  --dc3-be-fill: rgba(6, 78, 59, 0.25);
  --dc3-be-stroke: #34d399;
  --dc3-be-text: #34d399;
  --dc3-db-fill: rgba(76, 29, 149, 0.22);
  --dc3-db-stroke: #a78bfa;
  --dc3-db-text: #cbb6f7;
  --dc3-amber-fill: rgba(120, 53, 15, 0.18);
  --dc3-amber-stroke: #fbbf24;
  --dc3-rose-stroke: #fb7185;
  --dc3-bus-fill: rgba(251, 146, 60, 0.15);
  --dc3-bus-stroke: #fb923c;
  --dc3-bus-text: #fcd9b6;
  --dc3-ext-fill: rgba(100, 116, 139, 0.07);
  --dc3-ext-stroke: #94a3b8;
  --dc3-arrow: #64748b;
  --dc3-arrow-label: #94a3b8;
  --dc3-region-be: rgba(52, 211, 153, 0.04);
  --dc3-region-amber: rgba(251, 191, 36, 0.05);
}

.dc3-arch svg {
  width: 100%;
  height: auto;
  display: block;
  font-family: 'JetBrains Mono', ui-monospace, 'SFMono-Regular', Consolas, monospace;
}
</style>
