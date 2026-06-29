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
        <el-tab-pane :label="$t('settings.resource.detailTitle')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('settings.resource.resourceName')">
                {{ reactiveData.data.resourceName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.resource.resourceCode')">
                {{ reactiveData.data.resourceCode }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.resource.serviceName')">
                {{ reactiveData.data.serviceName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.resource.resourceType')">
                {{ reactiveData.data.resourceTypeFlag }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.resource.resourceScope')">
                {{ reactiveData.data.resourceScopeFlag }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.resource.parentResourceId')">
                {{ reactiveData.data.parentResourceId || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.resource.entityId')">
                {{ reactiveData.data.entityId || '-' }}
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

        <el-tab-pane :label="$t('settings.resource.rolesOfResource')" name="role">
          <el-table v-loading="reactiveData.rolesLoading" :data="reactiveData.roles" stripe>
            <el-table-column :label="$t('settings.role.roleName')" min-width="180" prop="roleName" />
            <el-table-column :label="$t('settings.role.roleCode')" min-width="180" prop="roleCode" />
            <el-table-column :label="$t('common.enable')" width="90">
              <template #default="{row}">
                <enable-tag :value="row.enableFlag" />
              </template>
            </el-table-column>
            <el-table-column :label="$t('common.remark')" min-width="220" prop="remark" show-overflow-tooltip />
            <template #empty>
              <el-empty :description="$t('settings.resource.empty')" :image-size="60" />
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="$t('settings.resource.childResources')" name="children">
          <el-table
            v-loading="reactiveData.childrenLoading"
            :data="reactiveData.children"
            :tree-props="{children: 'children', hasChildren: 'hasChildren'}"
            default-expand-all
            row-key="id"
            stripe
          >
            <el-table-column :label="$t('settings.resource.resourceName')" min-width="220" prop="resourceName" />
            <el-table-column
              :label="$t('settings.resource.resourceCode')"
              min-width="180"
              prop="resourceCode"
              show-overflow-tooltip
            />
            <el-table-column :label="$t('settings.resource.resourceType')" min-width="110" prop="resourceTypeFlag" />
            <el-table-column :label="$t('settings.resource.resourceScope')" min-width="100" prop="resourceScopeFlag" />
            <el-table-column :label="$t('common.remark')" min-width="180" prop="remark" show-overflow-tooltip />
            <template #empty>
              <el-empty :description="$t('settings.resource.empty')" :image-size="60" />
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

  import {getResourceById, listResourceTree} from '@/api/resource';
  import {listRoleByResourceId} from '@/api/roleResourceBind';
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
    children: [] as any[],
    childrenLoaded: false,
    childrenLoading: false,
  });

  const load = () => {
    if (!reactiveData.id) return;
    getResourceById(reactiveData.id)
      .then((res: any) => {
        reactiveData.data = res.data || {};
      })
      .catch(() => {
        // handled globally
      });
  };

  const loadRoles = () => {
    if (!reactiveData.id || reactiveData.rolesLoaded) return;
    reactiveData.rolesLoading = true;
    listRoleByResourceId(reactiveData.id)
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

  // Walk the resource tree to find the current node, then show its children.
  // Avoids a dedicated "list by parentId" endpoint — the tree is already
  // shaped correctly on the server side, just relocate our own subtree.
  const findNode = (nodes: any[], id: string): any => {
    for (const n of nodes || []) {
      if (String(n.id) === id) return n;
      if (n.children) {
        const hit = findNode(n.children, id);
        if (hit) return hit;
      }
    }
    return null;
  };

  const loadChildren = () => {
    if (!reactiveData.id || reactiveData.childrenLoaded) return;
    reactiveData.childrenLoading = true;
    listResourceTree({})
      .then((res: any) => {
        const tree = (res.data as any[]) || [];
        const node = findNode(tree, reactiveData.id);
        reactiveData.children = node?.children || [];
        reactiveData.childrenLoaded = true;
      })
      .catch(() => {
        // handled globally
      })
      .finally(() => {
        reactiveData.childrenLoading = false;
      });
  };

  const changeActive = (tab: TabsPaneContext) => {
    const name = String(tab.props.name || '');
    router.push({query: {...route.query, active: name}}).catch(() => {});
    if (name === 'role') loadRoles();
    if (name === 'children') loadChildren();
  };

  onMounted(() => {
    load();
    if (reactiveData.active === 'role') loadRoles();
    if (reactiveData.active === 'children') loadChildren();
  });
</script>
