<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<!--
  Home SLA strip — a thin always-visible summary of the two SLA signals
  that operators should never have to click through to discover:
    - how many alarms have been sitting unconfirmed for more than 24h
    - how many (device, point) pairs have gone silent in the last 15m
  When both are zero the strip hides entirely so the home page doesn't
  carry dead visual weight. Each chip is a deeplink into the
  corresponding event-overview tab.
-->
<template>
  <div v-if="visible" :class="{'sla-badge--warn': warn}" class="sla-badge">
    <el-icon class="sla-badge__icon">
      <Warning />
    </el-icon>
    <div v-if="backlog.over24h > 0" class="sla-badge__chip sla-badge__chip--sla" @click="jumpTo('sla')">
      <span class="sla-badge__value">{{ backlog.over24h }}</span>
      <span class="sla-badge__label">{{ $t('home.sla.unackOver24h') }}</span>
    </div>
    <div v-if="silentCount > 0" class="sla-badge__chip sla-badge__chip--avail" @click="jumpTo('availability')">
      <span class="sla-badge__value">{{ silentCount }}</span>
      <span class="sla-badge__label">{{ $t('home.sla.silentDevices') }}</span>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import {computed, onMounted, reactive, ref} from 'vue';
  import {useRouter} from 'vue-router';
  import {Warning} from '@element-plus/icons-vue';

  import {alertAging, silentSources} from '@/api/dashboard';
  import type {AgingBacklog, SilentSource} from '@/config/types/dashboard';
  import {useAsyncLoader} from '@/utils/asyncLoaderUtil';

  const router = useRouter();
  const {run} = useAsyncLoader();

  const backlog = reactive<AgingBacklog>({under1h: 0, h1to6: 0, h6to24: 0, over24h: 0, total: 0});
  const silentCount = ref(0);

  const visible = computed(() => backlog.over24h > 0 || silentCount.value > 0);
  const warn = computed(() => backlog.over24h > 0);

  const load = () =>
    run(async () => {
      const [a, s]: [{data?: AgingBacklog}, {data?: SilentSource[]}] = await Promise.all([
        alertAging(),
        silentSources(7, 15, 200),
      ]);
      Object.assign(backlog, a?.data ?? {under1h: 0, h1to6: 0, h6to24: 0, over24h: 0, total: 0});
      silentCount.value = (s?.data ?? []).length;
    });

  const jumpTo = (tab: 'sla' | 'availability') => {
    router.push({name: 'settingsAlarmOverview', query: {tab}}).catch(() => {});
  };

  onMounted(load);
  defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
  .sla-badge {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 8px 14px;
    border-radius: 6px;
    background: #fdf6ec;
    border: 1px solid #faecd8;
    color: #e6a23c;
    font-size: 13px;

    &--warn {
      background: #fef0f0;
      border-color: #fde2e2;
      color: #f56c6c;
    }

    .sla-badge__icon {
      font-size: 18px;
    }

    .sla-badge__chip {
      display: inline-flex;
      align-items: baseline;
      gap: 6px;
      padding: 2px 10px;
      border-radius: 10px;
      background: rgba(255, 255, 255, 0.5);
      cursor: pointer;
      transition: background-color 0.12s ease;

      &:hover {
        background: #ffffff;
      }
    }

    .sla-badge__chip--sla {
      color: #f56c6c;
    }

    .sla-badge__chip--avail {
      color: #e6a23c;
    }

    .sla-badge__value {
      font-weight: 700;
      font-size: 16px;
    }

    .sla-badge__label {
      font-size: 12px;
      opacity: 0.9;
    }
  }
</style>
