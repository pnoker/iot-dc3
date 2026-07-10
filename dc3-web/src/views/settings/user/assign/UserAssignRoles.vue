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
    :title="t('settings.user.assignRolesTitle')"
    width="960px"
  >
    <div v-loading="reactiveData.loading" class="assign-body">
      <div class="assign-target">
        <span class="assign-label">{{ t('settings.user.userName') }}:</span>
        <span class="assign-value">{{ reactiveData.user.userName }}</span>
        <span class="assign-sep">/</span>
        <span class="assign-value">{{ reactiveData.user.nickName }}</span>
      </div>

      <div class="assign-dual">
        <div class="assign-pane">
          <div class="assign-pane__header">
            <span class="assign-pane__title">
              {{ t('settings.user.rolesAvailable') }}
              <span class="assign-pane__count">({{ filteredAvailable.length }})</span>
            </span>
            <el-input
              v-model="reactiveData.leftFilter"
              :placeholder="t('settings.user.rolesSearchPlaceholder')"
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
          <el-table
            ref="leftTableRef"
            :data="filteredAvailable"
            class="assign-pane__table"
            height="360"
            row-key="id"
            stripe
            @selection-change="(rows: RoleRow[]) => (reactiveData.leftSelection = rows)"
          >
            <el-table-column type="selection" width="42"/>
            <el-table-column
              :label="t('settings.role.roleName')"
              min-width="140"
              prop="roleName"
              show-overflow-tooltip
            />
            <el-table-column
              :label="t('settings.role.roleCode')"
              min-width="140"
              prop="roleCode"
              show-overflow-tooltip
            />
            <el-table-column :label="t('common.remark')" min-width="160" prop="remark" show-overflow-tooltip/>
            <template #empty>
              <el-empty :description="t('settings.user.empty')" :image-size="60"/>
            </template>
          </el-table>
        </div>

        <div class="assign-actions">
          <el-button :disabled="reactiveData.leftSelection.length === 0" size="small" type="primary" @click="moveRight">
            {{ t('settings.user.rolesMoveRight') }}
            <el-icon class="assign-actions__icon">
              <ArrowRight/>
            </el-icon>
          </el-button>
          <el-button :disabled="reactiveData.rightSelection.length === 0" size="small" @click="moveLeft">
            <el-icon class="assign-actions__icon">
              <ArrowLeft/>
            </el-icon>
            {{ t('settings.user.rolesMoveLeft') }}
          </el-button>
        </div>

        <div class="assign-pane">
          <div class="assign-pane__header">
            <span class="assign-pane__title">
              {{ t('settings.user.rolesOfUser') }}
              <span class="assign-pane__count">({{ filteredAssigned.length }})</span>
            </span>
            <el-input
              v-model="reactiveData.rightFilter"
              :placeholder="t('settings.user.rolesSearchPlaceholder')"
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
          <el-table
            ref="rightTableRef"
            :data="filteredAssigned"
            class="assign-pane__table"
            height="360"
            row-key="id"
            stripe
            @selection-change="(rows: RoleRow[]) => (reactiveData.rightSelection = rows)"
          >
            <el-table-column type="selection" width="42"/>
            <el-table-column
              :label="t('settings.role.roleName')"
              min-width="140"
              prop="roleName"
              show-overflow-tooltip
            />
            <el-table-column
              :label="t('settings.role.roleCode')"
              min-width="140"
              prop="roleCode"
              show-overflow-tooltip
            />
            <el-table-column :label="t('common.remark')" min-width="160" prop="remark" show-overflow-tooltip/>
            <template #empty>
              <el-empty :description="t('settings.user.empty')" :image-size="60"/>
            </template>
          </el-table>
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
import {computed, reactive, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {ArrowLeft, ArrowRight, Search} from '@element-plus/icons-vue';
import type {TableInstance} from 'element-plus';

import {listRole} from '@/api/role';
import {listRoleByPrincipalId, listRolePrincipalBind} from '@/api/rolePrincipalBind';

interface RoleRow {
  id: string;
  roleName: string;
  roleCode: string;
  remark?: string;
}

const {t} = useI18n();
const emit = defineEmits<{
  (e: 'save', principalId: string, addIds: string[], removeBindIds: string[], done: () => void): void;
}>();

const leftTableRef = ref<TableInstance>();
const rightTableRef = ref<TableInstance>();

const reactiveData = reactive({
  visible: false,
  loading: false,
  submitting: false,
  user: {} as any,
  // bindId(RolePrincipalBind.id) -> roleId lookup; delete endpoint wants bindId
  bindIdByRoleId: new Map<string, string>(),
  originalRoleIds: [] as string[],
  available: [] as RoleRow[],
  assigned: [] as RoleRow[],
  leftSelection: [] as RoleRow[],
  rightSelection: [] as RoleRow[],
  leftFilter: '',
  rightFilter: '',
});

const matches = (r: RoleRow, kw: string) => {
  if (!kw) return true;
  const k = kw.toLowerCase();
  return (
    (r.roleName || '').toLowerCase().includes(k) ||
    (r.roleCode || '').toLowerCase().includes(k) ||
    (r.remark || '').toLowerCase().includes(k)
  );
};

const filteredAvailable = computed(() => reactiveData.available.filter((r) => matches(r, reactiveData.leftFilter)));
const filteredAssigned = computed(() => reactiveData.assigned.filter((r) => matches(r, reactiveData.rightFilter)));

const toRow = (r: any): RoleRow => ({
  id: String(r.id),
  roleName: r.roleName || '',
  roleCode: r.roleCode || '',
  remark: r.remark || '',
});

const load = async () => {
  reactiveData.loading = true;
  try {
    const principalId = String(reactiveData.user.principalId || '');
    const [allRes, ownRes, bindsRes] = await Promise.all([
      listRole({page: {size: 1000, current: 1}}) as Promise<any>,
      listRoleByPrincipalId(principalId) as Promise<any>,
      listRolePrincipalBind({page: {size: 1000, current: 1}, principalId}) as Promise<any>,
    ]);

    const allRoles: RoleRow[] = ((allRes.data?.records as any[]) || []).map(toRow);
    const ownRoles: RoleRow[] = ((ownRes.data as any[]) || []).map(toRow);
    const ownIds = new Set(ownRoles.map((r) => r.id));

    reactiveData.assigned = ownRoles;
    reactiveData.available = allRoles.filter((r) => !ownIds.has(r.id));
    reactiveData.originalRoleIds = ownRoles.map((r) => r.id);

    const bindMap = new Map<string, string>();
    for (const bind of (bindsRes.data?.records as any[]) || []) {
      bindMap.set(String(bind.roleId), String(bind.id));
    }
    reactiveData.bindIdByRoleId = bindMap;
  } catch {
    // handled globally
  } finally {
    reactiveData.loading = false;
  }
};

const show = (user: any) => {
  reactiveData.user = user;
  reactiveData.available = [];
  reactiveData.assigned = [];
  reactiveData.leftSelection = [];
  reactiveData.rightSelection = [];
  reactiveData.leftFilter = '';
  reactiveData.rightFilter = '';
  reactiveData.originalRoleIds = [];
  reactiveData.bindIdByRoleId = new Map();
  reactiveData.visible = true;
  load();
};

const moveRight = () => {
  if (!reactiveData.leftSelection.length) return;
  const movingIds = new Set(reactiveData.leftSelection.map((r) => r.id));
  reactiveData.assigned = reactiveData.assigned.concat(reactiveData.leftSelection);
  reactiveData.available = reactiveData.available.filter((r) => !movingIds.has(r.id));
  leftTableRef.value?.clearSelection();
  reactiveData.leftSelection = [];
};

const moveLeft = () => {
  if (!reactiveData.rightSelection.length) return;
  const movingIds = new Set(reactiveData.rightSelection.map((r) => r.id));
  reactiveData.available = reactiveData.available.concat(reactiveData.rightSelection);
  reactiveData.assigned = reactiveData.assigned.filter((r) => !movingIds.has(r.id));
  rightTableRef.value?.clearSelection();
  reactiveData.rightSelection = [];
};

const submit = () => {
  const originalSet = new Set(reactiveData.originalRoleIds);
  const currentSet = new Set(reactiveData.assigned.map((r) => r.id));

  const addIds: string[] = [];
  const removeBindIds: string[] = [];
  for (const id of currentSet) {
    if (!originalSet.has(id)) addIds.push(id);
  }
  for (const id of originalSet) {
    if (!currentSet.has(id)) {
      const bindId = reactiveData.bindIdByRoleId.get(id);
      if (bindId) removeBindIds.push(bindId);
    }
  }

  if (addIds.length === 0 && removeBindIds.length === 0) {
    reactiveData.visible = false;
    return;
  }

  reactiveData.submitting = true;
  emit('save', String(reactiveData.user.principalId), addIds, removeBindIds, () => {
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

.assign-pane__table {
  flex: 1;
}

.assign-actions {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  padding-top: 40px;
  flex-shrink: 0;

  .el-button {
    width: 96px;
    margin-left: 0;
  }
}

.assign-actions__icon {
  margin: 0 4px;
}
</style>
