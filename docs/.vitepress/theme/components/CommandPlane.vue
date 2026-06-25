<!--
  Copyright 2016-present the IoT DC3 original author or authors.
  Licensed under the GNU Affero General Public License v3.0.

  命令平面：一条读写命令从客户端下发到现场设备、再把结果回执到数据中心的链路。
  纯内联 SVG，颜色走 .dc3-diagram CSS 变量随明暗主题切换，文案由 lang prop 切中英。
-->
<script setup lang="ts">
import {computed} from 'vue'

const props = withDefaults(defineProps<{ lang?: 'zh' | 'en' }>(), {lang: 'zh'})

const DICT = {
  zh: {
    aria: '命令平面链路：客户端到现场设备的命令下发与回执',
    client: '客户端', clientSub: 'Web · CLI · AI Agent',
    gw: 'dc3-gateway', gwSub: 'API 网关 · 注入租户/principal',
    data: 'dc3-center-data', dataSub1: 'PointCommandServiceImpl', dataSub2: '校验 · 落库 PENDING → SENT',
    bus: 'RabbitMQ', busSub1: 'exchange dc3.e.point_command', busSub2: 'result 经 dc3.e.point_command_result',
    drv: 'dc3-driver-*', drvSub1: 'PointCommandReceiver', drvSub2: 'expireAt 预检 · 去重 · 加锁',
    dev: '现场设备', devSub: 'PLC · 传感器 · 电表',
    down: '▼ 命令下发', up: '▲ 结果回执',
    note1: 'PointCommandDTO.expireAt 默认 now + 10s',
    note2: '写失败 responseValue = null（不回显值）',
    note3: '类型 READ(0) / READ_BATCH(1) / WRITE(2) / WRITE_BATCH(3) / CONFIG(4)'
  },
  en: {
    aria: 'Command plane: command dispatch and receipt from client to field device',
    client: 'Client', clientSub: 'Web · CLI · AI Agent',
    gw: 'dc3-gateway', gwSub: 'API Gateway · inject tenant/principal',
    data: 'dc3-center-data', dataSub1: 'PointCommandServiceImpl', dataSub2: 'validate · persist PENDING → SENT',
    bus: 'RabbitMQ', busSub1: 'exchange dc3.e.point_command', busSub2: 'result via dc3.e.point_command_result',
    drv: 'dc3-driver-*', drvSub1: 'PointCommandReceiver', drvSub2: 'expireAt precheck · dedup · lock',
    dev: 'Field Device', devSub: 'PLC · Sensor · Meter',
    down: '▼ command dispatch', up: '▲ result receipt',
    note1: 'PointCommandDTO.expireAt defaults to now + 10s',
    note2: 'on write failure responseValue = null (no echo)',
    note3: 'type READ(0) / READ_BATCH(1) / WRITE(2) / WRITE_BATCH(3) / CONFIG(4)'
  }
} as const

const s = computed(() => DICT[props.lang] ?? DICT.zh)
</script>

<template>
  <div class="dc3-diagram">
    <svg viewBox="0 0 1180 320" role="img" :aria-label="s.aria">
      <defs>
        <marker id="cp-ah" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
          <polygon points="0 0, 10 3.5, 0 7" fill="var(--dc3-arrow)"/>
        </marker>
      </defs>

      <!-- direction legend (top) -->
      <line x1="476" y1="42" x2="512" y2="42" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#cp-ah)"/>
      <text x="468" y="46" fill="var(--dc3-arrow-label)" font-size="11" text-anchor="end">{{ s.down }}</text>
      <line x1="640" y1="42" x2="604" y2="42" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#cp-ah)"/>
      <text x="648" y="46" fill="var(--dc3-arrow-label)" font-size="11" text-anchor="start">{{ s.up }}</text>

      <!-- arrows: each gap has ▼ command dispatch (top, rightward) + ▲ result receipt (bottom, leftward) -->
      <!-- client <-> gateway -->
      <line x1="178" y1="124" x2="216" y2="124" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#cp-ah)"/>
      <line x1="216" y1="152" x2="178" y2="152" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#cp-ah)"/>
      <!-- gateway <-> center-data -->
      <line x1="374" y1="124" x2="412" y2="124" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#cp-ah)"/>
      <line x1="412" y1="152" x2="374" y2="152" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#cp-ah)"/>
      <!-- center-data <-> rabbitmq -->
      <line x1="582" y1="124" x2="620" y2="124" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#cp-ah)"/>
      <line x1="620" y1="152" x2="582" y2="152" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#cp-ah)"/>
      <!-- rabbitmq <-> driver -->
      <line x1="790" y1="124" x2="828" y2="124" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#cp-ah)"/>
      <line x1="828" y1="152" x2="790" y2="152" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#cp-ah)"/>
      <!-- driver <-> device -->
      <line x1="986" y1="124" x2="1024" y2="124" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#cp-ah)"/>
      <line x1="1024" y1="152" x2="986" y2="152" stroke="var(--dc3-arrow)" stroke-width="1.5" marker-end="url(#cp-ah)"/>

      <!-- client -->
      <rect x="20" y="98" width="158" height="80" rx="9" fill="var(--dc3-fe-fill)" stroke="var(--dc3-fe-stroke)" stroke-width="1.5"/>
      <text x="99" y="133" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.client }}</text>
      <text x="99" y="152" fill="var(--dc3-text2)" font-size="10" text-anchor="middle">{{ s.clientSub }}</text>

      <!-- gateway -->
      <rect x="216" y="98" width="158" height="80" rx="9" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="295" y="133" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.gw }}</text>
      <text x="295" y="152" fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle">{{ s.gwSub }}</text>

      <!-- center-data -->
      <rect x="412" y="90" width="170" height="96" rx="9" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="497" y="120" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.data }}</text>
      <text x="497" y="140" fill="var(--dc3-be-text)" font-size="9.5" text-anchor="middle">{{ s.dataSub1 }}</text>
      <text x="497" y="157" fill="var(--dc3-text2)" font-size="9" text-anchor="middle">{{ s.dataSub2 }}</text>

      <!-- rabbitmq -->
      <rect x="620" y="90" width="170" height="96" rx="9" fill="var(--dc3-bus-fill)" stroke="var(--dc3-bus-stroke)" stroke-width="1.5"/>
      <text x="705" y="120" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.bus }}</text>
      <text x="705" y="141" fill="var(--dc3-bus-text)" font-size="9" text-anchor="middle">{{ s.busSub1 }}</text>
      <text x="705" y="157" fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle">{{ s.busSub2 }}</text>

      <!-- driver -->
      <rect x="828" y="90" width="158" height="96" rx="9" fill="var(--dc3-be-fill)" stroke="var(--dc3-be-stroke)" stroke-width="1.5"/>
      <text x="907" y="120" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.drv }}</text>
      <text x="907" y="140" fill="var(--dc3-be-text)" font-size="9" text-anchor="middle">{{ s.drvSub1 }}</text>
      <text x="907" y="157" fill="var(--dc3-text2)" font-size="8.5" text-anchor="middle">{{ s.drvSub2 }}</text>

      <!-- field device -->
      <rect x="1024" y="98" width="136" height="80" rx="9" fill="var(--dc3-ext-fill)" stroke="var(--dc3-ext-stroke)" stroke-width="1.5"/>
      <text x="1092" y="133" class="d-name" fill="var(--dc3-box-name)" font-size="13" text-anchor="middle">{{ s.dev }}</text>
      <text x="1092" y="152" fill="var(--dc3-text2)" font-size="10" text-anchor="middle">{{ s.devSub }}</text>

      <!-- key facts (据 command-plane.md 核对) -->
      <text x="590" y="240" fill="var(--dc3-text2)" font-size="10" text-anchor="middle">{{ s.note1 }}</text>
      <text x="590" y="262" fill="var(--dc3-text2)" font-size="10" text-anchor="middle">{{ s.note2 }}</text>
      <text x="590" y="290" fill="var(--dc3-text2)" font-size="9.5" text-anchor="middle">{{ s.note3 }}</text>
    </svg>
  </div>
</template>
