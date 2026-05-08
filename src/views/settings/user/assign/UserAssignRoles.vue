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
    :title="t('settings.user.assignRolesTitle')"
    width="960px"
    :close-on-click-modal="false"
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
              size="small"
              :placeholder="t('settings.user.rolesSearchPlaceholder')"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          <el-table
            ref="leftTableRef"
            :data="filteredAvailable"
            height="360"
            row-key="id"
            stripe
            class="assign-pane__table"
            @selection-change="(rows: RoleRow[]) => (reactiveData.leftSelection = rows)"
          >
            <el-table-column type="selection" width="42" />
            <el-table-column
              prop="roleName"
              :label="t('settings.role.roleName')"
              min-width="140"
              show-overflow-tooltip
            />
            <el-table-column
              prop="roleCode"
              :label="t('settings.role.roleCode')"
              min-width="140"
              show-overflow-tooltip
            />
            <el-table-column prop="remark" :label="t('common.remark')" min-width="160" show-overflow-tooltip />
            <template #empty>
              <el-empty :description="t('settings.user.empty')" :image-size="60" />
            </template>
          </el-table>
        </div>

        <div class="assign-actions">
          <el-button :disabled="reactiveData.leftSelection.length === 0" type="primary" size="small" @click="moveRight">
            {{ t('settings.user.rolesMoveRight') }}
            <el-icon class="assign-actions__icon"><ArrowRight /></el-icon>
          </el-button>
          <el-button :disabled="reactiveData.rightSelection.length === 0" size="small" @click="moveLeft">
            <el-icon class="assign-actions__icon"><ArrowLeft /></el-icon>
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
              size="small"
              :placeholder="t('settings.user.rolesSearchPlaceholder')"
              clearable
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </div>
          <el-table
            ref="rightTableRef"
            :data="filteredAssigned"
            height="360"
            row-key="id"
            stripe
            class="assign-pane__table"
            @selection-change="(rows: RoleRow[]) => (reactiveData.rightSelection = rows)"
          >
            <el-table-column type="selection" width="42" />
            <el-table-column
              prop="roleName"
              :label="t('settings.role.roleName')"
              min-width="140"
              show-overflow-tooltip
            />
            <el-table-column
              prop="roleCode"
              :label="t('settings.role.roleCode')"
              min-width="140"
              show-overflow-tooltip
            />
            <el-table-column prop="remark" :label="t('common.remark')" min-width="160" show-overflow-tooltip />
            <template #empty>
              <el-empty :description="t('settings.user.empty')" :image-size="60" />
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
  import { computed, reactive, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { ArrowLeft, ArrowRight, Search } from '@element-plus/icons-vue';
  import type { ElTable } from 'element-plus';

  import { getRoleList } from '@/api/role';
  import { getRoleListByUserId, getRoleUserList } from '@/api/roleUserBind';

  interface RoleRow {
    id: string;
    roleName: string;
    roleCode: string;
    remark?: string;
  }

  const { t } = useI18n();
  const emit = defineEmits<{
    (e: 'save', userId: string, addIds: string[], removeBindIds: string[], done: () => void): void;
  }>();

  const leftTableRef = ref<InstanceType<typeof ElTable>>();
  const rightTableRef = ref<InstanceType<typeof ElTable>>();

  const reactiveData = reactive({
    visible: false,
    loading: false,
    submitting: false,
    user: {} as any,
    // bindId(RoleUserBind.id) -> roleId lookup; delete endpoint wants bindId
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
      const [allRes, ownRes, bindsRes] = await Promise.all([
        getRoleList({ page: { size: 1000, current: 1 } }) as Promise<any>,
        getRoleListByUserId(reactiveData.user.id) as Promise<any>,
        getRoleUserList({ page: { size: 1000, current: 1 }, userId: reactiveData.user.id }) as Promise<any>,
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
    emit('save', String(reactiveData.user.id), addIds, removeBindIds, () => {
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
