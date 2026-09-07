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
        <el-tab-pane :label="$t('settings.user.detailTitle')" name="detail">
          <detail-card v-loading="state.detailLoading" :aria-busy="state.detailLoading">
            <el-descriptions :column="isMobile ? 1 : 2" border>
              <el-descriptions-item :label="$t('settings.user.nickName')">
                {{ state.data.nickName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.user.userName')">
                {{ state.data.userName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.user.phone')">
                {{ state.data.phone || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.user.email')">
                {{ state.data.email || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <enable-tag :value="state.data.enableFlag"/>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.remark')">
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

        <el-tab-pane :label="$t('settings.user.rolesOfUser')" name="role">
          <responsive-record-list
            :columns="roleColumns"
            :empty-text="$t('settings.user.empty')"
            :loading="state.rolesLoading"
            :rows="state.roles"
            :status="state.rolesStatus"
            embedded
            row-key="id"
            @retry="() => loadRoles(true)"
          />
        </el-tab-pane>

        <el-tab-pane :label="$t('settings.user.resourcesOfUser')" name="resource">
          <responsive-record-list
            :columns="resourceColumns"
            :empty-text="$t('settings.user.empty')"
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

import {listResourceByPrincipalId} from '@/api/roleResourceBind';
import {listRoleByPrincipalId} from '@/api/rolePrincipalBind';
import {getUserById} from '@/api/user';
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
  remark?: string;
  enableFlag?: unknown;
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
  roles: [] as DetailRow[],
  rolesLoading: false,
  rolesStatus: 'idle' as 'idle' | 'loading' | 'success' | 'error',
  resources: [] as DetailRow[],
  resourcesLoading: false,
  resourcesStatus: 'idle' as 'idle' | 'loading' | 'success' | 'error',
});
let detailRequest = 0;
let rolesRequest = 0;
let resourcesRequest = 0;
let detailPromise: Promise<void> | null = null;

const roleColumns = computed<ResponsiveListColumn<DetailRow>[]>(() => [
  {key: 'roleName', label: t('settings.role.roleName'), minWidth: 180, mobile: 'primary'},
  {key: 'roleCode', label: t('settings.role.roleCode'), minWidth: 180, kind: 'code', mobile: 'detail'},
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

const principalId = () => String(state.data.principalId || '');

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
      const result = await getUserById(state.id);
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

const loadRoles = async (force = false) => {
  if (!state.id || state.detailStatus !== 'success') return;
  const targetId = state.id;
  if (!principalId()) await loadDetail();
  if (targetId !== state.id) return;
  const id = principalId();
  if (!id) {
    // A detail record without a principalId is an inconsistent state;
    // show the error (retry) pane instead of a misleading empty list.
    state.rolesStatus = 'error';
    return;
  }
  if (state.rolesStatus === 'success' && !force) return;
  const requestId = ++rolesRequest;
  state.rolesLoading = true;
  state.rolesStatus = 'loading';
  try {
    const result = await listRoleByPrincipalId(id);
    if (requestId !== rolesRequest) return;
    state.roles = ((result as any[]) || []).map((row) => ({...row, id: String(row.id)}));
    state.rolesStatus = 'success';
  } catch {
    if (requestId === rolesRequest) state.rolesStatus = 'error';
  } finally {
    if (requestId === rolesRequest) state.rolesLoading = false;
  }
};

const loadResources = async (force = false) => {
  if (!state.id || state.detailStatus !== 'success') return;
  const targetId = state.id;
  if (!principalId()) await loadDetail();
  if (targetId !== state.id) return;
  const id = principalId();
  if (!id) {
    state.resourcesStatus = 'error';
    return;
  }
  if (state.resourcesStatus === 'success' && !force) return;
  const requestId = ++resourcesRequest;
  state.resourcesLoading = true;
  state.resourcesStatus = 'loading';
  try {
    const result = await listResourceByPrincipalId(id);
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
  if (name === 'role') void loadRoles();
  if (name === 'resource') void loadResources();
};

const resetForId = (id: string) => {
  state.id = id;
  detailPromise = null;
  detailRequest += 1;
  rolesRequest += 1;
  resourcesRequest += 1;
  state.data = {};
  state.detailLoading = false;
  state.detailStatus = 'idle';
  state.roles = [];
  state.rolesLoading = false;
  state.rolesStatus = 'idle';
  state.resources = [];
  state.resourcesLoading = false;
  state.resourcesStatus = 'idle';
};

const loadActive = async () => {
  await loadDetail();
  if (state.active === 'role') await loadRoles();
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
    if (!['detail', 'role', 'resource'].includes(active)) return;
    if (active === state.active) return;
    state.active = active;
    if (active === 'role') void loadRoles();
    if (active === 'resource') void loadResources();
  }
);

onMounted(() => void loadActive());

onBeforeUnmount(() => {
  detailRequest += 1;
  rolesRequest += 1;
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
