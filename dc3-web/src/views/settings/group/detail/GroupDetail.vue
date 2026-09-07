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
        <el-tab-pane :label="$t('settings.group.detailTitle')" name="detail">
          <detail-card>
            <el-descriptions :column="isMobile ? 1 : 2" border>
              <el-descriptions-item :label="$t('settings.group.groupName')">
                {{ reactiveData.data.groupName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.group.groupCode')">
                {{ reactiveData.data.groupCode || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.common.entityType')">
                {{ reactiveData.data.groupTypeFlag || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.group.parentGroupId')">
                {{ parentName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.group.groupLevel')">
                {{ reactiveData.data.groupLevel ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.group.groupIndex')">
                {{ reactiveData.data.groupIndex ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <enable-tag :value="reactiveData.data.enableFlag"/>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.remark')" :span="isMobile ? 1 : 2">
                {{ reactiveData.data.remark || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.creatorName')">
                {{ reactiveData.data.creatorName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.createTime')">
                {{ timestampLabel(reactiveData.data.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.operatorName')">
                {{ reactiveData.data.operatorName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.operationTime')">
                {{ timestampLabel(reactiveData.data.operateTime) }}
              </el-descriptions-item>
            </el-descriptions>
          </detail-card>
        </el-tab-pane>
      </el-tabs>
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, reactive, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRoute} from 'vue-router';

import {getGroupById, listGroup} from '@/api/group';
import BlankCard from '@/components/card/blank/BlankCard.vue';
import DetailCard from '@/components/card/detail/DetailCard.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import {timestampLabel} from '@/utils/dateUtil';
import {useBreakpoint} from '@/composables/useBreakpoint';

const route = useRoute();
const {t} = useI18n();
const {isMobile} = useBreakpoint();

const reactiveData = reactive({
  id: route.query.id as string,
  active: (route.query.active as string) || 'detail',
  loading: true,
  status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
  data: {} as Record<string, any>,
  parentMap: {} as Record<string, string>,
});
let requestId = 0;

const parentName = computed(() => {
  const id = reactiveData.data.parentGroupId;
  if (!id || String(id) === '0') return t('settings.group.rootGroup');
  return reactiveData.parentMap[String(id)] || String(id);
});

const loadParents = (currentRequestId: number) => {
  return listGroup({offset: 0, limit: 200})
    .then((res: any) => {
      if (currentRequestId !== requestId) return;
      reactiveData.parentMap = {};
      const records = res?.items || [];
      records.forEach((row: Record<string, any>) => {
        reactiveData.parentMap[String(row.id)] = row.groupName || String(row.id);
      });
    })
    .catch(() => {
      // Parent names are an optional enhancement; keep the raw id on failure.
    });
};

const load = () => {
  const currentRequestId = ++requestId;
  const groupId = String(reactiveData.id || '');
  if (!groupId) {
    reactiveData.status = 'error';
    reactiveData.loading = false;
    return;
  }
  reactiveData.loading = true;
  reactiveData.status = 'loading';
  reactiveData.data = {};
  reactiveData.parentMap = {};
  void loadParents(currentRequestId);
  getGroupById(groupId)
    .then((res: any) => {
      if (currentRequestId !== requestId || groupId !== String(reactiveData.id || '')) return;
      reactiveData.data = res || {};
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
