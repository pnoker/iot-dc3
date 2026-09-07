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
    <profile-tool
      :embedded="embedded"
      :page="reactiveData.page"
      @refresh="refresh"
      @reset="reset"
      @search="search"
      @sort="sort"
      @show-add="showAdd"
      @size-change="sizeChange"
      @current-change="currentChange"
    ></profile-tool>

    <el-alert
      v-if="reactiveData.status === 'error'"
      :closable="false"
      :title="$t('common.loadFailed')"
      class="entity-page-error"
      show-icon
      type="error"
    >
      <el-button :loading="reactiveData.loading" link type="danger" @click="refresh">
        {{ $t('common.retry') }}
      </el-button>
    </el-alert>

    <blank-card>
      <el-row>
        <template v-if="reactiveData.loading">
          <el-col v-for="data in 12" :key="data" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
            <skeleton-card :footer="true" :loading="true"/>
          </el-col>
        </template>
        <template v-else>
          <el-col v-if="reactiveData.status === 'success' && reactiveData.listData.length < 1">
            <el-empty :description="$t('profile.empty')"/>
          </el-col>
          <el-col v-else-if="reactiveData.status === 'error' && reactiveData.listData.length < 1">
            <el-empty :description="$t('common.loadFailed')"/>
          </el-col>
          <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
            <profile-card
              :data="data"
              :embedded="embedded != ''"
              :busy="isActionBusy(data)"
              @disable-thing="disableThing"
              @enable-thing="enableThing"
              @delete-thing="deleteThing"
            ></profile-card>
          </el-col>
        </template>
      </el-row>
    </blank-card>

    <profile-add-form ref="profileAddFormRef" @add-thing="addThing"></profile-add-form>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, reactive, ref, watch} from 'vue';

import {addProfile, deleteProfile, listProfile, updateProfile} from '@/api/profile';
import {usePagedList} from '@/composables/usePagedList';
import {successMessage} from '@/utils/notificationUtil';
import {isNull} from '@/utils/validationUtil';

import type {ProfileRecord} from '@/config/types/manager';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
import ProfileAddForm from '@/views/profile/add/ProfileAddForm.vue';
import ProfileCard from '@/views/profile/card/ProfileCard.vue';
import ProfileTool from '@/views/profile/tool/ProfileTool.vue';

type DialogInstance = { show: () => void };

const props = withDefaults(
  defineProps<{
    embedded?: string;
    deviceId?: string;
  }>(),
  {
    embedded: '',
    deviceId: '',
  }
);

const profileAddFormRef = ref<DialogInstance | null>(null);
const actionBusy = reactive(new Set<string>());
let disposed = false;

const {
  state: reactiveData,
  load,
  search: _search,
  sort,
  sizeChange,
  currentChange,
} = usePagedList<ProfileRecord>({
  pageSize: 12,
  sortColumn: 'createTime',
  request: (query) => listProfile(query),
});

const baseProfileQuery = computed(() => {
  const q: Record<string, unknown> = {};
  if (!isNull(props.deviceId)) q.deviceId = props.deviceId;
  return q;
});

const search = (params: Record<string, unknown>) => {
  _search({...baseProfileQuery.value, ...params});
};

const reset = () => {
  _search(baseProfileQuery.value);
};

const showAdd = () => {
  profileAddFormRef.value?.show();
};

const addThing = (form: unknown, done: (successful?: boolean) => void) => {
  addProfile(form as Record<string, unknown>)
    .then(() => {
      if (disposed) return;
      successMessage();
      void load();
      done(true);
    })
    .catch(() => {
      if (!disposed) done(false);
    });
};

const isActionBusy = (profile: ProfileRecord) => actionBusy.has(String(profile.id));

const runAction = async (profile: ProfileRecord, action: () => Promise<unknown>) => {
  const id = String(profile.id);
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

const disableThing = (profile: ProfileRecord) =>
  runAction(profile, () => updateProfile({...profile, enableFlag: 'DISABLE'}));

const enableThing = (profile: ProfileRecord) =>
  runAction(profile, () => updateProfile({...profile, enableFlag: 'ENABLE'}));

const deleteThing = (profile: ProfileRecord) =>
  runAction(profile, () => deleteProfile(profile.id, profile.version));

const refresh = () => load();

watch(
  () => props.deviceId,
  () => {
    _search(baseProfileQuery.value);
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
