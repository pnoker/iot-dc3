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
      <el-tabs v-model="reactiveData.active" @tab-click="changeActive">
        <el-tab-pane :label="$t('settings.user.detailTitle')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('settings.user.nickName')">
                {{ reactiveData.data.nickName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.user.userName')">
                {{ reactiveData.data.userName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.user.phone')">
                {{ reactiveData.data.phone || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.user.email')">
                {{ reactiveData.data.email || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <enable-tag :value="reactiveData.data.enableFlag"/>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.remark')">
                {{ reactiveData.data.remark || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.createTime')">
                {{ timestampLabel(reactiveData.data.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.operationTime')">
                {{ timestampLabel(reactiveData.data.operateTime) }}
              </el-descriptions-item>
            </el-descriptions>
          </detail-card>
        </el-tab-pane>

        <el-tab-pane :label="$t('settings.user.rolesOfUser')" name="role">
          <el-table v-loading="reactiveData.rolesLoading" :data="reactiveData.roles" stripe>
            <el-table-column :label="$t('settings.role.roleName')" min-width="180" prop="roleName"/>
            <el-table-column :label="$t('settings.role.roleCode')" min-width="180" prop="roleCode"/>
            <el-table-column :label="$t('common.enable')" width="90">
              <template #default="{row}">
                <enable-tag :value="row.enableFlag"/>
              </template>
            </el-table-column>
            <el-table-column :label="$t('common.remark')" min-width="220" prop="remark" show-overflow-tooltip/>
            <template #empty>
              <el-empty :description="$t('settings.user.empty')" :image-size="60"/>
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="$t('settings.user.resourcesOfUser')" name="resource">
          <el-table v-loading="reactiveData.resourcesLoading" :data="reactiveData.resources" stripe>
            <el-table-column :label="$t('settings.resource.resourceName')" min-width="200" prop="resourceName"/>
            <el-table-column :label="$t('settings.resource.resourceCode')" min-width="180" prop="resourceCode"/>
            <el-table-column :label="$t('settings.resource.resourceType')" min-width="120" prop="resourceTypeFlag"/>
            <el-table-column :label="$t('settings.resource.resourceScope')" min-width="120" prop="resourceScopeFlag"/>
            <el-table-column :label="$t('common.remark')" min-width="180" prop="remark" show-overflow-tooltip/>
            <template #empty>
              <el-empty :description="$t('settings.user.empty')" :image-size="60"/>
            </template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
import {onMounted, reactive} from 'vue';
import type {TabsPaneContext} from 'element-plus';
import {useRoute, useRouter} from 'vue-router';

import {listResourceByPrincipalId} from '@/api/roleResourceBind';
import {listRoleByPrincipalId} from '@/api/rolePrincipalBind';
import {getUserById} from '@/api/user';
import {timestampLabel} from '@/utils/dateUtil';

import blankCard from '@/components/card/blank/BlankCard.vue';
import detailCard from '@/components/card/detail/DetailCard.vue';
import EnableTag from '@/components/tag/EnableTag.vue';

const route = useRoute();
const router = useRouter();

const reactiveData = reactive({
  id: route.query.id as string,
  active: (route.query.active as string) || 'detail',
  data: {} as Record<string, any>,
  roles: [] as any[],
  rolesLoaded: false,
  rolesLoading: false,
  resources: [] as any[],
  resourcesLoaded: false,
  resourcesLoading: false,
});

const principalId = () => String(reactiveData.data.principalId || '');

const load = async () => {
  if (!reactiveData.id) return;
  await getUserById(reactiveData.id)
    .then((res: any) => {
      reactiveData.data = res.data || {};
    })
    .catch(() => {
      // handled globally
    });
};

const loadRoles = () => {
  if (!principalId() || reactiveData.rolesLoaded) return;
  reactiveData.rolesLoading = true;
  listRoleByPrincipalId(principalId())
    .then((res: any) => {
      reactiveData.roles = (res.data as any[]) || [];
      reactiveData.rolesLoaded = true;
    })
    .catch(() => {
      // handled globally
    })
    .finally(() => {
      reactiveData.rolesLoading = false;
    });
};

const loadResources = () => {
  if (!principalId() || reactiveData.resourcesLoaded) return;
  reactiveData.resourcesLoading = true;
  listResourceByPrincipalId(principalId())
    .then((res: any) => {
      reactiveData.resources = (res.data as any[]) || [];
      reactiveData.resourcesLoaded = true;
    })
    .catch(() => {
      // handled globally
    })
    .finally(() => {
      reactiveData.resourcesLoading = false;
    });
};

const changeActive = (tab: TabsPaneContext) => {
  const name = String(tab.props.name || '');
  router.push({query: {...route.query, active: name}}).catch(() => {
  });
  if (name === 'role') loadRoles();
  if (name === 'resource') loadResources();
};

onMounted(async () => {
  await load();
  if (reactiveData.active === 'role') loadRoles();
  if (reactiveData.active === 'resource') loadResources();
});
</script>
