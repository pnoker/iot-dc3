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
  <el-dialog
    v-model="reactiveData.visible"
    :close-on-click-modal="false"
    :title="t('settings.role.assignResourcesTitle')"
    width="640px"
  >
    <div v-loading="reactiveData.loading" class="assign-body">
      <div class="assign-target">
        <span class="assign-label">{{ t('settings.role.roleName') }}:</span>
        <span class="assign-value">{{ reactiveData.role.roleName }}</span>
        <span class="assign-sep">/</span>
        <span class="assign-value">{{ reactiveData.role.roleCode }}</span>
        <span class="assign-summary">
          {{ t('settings.role.resourcesSelectedSummary', {count: reactiveData.selectedIds.length}) }}
        </span>
      </div>

      <div class="assign-pane">
        <div class="assign-pane__header">
          <span class="assign-pane__title">{{ t('settings.role.resourcesAll') }}</span>
          <el-input
            v-model="reactiveData.filter"
            :placeholder="t('settings.role.resourcesSearchPlaceholder')"
            clearable
            size="small"
          >
            <template #prefix>
              <el-icon>
                <Search/>
              </el-icon>
            </template>
          </el-input>
        </div>

        <el-tabs v-model="activeType" class="assign-pane__tabs">
          <el-tab-pane
            v-for="type in availableTypes"
            :key="type"
            :label="`${type} (${selectedCountByType[type] || 0}/${totalCountByType[type] || 0})`"
            :name="type"
          />
        </el-tabs>

        <div class="assign-pane__tree">
          <template v-for="type in availableTypes" :key="type">
            <el-tree
              v-show="activeType === type"
              :ref="(el) => registerTree(type, el)"
              :data="treesByType[type] || []"
              :filter-node-method="filterNode"
              :props="{label: 'resourceName', children: 'children'}"
              node-key="id"
              show-checkbox
              @check-change="onCheckChange"
            />
            <el-empty
              v-show="activeType === type && (treesByType[type] || []).length === 0"
              :description="t('settings.role.empty')"
              :image-size="60"
            />
          </template>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="reactiveData.visible = false">{{ t('common.cancel') }}</el-button>
      <el-button :loading="reactiveData.submitting" type="primary" @click="submit">
        {{ t('common.save') }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, nextTick, reactive, ref, watch} from 'vue';
import type {ElTree} from 'element-plus';
import {Search} from '@element-plus/icons-vue';
import {useI18n} from 'vue-i18n';

import {listResourceTree} from '@/api/resource';
import {listResourceByRoleId, listRoleResourceBind} from '@/api/roleResourceBind';

interface ResourceNode {
  id: string;
  parentResourceId?: string | number;
  resourceName: string;
  resourceCode: string;
  resourceTypeFlag: string;
  remark?: string;
  children?: ResourceNode[];
}

// Tabs are ordered by this list so tenants with all seven types always see
// the same layout. Types the tenant has no resources in are filtered out
// via `availableTypes`.
const TYPE_ORDER = ['MENU', 'API', 'DATA', 'DEVICE', 'POINT', 'PROFILE', 'DRIVER'];

const {t} = useI18n();
const emit = defineEmits<{
  (e: 'save', roleId: string, addIds: string[], removeBindIds: string[], done: () => void): void;
}>();

// One el-tree per resource type — each keeps its own checked state so
// switching tabs doesn't lose selections made in other tabs.
const treeRefs = reactive<Record<string, InstanceType<typeof ElTree> | null>>({});
const registerTree = (type: string, el: unknown) => {
  treeRefs[type] = (el as InstanceType<typeof ElTree>) || null;
};

const activeType = ref<string>('');

const reactiveData = reactive({
  visible: false,
  loading: false,
  submitting: false,
  role: {} as any,
  // trees grouped by resourceTypeFlag; each type's nodes keep their
  // within-type parent/child links (cross-type links are dropped since the
  // user is viewing a single type at a time).
  treesByType: {} as Record<string, ResourceNode[]>,
  // id -> flat node; kept around for counting the selection per-type
  // and for the submit diff against the original bindings.
  nodeMap: new Map<string, ResourceNode>(),
  bindIdByResourceId: new Map<string, string>(),
  originalResourceIds: [] as string[],
  // authoritative current selection; kept in sync with every tree's
  // checked keys via onCheckChange, and rebuilt on load.
  selectedIds: [] as string[],
  filter: '',
});

const treesByType = computed(() => reactiveData.treesByType);

const availableTypes = computed(() => {
  const present = Object.keys(reactiveData.treesByType);
  const ordered = TYPE_ORDER.filter((t) => present.includes(t));
  // catch any type not in TYPE_ORDER so nothing silently disappears
  for (const t of present) if (!ordered.includes(t)) ordered.push(t);
  return ordered;
});

const totalCountByType = computed(() => {
  const map: Record<string, number> = {};
  for (const type of Object.keys(reactiveData.treesByType)) {
    let count = 0;
    const walk = (ns: ResourceNode[]) => {
      for (const n of ns) {
        count += 1;
        if (n.children) walk(n.children);
      }
    };
    walk(reactiveData.treesByType[type] || []);
    map[type] = count;
  }
  return map;
});

const selectedCountByType = computed(() => {
  const map: Record<string, number> = {};
  for (const id of reactiveData.selectedIds) {
    const node = reactiveData.nodeMap.get(id);
    if (!node) continue;
    const type = String(node.resourceTypeFlag);
    map[type] = (map[type] || 0) + 1;
  }
  return map;
});

const filterNode = (value: string, data: any) => {
  if (!value) return true;
  const k = value.toLowerCase();
  return (data.resourceName || '').toLowerCase().includes(k) || (data.resourceCode || '').toLowerCase().includes(k);
};

// Only filter the currently visible tree. Re-apply on tab switch so the
// search box "follows" the user as they flip between types.
watch(
  () => reactiveData.filter,
  (val) => {
    treeRefs[activeType.value]?.filter(val);
  }
);
watch(activeType, (type) => {
  treeRefs[type]?.filter(reactiveData.filter);
});

// Pull all trees' checked state and merge — keeps selectedIds correct even
// when the user flips tabs between picks in different types.
const onCheckChange = () => {
  const all: string[] = [];
  for (const type of availableTypes.value) {
    const tree = treeRefs[type];
    if (!tree) continue;
    all.push(...((tree.getCheckedKeys(false) || []) as Array<string | number>).map(String));
  }
  reactiveData.selectedIds = all;
};

// Flatten the backend tree to a node-map and bucket nodes by type, rebuilding
// per-type parent/child links (a node parented to a different-typed node in
// the source tree becomes a root in its type's bucket).
const groupByType = (treeData: ResourceNode[]) => {
  const flat: ResourceNode[] = [];
  const collect = (ns: ResourceNode[]) => {
    for (const n of ns) {
      flat.push(n);
      if (n.children) collect(n.children);
    }
  };
  collect(treeData);

  const map = new Map<string, ResourceNode>();
  for (const n of flat) map.set(String(n.id), n);

  const buckets: Record<string, ResourceNode[]> = {};
  for (const n of flat) {
    const type = String(n.resourceTypeFlag || 'OTHER');
    if (!buckets[type]) buckets[type] = [];
    buckets[type].push({
      id: String(n.id),
      parentResourceId: n.parentResourceId,
      resourceName: n.resourceName,
      resourceCode: n.resourceCode,
      resourceTypeFlag: type,
      remark: n.remark,
      children: [],
    });
  }

  const trees: Record<string, ResourceNode[]> = {};
  for (const [type, nodes] of Object.entries(buckets)) {
    const byId = new Map<string, ResourceNode>();
    for (const n of nodes) byId.set(n.id, n);
    const roots: ResourceNode[] = [];
    for (const n of nodes) {
      const parentId = n.parentResourceId != null ? String(n.parentResourceId) : null;
      const parent = parentId && byId.get(parentId);
      if (parent) parent.children!.push(n);
      else roots.push(n);
    }
    trees[type] = roots;
  }

  return {flatMap: map, trees};
};

const applyCheckedToTrees = () => {
  const byType: Record<string, string[]> = {};
  for (const id of reactiveData.originalResourceIds) {
    const node = reactiveData.nodeMap.get(id);
    if (!node) continue;
    const type = String(node.resourceTypeFlag);
    (byType[type] ||= []).push(id);
  }
  for (const type of availableTypes.value) {
    treeRefs[type]?.setCheckedKeys(byType[type] || []);
  }
};

const load = async () => {
  reactiveData.loading = true;
  try {
    const [treeRes, ownRes, bindsRes] = await Promise.all([
      listResourceTree({}) as Promise<any>,
      listResourceByRoleId(reactiveData.role.id) as Promise<any>,
      listRoleResourceBind({page: {size: 1000, current: 1}, roleId: reactiveData.role.id}) as Promise<any>,
    ]);

    const treeData = (treeRes.data as any[]) || [];
    const {flatMap, trees} = groupByType(treeData);
    reactiveData.nodeMap = flatMap;
    reactiveData.treesByType = trees;

    const ownIds = ((ownRes.data as any[]) || []).map((r) => String(r.id));
    reactiveData.originalResourceIds = ownIds;
    reactiveData.selectedIds = [...ownIds];

    const bindMap = new Map<string, string>();
    for (const bind of (bindsRes.data?.records as any[]) || []) {
      bindMap.set(String(bind.resourceId), String(bind.id));
    }
    reactiveData.bindIdByResourceId = bindMap;

    // Land on the first type that actually has data so the user sees
    // something on open — default 'MENU' if it exists, else first available.
    const types = availableTypes.value;
    activeType.value = types.includes('MENU') ? 'MENU' : types[0] || '';

    // el-tree mounts after v-if/v-show paints; defer seeding so every
    // tree ref is registered before we push checked keys into them.
    nextTick(applyCheckedToTrees);
  } catch {
    // handled globally
  } finally {
    reactiveData.loading = false;
  }
};

const show = (role: any) => {
  reactiveData.role = role;
  reactiveData.treesByType = {};
  reactiveData.nodeMap = new Map();
  reactiveData.bindIdByResourceId = new Map();
  reactiveData.originalResourceIds = [];
  reactiveData.selectedIds = [];
  reactiveData.filter = '';
  activeType.value = '';
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

defineExpose({show});
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
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
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

.assign-summary {
  margin-left: auto;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.assign-pane {
  display: flex;
  flex-direction: column;
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
  margin-bottom: 6px;
}

.assign-pane__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  flex-shrink: 0;
}

.assign-pane__header .el-input {
  max-width: 240px;
}

.assign-pane__tabs {
  :deep(.el-tabs__header) {
    margin-bottom: 0;
  }

  :deep(.el-tabs__item) {
    font-size: 12px;
    padding: 0 10px;
    height: 32px;
    line-height: 32px;
  }
}

.assign-pane__tree {
  height: 440px;
  overflow: auto;
  border: 1px solid var(--el-border-color-extra-light);
  border-radius: 4px;
  padding: 4px 6px;
  margin-top: 4px;
}
</style>
