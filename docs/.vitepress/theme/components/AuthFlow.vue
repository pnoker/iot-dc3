<!--
  Copyright 2016-present the IoT DC3 original author or authors.
  Licensed under the GNU Affero General Public License v3.0.

  鉴权流：登录取令牌 + 带令牌鉴权请求的完整链路。
  纯内联 SVG，颜色走 .dc3-diagram CSS 变量随明暗主题切换，文案由 lang prop 切中英。
-->
<script setup lang="ts">
import {computed} from 'vue'

const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})

const DICT = {
  zh: {
    aria: '鉴权链路：登录取令牌与带令牌鉴权请求',
    cli: '客户端', cliSub: 'Web · CLI · AI Agent',
    auth: 'dc3-center-auth', authSub: '鉴权中心 · 签发 JWT',
    token: '客户端持令牌', tokenSub: 'JWT 绑定 principal_id + tenant_id',
    gw: 'dc3-gateway', gwSub: 'JWT 校验 · 解析 principal',
    svc: '中心服务', svcSub: 'RBAC 鉴权 + 租户隔离',
    e1: '① 取盐 POST /token/salt',
    e2: '② 取令牌 POST /token/generate',
    e3: '③ 调 /api 带头 X-Auth-Tenant / X-Auth-Login / X-Auth-Token',
    e4: '④ 校验通过',
    f1: '盐 5 分钟有效',
    f2: '令牌 12 小时有效',
    f3: 'HMAC 签名透传 X-Auth-Principal + X-Auth-Sign',
    f4: 'pre/pro 密钥为空或默认值时 fail-fast'
  },
  en: {
    aria: 'Auth flow: login to token and token-bearing requests',
    cli: 'Client', cliSub: 'Web · CLI · AI Agent',
    auth: 'dc3-center-auth', authSub: 'Auth Center · issues JWT',
    token: 'Client with token', tokenSub: 'JWT bound to principal_id + tenant_id',
    gw: 'dc3-gateway', gwSub: 'JWT verify · resolve principal',
    svc: 'Center Services', svcSub: 'RBAC + tenant isolation',
    e1: '① get salt POST /token/salt',
    e2: '② get token POST /token/generate',
    e3: '③ call /api with X-Auth-Tenant / X-Auth-Login / X-Auth-Token',
    e4: '④ verified',
    f1: 'salt valid 5 min',
    f2: 'token valid 12 h',
    f3: 'HMAC pass-through X-Auth-Principal + X-Auth-Sign',
    f4: 'pre/pro fail-fast when secret empty or default'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <div class="dc3-diagram">
    <svg viewBox="0 0 1180 300" role="img" :aria-label="s.aria">
      <defs>
        <marker id="af-ah" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="var(--dc3-arrow)"/>
        </marker>
        <marker id="af-ah-rose" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="var(--dc3-rose-stroke)"/>
        </marker>
      </defs>

      <!-- ① client -> auth center: get salt -->
      <line x1="190" y1="142" x2="248" y2="142" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#af-ah)"/>
      <text x="219" y="134" fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle">{{ s.e1 }}</text>
      <!-- ② auth center -> client with token: generate token -->
      <line x1="412" y1="158" x2="470" y2="158" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#af-ah)"/>
      <text x="441" y="176" fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle">{{ s.e2 }}</text>

      <!-- ③ client with token -> gateway: call /api with auth headers -->
      <line x1="640" y1="150" x2="698" y2="150" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#af-ah)"/>
      <text x="669" y="120" fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle">{{ s.e3 }}</text>

      <!-- ④ gateway -> center services: verified (rose emphasis) -->
      <line x1="902" y1="150" x2="960" y2="150" stroke="var(--dc3-rose-stroke)" stroke-width="1.5" stroke-dasharray="5,4" marker-end="url(#af-ah-rose)"/>
      <text x="931" y="134" fill="var(--dc3-rose-stroke)" font-size="10" text-anchor="middle">{{ s.e4 }}</text>

      <!-- client (fe / cyan) -->
      <rect x="28" y="116" width="162" height="68" rx="9" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text x="109" y="146" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.cli }}</text>
      <text x="109" y="165" fill="var(--dc3-text2)" font-size="10" text-anchor="middle">{{ s.cliSub }}</text>

      <!-- dc3-center-auth (be / green) -->
      <rect x="250" y="116" width="162" height="68" rx="9" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="331" y="146" class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle">{{ s.auth }}</text>
      <text x="331" y="165" fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle">{{ s.authSub }}</text>

      <!-- client with token (fe / cyan) -->
      <rect x="472" y="116" width="168" height="68" rx="9" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text x="556" y="146" class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle">{{ s.token }}</text>
      <text x="556" y="165" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.tokenSub }}</text>

      <!-- dc3-gateway (be / green) -->
      <rect x="700" y="116" width="202" height="68" rx="9" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="801" y="146" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.gw }}</text>
      <text x="801" y="165" fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle">{{ s.gwSub }}</text>

      <!-- center services (db / violet) -->
      <rect x="962" y="116" width="190" height="68" rx="9" fill="var(--dc3-db-fill)" stroke="var(--dc3-db-stroke)" stroke-width="1.5"/>
      <text x="1057" y="146" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.svc }}</text>
      <text x="1057" y="165" fill="var(--dc3-db-text)" font-size="9.5" text-anchor="middle">{{ s.svcSub }}</text>

      <!-- fact footnotes -->
      <text x="28" y="232" fill="var(--dc3-text2)" font-size="10">• {{ s.f1 }}</text>
      <text x="28" y="250" fill="var(--dc3-text2)" font-size="10">• {{ s.f2 }}</text>
      <text x="600" y="232" fill="var(--dc3-text2)" font-size="10">• {{ s.f3 }}</text>
      <text x="600" y="250" fill="var(--dc3-text2)" font-size="10">• {{ s.f4 }}</text>
    </svg>
  </div>
</template>
