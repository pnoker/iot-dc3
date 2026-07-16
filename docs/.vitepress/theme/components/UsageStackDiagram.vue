<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'部署栈架构', ingressT:'对外入口（仅这两个发布到宿主机）', web:'前端 dc3-web', webSub:'8080 / 8443', lv:'监听型驱动 listening-virtual', lvSub:'TCP 6270 / UDP 6271',
    appT:'app 栈：预构建镜像', gw:'网关 dc3-gateway', gwSub:'内部 8000', auth:'鉴权中心', mgr:'管理中心', data:'数据中心', agt:'智能中心', drv:'驱动容器组', drvSub:'modbus-tcp / opc-ua / mqtt ...',
    devT:'dev 栈：源码构建', devbuild:'现场 build 网关与四中心',
    dbT:'db 栈：基础设施', pg:'dc3-postgres', pgSub:'PostgreSQL + Timescale', mq:'dc3-rabbitmq', mqSub:'消息总线',
    optT:'optional 栈：可观测性（按需）', opt:'EMQX / ELK / Prometheus / Grafana'},
  en: {aria:'Deployment stack architecture', ingressT:'Public entry (only these two host-published)', web:'Frontend dc3-web', webSub:'8080 / 8443', lv:'Listening driver listening-virtual', lvSub:'TCP 6270 / UDP 6271',
    appT:'app stack: prebuilt images', gw:'Gateway dc3-gateway', gwSub:'internal 8000', auth:'Auth Center', mgr:'Manager Center', data:'Data Center', agt:'Agentic Center', drv:'Driver containers', drvSub:'modbus-tcp / opc-ua / mqtt ...',
    devT:'dev stack: source build', devbuild:'build gateway & four centers',
    dbT:'db stack: infrastructure', pg:'dc3-postgres', pgSub:'PostgreSQL + Timescale', mq:'dc3-rabbitmq', mqSub:'message bus',
    optT:'optional stack: observability', opt:'EMQX / ELK / Prometheus / Grafana'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1320 540">
      <defs><marker id="us-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <!-- ingress -->
      <rect fill="var(--dc3-ext-fill)" height="160" opacity="0.3" rx="12" width="400" x="20" y="20" stroke="var(--dc3-ext-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-text2)" font-size="10" font-weight="600" x="34" y="40">{{ s.ingressT }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="350" x="40" y="55"/><rect fill="var(--dc3-ext-fill)" height="50" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="350" x="40" y="55"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="215" y="78">{{ s.web }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="215" y="95">{{ s.webSub }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="350" x="40" y="115"/><rect fill="var(--dc3-ext-fill)" height="50" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="350" x="40" y="115"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="215" y="138">{{ s.lv }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="215" y="155">{{ s.lvSub }}</text>
      <!-- app -->
      <rect fill="var(--dc3-be-fill)" height="320" opacity="0.22" rx="12" width="560" x="460" y="20" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-be-text)" font-size="10" font-weight="600" x="476" y="40">{{ s.appT }}</text>
      <line marker-end="url(#us-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="390" x2="480" y1="80" y2="80"/>
      <line marker-end="url(#us-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="610" x2="650" y1="80" y2="65"/>
      <line marker-end="url(#us-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="610" x2="650" y1="90" y2="120"/>
      <line marker-end="url(#us-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="610" x2="650" y1="95" y2="180"/>
      <line marker-end="url(#us-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="610" x2="650" y1="100" y2="240"/>
      <line marker-end="url(#us-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="870" x2="890" y1="200" y2="200"/>
      <line marker-end="url(#us-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="390" x2="890" y1="140" y2="195"/>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="130" x="480" y="55"/><rect fill="var(--dc3-be-fill)" height="70" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="2" width="130" x="480" y="55"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle" x="545" y="80">{{ s.gw }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="545" y="100">{{ s.gwSub }}</text>
      <rect fill="var(--vp-c-bg)" height="46" rx="8" width="220" x="650" y="45"/><rect fill="var(--dc3-be-fill)" height="46" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="220" x="650" y="45"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="760" y="73">{{ s.auth }}</text>
      <rect fill="var(--vp-c-bg)" height="46" rx="8" width="220" x="650" y="100"/><rect fill="var(--dc3-be-fill)" height="46" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="220" x="650" y="100"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="760" y="128">{{ s.mgr }}</text>
      <rect fill="var(--vp-c-bg)" height="46" rx="8" width="220" x="650" y="160"/><rect fill="var(--dc3-be-fill)" height="46" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="2" width="220" x="650" y="160"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle" x="760" y="188">{{ s.data }}</text>
      <rect fill="var(--vp-c-bg)" height="46" rx="8" width="220" x="650" y="220"/><rect fill="var(--dc3-fe-fill)" height="46" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="220" x="650" y="220"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="760" y="248">{{ s.agt }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="160" x="890" y="165"/><rect fill="var(--dc3-bus-fill)" height="70" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="160" x="890" y="165"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="970" y="190">{{ s.drv }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="970" y="208">{{ s.drvSub }}</text>
      <!-- dev -->
      <rect fill="var(--dc3-amber-fill)" height="110" opacity="0.3" rx="12" width="220" x="1080" y="20" stroke="var(--dc3-amber-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-amber-stroke)" font-size="10" font-weight="600" x="1094" y="40">{{ s.devT }}</text>
      <rect fill="var(--vp-c-bg)" height="55" rx="8" width="190" x="1095" y="55"/><rect fill="var(--dc3-amber-fill)" height="55" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="190" x="1095" y="55"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="1190" y="87">{{ s.devbuild }}</text>
      <!-- db -->
      <rect fill="var(--dc3-db-fill)" height="150" opacity="0.25" rx="12" width="430" x="460" y="370" stroke="var(--dc3-db-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-db-text)" font-size="10" font-weight="600" x="476" y="390">{{ s.dbT }}</text>
      <path d="M480,410 a85,14 0 0 0 170,0 v70 a85,14 0 0 1 -170,0 z" fill="var(--vp-c-bg)"/>
      <path d="M480,410 a85,14 0 0 0 170,0 v70 a85,14 0 0 1 -170,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <ellipse cx="565" cy="410" fill="none" rx="85" ry="14" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="565" y="450">{{ s.pg }}</text><text fill="var(--dc3-db-text)" font-size="8" text-anchor="middle" x="565" y="468">{{ s.pgSub }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="180" x="690" y="410"/><rect fill="var(--dc3-bus-fill)" height="70" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="180" x="690" y="410"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="780" y="442">{{ s.mq }}</text><text fill="var(--dc3-bus-text)" font-size="8.5" text-anchor="middle" x="780" y="460">{{ s.mqSub }}</text>
      <line marker-end="url(#us-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="760" x2="780" y1="206" y2="410"/>
      <line marker-end="url(#us-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="970" x2="820" y1="235" y2="410"/>
      <!-- opt -->
      <rect fill="var(--dc3-fe-fill)" height="150" opacity="0.25" rx="12" width="380" x="920" y="370" stroke="var(--dc3-fe-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-fe-stroke)" font-size="10" font-weight="600" x="936" y="390">{{ s.optT }}</text>
      <rect fill="var(--vp-c-bg)" height="70" rx="8" width="340" x="940" y="410"/><rect fill="var(--dc3-fe-fill)" height="70" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="340" x="940" y="410"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="1110" y="450">{{ s.opt }}</text>
    </svg>
  </div></DiagramFrame>
</template>
