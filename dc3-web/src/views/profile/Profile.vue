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

    <blank-card>
      <el-row>
        <template v-if="reactiveData.loading">
          <el-col v-for="data in 12" :key="data" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
            <skeleton-card :footer="true" :loading="true" />
          </el-col>
        </template>
        <template v-else>
          <el-col v-if="reactiveData.listData.length < 1">
            <el-empty :description="$t('profile.empty')" />
          </el-col>
          <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
            <profile-card
              :data="data"
              :embedded="embedded != ''"
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
  import {computed, ref} from 'vue';

  import {addProfile, deleteProfile, listProfile, updateProfile} from '@/api/profile';
  import {usePagedList} from '@/composables/usePagedList';
  import {failMessage, successMessage} from '@/utils/notificationUtil';
  import {isNull} from '@/utils/validationUtil';

  import type {ProfileRecord} from '@/config/types/manager';

  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
  import ProfileAddForm from '@/views/profile/add/ProfileAddForm.vue';
  import ProfileCard from '@/views/profile/card/ProfileCard.vue';
  import ProfileTool from '@/views/profile/tool/ProfileTool.vue';

  type DialogInstance = {show: () => void};

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

  const {
    state: reactiveData,
    load,
    search: _search,
    sort,
    sizeChange,
    currentChange,
  } = usePagedList<ProfileRecord>({
    pageSize: 12,
    sortColumn: 'create_time',
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

  const addThing = (form: unknown, done: () => void) => {
    addProfile(form as Record<string, unknown>)
      .then(() => {
        successMessage();
        load();
      })
      .catch(() => {
        failMessage();
      })
      .finally(() => {
        done();
      });
  };

  const disableThing = (id: number | string, done: () => void) => {
    updateProfile({id: String(id), enableFlag: 'DISABLE'})
      .then(() => {
        successMessage();
        load();
      })
      .catch(() => {
        failMessage();
      })
      .finally(() => {
        done();
      });
  };

  const enableThing = (id: number | string, done: () => void) => {
    updateProfile({id: String(id), enableFlag: 'ENABLE'})
      .then(() => {
        successMessage();
        load();
      })
      .catch(() => {
        failMessage();
      })
      .finally(() => {
        done();
      });
  };

  const deleteThing = (id: number | string, done: () => void) => {
    deleteProfile(String(id))
      .then(() => {
        successMessage();
        load();
      })
      .catch(() => {
        failMessage();
      })
      .finally(() => {
        done();
      });
  };

  const refresh = () => load();

  defineExpose({
    reactiveData,
    refresh,
  });

  load();
</script>
