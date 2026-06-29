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
    <blank-card>
      <el-tabs v-model="reactiveData.active">
        <el-tab-pane :label="$t('settings.menu.detailTitle')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
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
                <enable-tag :value="reactiveData.data.enableFlag" />
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.remark')" :span="2">
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
  import {computed, onMounted, reactive} from 'vue';
  import {useRoute} from 'vue-router';

  import {getMenuById} from '@/api/menu';
  import {useMenuStore} from '@/store';
  import {timestampLabel} from '@/utils/dateUtil';

  import blankCard from '@/components/card/blank/BlankCard.vue';
  import detailCard from '@/components/card/detail/DetailCard.vue';
  import EnableTag from '@/components/tag/EnableTag.vue';

  const route = useRoute();
  const menuStore = useMenuStore();

  const reactiveData = reactive({
    id: route.query.id as string,
    active: (route.query.active as string) || 'detail',
    data: {} as Record<string, any>,
  });

  // parentMenuId on the wire is a raw id. Resolve it against the cached menu
  // tree so the descriptions row shows something useful ("Home" vs "1234").
  const parentMenuName = computed(() => {
    const pid = reactiveData.data.parentMenuId;
    if (!pid || String(pid) === '0') return '';
    const parent = menuStore.findById(pid);
    return parent?.menuName || String(pid);
  });

  const load = () => {
    if (!reactiveData.id) return;
    getMenuById(reactiveData.id)
      .then((res: any) => {
        reactiveData.data = res.data || {};
      })
      .catch(() => {
        // handled globally
      });
  };

  onMounted(() => {
    // Ensure the menu tree is primed before parentMenuName runs — Layout
    // usually loads it on startup, but a direct hit to this URL without
    // going through Layout first would leave us with a blank parent.
    menuStore.fetchTree();
    load();
  });
</script>
