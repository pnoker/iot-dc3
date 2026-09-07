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
    v-model="state.visible"
    :before-close="requestClose"
    :close-on-click-modal="false"
    :close-on-press-escape="!state.submitting"
    :show-close="!state.submitting"
    :title="t('settings.user.assignRolesTitle')"
    class="things-dialog things-dialog--wide assign-dialog"
    destroy-on-close
    width="960px"
  >
    <el-alert
      v-if="state.loadStatus === 'error'"
      :closable="false"
      :title="t('common.loadFailed')"
      class="assign-alert"
      show-icon
      type="error"
    >
      <el-button :loading="state.loading" link type="danger" @click="load(true)">
        {{ t('common.retry') }}
      </el-button>
    </el-alert>
    <el-alert
      v-if="state.saveError"
      :closable="false"
      :title="t('common.saveFailed')"
      class="assign-alert"
      show-icon
      type="error"
    />

    <div v-loading="state.loading" :aria-busy="state.loading" class="assign-body">
      <div class="assign-target">
        <span class="assign-label">{{ t('settings.user.userName') }}:</span>
        <span class="assign-value">{{ state.user.userName || '-' }}</span>
        <span class="assign-sep">/</span>
        <span class="assign-value">{{ state.user.nickName || '-' }}</span>
      </div>

      <div class="assign-dual">
        <section class="assign-pane" :aria-label="t('settings.user.rolesAvailable')">
          <div class="assign-pane__header">
            <span class="assign-pane__title">
              {{ t('settings.user.rolesAvailable') }}
              <span class="assign-pane__count">({{ filteredAvailable.length }})</span>
            </span>
            <el-input
              v-model="state.leftFilter"
              :aria-label="t('settings.user.rolesAvailable')"
              :disabled="state.loading || state.submitting"
              :placeholder="t('settings.user.rolesSearchPlaceholder')"
              clearable
              size="small"
            >
              <template #prefix>
                <el-icon><Search/></el-icon>
              </template>
            </el-input>
          </div>
          <responsive-record-list
            :columns="roleColumns"
            :loading="state.loading"
            :rows="filteredAvailable"
            :selected-rows="state.leftSelection"
            :selection-disabled="() => state.loading || state.submitting"
            embedded
            selectable
            row-key="id"
            class="assign-pane__list"
            @selection-change="setLeftSelection"
          />
        </section>

        <div class="assign-actions" :aria-label="t('settings.user.assignRolesTitle')">
          <el-button
            :disabled="state.leftSelection.length === 0 || state.loading || state.submitting"
            class="assign-actions__button"
            type="primary"
            @click="moveRight"
          >
            <span>{{ t('settings.user.rolesMoveRight') }}</span>
            <el-icon class="assign-actions__icon"><ArrowRight/></el-icon>
          </el-button>
          <el-button
            :disabled="state.rightSelection.length === 0 || state.loading || state.submitting"
            class="assign-actions__button"
            @click="moveLeft"
          >
            <el-icon class="assign-actions__icon"><ArrowLeft/></el-icon>
            <span>{{ t('settings.user.rolesMoveLeft') }}</span>
          </el-button>
        </div>

        <section class="assign-pane" :aria-label="t('settings.user.rolesOfUser')">
          <div class="assign-pane__header">
            <span class="assign-pane__title">
              {{ t('settings.user.rolesOfUser') }}
              <span class="assign-pane__count">({{ filteredAssigned.length }})</span>
            </span>
            <el-input
              v-model="state.rightFilter"
              :aria-label="t('settings.user.rolesOfUser')"
              :disabled="state.loading || state.submitting"
              :placeholder="t('settings.user.rolesSearchPlaceholder')"
              clearable
              size="small"
            >
              <template #prefix>
                <el-icon><Search/></el-icon>
              </template>
            </el-input>
          </div>
          <responsive-record-list
            :columns="roleColumns"
            :loading="state.loading"
            :rows="filteredAssigned"
            :selected-rows="state.rightSelection"
            :selection-disabled="() => state.loading || state.submitting"
            embedded
            selectable
            row-key="id"
            class="assign-pane__list"
            @selection-change="setRightSelection"
          />
        </section>
      </div>
    </div>

    <template #footer>
      <div class="things-dialog-footer">
        <el-button :disabled="state.submitting" @click="requestClose()">{{ t('common.cancel') }}</el-button>
        <el-button
          :disabled="state.submitting || state.loading || state.loadStatus !== 'success'"
          :loading="state.submitting"
          type="primary"
          @click="submit"
        >{{ t('common.save') }}</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, reactive, watch} from 'vue';
import {ArrowLeft, ArrowRight, Search} from '@element-plus/icons-vue';
import {ElMessageBox} from 'element-plus';
import {useI18n} from 'vue-i18n';

import {listRole} from '@/api/role';
import {listRoleByPrincipalId, listRolePrincipalBind} from '@/api/rolePrincipalBind';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';
import type {ResponsiveListColumn} from '@/config/types';

interface RoleRow extends Record<string, any> {
  id: string;
  roleName: string;
  roleCode: string;
  remark?: string;
}

const {t} = useI18n();
const emit = defineEmits<{
  (event: 'save', principalId: string, addIds: string[], removeBindIds: string[], done: (successful?: boolean) => void): void;
}>();

const state = reactive({
  visible: false,
  loading: false,
  submitting: false,
  loadStatus: 'idle' as 'idle' | 'loading' | 'success' | 'error',
  saveError: false,
  user: {} as any,
  bindIdByRoleId: new Map<string, string>(),
  originalRoleIds: [] as string[],
  available: [] as RoleRow[],
  assigned: [] as RoleRow[],
  leftSelection: [] as RoleRow[],
  rightSelection: [] as RoleRow[],
  leftFilter: '',
  rightFilter: '',
});
let loadSequence = 0;
let saveSequence = 0;

const roleColumns = computed<ResponsiveListColumn<RoleRow>[]>(() => [
  {key: 'roleName', label: t('settings.role.roleName'), minWidth: 140, mobile: 'primary'},
  {key: 'roleCode', label: t('settings.role.roleCode'), minWidth: 140, kind: 'code', mobile: 'detail'},
  {key: 'remark', label: t('common.remark'), minWidth: 160, mobile: 'detail'},
]);

const matches = (row: RoleRow, keyword: string) => {
  const value = keyword.trim().toLowerCase();
  if (!value) return true;
  return [row.roleName, row.roleCode, row.remark].some((field) => String(field || '').toLowerCase().includes(value));
};
const filteredAvailable = computed(() => state.available.filter((row) => matches(row, state.leftFilter)));
const filteredAssigned = computed(() => state.assigned.filter((row) => matches(row, state.rightFilter)));
const isDirty = computed(() => {
  const current = new Set(state.assigned.map((row) => row.id));
  return current.size !== state.originalRoleIds.length || state.originalRoleIds.some((id) => !current.has(id));
});

const toRow = (row: any): RoleRow => ({
  id: String(row.id),
  roleName: String(row.roleName || ''),
  roleCode: String(row.roleCode || ''),
  remark: String(row.remark || ''),
});

const load = async (force = false) => {
  if (!state.user.principalId || (state.loadStatus === 'success' && !force)) return;
  const sequence = ++loadSequence;
  state.loading = true;
  state.loadStatus = 'loading';
  state.saveError = false;
  try {
    const principalId = String(state.user.principalId);
    const [allResult, ownResult, bindsResult] = await Promise.all([
      listRole({offset: 0, limit: 200}) as Promise<any>,
      listRoleByPrincipalId(principalId) as Promise<any>,
      listRolePrincipalBind({offset: 0, limit: 200, principalId}) as Promise<any>,
    ]);
    if (sequence !== loadSequence) return;
    const allRoles = ((allResult?.items as any[]) || []).map(toRow);
    const ownRoles = ((ownResult as any[]) || []).map(toRow);
    const ownIds = ownRoles.map((row) => row.id);
    state.assigned = ownRoles;
    state.available = allRoles.filter((row) => !ownIds.includes(row.id));
    state.originalRoleIds = [...ownIds];
    state.bindIdByRoleId = new Map(
      ((bindsResult?.items as any[]) || []).map((bind: any) => [String(bind.roleId), String(bind.id)])
    );
    state.leftSelection = [];
    state.rightSelection = [];
    state.loadStatus = 'success';
  } catch {
    if (sequence === loadSequence) state.loadStatus = 'error';
  } finally {
    if (sequence === loadSequence) state.loading = false;
  }
};

const show = (user: any) => {
  loadSequence += 1;
  saveSequence += 1;
  state.user = user || {};
  state.available = [];
  state.assigned = [];
  state.leftSelection = [];
  state.rightSelection = [];
  state.leftFilter = '';
  state.rightFilter = '';
  state.originalRoleIds = [];
  state.bindIdByRoleId = new Map();
  state.loadStatus = 'idle';
  state.saveError = false;
  state.submitting = false;
  state.visible = true;
  void load();
};

const setLeftSelection = (rows: RoleRow[]) => {
  if (state.loading || state.submitting) return;
  state.leftSelection = rows;
};
const setRightSelection = (rows: RoleRow[]) => {
  if (state.loading || state.submitting) return;
  state.rightSelection = rows;
};
const moveRight = () => {
  if (state.loading || state.submitting || !state.leftSelection.length) return;
  const ids = new Set(state.leftSelection.map((row) => row.id));
  state.assigned = [...state.assigned, ...state.leftSelection];
  state.available = state.available.filter((row) => !ids.has(row.id));
  state.leftSelection = [];
};
const moveLeft = () => {
  if (state.loading || state.submitting || !state.rightSelection.length) return;
  const ids = new Set(state.rightSelection.map((row) => row.id));
  state.available = [...state.available, ...state.rightSelection];
  state.assigned = state.assigned.filter((row) => !ids.has(row.id));
  state.rightSelection = [];
};

const requestClose = async (done?: () => void) => {
  if (state.submitting) return;
  const sequence = loadSequence;
  if (!isDirty.value) {
    if (done) done();
    else state.visible = false;
    return;
  }
  try {
    await ElMessageBox.confirm(t('common.discardConfirm'), t('common.confirm'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    });
    if (sequence !== loadSequence || !state.visible) return;
    if (done) done();
    else state.visible = false;
  } catch {
    return;
  }
};

const submit = () => {
  if (state.submitting || state.loading || state.loadStatus !== 'success') return;
  if (!isDirty.value) {
    state.visible = false;
    return;
  }
  const original = new Set(state.originalRoleIds);
  const current = new Set(state.assigned.map((row) => row.id));
  const addIds = [...current].filter((id) => !original.has(id));
  const removeBindIds = [...original]
    .filter((id) => !current.has(id))
    .map((id) => state.bindIdByRoleId.get(id))
    .filter((id): id is string => Boolean(id));
  state.submitting = true;
  state.saveError = false;
  const session = ++saveSequence;
  emit('save', String(state.user.principalId), addIds, removeBindIds, (successful = true) => {
    if (session !== saveSequence || !state.visible) return;
    state.submitting = false;
    if (successful) {
      state.originalRoleIds = state.assigned.map((row) => row.id);
      state.visible = false;
    } else {
      state.saveError = true;
    }
  });
};

defineExpose({show});

watch(
  () => state.visible,
  (visible) => {
    if (visible) return;
    loadSequence += 1;
    saveSequence += 1;
    state.loading = false;
    state.leftSelection = [];
    state.rightSelection = [];
  }
);

onBeforeUnmount(() => {
  loadSequence += 1;
  saveSequence += 1;
});
</script>

<style lang="scss" scoped>
.assign-body {
  display: flex;
  flex-direction: column;
  gap: var(--dc3-space-3);
  min-width: 0;
}

.assign-alert {
  margin-bottom: var(--dc3-space-3);
}

.assign-target {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--dc3-space-1);
  color: var(--el-text-color-regular);
}

.assign-label,
.assign-sep {
  color: var(--el-text-color-secondary);
}

.assign-sep {
  margin-inline: var(--dc3-space-2);
}

.assign-value {
  font-weight: 600;
}

.assign-dual {
  display: flex;
  align-items: stretch;
  gap: var(--dc3-space-3);
  min-width: 0;
}

.assign-pane {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  gap: var(--dc3-space-2);
  min-width: 0;
  padding: var(--dc3-space-3);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--dc3-radius-md);
  background: var(--el-bg-color);
}

.assign-pane__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--dc3-space-2);
  min-width: 0;
}

.assign-pane__title {
  flex: 0 1 auto;
  min-width: 0;
  overflow: hidden;
  color: var(--el-text-color-primary);
  font-size: var(--el-font-size-small);
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.assign-pane__count {
  color: var(--el-text-color-secondary);
  font-weight: 400;
}

.assign-pane__header .el-input {
  flex: 1 1 180px;
  max-width: 220px;
  min-width: 0;
}

.assign-pane__list {
  max-height: min(360px, 42vh);
  overflow: auto;
}

.assign-actions {
  display: flex;
  flex: 0 0 auto;
  flex-direction: column;
  justify-content: center;
  gap: var(--dc3-space-2);
  min-width: 96px;
}

.assign-actions__button {
  min-height: var(--dc3-touch-target);
  margin: 0;
}

.assign-actions__icon {
  margin-inline: var(--dc3-space-1);
}

@media (max-width: $breakpoint-xs-max) {
  .assign-dual {
    flex-direction: column;
  }

  .assign-pane__header {
    align-items: stretch;
    flex-direction: column;
  }

  .assign-pane__header .el-input {
    max-width: none;
  }

  .assign-pane__list {
    max-height: 32vh;
  }

  .assign-actions {
    flex-direction: row;
    min-width: 0;
  }

  .assign-actions__button {
    flex: 1 1 0;
  }
}

@media (min-width: $breakpoint-sm) and (max-width: $breakpoint-md-max) {
  .assign-dual {
    gap: var(--dc3-space-2);
  }

  .assign-actions {
    min-width: 76px;
  }

  .assign-actions__button {
    padding-inline: var(--dc3-space-2);
  }
}
</style>
