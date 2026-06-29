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
        <el-tab-pane :label="$t('nav.settingsAgenticProviderDetail')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('settings.agentic.providerName')">
                {{ reactiveData.data.name || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.agentic.providerType')">
                {{ reactiveData.data.providerType || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.agentic.baseUrl')" :span="2">
                {{ reactiveData.data.baseUrl || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.agentic.default')">
                <default-tag :value="reactiveData.data.defaultFlag" />
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <enable-tag :value="reactiveData.data.enableFlag" />
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.remark')" :span="2">
                {{ reactiveData.data.remark || '-' }}
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

  import {listAgenticProviders} from '@/api/agentic';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import DetailCard from '@/components/card/detail/DetailCard.vue';
  import DefaultTag from '@/components/tag/DefaultTag.vue';
  import EnableTag from '@/components/tag/EnableTag.vue';
  import type {AgenticProvider} from '@/config/types';

  const route = useRoute();

  const reactiveData = reactive({
    id: route.query.id as string,
    active: (route.query.active as string) || 'detail',
    data: {} as AgenticProvider,
  });

  const load = () => {
    if (!reactiveData.id) return;
    listAgenticProviders()
      .then((res) => {
        reactiveData.data =
          (res.data || []).find((item) => String(item.id) === reactiveData.id) || ({} as AgenticProvider);
      })
      .catch(() => {
        // handled globally
      });
  };

  onMounted(() => {
    load();
  });
</script>
