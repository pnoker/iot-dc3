<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'故障排查决策树', start:'服务起不来 / 连不上', dep:'依赖就绪？', depSub:'PostgreSQL + RabbitMQ', fixDep:'make ps STACK=db', fixDepSub:'等健康检查通过',
    env:'环境变量已加载？', envSub:'(本地源码)', fixEnv:'source dc3/env/dev.env.sh', port:'端口被占用？', portSub:'8000 / 8300-8600 / 9300-9500', fixPort:'lsof / ss 查 PID', fixPortSub:'改 .env 端口覆盖',
    order:'依赖顺序就绪？', orderSub:'Gateway→Auth→Manager→Data→Agentic→Driver', fixOrder:'按序启动', fixOrderSub:'等上游再起下游',
    log:'读日志关键字', logSub:'Connection refused / 401 / HMAC / UnknownHost', yes:'是', no:'否'},
  en: {aria:'Troubleshooting decision tree', start:'Service won\'t start / connect', dep:'Dependencies ready?', depSub:'PostgreSQL + RabbitMQ', fixDep:'make ps STACK=db', fixDepSub:'wait for health checks',
    env:'Env vars loaded?', envSub:'(local source)', fixEnv:'source dc3/env/dev.env.sh', port:'Port in use?', portSub:'8000 / 8300-8600 / 9300-9500', fixPort:'lsof / ss find PID', fixPortSub:'override in .env',
    order:'Dependency order ready?', orderSub:'Gateway→Auth→Manager→Data→Agentic→Driver', fixOrder:'Start in order', fixOrderSub:'wait for upstream',
    log:'Read log keywords', logSub:'Connection refused / 401 / HMAC / UnknownHost', yes:'yes', no:'no'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1200 640">
      <defs><marker id="ts-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect fill="var(--vp-c-bg)" height="46" rx="23" width="240" x="480" y="20"/><rect fill="var(--dc3-rose-fill)" height="46" rx="23" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" width="240" x="480" y="20"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="600" y="48">{{ s.start }}</text>
      <line marker-end="url(#ts-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="600" x2="600" y1="66" y2="92"/>
      <polygon fill="var(--dc3-amber-fill)" points="600,92 740,140 600,188 460,140" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="600" y="138">{{ s.dep }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="600" y="156">{{ s.depSub }}</text>
      <line marker-end="url(#ts-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="460" x2="350" y1="140" y2="140"/><text fill="var(--dc3-arrow-label)" font-size="9.5" x="395" y="132">{{ s.no }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="240" x="110" y="115"/><rect fill="var(--dc3-be-fill)" height="50" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="240" x="110" y="115"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="230" y="137">{{ s.fixDep }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="230" y="153">{{ s.fixDepSub }}</text>
      <line marker-end="url(#ts-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="600" x2="600" y1="188" y2="214"/><text fill="var(--dc3-arrow-label)" font-size="9.5" x="610" y="206">{{ s.yes }}</text>
      <polygon fill="var(--dc3-amber-fill)" points="600,214 740,262 600,310 460,262" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="600" y="260">{{ s.env }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="600" y="278">{{ s.envSub }}</text>
      <line marker-end="url(#ts-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="460" x2="350" y1="262" y2="262"/><text fill="var(--dc3-arrow-label)" font-size="9.5" x="395" y="254">{{ s.no }}</text>
      <rect fill="var(--vp-c-bg)" height="46" rx="8" width="240" x="110" y="240"/><rect fill="var(--dc3-be-fill)" height="46" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="240" x="110" y="240"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" text-anchor="middle" x="230" y="268">{{ s.fixEnv }}</text>
      <line marker-end="url(#ts-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="600" x2="600" y1="310" y2="336"/><text fill="var(--dc3-arrow-label)" font-size="9.5" x="610" y="328">{{ s.yes }}</text>
      <polygon fill="var(--dc3-amber-fill)" points="600,336 740,384 600,432 460,384" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="600" y="382">{{ s.port }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="600" y="400">{{ s.portSub }}</text>
      <line marker-end="url(#ts-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="460" x2="350" y1="384" y2="384"/><text fill="var(--dc3-arrow-label)" font-size="9.5" x="395" y="376">{{ s.yes }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="240" x="110" y="359"/><rect fill="var(--dc3-be-fill)" height="50" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="240" x="110" y="359"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="230" y="381">{{ s.fixPort }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="230" y="397">{{ s.fixPortSub }}</text>
      <line marker-end="url(#ts-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="600" x2="600" y1="432" y2="458"/><text fill="var(--dc3-arrow-label)" font-size="9.5" x="610" y="450">{{ s.no }}</text>
      <polygon fill="var(--dc3-amber-fill)" points="600,458 740,506 600,554 460,506" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="600" y="504">{{ s.order }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="600" y="522">{{ s.orderSub }}</text>
      <line marker-end="url(#ts-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="460" x2="350" y1="506" y2="506"/><text fill="var(--dc3-arrow-label)" font-size="9.5" x="395" y="498">{{ s.no }}</text>
      <rect fill="var(--vp-c-bg)" height="50" rx="8" width="240" x="110" y="481"/><rect fill="var(--dc3-be-fill)" height="50" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="240" x="110" y="481"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="230" y="503">{{ s.fixOrder }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="230" y="519">{{ s.fixOrderSub }}</text>
      <line marker-end="url(#ts-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="600" x2="600" y1="554" y2="584"/><text fill="var(--dc3-arrow-label)" font-size="9.5" x="610" y="576">{{ s.yes }}</text>
      <rect fill="var(--vp-c-bg)" height="46" rx="23" width="380" x="820" y="568"/><rect fill="var(--dc3-fe-fill)" height="46" rx="23" stroke="var(--dc3-fe-stroke)" stroke-width="2" width="380" x="820" y="568"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" font-weight="700" text-anchor="middle" x="1010" y="588">{{ s.log }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="1010" y="606">{{ s.logSub }}</text>
      <path d="M350,140 Q800,400 820,580" fill="none" marker-end="url(#ts-ah)" stroke="var(--dc3-arrow)" stroke-width="1.2" stroke-dasharray="4,3"/>
    </svg>
  </div></DiagramFrame>
</template>
