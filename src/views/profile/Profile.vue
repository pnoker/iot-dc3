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
        <el-col v-for="data in 12" :key="data" :lg="8" :md="12" :sm="12" :xl="6" :xs="24">
          <skeleton-card :footer="true" :loading="reactiveData.loading"></skeleton-card>
        </el-col>
        <el-col v-if="hasData">
          <el-empty :description="$t('profile.empty')"></el-empty>
        </el-col>
        <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="8" :md="12" :sm="12" :xl="6" :xs="24">
          <profile-card
            :data="data"
            :embedded="embedded != ''"
            @disable-thing="disableThing"
            @enable-thing="enableThing"
            @delete-thing="deleteThing"
          ></profile-card>
        </el-col>
      </el-row>
    </blank-card>

    <profile-add-form ref="profileAddFormRef" @add-thing="addThing"></profile-add-form>
  </div>
</template>

<script lang="ts" setup>
  import { computed, reactive, ref } from 'vue';

  import { addProfile, deleteProfile, getProfileList, updateProfile } from '@/api/profile';

  import type { Order } from '@/config/entity';

  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import SkeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
  import { failMessage } from '@/utils/NotificationUtil';
  import { isNull } from '@/utils/ValidationUtil';
  import ProfileAddForm from '@/views/profile/add/ProfileAddForm.vue';
  import ProfileCard from '@/views/profile/card/ProfileCard.vue';
  import ProfileTool from '@/views/profile/tool/ProfileTool.vue';

  interface ProfileListItem {
    id: number | string;
    [key: string]: unknown;
  }

  interface ProfileListPage {
    total: number;
    records: ProfileListItem[];
  }

  interface ProfileQuery extends Record<string, unknown> {
    deviceId?: string;
  }

  type ProfileListResponse = R<ProfileListPage>;
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

  const reactiveData = reactive({
    loading: true,
    listData: [] as ProfileListItem[],
    query: {} as ProfileQuery,
    order: false,
    page: {
      total: 0,
      size: 12,
      current: 1,
      orders: [] as Order[],
    },
  });

  const hasData = computed(() => !reactiveData.loading && reactiveData.listData.length < 1);

  const withFixedQuery = (params: ProfileQuery = {}) => {
    const nextQuery: ProfileQuery = { ...params };

    if (!isNull(props.deviceId)) {
      nextQuery.deviceId = props.deviceId;
    }

    return nextQuery;
  };

  const list = () => {
    const query = withFixedQuery(reactiveData.query);
    reactiveData.query = query;

    getProfileList<ProfileListResponse>({
      page: reactiveData.page,
      ...query,
    })
      .then((res) => {
        const data = res.data;
        reactiveData.page.total = data.total;
        reactiveData.listData = data.records;
      })
      .catch(() => {
        // nothing to do
      })
      .finally(() => {
        reactiveData.loading = false;
      });
  };

  const search = (params: ProfileQuery) => {
    reactiveData.query = withFixedQuery(params);
    list();
  };

  const reset = () => {
    reactiveData.query = withFixedQuery();
    list();
  };

  const showAdd = () => {
    profileAddFormRef.value?.show();
  };

  const addThing = (form: unknown, done: () => void) => {
    addProfile(form).then(() => {
      list();
      done();
    });
  };

  const disableThing = (id: number | string, done: () => void) => {
    updateProfile({ id, enableFlag: 'DISABLE' }).then(() => {
      list();
      done();
    });
  };

  const enableThing = (id: number | string, done: () => void) => {
    updateProfile({ id, enableFlag: 'ENABLE' }).then(() => {
      list();
      done();
    });
  };

  const deleteThing = (id: number | string, done: () => void) => {
    deleteProfile(String(id))
      .then((res) => {
        if (res.data.ok) {
          list();
          done();
        } else {
          failMessage(res.data.message);
        }
      })
      .catch(() => {
        // nothing to do
      });
  };

  const refresh = () => {
    list();
  };

  const sort = () => {
    reactiveData.order = !reactiveData.order;
    if (reactiveData.order) {
      reactiveData.page.orders = [{ column: 'create_time', asc: true }];
    } else {
      reactiveData.page.orders = [{ column: 'create_time', asc: false }];
    }
    list();
  };

  const sizeChange = (size: number) => {
    reactiveData.page.size = size;
    list();
  };

  const currentChange = (current: number) => {
    reactiveData.page.current = current;
    list();
  };

  list();
</script>

<style lang="scss" scoped></style>
