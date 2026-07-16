<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'可观测性栈', appT:'核心服务 (dev/app 栈)', svc:'dc3-gateway / dc3-center-*', svcSub:'JSON 文件日志', metric:'Micrometer 指标端点', metricSub:'/actuator/prometheus',
    ls:'Logstash', es:'Elasticsearch', kb:'Kibana :5601', prom:'Prometheus (7d)', graf:'Grafana :3000', pgexp:'postgres-exporter', ngxexp:'nginx-exporter',
    dev:'现场设备 / MQTT 客户端', emqx:'EMQX · dashboard 18083', e1:'共享卷 logs', e2:'周期抓取 scrape', e3:'MQTT 31883'},
  en: {aria:'Observability stack', appT:'Core services (dev/app stack)', svc:'dc3-gateway / dc3-center-*', svcSub:'JSON file logs', metric:'Micrometer endpoint', metricSub:'/actuator/prometheus',
    ls:'Logstash', es:'Elasticsearch', kb:'Kibana :5601', prom:'Prometheus (7d)', graf:'Grafana :3000', pgexp:'postgres-exporter', ngxexp:'nginx-exporter',
    dev:'Field device / MQTT client', emqx:'EMQX · dashboard 18083', e1:'shared volume logs', e2:'periodic scrape', e3:'MQTT 31883'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1300 420">
      <defs><marker id="ob-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect fill="var(--dc3-be-fill)" height="360" opacity="0.25" rx="12" width="340" x="20" y="30" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-be-text)" font-size="11" font-weight="600" x="36" y="52">{{ s.appT }}</text>
      <line marker-end="url(#ob-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="340" x2="420" y1="95" y2="95"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="380" y="87">{{ s.e1 }}</text>
      <line marker-end="url(#ob-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="580" x2="640" y1="95" y2="95"/>
      <line marker-end="url(#ob-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="800" x2="860" y1="95" y2="95"/>
      <line marker-end="url(#ob-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="340" x2="640" y1="205" y2="205"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="490" y="197">{{ s.e2 }}</text>
      <line marker-end="url(#ob-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="420" x2="640" y1="280" y2="230"/>
      <line marker-end="url(#ob-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="420" x2="640" y1="340" y2="235"/>
      <line marker-end="url(#ob-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="800" x2="860" y1="205" y2="205"/>
      <line marker-end="url(#ob-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1080" x2="1080" y1="125" y2="180"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="1130" y="158">{{ s.e3 }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="300" x="40" y="65"/><rect fill="var(--dc3-be-fill)" height="60" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="300" x="40" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="190" y="90">{{ s.svc }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="190" y="108">{{ s.svcSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="300" x="40" y="175"/><rect fill="var(--dc3-be-fill)" height="60" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="300" x="40" y="175"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="190" y="200">{{ s.metric }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="190" y="218">{{ s.metricSub }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="160" x="420" y="65"/><rect fill="var(--dc3-amber-fill)" height="60" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="160" x="420" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="500" y="100">{{ s.ls }}</text>
      <path d="M640,60 a80,14 0 0 0 160,0 v70 a80,14 0 0 1 -160,0 z" fill="var(--vp-c-bg)"/>
      <path d="M640,60 a80,14 0 0 0 160,0 v70 a80,14 0 0 1 -160,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <ellipse cx="720" cy="60" fill="none" rx="80" ry="14" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="720" y="105">{{ s.es }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="160" x="860" y="65"/><rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="160" x="860" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="940" y="100">{{ s.kb }}</text>
      <path d="M640,170 a80,14 0 0 0 160,0 v70 a80,14 0 0 1 -160,0 z" fill="var(--vp-c-bg)"/>
      <path d="M640,170 a80,14 0 0 0 160,0 v70 a80,14 0 0 1 -160,0 z" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <ellipse cx="720" cy="170" fill="none" rx="80" ry="14" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="720" y="215">{{ s.prom }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="160" x="860" y="175"/><rect fill="var(--dc3-fe-fill)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="160" x="860" y="175"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="940" y="210">{{ s.graf }}</text>
      <rect fill="var(--vp-c-bg)" height="46" rx="8" width="180" x="240" y="258"/><rect fill="var(--dc3-amber-fill)" height="46" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1" width="180" x="240" y="258"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="330" y="286">{{ s.pgexp }}</text>
      <rect fill="var(--vp-c-bg)" height="46" rx="8" width="180" x="240" y="318"/><rect fill="var(--dc3-amber-fill)" height="46" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1" width="180" x="240" y="318"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="330" y="346">{{ s.ngxexp }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="200" x="1080" y="65"/><rect fill="var(--dc3-ext-fill)" height="60" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="200" x="1080" y="65"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="1180" y="100">{{ s.dev }}</text>
      <rect fill="var(--vp-c-bg)" height="60" rx="8" width="200" x="1080" y="180"/><rect fill="var(--dc3-bus-fill)" height="60" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="200" x="1080" y="180"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="1180" y="215">{{ s.emqx }}</text>
    </svg>
  </div></DiagramFrame>
</template>
