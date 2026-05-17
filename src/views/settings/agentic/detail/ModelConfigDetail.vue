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
        <el-tab-pane :label="$t('nav.settingsAgenticDetail')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="Label">{{ reactiveData.data.label || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Model">{{ reactiveData.data.model || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Provider">{{ reactiveData.data.providerName || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Provider ID">{{ reactiveData.data.providerId || '-' }}</el-descriptions-item>
              <el-descriptions-item label="Capabilities" :span="2">
                <div class="agentic-tags">
                  <el-tag :type="reactiveData.data.stream ? 'success' : 'info'" size="small">Stream</el-tag>
                  <el-tag :type="reactiveData.data.toolCall ? 'success' : 'info'" size="small">Tools</el-tag>
                  <el-tag :type="reactiveData.data.vision ? 'success' : 'info'" size="small">Vision</el-tag>
                  <el-tag :type="reactiveData.data.reasoning ? 'success' : 'info'" size="small">Reasoning</el-tag>
                </div>
              </el-descriptions-item>
              <el-descriptions-item label="Default">
                <el-tag :type="reactiveData.data.defaultFlag === 'DEFAULT' ? 'success' : 'info'">
                  {{ reactiveData.data.defaultFlag === 'DEFAULT' ? $t('common.yes') : $t('common.no') }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <el-tag :type="reactiveData.data.enableFlag === 'ENABLE' ? 'success' : 'info'">
                  {{ reactiveData.data.enableFlag === 'ENABLE' ? $t('common.enable') : $t('common.disable') }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="Temperature">
                {{ reactiveData.data.temperature ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="Max Tokens">
                {{ reactiveData.data.maxTokens ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.remark')" :span="2">
                {{ reactiveData.data.remark || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.createTime')">
                {{ reactiveData.data.createTime ? timestamp(reactiveData.data.createTime) : '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.operationTime')">
                {{ reactiveData.data.operateTime ? timestamp(reactiveData.data.operateTime) : '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </detail-card>
        </el-tab-pane>
      </el-tabs>
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, reactive } from 'vue';
  import { useRoute } from 'vue-router';

  import { getAgenticModelConfigs } from '@/api/agentic';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import DetailCard from '@/components/card/detail/DetailCard.vue';
  import type { AgenticModelConfig } from '@/config/types';
  import { timestamp } from '@/utils/dateUtil';

  const route = useRoute();

  const reactiveData = reactive({
    id: route.query.id as string,
    active: (route.query.active as string) || 'detail',
    data: {} as AgenticModelConfig,
  });

  const load = () => {
    if (!reactiveData.id) return;
    getAgenticModelConfigs()
      .then((res) => {
        reactiveData.data =
          (res.data || []).find((item) => String(item.id) === reactiveData.id) || ({} as AgenticModelConfig);
      })
      .catch(() => {
        // handled globally
      });
  };

  onMounted(() => {
    load();
  });
</script>

<style lang="scss" scoped>
  @use '@/styles/things-card.scss';

  .agentic-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
</style>
