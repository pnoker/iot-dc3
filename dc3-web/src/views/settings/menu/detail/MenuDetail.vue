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
    <blank-card>
      <el-alert
        v-if="reactiveData.status === 'error'"
        :closable="false"
        :title="$t('common.loadFailed')"
        class="entity-page-error"
        show-icon
        type="error"
      >
        <el-button :loading="reactiveData.loading" link type="danger" @click="load">
          {{ $t('common.retry') }}
        </el-button>
      </el-alert>
      <el-empty
        v-if="reactiveData.status === 'error' && !reactiveData.data.id"
        :description="$t('common.loadFailed')"
      />
      <el-tabs v-else v-model="reactiveData.active" v-loading="reactiveData.loading">
        <el-tab-pane :label="$t('settings.menu.detailTitle')" name="detail">
          <detail-card>
            <el-descriptions :column="isMobile ? 1 : 2" border>
              <el-descriptions-item :label="$t('settings.menu.menuName')">
                {{ reactiveData.data.menuName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.menu.menuCode')">
                {{ reactiveData.data.menuCode }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.menu.menuType')">
                {{ reactiveData.data.menuTypeFlag }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.menu.menuLevel')">
                {{ reactiveData.data.menuLevel }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.menu.menuIndex')">
                {{ reactiveData.data.menuIndex }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.menu.parentMenuId')">
                {{ parentMenuName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.menu.menuIcon')">
                {{ reactiveData.data.menuExt?.content?.icon || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.menu.menuUrl')">
                {{ reactiveData.data.menuExt?.content?.url || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <enable-tag :value="reactiveData.data.enableFlag"/>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.remark')" :span="isMobile ? 1 : 2">
                {{ reactiveData.data.remark || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.createTime')">
                {{ timestampLabel(reactiveData.data.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.operationTime')">
                {{ timestampLabel(reactiveData.data.operateTime) }}
              </el-descriptions-item>
            </el-descriptions>
          </detail-card>
        </el-tab-pane>
      </el-tabs>
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, reactive, watch} from 'vue';
import {useRoute} from 'vue-router';

import {getMenuById} from '@/api/menu';
import {useMenuStore} from '@/store';
import {timestampLabel} from '@/utils/dateUtil';

import blankCard from '@/components/card/blank/BlankCard.vue';
import detailCard from '@/components/card/detail/DetailCard.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import {useBreakpoint} from '@/composables/useBreakpoint';

const route = useRoute();
const menuStore = useMenuStore();
const {isMobile} = useBreakpoint();

const reactiveData = reactive({
  id: route.query.id as string,
  active: (route.query.active as string) || 'detail',
  loading: true,
  status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
  data: {} as Record<string, any>,
});
let requestId = 0;

// parentMenuId on the wire is a raw id. Resolve it against the cached menu
// tree so the descriptions row shows something useful ("Home" vs "1234").
const parentMenuName = computed(() => {
  const pid = reactiveData.data.parentMenuId;
  if (!pid || String(pid) === '0') return '';
  const parent = menuStore.findById(pid);
  return parent?.menuName || String(pid);
});

const load = () => {
  const currentRequestId = ++requestId;
  const menuId = String(reactiveData.id || '');
  if (!menuId) {
    reactiveData.status = 'error';
    reactiveData.loading = false;
    return;
  }
  reactiveData.loading = true;
  reactiveData.status = 'loading';
  reactiveData.data = {};
  getMenuById(menuId)
    .then((res: any) => {
      if (currentRequestId !== requestId || menuId !== String(reactiveData.id || '')) return;
      reactiveData.data = res || {};
      reactiveData.status = reactiveData.data.id ? 'success' : 'error';
    })
    .catch(() => {
      if (currentRequestId === requestId) reactiveData.status = 'error';
    })
    .finally(() => {
      if (currentRequestId === requestId) reactiveData.loading = false;
    });
};

watch(
  () => route.query.id,
  (id) => {
    const nextId = String(id || '');
    if (nextId !== String(reactiveData.id || '')) {
      reactiveData.id = nextId;
      load();
    }
  },
);

onMounted(() => {
  // Ensure the menu tree is primed before parentMenuName runs — Layout
  // usually loads it on startup, but a direct hit to this URL without
  // going through Layout first would leave us with a blank parent.
  menuStore.fetchTree();
  load();
});

onBeforeUnmount(() => {
  requestId += 1;
});
</script>
