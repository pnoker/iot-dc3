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
      <el-tabs v-model="reactiveData.active">
        <el-tab-pane :label="$t('nav.settingsAgenticDetail')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('settings.agentic.label')">
                {{ reactiveData.data.label || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.agentic.model')">
                {{ reactiveData.data.model || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.agentic.provider')">
                {{ reactiveData.data.providerName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.agentic.providerId')">
                {{ reactiveData.data.providerId || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.agentic.capabilities')" :span="2">
                <div class="agentic-tags">
                  <el-tag :type="reactiveData.data.stream ? 'success' : 'info'" size="small">
                    {{ $t('agentic.capStream') }}
                  </el-tag>
                  <el-tag :type="reactiveData.data.toolCall ? 'success' : 'info'" size="small">
                    {{ $t('agentic.capTools') }}
                  </el-tag>
                  <el-tag :type="reactiveData.data.vision ? 'success' : 'info'" size="small">
                    {{ $t('agentic.capVision') }}
                  </el-tag>
                  <el-tag :type="reactiveData.data.reasoning ? 'success' : 'info'" size="small">
                    {{ $t('agentic.capReasoning') }}
                  </el-tag>
                </div>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.agentic.default')">
                <default-tag :value="reactiveData.data.defaultFlag"/>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <enable-tag :value="reactiveData.data.enableFlag"/>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.agentic.temperature')">
                {{ reactiveData.data.temperature ?? '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.agentic.maxTokens')">
                {{ reactiveData.data.maxTokens ?? '-' }}
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

import {listAgenticModelConfigs} from '@/api/agentic';
import BlankCard from '@/components/card/blank/BlankCard.vue';
import DetailCard from '@/components/card/detail/DetailCard.vue';
import DefaultTag from '@/components/tag/DefaultTag.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import type {AgenticModelConfig} from '@/config/types';
import {timestampLabel} from '@/utils/dateUtil';

const route = useRoute();

const reactiveData = reactive({
  id: route.query.id as string,
  active: (route.query.active as string) || 'detail',
  data: {} as AgenticModelConfig,
});

const load = () => {
  if (!reactiveData.id) return;
  listAgenticModelConfigs()
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
.agentic-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
