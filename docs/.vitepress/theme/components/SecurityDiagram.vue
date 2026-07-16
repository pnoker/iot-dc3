<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'物联网安全分层与威胁对策', devT:'设备 (物理可达)', dev:'传感器 / 执行器', devSub:'固件 + 密钥',
    netT:'通信链路 (网络暴露)', net:'TLS / DTLS', netSub:'认证 + 防重放',
    platT:'平台 (高价值目标)', gw:'网关', gwSub:'唯一入口', auth:'认证 + RBAC', authSub:'租户隔离 + 审计',
    dataT:'数据 (隐私 + 合规)', data:'脱敏 + 加密', dataSub:'最小留存',
    t1:'设备伪造', t2:'固件篡改', t3:'重放攻击', t4:'中间人', t5:'DDoS', t6:'越权 / 跨租户',
    c1:'安全启动 + 验签 OTA', c2:'时间戳 + nonce', c3:'双向认证', c4:'限流 + 网关收敛', c5:'fail-closed + 隔离'},
  en: {aria:'IoT security layers & threat countermeasures', devT:'Device (physical access)', dev:'Sensor / actuator', devSub:'firmware + key',
    netT:'Link (network exposed)', net:'TLS / DTLS', netSub:'auth + anti-replay',
    platT:'Platform (high-value target)', gw:'Gateway', gwSub:'sole entry', auth:'Auth + RBAC', authSub:'tenant isolation + audit',
    dataT:'Data (privacy + compliance)', data:'Desensitize + encrypt', dataSub:'minimal retention',
    t1:'device spoofing', t2:'firmware tamper', t3:'replay', t4:'MITM', t5:'DDoS', t6:'privilege escalation / cross-tenant',
    c1:'secure boot + signed OTA', c2:'timestamp + nonce', c3:'mutual auth', c4:'rate-limit + gateway', c5:'fail-closed + isolation'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1260 460">
      <defs><marker id="se-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <line marker-end="url(#se-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="230" x2="300" y1="120" y2="120"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="265" y="112">采集</text>
      <line marker-end="url(#se-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="490" x2="560" y1="120" y2="120"/>
      <line marker-end="url(#se-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="890" x2="960" y1="120" y2="120"/>
      <!-- device -->
      <rect fill="var(--dc3-ext-fill)" height="160" opacity="0.3" rx="12" width="220" x="20" y="60" stroke="var(--dc3-ext-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-text2)" font-size="10" font-weight="600" x="34" y="80">{{ s.devT }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="180" x="40" y="100"/><rect fill="var(--dc3-ext-fill)" height="80" rx="8" stroke="var(--dc3-ext-stroke)" stroke-width="1.5" width="180" x="40" y="100"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="130" y="128">{{ s.dev }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="130" y="148">{{ s.devSub }}</text>
      <!-- net -->
      <rect fill="var(--dc3-bus-fill)" height="160" opacity="0.3" rx="12" width="220" x="280" y="60" stroke="var(--dc3-bus-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-bus-text)" font-size="10" font-weight="600" x="294" y="80">{{ s.netT }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="180" x="300" y="100"/><rect fill="var(--dc3-bus-fill)" height="80" rx="8" stroke="var(--dc3-bus-stroke)" stroke-width="1.5" width="180" x="300" y="100"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="390" y="128">{{ s.net }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="390" y="148">{{ s.netSub }}</text>
      <!-- platform -->
      <rect fill="var(--dc3-be-fill)" height="160" opacity="0.3" rx="12" width="340" x="540" y="60" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-be-text)" font-size="10" font-weight="600" x="554" y="80">{{ s.platT }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="140" x="560" y="100"/><rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="140" x="560" y="100"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="630" y="128">{{ s.gw }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="630" y="148">{{ s.gwSub }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="150" x="720" y="100"/><rect fill="var(--dc3-be-fill)" height="80" rx="8" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="150" x="720" y="100"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="795" y="128">{{ s.auth }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="795" y="148">{{ s.authSub }}</text>
      <!-- data -->
      <rect fill="var(--dc3-db-fill)" height="160" opacity="0.3" rx="12" width="240" x="940" y="60" stroke="var(--dc3-db-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-db-text)" font-size="10" font-weight="600" x="954" y="80">{{ s.dataT }}</text>
      <rect fill="var(--vp-c-bg)" height="80" rx="8" width="200" x="960" y="100"/><rect fill="var(--dc3-db-fill)" height="80" rx="8" stroke="var(--dc3-db-stroke)" stroke-width="1.5" width="200" x="960" y="100"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="1060" y="128">{{ s.data }}</text><text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="1060" y="148">{{ s.dataSub }}</text>
      <!-- threats -->
      <line stroke="var(--dc3-rose-stroke)" stroke-width="1.2" stroke-dasharray="4,3" x1="130" x2="130" y1="180" y2="250"/>
      <text fill="var(--dc3-rose-stroke)" font-size="9.5" text-anchor="middle" x="130" y="268">{{ s.t1 }}</text>
      <text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="130" y="284">{{ s.c1 }}</text>
      <line stroke="var(--dc3-rose-stroke)" stroke-width="1.2" stroke-dasharray="4,3" x1="390" x2="390" y1="180" y2="250"/>
      <text fill="var(--dc3-rose-stroke)" font-size="9.5" text-anchor="middle" x="390" y="268">{{ s.t3 }} / {{ s.t4 }}</text>
      <text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="390" y="284">{{ s.c2 }} · {{ s.c3 }}</text>
      <line stroke="var(--dc3-rose-stroke)" stroke-width="1.2" stroke-dasharray="4,3" x1="630" x2="630" y1="180" y2="250"/>
      <text fill="var(--dc3-rose-stroke)" font-size="9.5" text-anchor="middle" x="630" y="268">{{ s.t5 }}</text>
      <text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="630" y="284">{{ s.c4 }}</text>
      <line stroke="var(--dc3-rose-stroke)" stroke-width="1.2" stroke-dasharray="4,3" x1="795" x2="795" y1="180" y2="250"/>
      <text fill="var(--dc3-rose-stroke)" font-size="9.5" text-anchor="middle" x="795" y="268">{{ s.t6 }}</text>
      <text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="795" y="284">{{ s.c5 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
