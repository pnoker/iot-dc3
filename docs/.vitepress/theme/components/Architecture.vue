<!--
  Copyright 2016-present the IoT DC3 original author or authors.
  Licensed under the GNU Affero General Public License v3.0.

  IoT DC3 product architecture overview / Product architecture diagram.
  Pure inline SVG; colors use CSS variables to switch with the light/dark theme, and the text switches between Chinese and English via the lang prop.
-->
<script setup lang="ts">
import {computed} from 'vue'

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
    legAuth: '鉴权流', legPlane: '▼ 命令下行   ▲ 数据上行'
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
    legAuth: 'Auth flow', legPlane: '▼ Command down   ▲ Data up'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <div class="dc3-arch">
    <svg viewBox="0 0 1200 768" role="img" :aria-label="s.busName">
      <defs>
        <marker id="dc3-ah" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="var(--dc3-arrow)"/>
        </marker>
        <marker id="dc3-ah-amber" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="var(--dc3-amber-stroke)"/>
        </marker>
        <marker id="dc3-ah-rose" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="var(--dc3-rose-stroke)"/>
        </marker>
        <pattern id="dc3-grid" width="40" height="40" patternUnits="userSpaceOnUse">
          <path d="M 40 0 L 0 0 0 40" fill="none" stroke="var(--dc3-grid)" stroke-width="0.5"/>
        </pattern>
      </defs>

      <rect width="100%" height="100%" fill="url(#dc3-grid)"/>

      <!-- regions -->
      <rect x="52" y="262" width="756" height="108" rx="12" fill="var(--dc3-region-be)" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="8,4"/>
      <text x="64" y="280" fill="var(--dc3-be-stroke)" font-size="10" font-weight="600">{{ s.centerRegion }}</text>
      <rect x="52" y="496" width="756" height="108" rx="12" fill="var(--dc3-region-be)" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="8,4"/>
      <text x="64" y="514" fill="var(--dc3-be-stroke)" font-size="10" font-weight="600">{{ s.driverRegion }}</text>
      <rect x="840" y="502" width="300" height="200" rx="12" fill="var(--dc3-region-amber)" stroke="var(--dc3-amber-stroke)" stroke-width="1" stroke-dasharray="8,4"/>
      <text x="852" y="520" fill="var(--dc3-amber-stroke)" font-size="10" font-weight="600">{{ s.obsRegion }}</text>

      <!-- arrows -->
      <line x1="175" y1="134" x2="175" y2="171" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dc3-ah)"/>
      <line x1="412" y1="134" x2="412" y2="171" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dc3-ah)"/>
      <line x1="667" y1="134" x2="667" y2="171" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dc3-ah)"/>
      <line x1="415" y1="232" x2="415" y2="289" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dc3-ah)"/>
      <text x="423" y="252" fill="var(--dc3-arrow-label)" font-size="9">{{ s.httpApi }}</text>
      <line x1="120" y1="232" x2="120" y2="289" stroke="var(--dc3-rose-stroke)" stroke-width="1.3" stroke-dasharray="5,4" marker-end="url(#dc3-ah-rose)"/>
      <text x="58" y="258" fill="var(--dc3-rose-stroke)" font-size="8">{{ s.jwtVerify }}</text>
      <line x1="398" y1="372" x2="398" y2="406" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dc3-ah)"/>
      <text x="352" y="392" fill="var(--dc3-arrow-label)" font-size="8">{{ s.cmdDown }}</text>
      <line x1="462" y1="406" x2="462" y2="374" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dc3-ah)"/>
      <text x="470" y="392" fill="var(--dc3-arrow-label)" font-size="8">{{ s.dataUp }}</text>
      <line x1="398" y1="466" x2="398" y2="494" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dc3-ah)"/>
      <line x1="462" y1="494" x2="462" y2="468" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dc3-ah)"/>
      <line x1="398" y1="604" x2="398" y2="642" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dc3-ah)"/>
      <text x="352" y="626" fill="var(--dc3-arrow-label)" font-size="8">{{ s.writeDown }}</text>
      <line x1="462" y1="642" x2="462" y2="606" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#dc3-ah)"/>
      <text x="470" y="626" fill="var(--dc3-arrow-label)" font-size="8">{{ s.readUp }}</text>
      <line x1="808" y1="330" x2="838" y2="330" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-start="url(#dc3-ah)" marker-end="url(#dc3-ah)"/>
      <text x="812" y="322" fill="var(--dc3-arrow-label)" font-size="8">JDBC</text>
      <line x1="980" y1="466" x2="980" y2="501" stroke="var(--dc3-amber-stroke)" stroke-width="1.2" stroke-dasharray="5,4" marker-end="url(#dc3-ah-amber)"/>
      <text x="986" y="487" fill="var(--dc3-amber-stroke)" font-size="8">{{ s.metricsLogs }}</text>

      <!-- Band 1: clients -->
      <rect x="60" y="70" width="230" height="64" rx="6" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text x="175" y="98" fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle">{{ s.webName }}</text>
      <text x="175" y="116" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.webSub }}</text>
      <rect x="305" y="70" width="215" height="64" rx="6" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text x="412" y="98" fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle">{{ s.cliName }}</text>
      <text x="412" y="116" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.cliSub }}</text>
      <rect x="535" y="70" width="265" height="64" rx="6" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text x="667" y="98" fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle">{{ s.agentName }}</text>
      <text x="667" y="116" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.agentSub }}</text>

      <!-- Band 2: gateway -->
      <rect x="60" y="174" width="740" height="58" rx="6" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="430" y="200" fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle">{{ s.gwName }}</text>
      <text x="430" y="218" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.gwSub }}</text>

      <!-- Band 3: center services -->
      <rect x="60" y="292" width="176" height="62" rx="6" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="148" y="316" fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle">auth</text>
      <text x="148" y="332" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.authSub }}</text>
      <text x="148" y="346" fill="var(--dc3-be-text)" font-size="8" text-anchor="middle">:8300 / gRPC 9300</text>
      <rect x="246" y="292" width="176" height="62" rx="6" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="334" y="316" fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle">manager</text>
      <text x="334" y="332" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.mgrSub }}</text>
      <text x="334" y="346" fill="var(--dc3-be-text)" font-size="8" text-anchor="middle">:8400 / gRPC 9400</text>
      <rect x="432" y="292" width="176" height="62" rx="6" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="520" y="316" fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle">data</text>
      <text x="520" y="332" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.dataSub }}</text>
      <text x="520" y="346" fill="var(--dc3-be-text)" font-size="8" text-anchor="middle">:8500 / gRPC 9500</text>
      <rect x="618" y="292" width="182" height="62" rx="6" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="709" y="316" fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle">agentic</text>
      <text x="709" y="332" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.agSub }}</text>
      <text x="709" y="346" fill="var(--dc3-be-text)" font-size="8" text-anchor="middle">:8600 · Spring AI</text>

      <!-- Band 4: message bus -->
      <rect x="60" y="410" width="740" height="56" rx="6" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text x="430" y="435" fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle">{{ s.busName }}</text>
      <text x="430" y="453" fill="var(--dc3-bus-text)" font-size="9" text-anchor="middle">{{ s.busSub }}</text>

      <!-- Band 5: drivers -->
      <rect x="60" y="526" width="150" height="62" rx="6" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="135" y="552" fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle">Modbus</text>
      <text x="135" y="570" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.modbusSub }}</text>
      <rect x="220" y="526" width="140" height="62" rx="6" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="290" y="552" fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle">{{ s.opcName }}</text>
      <text x="290" y="570" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.opcSub }}</text>
      <rect x="370" y="526" width="120" height="62" rx="6" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="430" y="552" fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle">{{ s.s7Name }}</text>
      <text x="430" y="570" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.s7Sub }}</text>
      <rect x="500" y="526" width="120" height="62" rx="6" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="560" y="552" fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle">{{ s.mqttName }}</text>
      <text x="560" y="570" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.mqttSub }}</text>
      <rect x="630" y="526" width="170" height="62" rx="6" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text x="715" y="552" fill="var(--dc3-box-name)" font-size="11" font-weight="600" text-anchor="middle">{{ s.moreName }}</text>
      <text x="715" y="570" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.moreSub }}</text>

      <!-- Band 6: devices -->
      <rect x="60" y="644" width="740" height="58" rx="6" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text x="430" y="670" fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle">{{ s.devName }}</text>
      <text x="430" y="688" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.devSub }}</text>

      <!-- PostgreSQL -->
      <rect x="840" y="262" width="300" height="204" rx="6" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text x="990" y="290" fill="var(--dc3-box-name)" font-size="12" font-weight="600" text-anchor="middle">{{ s.pgTitle }}</text>
      <text x="858" y="318" fill="var(--dc3-db-text)" font-size="9">{{ s.pgL1 }}</text>
      <text x="858" y="338" fill="var(--dc3-db-text)" font-size="9">{{ s.pgL2 }}</text>
      <text x="858" y="358" fill="var(--dc3-db-text)" font-size="9">{{ s.pgL3 }}</text>
      <text x="858" y="384" fill="var(--dc3-text2)" font-size="9">{{ s.pgSchemaLabel }}</text>
      <text x="858" y="402" fill="var(--dc3-text2)" font-size="8">dc3_auth · dc3_manager · dc3_data</text>
      <text x="858" y="416" fill="var(--dc3-text2)" font-size="8">dc3_agentic · dc3_common</text>
      <text x="990" y="446" fill="var(--dc3-db-stroke)" font-size="9" text-anchor="middle">{{ s.pgPort }}</text>

      <!-- Observability -->
      <rect x="852" y="528" width="276" height="44" rx="6" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text x="990" y="548" fill="var(--dc3-box-name)" font-size="10" font-weight="600" text-anchor="middle">EMQX · MQTT Broker</text>
      <text x="990" y="562" fill="var(--dc3-text2)" font-size="8" text-anchor="middle">:31883 · Dashboard :18083</text>
      <rect x="852" y="584" width="276" height="44" rx="6" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text x="990" y="604" fill="var(--dc3-box-name)" font-size="10" font-weight="600" text-anchor="middle">{{ s.elkName }}</text>
      <text x="990" y="618" fill="var(--dc3-text2)" font-size="8" text-anchor="middle">{{ s.elkSub }}</text>
      <rect x="852" y="640" width="276" height="44" rx="6" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text x="990" y="660" fill="var(--dc3-box-name)" font-size="10" font-weight="600" text-anchor="middle">{{ s.promName }}</text>
      <text x="990" y="674" fill="var(--dc3-text2)" font-size="8" text-anchor="middle">{{ s.promSub }}</text>

      <!-- legend -->
      <rect x="60" y="724" width="16" height="11" rx="2" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1"/>
      <text x="82" y="733" fill="var(--dc3-text2)" font-size="9">{{ s.legAccess }}</text>
      <rect x="160" y="724" width="16" height="11" rx="2" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1"/>
      <text x="182" y="733" fill="var(--dc3-text2)" font-size="9">{{ s.legService }}</text>
      <rect x="300" y="724" width="16" height="11" rx="2" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1"/>
      <text x="322" y="733" fill="var(--dc3-text2)" font-size="9">{{ s.legBus }}</text>
      <rect x="430" y="724" width="16" height="11" rx="2" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1"/>
      <text x="452" y="733" fill="var(--dc3-text2)" font-size="9">{{ s.legData }}</text>
      <rect x="560" y="724" width="16" height="11" rx="2" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1"/>
      <text x="582" y="733" fill="var(--dc3-text2)" font-size="9">{{ s.legOps }}</text>
      <rect x="700" y="724" width="16" height="11" rx="2" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1"/>
      <text x="722" y="733" fill="var(--dc3-text2)" font-size="9">{{ s.legDevice }}</text>
      <line x1="870" y1="730" x2="886" y2="730" stroke="var(--dc3-rose-stroke)" stroke-width="1.3" stroke-dasharray="4,3"/>
      <text x="892" y="733" fill="var(--dc3-text2)" font-size="9">{{ s.legAuth }}</text>
      <text x="960" y="733" fill="var(--dc3-arrow-label)" font-size="9">{{ s.legPlane }}</text>
    </svg>
  </div>
</template>

<style>
/* light theme (default) */
.dc3-arch {
  --dc3-grid: #e2e8f0;
  --dc3-text2: #475569;
  --dc3-box-name: #0f172a;
  --dc3-fe-fill: #ecfeff;   --dc3-fe-stroke: #0891b2;
  --dc3-be-fill: #ecfdf5;   --dc3-be-stroke: #059669;  --dc3-be-text: #047857;
  --dc3-db-fill: #f5f3ff;   --dc3-db-stroke: #7c3aed;  --dc3-db-text: #6d28d9;
  --dc3-amber-fill: #fffbeb; --dc3-amber-stroke: #d97706;
  --dc3-rose-stroke: #e11d48;
  --dc3-bus-fill: #fff7ed;  --dc3-bus-stroke: #ea580c;  --dc3-bus-text: #c2410c;
  --dc3-ext-fill: #f1f5f9;  --dc3-ext-stroke: #64748b;
  --dc3-arrow: #94a3b8;     --dc3-arrow-label: #64748b;
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
  --dc3-fe-fill: rgba(8, 51, 68, 0.5);    --dc3-fe-stroke: #22d3ee;
  --dc3-be-fill: rgba(6, 78, 59, 0.5);    --dc3-be-stroke: #34d399;  --dc3-be-text: #34d399;
  --dc3-db-fill: rgba(76, 29, 149, 0.45); --dc3-db-stroke: #a78bfa;  --dc3-db-text: #cbb6f7;
  --dc3-amber-fill: rgba(120, 53, 15, 0.35); --dc3-amber-stroke: #fbbf24;
  --dc3-rose-stroke: #fb7185;
  --dc3-bus-fill: rgba(251, 146, 60, 0.3); --dc3-bus-stroke: #fb923c;  --dc3-bus-text: #fcd9b6;
  --dc3-ext-fill: rgba(30, 41, 59, 0.5);  --dc3-ext-stroke: #94a3b8;
  --dc3-arrow: #64748b;     --dc3-arrow-label: #94a3b8;
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
