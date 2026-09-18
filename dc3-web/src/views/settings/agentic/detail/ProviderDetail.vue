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
    <blank-card>
      <el-alert
        v-if="reactiveData.status === 'error'"
        :closable="false"
        :title="$t('common.loadFailed')"
        class="entity-page-error"
        show-icon
        type="error"
      >
        <el-button :loading="reactiveData.loading" link type="danger" @click="load">
          {{ $t('common.retry') }}
        </el-button>
      </el-alert>
      <el-empty
        v-if="reactiveData.status === 'error' && !reactiveData.data.id"
        :description="$t('common.loadFailed')"
      />
      <el-tabs v-else v-model="reactiveData.active" v-loading="reactiveData.loading">
        <el-tab-pane :label="$t('nav.settingsModelProviderDetail')" name="detail">
          <detail-card>
            <el-descriptions :column="isMobile ? 1 : 2" border>
              <el-descriptions-item :label="$t('settings.agentic.providerName')">
                {{ reactiveData.data.name || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.agentic.providerType')">
                {{ reactiveData.data.providerType || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.agentic.baseUrl')" :span="isMobile ? 1 : 2">
                {{ reactiveData.data.baseUrl || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.agentic.default')">
                <default-tag :value="reactiveData.data.defaultFlag"/>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <enable-tag :value="reactiveData.data.enableFlag"/>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.remark')" :span="isMobile ? 1 : 2">
                {{ reactiveData.data.remark || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </detail-card>
        </el-tab-pane>
      </el-tabs>
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
import {onBeforeUnmount, onMounted, reactive, watch} from 'vue';
import {useRoute} from 'vue-router';

import {listAgenticProviders} from '@/api/agentic';
import BlankCard from '@/components/card/blank/BlankCard.vue';
import DetailCard from '@/components/card/detail/DetailCard.vue';
import DefaultTag from '@/components/tag/DefaultTag.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import type {AgenticProvider} from '@/config/types';
import {useBreakpoint} from '@/composables/useBreakpoint';

const route = useRoute();
const {isMobile} = useBreakpoint();

const reactiveData = reactive({
  id: route.query.id as string,
  active: (route.query.active as string) || 'detail',
  loading: true,
  status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
  data: {} as AgenticProvider,
});
let requestId = 0;

const load = () => {
  const currentRequestId = ++requestId;
  const providerId = String(reactiveData.id || '');
  if (!providerId) {
    reactiveData.status = 'error';
    reactiveData.loading = false;
    return;
  }
  reactiveData.loading = true;
  reactiveData.status = 'loading';
  reactiveData.data = {} as AgenticProvider;
  listAgenticProviders()
    .then((res) => {
      if (currentRequestId !== requestId || providerId !== String(reactiveData.id || '')) return;
      reactiveData.data =
        (res || []).find((item) => String(item.id) === providerId) || ({} as AgenticProvider);
      reactiveData.status = reactiveData.data.id ? 'success' : 'error';
    })
    .catch(() => {
      if (currentRequestId === requestId) reactiveData.status = 'error';
    })
    .finally(() => {
      if (currentRequestId === requestId) reactiveData.loading = false;
    });
};

watch(
  () => route.query.id,
  (id) => {
    const nextId = String(id || '');
    if (nextId !== String(reactiveData.id || '')) {
      reactiveData.id = nextId;
      load();
    }
  },
);

onMounted(() => {
  load();
});

onBeforeUnmount(() => {
  requestId += 1;
});
</script>
