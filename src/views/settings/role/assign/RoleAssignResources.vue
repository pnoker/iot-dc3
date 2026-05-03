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
  <el-dialog
    v-model="reactiveData.visible"
    :title="t('settings.role.assignResourcesTitle')"
    width="960px"
    :close-on-click-modal="false"
  >
    <div v-loading="reactiveData.loading" class="assign-body">
      <div class="assign-target">
        <span class="assign-label">{{ t('settings.role.roleName') }}:</span>
        <span class="assign-value">{{ reactiveData.role.roleName }}</span>
        <span class="assign-sep">/</span>
        <span class="assign-value">{{ reactiveData.role.roleCode }}</span>
      </div>

      <div class="assign-dual">
        <div class="assign-pane">
          <div class="assign-pane__header">
            <span class="assign-pane__title">{{ t('settings.role.resourcesAll') }}</span>
            <el-input
              v-model="reactiveData.filter"
              size="small"
              :placeholder="t('settings.role.resourcesSearchPlaceholder')"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          <div class="assign-pane__tree">
            <el-tree
              ref="treeRef"
              :data="reactiveData.treeData"
              :props="{ label: 'resourceName', children: 'children' }"
              node-key="id"
              show-checkbox
              check-strictly
              :filter-node-method="filterNode"
              @check-change="onCheckChange"
            />
          </div>
        </div>

        <div class="assign-pane">
          <div class="assign-pane__header">
            <span class="assign-pane__title">
              {{ t('settings.role.resourcesOfRole') }}
              <span class="assign-pane__count">({{ assignedList.length }})</span>
            </span>
          </div>
          <el-table :data="assignedList" height="420" stripe class="assign-pane__table" row-key="id">
            <el-table-column
              prop="resourceName"
              :label="t('settings.resource.resourceName')"
              min-width="180"
              show-overflow-tooltip
            />
            <el-table-column
              prop="resourceCode"
              :label="t('settings.resource.resourceCode')"
              min-width="140"
              show-overflow-tooltip
            />
            <el-table-column prop="resourceTypeFlag" :label="t('settings.resource.resourceType')" width="110" />
            <el-table-column :label="t('common.operation')" width="88" fixed="right">
              <template #default="{ row }">
                <el-button link type="danger" @click="removeOne(row.id)">{{ t('common.remove') }}</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty :description="t('settings.role.empty')" :image-size="60" />
            </template>
          </el-table>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="reactiveData.visible = false">{{ t('common.cancel') }}</el-button>
      <el-button type="primary" :loading="reactiveData.submitting" @click="submit">
        {{ t('common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref, watch } from 'vue';
  import type { ElTree } from 'element-plus';
  import { Search } from '@element-plus/icons-vue';
  import { useI18n } from 'vue-i18n';

  import { getResourceTree } from '@/api/resource';
  import { getRoleResourceList, listResourceByRoleId } from '@/api/roleResourceBind';

  interface ResourceNode {
    id: string;
    resourceName: string;
    resourceCode: string;
    resourceTypeFlag?: string | number;
    remark?: string;
    children?: ResourceNode[];
  }

  const { t } = useI18n();
  const emit = defineEmits<{
    (e: 'save', roleId: string, addIds: string[], removeBindIds: string[], done: () => void): void;
  }>();

  const treeRef = ref<InstanceType<typeof ElTree>>();

  const reactiveData = reactive({
    visible: false,
    loading: false,
    submitting: false,
    role: {} as any,
    treeData: [] as ResourceNode[],
    // id -> flat node (after flattening the tree) used to render the
    // right-hand "Assigned" table from a list of ids.
    nodeMap: new Map<string, ResourceNode>(),
    bindIdByResourceId: new Map<string, string>(),
    originalResourceIds: [] as string[],
    // authoritative current-selection list, kept in sync with el-tree
    // via @check-change. Using a Set inside a reactive breaks deep
    // reactivity on mutation, so we use an array + helpers.
    selectedIds: [] as string[],
    filter: '',
  });

  const flatten = (nodes: ResourceNode[], into: Map<string, ResourceNode>) => {
    for (const n of nodes || []) {
      into.set(String(n.id), n);
      if (n.children && n.children.length) {
        flatten(n.children, into);
      }
    }
  };

  const filterNode = (value: string, data: any) => {
    if (!value) return true;
    const k = value.toLowerCase();
    return (data.resourceName || '').toLowerCase().includes(k) || (data.resourceCode || '').toLowerCase().includes(k);
  };

  watch(
    () => reactiveData.filter,
    (val) => {
      treeRef.value?.filter(val);
    }
  );

  // el-tree fires check-change per node rather than a batch event. Pull the
  // current state from the tree instead of mutating manually.
  const onCheckChange = () => {
    const keys = (treeRef.value?.getCheckedKeys(false) || []) as Array<string | number>;
    reactiveData.selectedIds = keys.map(String);
  };

  const assignedList = computed(() =>
    reactiveData.selectedIds.map((id) => reactiveData.nodeMap.get(id)).filter((n): n is ResourceNode => !!n)
  );

  const removeOne = (id: string | number) => {
    const key = String(id);
    treeRef.value?.setChecked(key, false, false);
    reactiveData.selectedIds = reactiveData.selectedIds.filter((v) => v !== key);
  };

  const load = async () => {
    reactiveData.loading = true;
    try {
      const [treeRes, ownRes, bindsRes] = await Promise.all([
        getResourceTree({}) as Promise<any>,
        listResourceByRoleId(reactiveData.role.id) as Promise<any>,
        getRoleResourceList({ page: { size: 1000, current: 1 }, roleId: reactiveData.role.id }) as Promise<any>,
      ]);

      const treeData = (treeRes.data as any[]) || [];
      reactiveData.treeData = treeData;

      const map = new Map<string, ResourceNode>();
      flatten(treeData, map);
      reactiveData.nodeMap = map;

      const ownIds = ((ownRes.data as any[]) || []).map((r) => String(r.id));
      reactiveData.originalResourceIds = ownIds;
      reactiveData.selectedIds = [...ownIds];

      const bindMap = new Map<string, string>();
      for (const bind of (bindsRes.data?.records as any[]) || []) {
        bindMap.set(String(bind.resourceId), String(bind.id));
      }
      reactiveData.bindIdByResourceId = bindMap;

      // Seed the tree's checked state after render so the right-hand table
      // matches on open. setCheckedKeys is idempotent and won't fire
      // check-change, so we set selectedIds first above.
      setTimeout(() => {
        treeRef.value?.setCheckedKeys(ownIds);
      });
    } catch {
      // handled globally
    } finally {
      reactiveData.loading = false;
    }
  };

  const show = (role: any) => {
    reactiveData.role = role;
    reactiveData.treeData = [];
    reactiveData.nodeMap = new Map();
    reactiveData.bindIdByResourceId = new Map();
    reactiveData.originalResourceIds = [];
    reactiveData.selectedIds = [];
    reactiveData.filter = '';
    reactiveData.visible = true;
    load();
  };

  const submit = () => {
    const originalSet = new Set(reactiveData.originalResourceIds);
    const currentSet = new Set(reactiveData.selectedIds);

    const addIds: string[] = [];
    const removeBindIds: string[] = [];
    for (const id of currentSet) {
      if (!originalSet.has(id)) addIds.push(id);
    }
    for (const id of originalSet) {
      if (!currentSet.has(id)) {
        const bindId = reactiveData.bindIdByResourceId.get(id);
        if (bindId) removeBindIds.push(bindId);
      }
    }

    if (addIds.length === 0 && removeBindIds.length === 0) {
      reactiveData.visible = false;
      return;
    }

    reactiveData.submitting = true;
    emit('save', String(reactiveData.role.id), addIds, removeBindIds, () => {
      reactiveData.submitting = false;
      reactiveData.visible = false;
    });
  };

  defineExpose({ show });
</script>

<style lang="scss" scoped>
  .assign-body {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .assign-target {
    font-size: 14px;
    color: var(--el-text-color-regular);
  }
  .assign-label {
    color: var(--el-text-color-secondary);
    margin-right: 4px;
  }
  .assign-value {
    font-weight: 600;
  }
  .assign-sep {
    margin: 0 6px;
    color: var(--el-text-color-secondary);
  }

  .assign-dual {
    display: flex;
    align-items: stretch;
    gap: 12px;
  }

  .assign-pane {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 8px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 4px;
    padding: 10px;
    background: var(--el-bg-color);
  }

  .assign-pane__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  .assign-pane__title {
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);
    flex-shrink: 0;
  }

  .assign-pane__count {
    color: var(--el-text-color-secondary);
    font-weight: 400;
    margin-left: 4px;
  }

  .assign-pane__header .el-input {
    max-width: 200px;
  }

  .assign-pane__tree {
    height: 420px;
    overflow: auto;
    border: 1px solid var(--el-border-color-extra-light);
    border-radius: 4px;
    padding: 4px 6px;
  }

  .assign-pane__table {
    flex: 1;
  }
</style>
