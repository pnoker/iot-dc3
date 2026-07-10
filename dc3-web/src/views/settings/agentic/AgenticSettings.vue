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

<template>
  <div>
    <model-config-tool
      :page="reactiveData.page"
      :providers="providers"
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
        <el-table-column :label="$t('settings.agentic.label')" min-width="160" prop="label" show-overflow-tooltip/>
        <el-table-column :label="$t('settings.agentic.model')" min-width="180" prop="model" show-overflow-tooltip/>
        <el-table-column
          :label="$t('settings.agentic.provider')"
          min-width="150"
          prop="providerName"
          show-overflow-tooltip
        />
        <el-table-column :label="$t('settings.agentic.capabilities')" min-width="210">
          <template #default="{row}">
            <div class="agentic-tags">
              <el-tag :type="row.stream ? 'success' : 'info'" size="small">{{ $t('agentic.capStream') }}</el-tag>
              <el-tag :type="row.toolCall ? 'success' : 'info'" size="small">{{ $t('agentic.capTools') }}</el-tag>
              <el-tag :type="row.vision ? 'success' : 'info'" size="small">{{ $t('agentic.capVision') }}</el-tag>
              <el-tag :type="row.reasoning ? 'success' : 'info'" size="small">{{ $t('agentic.capReasoning') }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column :label="$t('settings.agentic.default')" width="100">
          <template #default="{row}">
            <default-tag :value="row.defaultFlag" size="small"/>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.enable')" width="100">
          <template #default="{row}">
            <enable-tag :value="row.enableFlag" size="small"/>
          </template>
        </el-table-column>
        <el-table-column :label="$t('common.remark')" min-width="140" prop="remark" show-overflow-tooltip/>
        <!-- @vue-generic {AgenticModelConfig} -->
        <el-table-column :label="$t('common.operation')" fixed="right" width="210">
          <template #default="{row}">
            <el-button :disabled="!row.id" link type="primary" @click="openDetail(row)"
            >{{ $t('common.detail') }}
            </el-button>
            <el-button :disabled="!row.id" link type="primary" @click="openEdit(row)"
            >{{ $t('common.edit') }}
            </el-button>
            <el-popconfirm
              :cancel-button-text="$t('common.cancel')"
              :confirm-button-text="$t('common.confirm')"
              :title="$t('settings.agentic.confirmDeleteModel', {name: row.model})"
              @confirm="remove(row)"
            >
              <template #reference>
                <el-button :disabled="!row.id" link type="danger">{{ $t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="$t('common.empty')"/>
        </template>
      </el-table>
    </blank-card>

    <model-config-edit-form ref="editRef" :providers="providers" @save="onSave"/>
  </div>
</template>

<script lang="ts" setup>
import {ref} from 'vue';
import {useRouter} from 'vue-router';

import {
  addAgenticModelConfig,
  deleteAgenticModelConfig,
  listAgenticModelConfigs,
  listAgenticProviders,
  updateAgenticModelConfig,
} from '@/api/agentic';
import BlankCard from '@/components/card/blank/BlankCard.vue';
import DefaultTag from '@/components/tag/DefaultTag.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import {usePagedList} from '@/composables/usePagedList';
import type {AgenticModelConfig, AgenticProvider} from '@/config/types';
import {successMessage} from '@/utils/notificationUtil';

import modelConfigTool from './tool/ModelConfigTool.vue';
import modelConfigEditForm from './edit/ModelConfigEditForm.vue';

const router = useRouter();
const editRef = ref<InstanceType<typeof modelConfigEditForm>>();
const providers = ref<AgenticProvider[]>([]);

interface ModelConfigQuery {
  model?: string;
  providerId?: string | number;
  enableFlag?: string;
}

const {
  state: reactiveData,
  setAllData,
  search,
  reset,
  sort,
  sizeChange,
  currentChange,
  withLoading,
} = usePagedList<AgenticModelConfig, ModelConfigQuery>({
  filter: (rows, query) => {
    let filtered = rows;
    if (query.model) {
      const model = String(query.model).toLowerCase();
      filtered = filtered.filter((row) => row.model.toLowerCase().includes(model));
    }
    if (query.providerId) {
      filtered = filtered.filter((row) => String(row.providerId) === String(query.providerId));
    }
    if (query.enableFlag) {
      filtered = filtered.filter((row) => row.enableFlag === query.enableFlag);
    }
    return filtered;
  },
  sortValue: (row) => row.createTime,
});

const load = () =>
  withLoading(async () => {
    const [configResponse, providerResponse] = await Promise.all([listAgenticModelConfigs(), listAgenticProviders()]);
    providers.value = providerResponse.data || [];
    setAllData(configResponse.data || []);
  });

const refresh = () => load();

const openAdd = () => editRef.value?.show();
const openDetail = (row: AgenticModelConfig) => {
  router.push({name: 'settingsModelConfigDetail', query: {id: String(row.id)}}).catch(() => {
    // handled globally
  });
};
const openEdit = (row: AgenticModelConfig) => editRef.value?.showEdit(row);

const onSave = (form: AgenticModelConfig, done: () => void) => {
  const apiCall = form.id ? updateAgenticModelConfig(form) : addAgenticModelConfig(form);
  apiCall
    .then(() => {
      successMessage();
      load();
      done();
    })
    .catch(() => {
    });
};

const remove = (row: AgenticModelConfig) => {
  if (!row.id) return;
  deleteAgenticModelConfig(String(row.id))
    .then(() => {
      successMessage();
      load();
    })
    .catch(() => {
    });
};

load();
</script>

<style lang="scss" scoped>
.agentic-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
