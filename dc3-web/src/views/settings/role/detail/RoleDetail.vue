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
    <blank-card>
      <el-tabs v-model="reactiveData.active" @tab-click="changeActive">
        <el-tab-pane :label="$t('settings.role.detailTitle')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('settings.role.roleName')">
                {{ reactiveData.data.roleName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.role.roleCode')">
                {{ reactiveData.data.roleCode || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.role.parentRoleId')">
                {{ reactiveData.data.parentRoleId || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <enable-tag :value="reactiveData.data.enableFlag" />
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.remark')" :span="2">
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

        <el-tab-pane :label="$t('settings.role.usersOfRole')" name="user">
          <el-table v-loading="reactiveData.usersLoading" :data="reactiveData.users" stripe>
            <el-table-column :label="$t('settings.user.nickName')" min-width="140" prop="nickName" />
            <el-table-column :label="$t('settings.user.userName')" min-width="160" prop="userName" />
            <el-table-column :label="$t('settings.user.phone')" min-width="140" prop="phone" />
            <el-table-column :label="$t('settings.user.email')" min-width="180" prop="email" show-overflow-tooltip />
            <el-table-column :label="$t('common.enable')" width="90">
              <template #default="{row}">
                <enable-tag :value="row.enableFlag" />
              </template>
            </el-table-column>
            <template #empty>
              <el-empty :description="$t('settings.role.empty')" :image-size="60" />
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="$t('settings.role.resourcesOfRole')" name="resource">
          <el-table v-loading="reactiveData.resourcesLoading" :data="reactiveData.resources" stripe>
            <el-table-column :label="$t('settings.resource.resourceName')" min-width="200" prop="resourceName" />
            <el-table-column :label="$t('settings.resource.resourceCode')" min-width="180" prop="resourceCode" />
            <el-table-column :label="$t('settings.resource.resourceType')" min-width="120" prop="resourceTypeFlag" />
            <el-table-column :label="$t('settings.resource.resourceScope')" min-width="120" prop="resourceScopeFlag" />
            <el-table-column :label="$t('common.remark')" min-width="180" prop="remark" show-overflow-tooltip />
            <template #empty>
              <el-empty :description="$t('settings.role.empty')" :image-size="60" />
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

  import {getRoleById} from '@/api/role';
  import {listResourceByRoleId} from '@/api/roleResourceBind';
  import {listUserByRoleId} from '@/api/rolePrincipalBind';
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
    users: [] as any[],
    usersLoaded: false,
    usersLoading: false,
    resources: [] as any[],
    resourcesLoaded: false,
    resourcesLoading: false,
  });

  const load = () => {
    if (!reactiveData.id) return;
    getRoleById(reactiveData.id)
      .then((res: any) => {
        reactiveData.data = res.data || {};
      })
      .catch(() => {
        // handled globally
      });
  };

  const loadUsers = () => {
    if (!reactiveData.id || reactiveData.usersLoaded) return;
    reactiveData.usersLoading = true;
    listUserByRoleId(reactiveData.id)
      .then((res: any) => {
        reactiveData.users = (res.data as any[]) || [];
        reactiveData.usersLoaded = true;
      })
      .catch(() => {
        // handled globally
      })
      .finally(() => {
        reactiveData.usersLoading = false;
      });
  };

  const loadResources = () => {
    if (!reactiveData.id || reactiveData.resourcesLoaded) return;
    reactiveData.resourcesLoading = true;
    listResourceByRoleId(reactiveData.id)
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
    router.push({query: {...route.query, active: name}}).catch(() => {});
    if (name === 'user') loadUsers();
    if (name === 'resource') loadResources();
  };

  onMounted(() => {
    load();
    if (reactiveData.active === 'user') loadUsers();
    if (reactiveData.active === 'resource') loadResources();
  });
</script>
