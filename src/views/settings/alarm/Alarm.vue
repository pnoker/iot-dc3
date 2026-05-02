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
  <div>
    <blank-card class="settings-tool">
      <el-form :inline="true" :model="query" class="alarm-tool">
        <el-form-item :label="t('settings.event.source')">
          <el-select
            v-model="query.source"
            clearable
            size="small"
            style="width: 140px"
            :placeholder="t('settings.event.all')"
          >
            <el-option :label="t('settings.event.sourceDevice')" value="device" />
            <el-option :label="t('settings.event.sourceDriver')" value="driver" />
          </el-select>
        </el-form-item>
        <el-form-item :label="t('settings.event.confirmFlag')">
          <el-select
            v-model="query.confirmFlag"
            clearable
            size="small"
            style="width: 140px"
            :placeholder="t('settings.event.all')"
          >
            <el-option :label="t('settings.alarm.unconfirmed')" :value="0" />
            <el-option :label="t('settings.alarm.confirmed')" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="small" @click="load">{{ t('common.search') }}</el-button>
          <el-button size="small" @click="reset">{{ t('common.reset') }}</el-button>
          <el-button :icon="Refresh" circle size="small" :loading="loading" @click="load" />
        </el-form-item>
      </el-form>
    </blank-card>

    <blank-card>
      <el-table v-loading="loading" :data="rows" stripe class="settings-table">
        <el-table-column :label="t('settings.event.source')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.source === 'device' ? 'primary' : 'info'" size="small">
              {{ row.source === 'device' ? t('settings.event.sourceDevice') : t('settings.event.sourceDriver') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.event.sourceId')" prop="sourceId" width="140" />
        <el-table-column :label="t('settings.event.pointId')" prop="pointId" width="140" />
        <el-table-column :label="t('settings.event.message')" prop="message" show-overflow-tooltip min-width="240" />
        <el-table-column :label="t('settings.event.confirmFlag')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.confirmFlag === 1 ? 'success' : 'danger'" size="small">
              {{ row.confirmFlag === 1 ? t('settings.alarm.confirmed') : t('settings.alarm.unconfirmed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.event.createTime')" prop="createTime" width="180" />
        <el-table-column :label="t('common.operation')" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.confirmFlag !== 1" link type="primary" @click="confirmRow(row)">
              {{ t('settings.alarm.confirm') }}
            </el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.alarm.empty')" />
        </template>
      </el-table>
      <el-pagination
        class="settings-pagination"
        :current-page="page.current"
        :page-size="page.size"
        :total="page.total"
        :page-sizes="[10, 20, 50, 100]"
        background
        layout="total, sizes, prev, pager, next"
        @current-change="onCurrent"
        @size-change="onSize"
      />
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { Refresh } from '@element-plus/icons-vue';

  import { alertConfirm, alertPage } from '@/api/dashboard';
  import { successMessage } from '@/utils/NotificationUtil';
  import BlankCard from '@/components/card/blank/BlankCard.vue';

  interface Row {
    id: number | string;
    source: 'device' | 'driver';
    sourceId: number | string;
    pointId: number | string;
    eventTypeFlag: number;
    confirmFlag: number;
    createTime: string;
    message: string;
  }

  const { t } = useI18n();

  const loading = ref(false);
  const rows = ref<Row[]>([]);
  // Alarm page pins eventTypeFlag=1 on every request; users only pick
  // source / confirm state.
  const query = reactive<{ source: string; confirmFlag: number | null }>({
    source: '',
    confirmFlag: null,
  });
  const page = reactive({ current: 1, size: 20, total: 0 });

  const load = async () => {
    loading.value = true;
    try {
      const res: any = await alertPage({
        source: (query.source || null) as any,
        eventTypeFlag: 1,
        confirmFlag: query.confirmFlag,
        current: page.current,
        size: page.size,
      });
      const data = res?.data ?? {};
      rows.value = data.records ?? [];
      page.total = Number(data.total ?? 0);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  const reset = () => {
    query.source = '';
    query.confirmFlag = null;
    page.current = 1;
    load();
  };

  const onCurrent = (v: number) => {
    page.current = v;
    load();
  };
  const onSize = (v: number) => {
    page.size = v;
    page.current = 1;
    load();
  };

  const confirmRow = async (row: Row) => {
    try {
      await alertConfirm(row.source, row.id);
      successMessage();
      load();
    } catch {
      // handled globally
    }
  };

  load();
</script>

<style lang="scss" scoped>
  .settings-tool {
    margin-bottom: 12px;

    :deep(.el-card__body) {
      padding: 12px 16px 0;
    }
  }

  .alarm-tool {
    :deep(.el-form-item) {
      margin-bottom: 12px;
    }
  }

  .settings-table {
    margin-top: 1px;
    border-radius: 4px;
  }

  .settings-pagination {
    margin-top: 12px;
    justify-content: flex-end;
  }
</style>
