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
        <el-tab-pane :label="$t('settings.group.detailTitle')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('settings.group.groupName')">
                {{ reactiveData.data.groupName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.group.groupCode')">
                {{ reactiveData.data.groupCode || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.common.entityType')">
                {{ reactiveData.data.groupTypeFlag || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.group.parentGroupId')">
                {{ parentName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.group.groupLevel')">
                {{ reactiveData.data.groupLevel ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.group.groupIndex')">
                {{ reactiveData.data.groupIndex ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <enable-tag :value="reactiveData.data.enableFlag" />
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.remark')" :span="2">
                {{ reactiveData.data.remark || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.creatorName')">
                {{ reactiveData.data.creatorName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.createTime')">
                {{ timestampLabel(reactiveData.data.createTime) }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.operatorName')">
                {{ reactiveData.data.operatorName || '-' }}
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
  import {useI18n} from 'vue-i18n';
  import {useRoute} from 'vue-router';

  import {getGroupById, listGroup} from '@/api/group';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import DetailCard from '@/components/card/detail/DetailCard.vue';
  import EnableTag from '@/components/tag/EnableTag.vue';
  import {timestampLabel} from '@/utils/dateUtil';

  const route = useRoute();
  const {t} = useI18n();

  const reactiveData = reactive({
    id: route.query.id as string,
    active: (route.query.active as string) || 'detail',
    data: {} as Record<string, any>,
    parentMap: {} as Record<string, string>,
  });

  const parentName = computed(() => {
    const id = reactiveData.data.parentGroupId;
    if (!id || String(id) === '0') return t('settings.group.rootGroup');
    return reactiveData.parentMap[String(id)] || String(id);
  });

  const loadParents = () => {
    listGroup({page: {current: 1, size: 5000}})
      .then((res: any) => {
        const records = res.data?.records || [];
        records.forEach((row: Record<string, any>) => {
          reactiveData.parentMap[String(row.id)] = row.groupName || String(row.id);
        });
      })
      .catch(() => {
        // handled globally
      });
  };

  const load = () => {
    if (!reactiveData.id) return;
    getGroupById(reactiveData.id)
      .then((res: any) => {
        reactiveData.data = res.data || {};
      })
      .catch(() => {
        // handled globally
      });
  };

  onMounted(() => {
    loadParents();
    load();
  });
</script>
