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
    <command-tool
      :page="reactiveData.page"
      @add="openAdd"
      @refresh="refresh"
      @reset="reset"
      @search="search"
      @sort="sort"
      @size-change="sizeChange"
      @current-change="currentChange"
    />

    <blank-card>
      <el-table v-loading="reactiveData.loading" :data="reactiveData.listData" class="settings-table" stripe>
        <el-table-column :label="$t('common.name')" min-width="160" prop="commandName" />
        <el-table-column label="Code" min-width="150" prop="commandCode" show-overflow-tooltip />
        <el-table-column label="Type" prop="commandTypeFlag" width="110" />
        <el-table-column label="Call Type" prop="callTypeFlag" width="110" />
        <el-table-column label="Timeout (ms)" prop="timeout" width="120" />
        <el-table-column :label="$t('common.enable')" width="90">
          <template #default="{ row }">
            <el-tag :type="String(row.enableFlag) === 'ENABLE' || Number(row.enableFlag) === 0 ? 'success' : 'info'">
              {{
                String(row.enableFlag) === 'ENABLE' || Number(row.enableFlag) === 0
                  ? $t('common.enable')
                  : $t('common.disable')
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.remark')" min-width="180" prop="remark" show-overflow-tooltip />
        <el-table-column :formatter="timestampColumn" :label="$t('common.createTime')" prop="createTime" width="165" />
        <el-table-column :label="$t('common.operation')" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ $t('common.detail') }}</el-button>
            <el-button link type="primary" @click="openEdit(row)">{{ $t('common.edit') }}</el-button>
            <el-popconfirm
              :cancel-button-text="$t('common.cancel')"
              :confirm-button-text="$t('common.confirm')"
              :title="$t('common.confirm') + ' ' + $t('common.delete') + '?'"
              @confirm="remove(row.id)"
            >
              <template #reference>
                <el-button link type="danger">{{ $t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="$t('common.description')" />
        </template>
      </el-table>
    </blank-card>

    <command-edit-form ref="editRef" @add-thing="onAdd" @update-thing="onUpdate" />
  </div>
</template>

<script lang="ts" setup>
  import { reactive, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { addCommand, deleteCommand, listCommand, updateCommand } from '@/api/command';
  import { timestampColumn } from '@/utils/dateUtil';
  import { successMessage } from '@/utils/notificationUtil';
  import { isNull } from '@/utils/validationUtil';
  import type { Order } from '@/config/types';
  import type { CommandRecord, CommandForm } from '@/config/types';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import CommandTool from './tool/CommandTool.vue';
  import CommandEditForm from './edit/CommandEditForm.vue';

  const props = withDefaults(
    defineProps<{
      embedded?: string;
      profileId?: string;
    }>(),
    { embedded: '', profileId: '' }
  );

  const router = useRouter();

  const editRef = ref<InstanceType<typeof CommandEditForm>>();

  const reactiveData = reactive({
    loading: false,
    listData: [] as CommandRecord[],
    query: {} as Record<string, unknown>,
    order: false,
    page: {
      total: 0,
      size: 12,
      current: 1,
      orders: [] as Order[],
    },
  });

  const withFixedQuery = (params: Record<string, unknown> = {}) => {
    const q = { ...params };
    if (!isNull(props.profileId)) q.profileId = props.profileId;
    return q;
  };

  const load = () => {
    reactiveData.loading = true;
    const query = withFixedQuery(reactiveData.query);
    listCommand({ page: reactiveData.page, ...query })
      .then((res) => {
        const data = res.data || {};
        reactiveData.listData = data.records || [];
        reactiveData.page.total = data.total || 0;
      })
      .finally(() => {
        reactiveData.loading = false;
      });
  };

  const search = (params: Record<string, unknown>) => {
    reactiveData.query = withFixedQuery(params || {});
    reactiveData.page.current = 1;
    load();
  };

  const reset = () => {
    reactiveData.query = withFixedQuery({});
    reactiveData.page.current = 1;
    load();
  };

  const refresh = () => load();

  const sort = () => {
    reactiveData.order = !reactiveData.order;
    reactiveData.page.orders = [{ column: 'create_time', asc: reactiveData.order }];
    load();
  };

  const openAdd = () => editRef.value?.show();
  const openDetail = (row: CommandRecord) => {
    router.push({ name: 'settingsCommandDefinitionDetail', query: { id: String(row.id) } }).catch(() => {});
  };
  const openEdit = (row: CommandRecord) => editRef.value?.showEdit(row);

  const onAdd = (form: CommandForm, done: () => void) => {
    addCommand(form).then(() => {
      successMessage();
      load();
      done();
    });
  };

  const onUpdate = (form: CommandForm, done: () => void) => {
    updateCommand(form).then(() => {
      successMessage();
      load();
      done();
    });
  };

  const remove = (id: string) => {
    deleteCommand(id).then(() => {
      successMessage();
      load();
    });
  };

  const sizeChange = (size: number) => {
    reactiveData.page.size = size;
    load();
  };

  const currentChange = (current: number) => {
    reactiveData.page.current = current;
    load();
  };

  load();
</script>

<style lang="scss" scoped>
  .settings-table {
    margin-top: 1px;
    border-radius: 4px;
  }
</style>
