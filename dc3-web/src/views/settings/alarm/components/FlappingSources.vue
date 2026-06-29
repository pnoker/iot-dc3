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

<template>
  <dashboard-card
    :empty="!loading && rows.length === 0"
    :empty-image-size="60"
    :empty-text="t('settings.event.overview.flappingEmpty')"
    :loading="loading"
    :subtitle="t('settings.event.overview.flappingSubtitle', {hours: windowKey, min: minCount})"
    :title="t('settings.event.overview.flappingTitle')"
    body-mode="scroll"
    class="flapping-sources"
    loading-target="button"
    @refresh="load"
  >
    <template #tools>
      <el-segmented v-model="windowKey" :options="windowOptions" size="small" />
    </template>

    <ul class="flapping-sources__list">
      <li v-for="row in rows" :key="rowKey(row)" class="flapping-sources__item" @click="onJump(row)">
        <el-tag :type="row.source === 'device' ? 'primary' : 'warning'" size="small">
          {{ row.source === 'device' ? t('settings.event.device') : t('settings.event.driver') }}
        </el-tag>
        <span class="flapping-sources__name">{{ nameBySource(row.source, row.sourceId) }}</span>
        <el-tag effect="plain" size="small" type="info">
          {{ eventTypeLabel(row.eventTypeFlag) }}
        </el-tag>
        <span class="flapping-sources__count">
          <el-icon><Warning /></el-icon>
          {{ row.count }}
        </span>
      </li>
    </ul>
  </dashboard-card>
</template>

<script lang="ts" setup>
  import {computed, onMounted, ref, watch} from 'vue';
  import {useI18n} from 'vue-i18n';
  import {useRouter} from 'vue-router';
  import {Warning} from '@element-plus/icons-vue';

  import {alertFlapping} from '@/api/dashboard';
  import type {FlappingSource} from '@/config/types/dashboard';
  import DashboardCard from '@/components/card/dashboard/DashboardCard.vue';
  import {useAsyncLoader} from '@/utils/asyncLoaderUtil';
  import {useEntityNames} from '@/composables/useEntityNames';
  import {jumpToSourceEvents} from '@/utils/jumpUtil';

  const {t} = useI18n();
  const router = useRouter();
  const {loading, run} = useAsyncLoader();
  const {resolveBySource, nameBySource} = useEntityNames();

  // Same window semantics + preset thresholds as Storm — operator learns
  // one model of "how the event page works" instead of two.
  const WINDOW_SPECS: Record<'1' | '6' | '24', {minCount: number}> = {
    '1': {minCount: 5},
    '6': {minCount: 15},
    '24': {minCount: 40},
  };
  const windowOptions = [
    {label: '1h', value: '1'},
    {label: '6h', value: '6'},
    {label: '24h', value: '24'},
  ];
  const windowKey = ref<'1' | '6' | '24'>('6');
  const minCount = computed(() => WINDOW_SPECS[windowKey.value].minCount);

  const rows = ref<FlappingSource[]>([]);

  const load = () =>
    run(async () => {
      const res: {data?: FlappingSource[]} = await alertFlapping(Number(windowKey.value), minCount.value, 30);
      rows.value = res?.data ?? [];
      await resolveBySource(rows.value);
    });

  watch(windowKey, load);
  onMounted(load);

  const eventTypeLabel = (flag: number) => {
    if (flag >= 2) return t('common.levelError');
    if (flag === 1) return t('common.levelWarn');
    return t('common.levelInfo');
  };

  const rowKey = (r: FlappingSource) => `${r.source}:${r.sourceId}:${r.eventTypeFlag}`;
  const onJump = (r: FlappingSource) => jumpToSourceEvents(router, r.source, r.sourceId);

  defineExpose({refresh: load});
</script>

<style lang="scss" scoped>
  .flapping-sources {
    .flapping-sources__list {
      list-style: none;
      margin: 0;
      padding: 0;
    }

    .flapping-sources__item {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px 16px;
      border-bottom: 1px solid var(--el-border-color-lighter);
      cursor: pointer;
      transition: background-color 0.12s ease;

      &:hover {
        background: #fafafa;
      }

      &:last-child {
        border-bottom: none;
      }
    }

    .flapping-sources__name {
      flex: 1;
      min-width: 0;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      font-size: 13px;
      color: #303133;
    }

    .flapping-sources__count {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      color: #f56c6c;
      font-weight: 600;
      font-size: 13px;
    }
  }
</style>
