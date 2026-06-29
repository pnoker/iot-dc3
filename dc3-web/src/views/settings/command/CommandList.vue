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

    <blank-card>
      <el-row>
        <el-col v-for="data in 12" :key="data" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
          <skeleton-card :footer="canManage" :loading="reactiveData.loading"></skeleton-card>
        </el-col>
        <el-col v-if="hasData">
          <el-empty :description="$t('command.empty')" />
        </el-col>
        <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
          <command-card
            :data="data"
            :embedded="embedded !== '' && embedded !== 'edit'"
            icon="images/common/command.png"
            @delete-thing="remove"
            @detail-thing="openDetail"
            @disable-thing="disableThing"
            @edit-thing="openEdit"
            @enable-thing="enableThing"
          ></command-card>
        </el-col>
      </el-row>
    </blank-card>

    <command-edit-form ref="editRef" @add-thing="onAdd" @update-thing="onUpdate" />

    <el-drawer v-model="reactiveData.detailVisible" :title="$t('command.detail.title')" size="520px">
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
          <enable-tag :value="reactiveData.detailRecord.enableFlag" />
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
      <el-empty v-else :description="$t('common.description')" />
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
  import {computed, ref, watch} from 'vue';
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
  import {timestampLabel} from '@/utils/dateUtil';
  import {failMessage, successMessage} from '@/utils/notificationUtil';
  import {commandTimeoutLabel} from '@/utils/thingModelFormatUtil';
  import {isNull} from '@/utils/validationUtil';
  import type {CommandForm, CommandParamRecord, CommandRecord} from '@/config/types';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
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

  const emit = defineEmits<{
    (e: 'pre-handle'): void;
    (e: 'next-handle'): void;
  }>();

  const editRef = ref<InstanceType<typeof CommandEditForm>>();
  const {t} = useI18n();
  const canManage = computed(() => props.embedded === '' || props.embedded === 'edit');
  const hasData = computed(() => !reactiveData.loading && reactiveData.listData.length < 1);

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
    request: (query) => listCommand(withFixedQuery(query)),
  });

  const reactiveData = state as typeof state & {
    detailVisible: boolean;
    detailRecord: CommandRecord | null;
  };
  reactiveData.detailVisible = false;
  reactiveData.detailRecord = null;

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
      .map((item) => deleteCommandParam(String(item.id)));

    const saveTasks = params.map((item) => {
      const payload = {...item, commandId};
      return item.id ? updateCommandParam(payload) : addCommandParam(payload);
    });

    return Promise.all(deleteTasks).then(() => Promise.all(saveTasks));
  };

  const onAdd = (form: CommandForm, params: CommandParamRecord[], done: DoneCallback) => {
    addCommand(withFixedProfile(form))
      .then((res) => {
        const commandId = String(res.data || '');
        if (!isValidCreatedId(commandId)) {
          failMessage(t('command.errors.idNotReturned'));
          return Promise.reject(new Error(t('command.errors.idNotReturned')));
        }
        return syncCommandParams(commandId, params).then(() => {
          successMessage();
          load();
          done();
        });
      })
      .catch(() => {
        done(false);
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
          successMessage();
          load();
          done();
        });
      })
      .catch(() => {
        done(false);
      });
  };

  const disableThing = (id: string, profileId: string, done: () => void) => {
    updateCommand({id, profileId, enableFlag: 'DISABLE'}).then(() => {
      load();
      done();
    });
  };

  const enableThing = (id: string, profileId: string, done: () => void) => {
    updateCommand({id, profileId, enableFlag: 'ENABLE'}).then(() => {
      load();
      done();
    });
  };

  const remove = (id: string, done?: () => void) => {
    deleteCommand(id).then(() => {
      load();
      if (done) {
        done();
      } else {
        successMessage();
      }
    });
  };

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

  defineExpose({
    reactiveData,
    refresh,
  });

  load();
</script>
