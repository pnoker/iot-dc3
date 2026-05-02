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
    <tool-card
      :form-model="formData"
      :page="page"
      hide-sort
      @search="onSearch"
      @reset="onReset"
      @refresh="load"
      @size-change="sizeChange"
      @current-change="currentChange"
    >
      <template #filters="{ formData: fd }">
        <el-form-item :label="$t('settings.event.source')" prop="source">
          <el-segmented
            v-model="fd.source"
            :options="[
              { label: $t('common.all'), value: '' },
              { label: $t('settings.event.sourceDevice'), value: 'device' },
              { label: $t('settings.event.sourceDriver'), value: 'driver' },
            ]"
          />
        </el-form-item>
        <el-form-item :label="$t('settings.event.eventType')" prop="eventTypeFlag">
          <el-segmented
            v-model="fd.eventTypeFlag"
            :options="[
              { label: $t('common.all'), value: '' },
              { label: 'HEARTBEAT', value: 0 },
              { label: 'ALARM', value: 1 },
            ]"
          />
        </el-form-item>
        <el-form-item :label="$t('settings.event.confirmFlag')" prop="confirmFlag">
          <el-segmented
            v-model="fd.confirmFlag"
            :options="[
              { label: $t('common.all'), value: '' },
              { label: $t('settings.event.unconfirmed'), value: 0 },
              { label: $t('settings.event.confirmed'), value: 1 },
            ]"
          />
        </el-form-item>
      </template>
      <template v-if="selection.length > 0" #actions>
        <el-popconfirm
          :title="$t('settings.event.bulkConfirmTitle', { n: selection.length })"
          :confirm-button-text="$t('common.confirm')"
          :cancel-button-text="$t('common.cancel')"
          @confirm="bulkConfirm(true)"
        >
          <template #reference>
            <el-button type="primary" :loading="bulkRunning">
              {{ $t('settings.event.bulkConfirm', { n: selection.length }) }}
            </el-button>
          </template>
        </el-popconfirm>
        <el-popconfirm
          :title="$t('settings.event.bulkUnconfirmTitle', { n: selection.length })"
          :confirm-button-text="$t('common.confirm')"
          :cancel-button-text="$t('common.cancel')"
          @confirm="bulkConfirm(false)"
        >
          <template #reference>
            <el-button :loading="bulkRunning">
              {{ $t('settings.event.bulkUnconfirm') }}
            </el-button>
          </template>
        </el-popconfirm>
      </template>
    </tool-card>

    <blank-card>
      <el-table
        v-loading="loading"
        :data="rows"
        stripe
        class="settings-table"
        :row-key="(row: Row) => `${row.source}:${row.id}`"
        @selection-change="onSelectionChange"
      >
        <el-table-column type="selection" width="44" />
        <el-table-column :label="t('settings.event.source')" width="100">
          <template #default="{ row }">
            <el-tag :type="row.source === 'device' ? 'primary' : 'info'" size="small">
              {{ row.source === 'device' ? t('settings.event.sourceDevice') : t('settings.event.sourceDriver') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.event.sourceId')" prop="sourceId" width="140" />
        <el-table-column :label="t('settings.event.pointId')" prop="pointId" width="140" />
        <el-table-column :label="t('settings.event.eventType')" width="110">
          <template #default="{ row }">
            <span>{{ row.eventTypeFlag === 1 ? 'ALARM' : 'HEARTBEAT' }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.event.message')" prop="message" show-overflow-tooltip min-width="240" />
        <el-table-column :label="t('settings.event.confirmFlag')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.confirmFlag === 1 ? 'success' : 'warning'" size="small">
              {{ row.confirmFlag === 1 ? t('settings.event.confirmed') : t('settings.event.unconfirmed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="t('settings.event.createTime')" prop="createTime" width="180" />
        <el-table-column :label="t('common.operation')" width="140" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              v-if="row.confirmFlag !== 1"
              :title="t('settings.event.confirmTitle')"
              :confirm-button-text="t('common.confirm')"
              :cancel-button-text="t('common.cancel')"
              @confirm="confirmRow(row)"
            >
              <template #reference>
                <el-button link type="primary">{{ t('settings.event.confirm') }}</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              v-else
              :title="t('settings.event.unconfirmTitle')"
              :confirm-button-text="t('common.confirm')"
              :cancel-button-text="t('common.cancel')"
              @confirm="unconfirmRow(row)"
            >
              <template #reference>
                <el-button link type="warning">{{ t('settings.event.unconfirm') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.event.empty')" />
        </template>
      </el-table>
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { useI18n } from 'vue-i18n';

  import { alertBulkConfirm, alertConfirm, alertPage, alertUnconfirm } from '@/api/dashboard';
  import { successMessage } from '@/utils/NotificationUtil';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import ToolCard from '@/components/card/tool/ToolCard.vue';

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
  const bulkRunning = ref(false);
  const selection = ref<Row[]>([]);
  const rows = ref<Row[]>([]);

  // ToolCard binds its inputs to this exact object; empty-string is the
  // "no filter" sentinel so el-segmented can represent it.
  const formData = reactive<{ source: string; eventTypeFlag: number | ''; confirmFlag: number | '' }>({
    source: '',
    eventTypeFlag: '',
    confirmFlag: '',
  });
  const page = reactive({ current: 1, size: 20, total: 0 });

  const normalizedQuery = () => ({
    source: (formData.source || null) as 'device' | 'driver' | null,
    eventTypeFlag: formData.eventTypeFlag === '' ? null : Number(formData.eventTypeFlag),
    confirmFlag: formData.confirmFlag === '' ? null : Number(formData.confirmFlag),
  });

  const load = async () => {
    loading.value = true;
    try {
      const res: any = await alertPage({
        ...normalizedQuery(),
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

  const onSearch = () => {
    page.current = 1;
    load();
  };

  const onReset = () => {
    formData.source = '';
    formData.eventTypeFlag = '';
    formData.confirmFlag = '';
    page.current = 1;
    load();
  };

  const sizeChange = (v: number) => {
    page.size = v;
    page.current = 1;
    load();
  };

  const currentChange = (v: number) => {
    page.current = v;
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

  const unconfirmRow = async (row: Row) => {
    try {
      await alertUnconfirm(row.source, row.id);
      successMessage();
      load();
    } catch {
      // handled globally
    }
  };

  const onSelectionChange = (rows: Row[]) => {
    selection.value = rows;
  };

  const bulkConfirm = async (confirm: boolean) => {
    if (selection.value.length === 0) return;
    bulkRunning.value = true;
    try {
      const items = selection.value.map((r) => ({ source: r.source, id: r.id }));
      await alertBulkConfirm(items, confirm);
      successMessage();
      selection.value = [];
      load();
    } catch {
      // handled globally
    } finally {
      bulkRunning.value = false;
    }
  };

  load();
</script>

<style lang="scss" scoped>
  .settings-table {
    margin-top: 1px;
    border-radius: 4px;
  }
</style>
