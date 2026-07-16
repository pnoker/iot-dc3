<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'Agentic 读值与写命令完整时序', user:'操作员', gw:'dc3-gateway :8000', auth:'dc3-center-auth', agentic:'dc3-center-agentic', tools:'10x @Tool', data:'dc3-center-data', db:'PostgreSQL',
    m1:'提问"读锅炉温度和风机转速"', m2:'验证 X-Auth-Tenant / Token', m3:'Principal + tenant_id + 权限', m4:'转发聊天请求', m5:'getLatestPointValue(pointId=42)', m6:'查最新值（租户隔离）', m7:'SQL 查询', m8:'{value: 86.4, unit: °C}', m9:'PointValueBO', m10:'工具结果', m11:'ChatCompletion', m12:'"温度 86.4°C，风机 60%"',
    m13:'"风机调到 100%"', note1:'写入工具 → 生成待确认 Action', m14:'待确认 Action #abc123', m15:'POST /action/confirm', m16:'PointCommandFacade.submitWrite()', m17:'风机已设为 100%，指令已下发'},
  en: {aria:'Agentic read & write full sequence', user:'Operator', gw:'dc3-gateway :8000', auth:'dc3-center-auth', agentic:'dc3-center-agentic', tools:'10x @Tool', data:'dc3-center-data', db:'PostgreSQL',
    m1:'ask "read boiler temp and fan speed"', m2:'verify X-Auth-Tenant / Token', m3:'Principal + tenant_id + perms', m4:'forward chat request', m5:'getLatestPointValue(pointId=42)', m6:'query latest (tenant-scoped)', m7:'SQL query', m8:'{value: 86.4, unit: °C}', m9:'PointValueBO', m10:'tool result', m11:'ChatCompletion', m12:'"temp 86.4°C, fan 60%"',
    m13:'"set fan to 100%"', note1:'write tool → creates a pending Action', m14:'pending Action #abc123', m15:'POST /action/confirm', m16:'PointCommandFacade.submitWrite()', m17:'fan set to 100%, command dispatched'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
const P = ['user','gw','auth','agentic','tools','data','db']
const PX: Record<string, number> = {user: 90, gw: 245, auth: 390, agentic: 540, tools: 700, data: 860, db: 1020}
const fill = ['var(--dc3-ext-fill)','var(--dc3-be-fill)','var(--dc3-rose-fill)','var(--dc3-fe-fill)','var(--dc3-amber-fill)','var(--dc3-be-fill)','var(--dc3-db-fill)']
const stroke = ['var(--dc3-ext-stroke)','var(--dc3-be-stroke)','var(--dc3-rose-stroke)','var(--dc3-fe-stroke)','var(--dc3-amber-stroke)','var(--dc3-be-stroke)','var(--dc3-db-stroke)']
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1110 760">
      <defs><marker id="sas-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line v-for="p in P" :key="'l'+p" :x1="PX[p]" :x2="PX[p]" y1="70" y2="730" stroke="var(--dc3-divider)" stroke-width="1" stroke-dasharray="4,4"/>
      <rect v-for="(p,i) in P" :key="'b'+p" :x="PX[p]-72" y="22" width="144" height="48" rx="8" :fill="fill[i]" :stroke="stroke[i]" stroke-width="1.5" opacity="0.65"/>
      <text v-for="p in P" :key="'t'+p" :x="PX[p]" y="51" class="d-name" fill="var(--dc3-box-name)" font-size="9.5" font-weight="700" text-anchor="middle">{{ s[p] }}</text>
      <!-- read round -->
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" :x1="PX.user" :x2="PX.gw" y1="100" y2="100"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.user+PX.gw)/2" y="92">{{ s.m1 }}</text>
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" :x1="PX.gw" :x2="PX.auth" y1="130" y2="130"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.gw+PX.auth)/2" y="122">{{ s.m2 }}</text>
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" stroke-dasharray="5,4" :x1="PX.auth" :x2="PX.gw" y1="158" y2="158"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.auth+PX.gw)/2" y="150">{{ s.m3 }}</text>
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" :x1="PX.gw" :x2="PX.agentic" y1="190" y2="190"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.gw+PX.agentic)/2" y="182">{{ s.m4 }}</text>
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" :x1="PX.agentic" :x2="PX.tools" y1="222" y2="222"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.agentic+PX.tools)/2" y="214">{{ s.m5 }}</text>
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" :x1="PX.tools" :x2="PX.data" y1="252" y2="252"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.tools+PX.data)/2" y="244">{{ s.m6 }}</text>
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" :x1="PX.data" :x2="PX.db" y1="282" y2="282"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.data+PX.db)/2" y="274">{{ s.m7 }}</text>
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" stroke-dasharray="5,4" :x1="PX.db" :x2="PX.data" y1="310" y2="310"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.db+PX.data)/2" y="302">{{ s.m8 }}</text>
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" stroke-dasharray="5,4" :x1="PX.data" :x2="PX.tools" y1="338" y2="338"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.data+PX.tools)/2" y="330">{{ s.m9 }}</text>
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" stroke-dasharray="5,4" :x1="PX.tools" :x2="PX.agentic" y1="366" y2="366"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.tools+PX.agentic)/2" y="358">{{ s.m10 }}</text>
      <path :d="`M${PX.agentic},400 Q${(PX.agentic+PX.user)/2},440 ${PX.user},410`" fill="none" marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.agentic+PX.user)/2" y="438">{{ s.m12 }}</text>
      <!-- write round -->
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" :x1="PX.user" :x2="PX.gw" y1="475" y2="475"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.user+PX.gw)/2" y="467">{{ s.m13 }}</text>
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" :x1="PX.gw" :x2="PX.agentic" y1="500" y2="500"/>
      <rect :x="PX.agentic-90" y="515" width="180" height="28" rx="4" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1"/>
      <text :x="PX.agentic" y="533" fill="var(--dc3-box-name)" font-size="8.5" text-anchor="middle">{{ s.note1 }}</text>
      <path :d="`M${PX.agentic},565 Q${(PX.agentic+PX.user)/2},600 ${PX.user},570`" fill="none" marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.agentic+PX.user)/2" y="600">{{ s.m14 }}</text>
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" :x1="PX.user" :x2="PX.gw" y1="630" y2="630"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.user+PX.gw)/2" y="622">{{ s.m15 }}</text>
      <line marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" :x1="PX.agentic" :x2="PX.data" y1="660" y2="660"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.agentic+PX.data)/2" y="652">{{ s.m16 }}</text>
      <path :d="`M${PX.agentic},695 Q${(PX.agentic+PX.user)/2},730 ${PX.user},700`" fill="none" marker-end="url(#sas-ah)" stroke="var(--dc3-arrow)" stroke-width="1.4" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="8.5" text-anchor="middle" :x="(PX.agentic+PX.user)/2" y="728">{{ s.m17 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
