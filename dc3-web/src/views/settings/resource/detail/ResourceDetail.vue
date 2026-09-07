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
        <el-tab-pane :label="$t('settings.resource.detailTitle')" name="detail">
          <detail-card v-loading="state.detailLoading" :aria-busy="state.detailLoading">
            <el-descriptions :column="isMobile ? 1 : 2" border>
              <el-descriptions-item :label="$t('settings.resource.resourceName')">
                {{ state.data.resourceName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.resource.resourceCode')">
                {{ state.data.resourceCode || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.resource.serviceName')">
                {{ state.data.serviceName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.resource.resourceType')">
                {{ state.data.resourceTypeFlag || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.resource.resourceScope')">
                {{ state.data.resourceScopeFlag || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.resource.parentResourceId')">
                {{ state.data.parentResourceId || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.resource.entityId')">
                {{ state.data.entityId || '-' }}
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

        <el-tab-pane :label="$t('settings.resource.rolesOfResource')" name="role">
          <responsive-record-list
            :columns="roleColumns"
            :empty-text="$t('settings.resource.empty')"
            :loading="state.rolesLoading"
            :rows="state.roles"
            :status="state.rolesStatus"
            embedded
            row-key="id"
            @retry="() => loadRoles(true)"
          />
        </el-tab-pane>

        <el-tab-pane :label="$t('settings.resource.childResources')" name="children">
          <responsive-record-list
            :columns="childColumns"
            :empty-text="$t('settings.resource.empty')"
            :loading="state.childrenLoading"
            :rows="state.children"
            :status="state.childrenStatus"
            embedded
            row-key="id"
            @retry="() => loadChildren(true)"
          >
            <template #cell-resourceName="{row}">
              <span
                class="resource-detail__child-name"
                :style="{'--resource-depth': Math.min(row.depth || 0, 6)}"
              >
                {{ row.resourceName || '-' }}
              </span>
            </template>
          </responsive-record-list>
        </el-tab-pane>
      </el-tabs>
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, reactive, watch} from 'vue';
import type {TabsPaneContext} from 'element-plus';
import {useRoute, useRouter} from 'vue-router';
import {useI18n} from 'vue-i18n';

import {getResourceById, listResourceTree} from '@/api/resource';
import {listRoleByResourceId} from '@/api/roleResourceBind';
import BlankCard from '@/components/card/blank/BlankCard.vue';
import DetailCard from '@/components/card/detail/DetailCard.vue';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import {useBreakpoint} from '@/composables/useBreakpoint';
import type {ResponsiveListColumn} from '@/config/types';
import {timestampLabel} from '@/utils/dateUtil';

interface ResourceRow extends Record<string, any> {
  id: string;
  resourceName?: string;
  resourceCode?: string;
  resourceTypeFlag?: string;
  resourceScopeFlag?: string;
  remark?: string;
  depth?: number;
  children?: ResourceRow[];
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
  roles: [] as ResourceRow[],
  rolesLoading: false,
  rolesStatus: 'idle' as 'idle' | 'loading' | 'success' | 'error',
  children: [] as ResourceRow[],
  childrenLoading: false,
  childrenStatus: 'idle' as 'idle' | 'loading' | 'success' | 'error',
});
let detailRequest = 0;
let rolesRequest = 0;
let childrenRequest = 0;
let detailPromise: Promise<void> | null = null;

const roleColumns = computed<ResponsiveListColumn<ResourceRow>[]>(() => [
  {key: 'roleName', label: t('settings.role.roleName'), minWidth: 180, mobile: 'primary'},
  {key: 'roleCode', label: t('settings.role.roleCode'), minWidth: 180, kind: 'code', mobile: 'detail'},
  {key: 'enableFlag', label: t('common.enable'), width: 90, kind: 'enable', mobile: 'detail'},
  {key: 'remark', label: t('common.remark'), minWidth: 220, mobile: 'detail'},
]);
const childColumns = computed<ResponsiveListColumn<ResourceRow>[]>(() => [
  {key: 'resourceName', label: t('settings.resource.resourceName'), minWidth: 220, mobile: 'primary'},
  {key: 'resourceCode', label: t('settings.resource.resourceCode'), minWidth: 180, kind: 'code', mobile: 'detail'},
  {key: 'resourceTypeFlag', label: t('settings.resource.resourceType'), minWidth: 110, mobile: 'detail'},
  {key: 'resourceScopeFlag', label: t('settings.resource.resourceScope'), minWidth: 100, mobile: 'detail'},
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
      const result = await getResourceById(state.id);
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
  if (!state.id || state.detailStatus !== 'success' || (state.rolesStatus === 'success' && !force)) return;
  const requestId = ++rolesRequest;
  state.rolesLoading = true;
  state.rolesStatus = 'loading';
  try {
    const result = await listRoleByResourceId(state.id);
    if (requestId !== rolesRequest) return;
    state.roles = ((result as any[]) || []).map((row) => ({...row, id: String(row.id)}));
    state.rolesStatus = 'success';
  } catch {
    if (requestId === rolesRequest) state.rolesStatus = 'error';
  } finally {
    if (requestId === rolesRequest) state.rolesLoading = false;
  }
};

const flattenChildren = (nodes: ResourceRow[], depth = 0): ResourceRow[] =>
  (nodes || []).flatMap((node) => [
    {...node, id: String(node.id), depth},
    ...flattenChildren(node.children || [], depth + 1),
  ]);

const findNode = (nodes: ResourceRow[], id: string): ResourceRow | null => {
  for (const node of nodes || []) {
    if (String(node.id) === id) return node;
    const child = findNode(node.children || [], id);
    if (child) return child;
  }
  return null;
};

const loadChildren = async (force = false) => {
  if (!state.id || state.detailStatus !== 'success' || (state.childrenStatus === 'success' && !force)) return;
  const requestId = ++childrenRequest;
  state.childrenLoading = true;
  state.childrenStatus = 'loading';
  try {
    const result = await listResourceTree({});
    if (requestId !== childrenRequest) return;
    const node = findNode((result as ResourceRow[]) || [], state.id);
    state.children = flattenChildren(node?.children || []);
    state.childrenStatus = 'success';
  } catch {
    if (requestId === childrenRequest) state.childrenStatus = 'error';
  } finally {
    if (requestId === childrenRequest) state.childrenLoading = false;
  }
};

const changeActive = (tab: TabsPaneContext) => {
  const name = String(tab.props.name || 'detail');
  state.active = name;
  void router.push({query: {...route.query, active: name}}).catch(() => undefined);
  if (name === 'role') void loadRoles();
  if (name === 'children') void loadChildren();
};

const resetForId = (id: string) => {
  state.id = id;
  detailPromise = null;
  detailRequest += 1;
  rolesRequest += 1;
  childrenRequest += 1;
  state.data = {};
  state.detailLoading = false;
  state.detailStatus = 'idle';
  state.roles = [];
  state.rolesLoading = false;
  state.rolesStatus = 'idle';
  state.children = [];
  state.childrenLoading = false;
  state.childrenStatus = 'idle';
};

const loadActive = async () => {
  await loadDetail();
  if (state.active === 'role') await loadRoles();
  if (state.active === 'children') await loadChildren();
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
    if (!['detail', 'role', 'children'].includes(active)) return;
    if (active === state.active) return;
    state.active = active;
    if (active === 'role') void loadRoles();
    if (active === 'children') void loadChildren();
  }
);

onMounted(() => {
  void loadActive();
});

onBeforeUnmount(() => {
  detailRequest += 1;
  rolesRequest += 1;
  childrenRequest += 1;
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

.resource-detail__child-name {
  display: inline-block;
  padding-inline-start: calc(var(--resource-depth, 0) * var(--dc3-space-3));
}

:deep(.responsive-record-list__embedded) {
  box-shadow: none;
}
</style>
