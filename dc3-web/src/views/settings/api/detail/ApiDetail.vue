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
        <el-tab-pane :label="$t('settings.api.detailTitle')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('settings.api.apiName')">
                {{ reactiveData.data.apiName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.api.apiCode')">
                {{ reactiveData.data.apiCode }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.api.apiGroup')">
                {{ reactiveData.data.apiGroup || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.api.serviceName')">
                {{ reactiveData.data.serviceName }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.api.apiType')">
                {{ reactiveData.data.apiTypeFlag }}
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
  import {onMounted, reactive} from 'vue';
  import {useRoute} from 'vue-router';

  import {getApiById} from '@/api/api';
  import {timestampLabel} from '@/utils/dateUtil';

  import blankCard from '@/components/card/blank/BlankCard.vue';
  import detailCard from '@/components/card/detail/DetailCard.vue';
  import EnableTag from '@/components/tag/EnableTag.vue';

  const route = useRoute();

  const reactiveData = reactive({
    id: route.query.id as string,
    active: (route.query.active as string) || 'detail',
    data: {} as Record<string, any>,
  });

  const load = () => {
    if (!reactiveData.id) return;
    getApiById(reactiveData.id)
      .then((res: any) => {
        reactiveData.data = res.data || {};
      })
      .catch(() => {
        // handled globally
      });
  };

  onMounted(() => {
    load();
  });
</script>
