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
                <default-tag :value="reactiveData.data.defaultFlag"/>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <enable-tag :value="reactiveData.data.enableFlag"/>
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
