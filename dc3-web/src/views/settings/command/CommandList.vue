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
    <command-tool
      :editable="canManage"
      :next="next"
      :page="reactiveData.page"
      :pre="pre"
      @add="openAdd"
      @refresh="refresh"
      @reset="reset"
      @search="search"
      @sort="sort"
      @pre-handle="preHandle"
      @next-handle="nextHandle"
      @size-change="sizeChange"
      @current-change="currentChange"
    />

    <el-alert
      v-if="reactiveData.status === 'error'"
      :closable="false"
      :title="$t('common.loadFailed')"
      class="command-list__error"
      show-icon
      type="error"
    >
      <el-button :loading="reactiveData.loading" link type="danger" @click="refresh">
        {{ $t('common.retry') }}
      </el-button>
    </el-alert>

    <!-- Fluid grid wall — see .entity-card-wall in global.scss. -->
    <div class="entity-card-wall">
      <template v-if="reactiveData.loading">
        <skeleton-card v-for="data in 12" :key="data" :footer="canManage" :loading="true"></skeleton-card>
      </template>
      <template v-else-if="hasData">
        <el-empty :description="$t('command.empty')"/>
      </template>
      <template v-else-if="reactiveData.status === 'error' && reactiveData.listData.length === 0">
        <el-empty :description="$t('common.loadFailed')"/>
      </template>
      <template v-else>
        <command-card
          v-for="data in reactiveData.listData"
          :key="data.id"
          :data="data"
          :embedded="embedded !== '' && embedded !== 'edit'"
          :busy="isActionBusy(data)"
          @delete-thing="remove"
          @detail-thing="openDetail"
          @disable-thing="disableThing"
          @edit-thing="openEdit"
          @enable-thing="enableThing"
        ></command-card>
      </template>
    </div>

    <command-edit-form ref="editRef" @add-thing="onAdd" @update-thing="onUpdate"/>

    <el-drawer
      v-model="reactiveData.detailVisible"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      :size="isMobile ? '100%' : '520px'"
      :title="$t('command.detail.title')"
      destroy-on-close
    >
      <el-descriptions v-if="reactiveData.detailRecord" :column="1" border>
        <el-descriptions-item :label="$t('common.name')"
        >{{ reactiveData.detailRecord.commandName || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('command.detail.code')">
          {{ reactiveData.detailRecord.commandCode || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('command.detail.commandType')"
        >{{ reactiveData.detailRecord.commandTypeFlag || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('command.detail.callType')"
        >{{ reactiveData.detailRecord.callTypeFlag || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('command.detail.timeout')"
        >{{ commandTimeoutLabel(reactiveData.detailRecord.timeout) }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('common.enableFlag')">
          <enable-tag :value="reactiveData.detailRecord.enableFlag"/>
        </el-descriptions-item>
        <el-descriptions-item :label="$t('common.remark')"
        >{{ reactiveData.detailRecord.remark || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('command.detail.profileId')">
          {{ reactiveData.detailRecord.profileId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('command.detail.tenantId')">
          {{ reactiveData.detailRecord.tenantId || '-' }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('common.createTime')"
        >{{ timestampLabel(reactiveData.detailRecord.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item :label="$t('common.operationTime')"
        >{{ timestampLabel(reactiveData.detailRecord.operateTime) }}
        </el-descriptions-item>
      </el-descriptions>
      <el-empty v-else :description="$t('common.description')"/>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, reactive, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {
  addCommand,
  addCommandParam,
  deleteCommand,
  deleteCommandParam,
  listCommand,
  updateCommand,
  updateCommandParam,
} from '@/api/command';
import {usePagedList} from '@/composables/usePagedList';
import {useBreakpoint} from '@/composables/useBreakpoint';
import {timestampLabel} from '@/utils/dateUtil';
import {failMessage, successMessage} from '@/utils/notificationUtil';
import {commandTimeoutLabel} from '@/utils/thingModelFormatUtil';
import {isNull} from '@/utils/validationUtil';
import type {CommandForm, CommandParamRecord, CommandRecord} from '@/config/types';
import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import CommandCard from './card/CommandCard.vue';
import CommandTool from './tool/CommandTool.vue';
import CommandEditForm from './edit/CommandEditForm.vue';

const props = withDefaults(
  defineProps<{
    embedded?: string;
    pre?: boolean;
    next?: boolean;
    profileId?: string;
  }>(),
  {embedded: '', pre: false, next: false, profileId: ''}
);

const {isMobile} = useBreakpoint();

const emit = defineEmits<{
  (e: 'pre-handle'): void;
  (e: 'next-handle'): void;
}>();

const editRef = ref<InstanceType<typeof CommandEditForm>>();
const {t} = useI18n();
const canManage = computed(() => props.embedded === '' || props.embedded === 'edit');
const hasData = computed(() => reactiveData.status === 'success' && !reactiveData.loading && reactiveData.listData.length < 1);

const withFixedQuery = (params: Record<string, unknown> = {}) => {
  const q = {...params};
  if (!isNull(props.profileId)) q.profileId = props.profileId;
  return q;
};

const {
  state,
  load,
  search: searchList,
  reset: resetList,
  sort,
  sizeChange,
  currentChange,
} = usePagedList<CommandRecord, Record<string, unknown>>({
  sortColumn: 'createTime',
  request: (query) => listCommand(withFixedQuery(query)),
});

const reactiveData = state as typeof state & {
  detailVisible: boolean;
  detailRecord: CommandRecord | null;
};
reactiveData.detailVisible = false;
reactiveData.detailRecord = null;
const actionBusy = reactive(new Set<string>());
let disposed = false;

const withFixedProfile = (form: CommandForm) => {
  const profileId = !isNull(props.profileId) ? props.profileId : form.profileId;
  return isNull(profileId) ? {...form} : {...form, profileId};
};

const search = (params: Record<string, unknown>) => {
  searchList(params || {});
};

const reset = () => {
  resetList();
};

const refresh = () => load();

const openAdd = () => editRef.value?.show(props.profileId);
const openDetail = (row: CommandRecord) => {
  reactiveData.detailRecord = row;
  reactiveData.detailVisible = true;
};
const openEdit = (row: CommandRecord) => editRef.value?.showEdit(row);

type DoneCallback = (close?: boolean) => void;

const isValidCreatedId = (id: string) => /^\d+$/.test(id);

const syncCommandParams = (
  commandId: string,
  params: CommandParamRecord[],
  originalParams: CommandParamRecord[] = []
) => {
  const currentIds = new Set(params.map((item) => String(item.id || '')).filter(Boolean));
  const deleteTasks = originalParams
    .filter((item) => item.id && !currentIds.has(String(item.id)))
    .map((item) => deleteCommandParam(String(item.id), item.version));

  const saveTasks = params.map((item) => {
    const payload = {...item, commandId};
    return item.id ? updateCommandParam(payload) : addCommandParam(payload);
  });

  return Promise.all(deleteTasks).then(() => Promise.all(saveTasks));
};

const onAdd = (form: CommandForm, params: CommandParamRecord[], done: DoneCallback) => {
  addCommand(withFixedProfile(form))
    .then((res) => {
      const commandId = String(res?.id || '');
      if (!isValidCreatedId(commandId)) {
        failMessage(t('command.errors.idNotReturned'));
        return Promise.reject(new Error(t('command.errors.idNotReturned')));
      }
      return syncCommandParams(commandId, params).then(() => {
        if (disposed) return;
        successMessage();
        void load();
        done();
      });
    })
    .catch(() => {
      if (!disposed) done(false);
    });
};

const onUpdate = (
  form: CommandForm,
  params: CommandParamRecord[],
  originalParams: CommandParamRecord[],
  done: DoneCallback
) => {
  updateCommand(withFixedProfile(form))
    .then(() => {
      const commandId = String(form.id || '');
      if (!isValidCreatedId(commandId)) {
        failMessage(t('command.errors.idMissing'));
        return Promise.reject(new Error(t('command.errors.idMissing')));
      }
      return syncCommandParams(commandId, params, originalParams).then(() => {
        if (disposed) return;
        successMessage();
        void load();
        done();
      });
    })
    .catch(() => {
      if (!disposed) done(false);
    });
};

const isActionBusy = (command: CommandRecord) => actionBusy.has(String(command.id));

const runAction = async (command: CommandRecord, action: () => Promise<unknown>) => {
  const id = String(command.id);
  if (actionBusy.has(id)) return;
  actionBusy.add(id);
  try {
    await action();
    if (disposed) return;
    successMessage();
    await load();
  } catch {
    // handled globally
  } finally {
    actionBusy.delete(id);
  }
};

const disableThing = (command: CommandRecord) =>
  runAction(command, () => updateCommand({...command, enableFlag: 'DISABLE'}));

const enableThing = (command: CommandRecord) =>
  runAction(command, () => updateCommand({...command, enableFlag: 'ENABLE'}));

const remove = (command: CommandRecord) =>
  runAction(command, () => deleteCommand(command.id, command.version));

const preHandle = () => {
  emit('pre-handle');
};

const nextHandle = () => {
  emit('next-handle');
};

watch(
  () => props.profileId,
  () => {
    reset();
  }
);

onBeforeUnmount(() => {
  disposed = true;
  actionBusy.clear();
});

defineExpose({
  reactiveData,
  refresh,
});

load();
</script>

<style lang="scss" scoped>
.command-list__error {
  margin-bottom: var(--dc3-space-3);
}
</style>
