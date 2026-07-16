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
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1180 300">
        <defs>
          <marker id="af-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0, 10 3.5, 0 7"/>
          </marker>
          <marker id="af-ah-rose" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-rose-stroke)" points="0 0, 10 3.5, 0 7"/>
          </marker>
        </defs>

        <!-- ① client -> auth center: get salt -->
        <line marker-end="url(#af-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="190" x2="248" y1="142"
              y2="142"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="219" y="134">{{ s.e1 }}</text>
        <!-- ② auth center -> client with token: generate token -->
        <line marker-end="url(#af-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="412" x2="470" y1="158"
              y2="158"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="441" y="176">{{ s.e2 }}</text>

        <!-- ③ client with token -> gateway: call /api with auth headers -->
        <line marker-end="url(#af-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="640" x2="698" y1="150"
              y2="150"/>
        <text fill="var(--dc3-arrow-label)" font-size="10" text-anchor="middle" x="669" y="120">{{ s.e3 }}</text>

        <!-- ④ gateway -> center services: verified (rose emphasis) -->
        <line marker-end="url(#af-ah-rose)" stroke="var(--dc3-rose-stroke)" stroke-dasharray="5,4" stroke-width="1.5"
              x1="902" x2="960"
              y1="150" y2="150"/>
        <text fill="var(--dc3-rose-stroke)" font-size="10" text-anchor="middle" x="931" y="134">{{ s.e4 }}</text>

        <!-- client (fe / cyan) -->
        <rect fill="var(--dc3-fe-fill)" height="68" rx="9" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="162"
              x="28"
              y="116"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="109" y="146">{{
          s.cli
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="10" text-anchor="middle" x="109" y="165">{{ s.cliSub }}</text>

        <!-- dc3-center-auth (be / green) -->
        <rect fill="var(--dc3-be-fill)" height="68" rx="9" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="162"
              x="250"
              y="116"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="331" y="146">{{
          s.auth
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="331" y="165">{{ s.authSub }}</text>

        <!-- client with token (fe / cyan) -->
        <rect fill="var(--dc3-fe-fill)" height="68" rx="9" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="168"
              x="472"
              y="116"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="12.5" text-anchor="middle" x="556" y="146">{{
          s.token
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9" text-anchor="middle" x="556" y="165">{{ s.tokenSub }}</text>

        <!-- dc3-gateway (be / green) -->
        <rect fill="var(--dc3-be-fill)" height="68" rx="9" stroke="var(--dc3-be-stroke)" stroke-width="1.5" width="202"
              x="700"
              y="116"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="801" y="146">{{
          s.gw
          }}
        </text>
        <text fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle" x="801" y="165">{{ s.gwSub }}</text>

        <!-- center services (db / violet) -->
        <rect fill="var(--dc3-db-fill)" height="68" rx="9" stroke="var(--dc3-db-stroke)" stroke-width="1.5" width="190"
              x="962"
              y="116"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle" x="1057" y="146">{{
          s.svc
          }}
        </text>
        <text fill="var(--dc3-db-text)" font-size="9.5" text-anchor="middle" x="1057" y="165">{{ s.svcSub }}</text>

        <!-- fact footnotes -->
        <text fill="var(--dc3-text2)" font-size="10" x="28" y="232">• {{ s.f1 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" x="28" y="250">• {{ s.f2 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" x="600" y="232">• {{ s.f3 }}</text>
        <text fill="var(--dc3-text2)" font-size="10" x="600" y="250">• {{ s.f4 }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
