<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'
const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {aria:'facade 三态', caller:'业务代码', callerSub:'注入 DeviceFacade 接口', iface:'DeviceFacade', ifaceSub:'协议中立契约 (facade-api)',
    distT:'grpc 模式 · 分布式多进程', grpc:'DeviceGrpcFacade', grpcSub:'facade-grpc', mgr:'管理中心 dc3-center-manager', svc1:'DeviceService',
    monoT:'local 模式 · 进程内单体', local:'DeviceLocalFacade', localSub:'facade-local-manager', svc2:'DeviceService (同进程)',
    e1:'dc3.facade.mode=grpc（默认）', e2:'dc3.facade.mode=local', e3:'跨进程 gRPC :9400', e4:'进程内调用（无网络）'},
  en: {aria:'facade three modes', caller:'Business code', callerSub:'injects DeviceFacade', iface:'DeviceFacade', ifaceSub:'protocol-neutral contract (facade-api)',
    distT:'grpc mode · distributed multi-process', grpc:'DeviceGrpcFacade', grpcSub:'facade-grpc', mgr:'Manager dc3-center-manager', svc1:'DeviceService',
    monoT:'local mode · in-process monolith', local:'DeviceLocalFacade', localSub:'facade-local-manager', svc2:'DeviceService (same process)',
    e1:'dc3.facade.mode=grpc (default)', e2:'dc3.facade.mode=local', e3:'cross-process gRPC :9400', e4:'in-process call (no network)'}
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame><div class="dc3-diagram">
    <svg :aria-label="s.aria" role="img" viewBox="0 0 1200 440">
      <defs><marker id="fm-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5"><polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/></marker></defs>
      <rect fill="var(--dc3-be-fill)" height="160" opacity="0.22" rx="12" width="670" x="500" y="60" stroke="var(--dc3-be-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-be-text)" font-size="11" font-weight="600" x="516" y="82">{{ s.distT }}</text>
      <rect fill="var(--dc3-amber-fill)" height="160" opacity="0.25" rx="12" width="670" x="500" y="250" stroke="var(--dc3-amber-stroke)" stroke-width="1" stroke-dasharray="6,4"/>
      <text fill="var(--dc3-amber-stroke)" font-size="11" font-weight="600" x="516" y="272">{{ s.monoT }}</text>
      <line marker-end="url(#fm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="200" x2="250" y1="225" y2="225"/>
      <path d="M450,215 L520,120" fill="none" marker-end="url(#fm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="500" y="160">{{ s.e1 }}</text>
      <path d="M450,245 L520,310" fill="none" marker-end="url(#fm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" stroke-dasharray="5,4"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="500" y="290">{{ s.e2 }}</text>
      <line marker-end="url(#fm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="700" x2="740" y1="120" y2="120"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="720" y="112">{{ s.e3 }}</text>
      <line marker-end="url(#fm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="960" x2="1000" y1="120" y2="120"/>
      <line marker-end="url(#fm-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="720" x2="760" y1="310" y2="310"/><text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="740" y="302">{{ s.e4 }}</text>
      <rect x="30" y="195" width="170" height="60" rx="8" fill="var(--vp-c-bg)"/><rect x="30" y="195" width="170" height="60" rx="8" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="12" text-anchor="middle" x="115" y="220">{{ s.caller }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="115" y="238">{{ s.callerSub }}</text>
      <rect x="250" y="190" width="200" height="70" rx="8" fill="var(--vp-c-bg)"/><rect x="250" y="190" width="200" height="70" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="2"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="13" font-weight="700" text-anchor="middle" x="350" y="218">{{ s.iface }}</text><text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="350" y="240">{{ s.ifaceSub }}</text>
      <rect x="520" y="92" width="180" height="56" rx="8" fill="var(--vp-c-bg)"/><rect x="520" y="92" width="180" height="56" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="610" y="116">{{ s.grpc }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="610" y="134">{{ s.grpcSub }}</text>
      <rect x="740" y="92" width="220" height="56" rx="8" fill="var(--vp-c-bg)"/><rect x="740" y="92" width="220" height="56" rx="8" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="850" y="125">{{ s.mgr }}</text>
      <rect x="1000" y="92" width="150" height="56" rx="8" fill="var(--vp-c-bg)"/><rect x="1000" y="92" width="150" height="56" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="1075" y="125">{{ s.svc1 }}</text>
      <rect x="520" y="282" width="200" height="56" rx="8" fill="var(--vp-c-bg)"/><rect x="520" y="282" width="200" height="56" rx="8" fill="var(--dc3-amber-fill)" stroke="var(--dc3-amber-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11.5" text-anchor="middle" x="620" y="306">{{ s.local }}</text><text fill="var(--dc3-text2)" font-size="8" text-anchor="middle" x="620" y="324">{{ s.localSub }}</text>
      <rect x="760" y="282" width="190" height="56" rx="8" fill="var(--vp-c-bg)"/><rect x="760" y="282" width="190" height="56" rx="8" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text class="d-name" fill="var(--dc3-box-name)" font-size="11" text-anchor="middle" x="855" y="315">{{ s.svc2 }}</text>
    </svg>
  </div></DiagramFrame>
</template>
