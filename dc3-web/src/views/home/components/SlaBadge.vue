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
      <Warning/>
    </el-icon>
    <button v-if="backlog.over24h > 0" class="sla-badge__chip sla-badge__chip--sla" type="button" @click="jumpTo('sla')">
      <span class="sla-badge__value">{{ backlog.over24h }}</span>
      <span class="sla-badge__label">{{ $t('home.sla.unackOver24h') }}</span>
    </button>
    <button v-if="silentCount > 0" class="sla-badge__chip sla-badge__chip--avail" type="button" @click="jumpTo('availability')">
      <span class="sla-badge__value">{{ silentCount }}</span>
      <span class="sla-badge__label">{{ $t('home.sla.silentDevices') }}</span>
    </button>
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
  run(
    async () => {
      const [agingBacklog, silentSourceRows]: [AgingBacklog, SilentSource[]] = await Promise.all([
        alertAging(),
        silentSources(7, 15, 200),
      ]);
      return {agingBacklog, silentCount: (silentSourceRows ?? []).length};
    },
    {
      apply: (result) => {
        Object.assign(
          backlog,
          result.agingBacklog ?? {under1h: 0, h1to6: 0, h6to24: 0, over24h: 0, total: 0}
        );
        silentCount.value = result.silentCount;
      },
    }
  );

const jumpTo = (tab: 'sla' | 'availability') => {
  router.push({name: 'settingsAlarmOverview', query: {tab}}).catch(() => {
  });
};

onMounted(load);
defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
.sla-badge {
  display: flex;
  align-items: center;
  gap: var(--dc3-space-2);
  padding: var(--dc3-space-1) var(--dc3-space-4);
  border-radius: var(--dc3-radius-md);
  background: var(--el-color-warning-light-9);
  border: 1px solid var(--el-color-warning-light-7);
  color: var(--el-color-warning);
  font-size: 13px;

  &--warn {
    background: var(--el-color-danger-light-9);
    border-color: var(--el-color-danger-light-7);
    color: var(--el-color-danger);
  }

  .sla-badge__icon {
    font-size: 18px;
  }

  .sla-badge__chip {
    position: relative;
    isolation: isolate;
    appearance: none;
    display: inline-flex;
    align-items: center;
    gap: var(--dc3-space-1);
    min-height: var(--dc3-space-8);
    padding: 0 var(--dc3-space-2);
    border: 0;
    border-radius: var(--dc3-radius-full);
    background: transparent;
    font: inherit;
    line-height: 1.2;
    white-space: nowrap;
    cursor: pointer;

    &::before {
      position: absolute;
      z-index: 0;
      inset: 2px 0;
      border-radius: inherit;
      background: var(--dc3-bg-elevated);
      content: '';
      pointer-events: none;
      transition: background-color var(--dc3-duration-fast) var(--dc3-ease-standard);
    }

    > * {
      position: relative;
      z-index: 1;
    }

    &:hover::before,
    &:focus-visible::before {
      background: var(--dc3-bg-elevated-strong);
    }

    &:focus-visible {
      outline: none;
      box-shadow: var(--dc3-focus-ring);
    }
  }

  .sla-badge__chip--sla {
    color: var(--el-color-danger);
  }

  .sla-badge__chip--avail {
    color: var(--el-color-warning);
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
