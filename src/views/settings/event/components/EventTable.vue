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
        <!-- Entity column: for device tables we look up deviceName via /device/ids,
             for driver tables via /driver/ids. Falls back to the raw id. -->
        <el-table-column :label="entityLabel" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span>{{ nameFor(row) }}</span>
            <span class="settings-table__sub">({{ row.sourceId }})</span>
          </template>
        </el-table-column>
        <el-table-column v-if="source === 'device'" :label="$t('settings.event.pointId')" prop="pointId" width="140" />
        <el-table-column :label="$t('settings.event.eventType')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.eventTypeFlag === 1 ? 'warning' : 'info'" size="small">
              {{ row.eventTypeFlag === 1 ? 'ALARM' : 'HEARTBEAT' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('settings.event.message')" prop="message" show-overflow-tooltip min-width="240" />
        <el-table-column :label="$t('settings.event.confirmFlag')" width="110">
          <template #default="{ row }">
            <el-tag :type="row.confirmFlag === 1 ? 'success' : 'warning'" size="small">
              {{ row.confirmFlag === 1 ? $t('settings.event.confirmed') : $t('settings.event.unconfirmed') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('settings.event.createTime')" prop="createTime" width="180" />
        <el-table-column :label="$t('common.operation')" width="140" fixed="right">
          <template #default="{ row }">
            <el-popconfirm
              v-if="row.confirmFlag !== 1"
              :title="$t('settings.event.confirmTitle')"
              :confirm-button-text="$t('common.confirm')"
              :cancel-button-text="$t('common.cancel')"
              @confirm="confirmRow(row)"
            >
              <template #reference>
                <el-button link type="primary">{{ $t('settings.event.confirm') }}</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm
              v-else
              :title="$t('settings.event.unconfirmTitle')"
              :confirm-button-text="$t('common.confirm')"
              :cancel-button-text="$t('common.cancel')"
              @confirm="unconfirmRow(row)"
            >
              <template #reference>
                <el-button link type="warning">{{ $t('settings.event.unconfirm') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="$t('settings.event.empty')" />
        </template>
      </el-table>
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref, watch } from 'vue';
  import { useI18n } from 'vue-i18n';

  import { alertBulkConfirm, alertConfirm, alertPage, alertUnconfirm } from '@/api/dashboard';
  import { getDeviceByIds } from '@/api/device';
  import { getDriverByIds } from '@/api/driver';
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

  const props = defineProps<{
    source: 'device' | 'driver';
  }>();

  const { t } = useI18n();

  const loading = ref(false);
  const bulkRunning = ref(false);
  const selection = ref<Row[]>([]);
  const rows = ref<Row[]>([]);
  const nameMap = reactive<Record<string, string>>({});

  const formData = reactive<{ eventTypeFlag: number | ''; confirmFlag: number | '' }>({
    eventTypeFlag: '',
    confirmFlag: '',
  });
  const page = reactive({ current: 1, size: 20, total: 0 });

  const entityLabel = computed(() =>
    props.source === 'device' ? t('settings.event.device') : t('settings.event.driver')
  );

  const load = async () => {
    loading.value = true;
    try {
      const res: any = await alertPage({
        source: props.source,
        eventTypeFlag: formData.eventTypeFlag === '' ? null : Number(formData.eventTypeFlag),
        confirmFlag: formData.confirmFlag === '' ? null : Number(formData.confirmFlag),
        current: page.current,
        size: page.size,
      });
      const data = res?.data ?? {};
      rows.value = data.records ?? [];
      page.total = Number(data.total ?? 0);
      await resolveNames(rows.value);
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  const resolveNames = async (batch: Row[]) => {
    const ids = Array.from(new Set(batch.map((r) => String(r.sourceId)).filter((id) => id && !nameMap[id])));
    if (ids.length === 0) return;
    try {
      const res: any = props.source === 'device' ? await getDeviceByIds(ids) : await getDriverByIds(ids);
      const data = res?.data || {};
      for (const id of ids) {
        const item = data[id];
        if (item) {
          nameMap[id] = props.source === 'device' ? item.deviceName || id : item.driverName || id;
        }
      }
    } catch {
      // handled globally
    }
  };

  const nameFor = (r: Row) => nameMap[String(r.sourceId)] || String(r.sourceId);

  const onSearch = () => {
    page.current = 1;
    load();
  };

  const onReset = () => {
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

  const onSelectionChange = (selected: Row[]) => {
    selection.value = selected;
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

  watch(
    () => props.source,
    () => {
      page.current = 1;
      load();
    }
  );

  load();
</script>

<style lang="scss" scoped>
  .settings-table {
    margin-top: 1px;
    border-radius: 4px;
  }

  .settings-table__sub {
    margin-left: 6px;
    color: #909399;
    font-size: 12px;
  }
</style>
