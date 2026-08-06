<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  - Licensed under AGPL-3.0. See LICENSE.
  -->
<script lang="ts" setup>
import {computed} from 'vue'
import DiagramFrame from './DiagramFrame.vue'

const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})
const DICT = {
  zh: {
    aria: 'CLI 模块结构',
    entry: 'dc3 入口 index.ts',
    entryM: '+parseAsync(argv)',
    commands: '14 个命令模块',
    commandsM: 'config / auth / device / driver / point / profile / ...',
    client: 'HTTP 客户端 client.ts',
    clientM: '+post(path,body) / +get(path)',
    config: '配置管理 config-manager.ts',
    configM: 'gateway / tenant / profile',
    token: 'token 管理 token-manager.ts',
    tokenM: '+ensureToken() · salt+JWT 续期',
    credential: '凭据存储 credential-store.ts',
    credentialM: 'keychain / encrypted / env / prompt',
    e1: '路由子命令',
    e2: '发起网关请求',
    e3: '取/续 token',
    e4: '读 profile',
    e5: '取密码'
  },
  en: {
    aria: 'CLI module structure',
    entry: 'dc3 entry index.ts',
    entryM: '+parseAsync(argv)',
    commands: '14 command modules',
    commandsM: 'config / auth / device / driver / point / profile / ...',
    client: 'HTTP client client.ts',
    clientM: '+post(path,body) / +get(path)',
    config: 'Config manager',
    configM: 'gateway / tenant / profile',
    token: 'Token manager',
    tokenM: '+ensureToken() · salt+JWT renew',
    credential: 'Credential store',
    credentialM: 'keychain / encrypted / env / prompt',
    e1: 'route subcommand',
    e2: 'gateway request',
    e3: 'get/renew token',
    e4: 'read profile',
    e5: 'get password'
  }
} as const
const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>
<template>
  <DiagramFrame>
    <div class="dc3-diagram">
      <svg :aria-label="s.aria" role="img" viewBox="0 0 1280 320">
        <defs>
          <marker id="clc-ah" markerHeight="7" markerWidth="10" orient="auto" refX="9" refY="3.5">
            <polygon fill="var(--dc3-arrow)" points="0 0,10 3.5,0 7"/>
          </marker>
        </defs>
        <line marker-end="url(#clc-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="240" x2="280" y1="130"
              y2="130"/>
        <text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="260" y="122">{{ s.e1 }}</text>
        <line marker-end="url(#clc-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="520" x2="560" y1="130"
              y2="130"/>
        <text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="540" y="122">{{ s.e2 }}</text>
        <line marker-end="url(#clc-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="800" x2="840" y1="130"
              y2="130"/>
        <text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="820" y="122">{{ s.e3 }}</text>
        <line marker-end="url(#clc-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1000" x2="1040" y1="100"
              y2="100"/>
        <text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="1020" y="92">{{ s.e4 }}</text>
        <line marker-end="url(#clc-ah)" stroke="var(--dc3-arrow)" stroke-width="1.5" x1="1000" x2="1040" y1="170"
              y2="170"/>
        <text fill="var(--dc3-arrow-label)" font-size="9" text-anchor="middle" x="1020" y="162">{{ s.e5 }}</text>
        <rect v-for="(c,i) in [['entry','entryM'],['commands','commandsM'],['client','clientM'],['token','tokenM']]"
              :key="i" :stroke="['var(--dc3-amber-stroke)','var(--dc3-fe-stroke)','var(--dc3-be-stroke)','var(--dc3-bus-stroke)'][i]" :x="20+i*260" fill="var(--vp-c-bg)" height="110" rx="8" stroke-width="1.5"
              width="220"
              y="75"/>
        <rect v-for="i in [0,1,2,3]" :key="'h'+i" :fill="['var(--dc3-amber-fill)','var(--dc3-fe-fill)','var(--dc3-be-fill)','var(--dc3-bus-fill)'][i]" :stroke="['var(--dc3-amber-stroke)','var(--dc3-fe-stroke)','var(--dc3-be-stroke)','var(--dc3-bus-stroke)'][i]" :x="20+i*260" height="36" rx="8"
              stroke-width="1.5"
              width="220"
              y="75"/>
        <text :x="130" class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle"
              y="93">{{ s.entry }}
        </text>
        <text :x="130" fill="var(--dc3-text2)" font-size="9" text-anchor="middle" y="130">{{ s.entryM }}</text>
        <text :x="390" class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle"
              y="93">{{ s.commands }}
        </text>
        <text :x="390" fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" y="130">{{ s.commandsM }}</text>
        <text :x="650" class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle"
              y="93">{{ s.client }}
        </text>
        <text :x="650" fill="var(--dc3-text2)" font-size="9" text-anchor="middle" y="130">{{ s.clientM }}</text>
        <text :x="910" class="d-name" fill="var(--dc3-box-name)" font-size="11" font-weight="700" text-anchor="middle"
              y="93">{{ s.token }}
        </text>
        <text :x="910" fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" y="130">{{ s.tokenM }}</text>
        <rect fill="var(--vp-c-bg)" height="60" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="220" x="1040"
              y="75"/>
        <rect fill="var(--dc3-fe-fill)" height="26" rx="8" stroke="var(--dc3-fe-stroke)" stroke-width="1.5" width="220" x="1040"
              y="75"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle" x="1150"
              y="93">{{ s.config }}
        </text>
        <text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="1150" y="118">{{ s.configM }}</text>
        <rect fill="var(--vp-c-bg)" height="60" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="220" x="1040"
              y="145"/>
        <rect fill="var(--dc3-amber-fill)" height="26" rx="8" stroke="var(--dc3-amber-stroke)" stroke-width="1.5" width="220"
              x="1040" y="145"/>
        <text class="d-name" fill="var(--dc3-box-name)" font-size="10.5" font-weight="700" text-anchor="middle" x="1150"
              y="163">{{ s.credential }}
        </text>
        <text fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle" x="1150" y="188">{{ s.credentialM }}</text>
      </svg>
    </div>
  </DiagramFrame>
</template>
