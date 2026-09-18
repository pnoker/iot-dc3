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
  <div class="settings-detail">
    <blank-card>
      <el-alert
        v-if="state.detailStatus === 'error'"
        :closable="false"
        :title="$t('common.loadFailed')"
        class="settings-detail__alert"
        show-icon
        type="error"
      >
        <el-button :loading="state.detailLoading" link type="danger" @click="loadDetail(true)">
          {{ $t('common.retry') }}
        </el-button>
      </el-alert>
      <el-empty
        v-if="state.detailStatus === 'error' && !state.data.id"
        :description="$t('common.loadFailed')"
      />
      <el-skeleton v-else-if="!state.data.id" :rows="6" animated />
      <el-tabs
        v-else
        v-model="state.active"
        @tab-click="changeActive"
      >
        <el-tab-pane :label="$t('settings.role.detailTitle')" name="detail">
          <detail-card v-loading="state.detailLoading" :aria-busy="state.detailLoading">
            <el-descriptions :column="isMobile ? 1 : 2" border>
              <el-descriptions-item :label="$t('settings.role.roleName')">
                {{ state.data.roleName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.role.roleCode')">
                {{ state.data.roleCode || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.role.parentRoleId')">
                {{ state.data.parentRoleId || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <enable-tag :value="state.data.enableFlag"/>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.remark')" :span="isMobile ? 1 : 2">
                {{ state.data.remark || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.createTime')">
                {{ timestampLabel(state.data.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.operationTime')">
                {{ timestampLabel(state.data.operateTime) }}
              </el-descriptions-item>
            </el-descriptions>
          </detail-card>
        </el-tab-pane>

        <el-tab-pane :label="$t('settings.role.usersOfRole')" name="user">
          <responsive-record-list
            :columns="userColumns"
            :empty-text="$t('settings.role.empty')"
            :loading="state.usersLoading"
            :rows="state.users"
            :status="state.usersStatus"
            embedded
            row-key="id"
            @retry="() => loadUsers(true)"
          />
        </el-tab-pane>

        <el-tab-pane :label="$t('settings.role.resourcesOfRole')" name="resource">
          <responsive-record-list
            :columns="resourceColumns"
            :empty-text="$t('settings.role.empty')"
            :loading="state.resourcesLoading"
            :rows="state.resources"
            :status="state.resourcesStatus"
            embedded
            row-key="id"
            @retry="() => loadResources(true)"
          />
        </el-tab-pane>
      </el-tabs>
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, reactive, watch} from 'vue';
import type {TabsPaneContext} from 'element-plus';
import {useI18n} from 'vue-i18n';
import {useRoute, useRouter} from 'vue-router';

import {getRoleById} from '@/api/role';
import {listResourceByRoleId} from '@/api/roleResourceBind';
import {listUserByRoleId} from '@/api/rolePrincipalBind';
import BlankCard from '@/components/card/blank/BlankCard.vue';
import DetailCard from '@/components/card/detail/DetailCard.vue';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import {useBreakpoint} from '@/composables/useBreakpoint';
import type {ResponsiveListColumn} from '@/config/types';
import {timestampLabel} from '@/utils/dateUtil';

interface DetailRow extends Record<string, any> {
  id: string;
  roleName?: string;
  roleCode?: string;
  resourceName?: string;
  resourceCode?: string;
  resourceTypeFlag?: string;
  resourceScopeFlag?: string;
  nickName?: string;
  userName?: string;
  phone?: string;
  email?: string;
  remark?: string;
}

const route = useRoute();
const router = useRouter();
const {t} = useI18n();
const {isMobile} = useBreakpoint();

const state = reactive({
  id: String(route.query.id || ''),
  active: String(route.query.active || 'detail'),
  data: {} as Record<string, any>,
  detailLoading: false,
  detailStatus: 'idle' as 'idle' | 'loading' | 'success' | 'error',
  users: [] as DetailRow[],
  usersLoading: false,
  usersStatus: 'idle' as 'idle' | 'loading' | 'success' | 'error',
  resources: [] as DetailRow[],
  resourcesLoading: false,
  resourcesStatus: 'idle' as 'idle' | 'loading' | 'success' | 'error',
});
let detailRequest = 0;
let usersRequest = 0;
let resourcesRequest = 0;
let detailPromise: Promise<void> | null = null;

const userColumns = computed<ResponsiveListColumn<DetailRow>[]>(() => [
  {key: 'nickName', label: t('settings.user.nickName'), minWidth: 140, mobile: 'primary'},
  {key: 'userName', label: t('settings.user.userName'), minWidth: 160, mobile: 'detail'},
  {key: 'phone', label: t('settings.user.phone'), minWidth: 140, mobile: 'detail'},
  {key: 'email', label: t('settings.user.email'), minWidth: 180, mobile: 'detail'},
  {key: 'enableFlag', label: t('common.enable'), width: 90, kind: 'enable', mobile: 'detail'},
  {key: 'remark', label: t('common.remark'), minWidth: 220, mobile: 'detail'},
]);
const resourceColumns = computed<ResponsiveListColumn<DetailRow>[]>(() => [
  {key: 'resourceName', label: t('settings.resource.resourceName'), minWidth: 200, mobile: 'primary'},
  {key: 'resourceCode', label: t('settings.resource.resourceCode'), minWidth: 180, kind: 'code', mobile: 'detail'},
  {key: 'resourceTypeFlag', label: t('settings.resource.resourceType'), minWidth: 120, mobile: 'detail'},
  {key: 'resourceScopeFlag', label: t('settings.resource.resourceScope'), minWidth: 120, mobile: 'detail'},
  {key: 'remark', label: t('common.remark'), minWidth: 180, mobile: 'detail'},
]);

const loadDetail = (force = false): Promise<void> => {
  if (!state.id) {
    state.data = {};
    state.detailStatus = 'error';
    state.detailLoading = false;
    return Promise.resolve();
  }
  if (state.detailStatus === 'success' && !force) return Promise.resolve();
  if (detailPromise && !force) return detailPromise;
  const requestId = ++detailRequest;
  state.detailLoading = true;
  state.detailStatus = 'loading';
  const request = (async () => {
    try {
      const result = await getRoleById(state.id);
      if (requestId !== detailRequest) return;
      state.data = (result as any) || {};
      state.detailStatus = state.data.id ? 'success' : 'error';
    } catch {
      if (requestId === detailRequest) state.detailStatus = 'error';
    } finally {
      if (requestId === detailRequest) state.detailLoading = false;
    }
  })();
  const trackedRequest = request.finally(() => {
    if (detailPromise === trackedRequest) detailPromise = null;
  });
  detailPromise = trackedRequest;
  return trackedRequest;
};

const loadUsers = async (force = false) => {
  if (!state.id || state.detailStatus !== 'success' || (state.usersStatus === 'success' && !force)) return;
  const requestId = ++usersRequest;
  state.usersLoading = true;
  state.usersStatus = 'loading';
  try {
    const result = await listUserByRoleId(state.id);
    if (requestId !== usersRequest) return;
    state.users = ((result as any[]) || []).map((row) => ({...row, id: String(row.id)}));
    state.usersStatus = 'success';
  } catch {
    if (requestId === usersRequest) state.usersStatus = 'error';
  } finally {
    if (requestId === usersRequest) state.usersLoading = false;
  }
};

const loadResources = async (force = false) => {
  if (!state.id || state.detailStatus !== 'success' || (state.resourcesStatus === 'success' && !force)) return;
  const requestId = ++resourcesRequest;
  state.resourcesLoading = true;
  state.resourcesStatus = 'loading';
  try {
    const result = await listResourceByRoleId(state.id);
    if (requestId !== resourcesRequest) return;
    state.resources = ((result as any[]) || []).map((row) => ({...row, id: String(row.id)}));
    state.resourcesStatus = 'success';
  } catch {
    if (requestId === resourcesRequest) state.resourcesStatus = 'error';
  } finally {
    if (requestId === resourcesRequest) state.resourcesLoading = false;
  }
};

const changeActive = (tab: TabsPaneContext) => {
  const name = String(tab.props.name || 'detail');
  state.active = name;
  void router.push({query: {...route.query, active: name}}).catch(() => undefined);
  if (name === 'user') void loadUsers();
  if (name === 'resource') void loadResources();
};

const resetForId = (id: string) => {
  state.id = id;
  detailPromise = null;
  detailRequest += 1;
  usersRequest += 1;
  resourcesRequest += 1;
  state.data = {};
  state.detailLoading = false;
  state.detailStatus = 'idle';
  state.users = [];
  state.usersLoading = false;
  state.usersStatus = 'idle';
  state.resources = [];
  state.resourcesLoading = false;
  state.resourcesStatus = 'idle';
};

const loadActive = async () => {
  await loadDetail();
  if (state.active === 'user') await loadUsers();
  if (state.active === 'resource') await loadResources();
};

watch(
  () => String(route.query.id || ''),
  (id) => {
    if (id === state.id) return;
    resetForId(id);
    void loadActive();
  }
);
watch(
  () => String(route.query.active || 'detail'),
  (active) => {
    if (!['detail', 'user', 'resource'].includes(active)) return;
    if (active === state.active) return;
    state.active = active;
    if (active === 'user') void loadUsers();
    if (active === 'resource') void loadResources();
  }
);

onMounted(() => void loadActive());

onBeforeUnmount(() => {
  detailRequest += 1;
  usersRequest += 1;
  resourcesRequest += 1;
  detailPromise = null;
});
</script>

<style lang="scss" scoped>
.settings-detail {
  min-width: 0;
}

.settings-detail__alert {
  margin-bottom: var(--dc3-space-3);
}

:deep(.responsive-record-list__embedded) {
  box-shadow: none;
}
</style>
