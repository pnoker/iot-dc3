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
    <base-card>
      <template #header>
        <span>Event Definition Detail</span>
      </template>
      <detail-card v-loading="loading">
        <el-descriptions v-if="record" :column="2" border>
          <el-descriptions-item :label="$t('common.name')">{{ record.eventName }}</el-descriptions-item>
          <el-descriptions-item label="Code">{{ record.eventCode }}</el-descriptions-item>
          <el-descriptions-item label="Event Type">{{ record.eventTypeFlag }}</el-descriptions-item>
          <el-descriptions-item label="Event Level">{{ record.eventLevelFlag }}</el-descriptions-item>
          <el-descriptions-item :label="$t('common.enableFlag')">
            <el-tag
              :type="String(record.enableFlag) === 'ENABLE' || Number(record.enableFlag) === 0 ? 'success' : 'info'"
            >
              {{
                String(record.enableFlag) === 'ENABLE' || Number(record.enableFlag) === 0
                  ? $t('common.enable')
                  : $t('common.disable')
              }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Profile ID">{{ record.profileId || '-' }}</el-descriptions-item>
          <el-descriptions-item :label="$t('common.remark')" :span="2">{{ record.remark || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Tenant ID">{{ record.tenantId || '-' }}</el-descriptions-item>
          <el-descriptions-item label="Signature">{{ record.signature || '-' }}</el-descriptions-item>
          <el-descriptions-item :formatter="timestampColumn" label="Create Time">{{
            record.createTime
          }}</el-descriptions-item>
          <el-descriptions-item :formatter="timestampColumn" label="Operate Time">{{
            record.operateTime
          }}</el-descriptions-item>
        </el-descriptions>
        <el-empty v-else :description="$t('common.description')" />
      </detail-card>
    </base-card>
  </div>
</template>

<script lang="ts" setup>
  import { ref, onMounted } from 'vue';
  import { useRoute } from 'vue-router';
  import { getEventById } from '@/api/event';
  import { timestampColumn } from '@/utils/dateUtil';
  import type { EventRecord } from '@/config/types';
  import BaseCard from '@/components/card/base/BaseCard.vue';
  import DetailCard from '@/components/card/detail/DetailCard.vue';

  const route = useRoute();
  const record = ref<EventRecord | null>(null);
  const loading = ref(false);

  onMounted(() => {
    const id = route.query.id as string;
    if (id) {
      loading.value = true;
      getEventById(id)
        .then((res) => {
          record.value = res.data || null;
        })
        .finally(() => {
          loading.value = false;
        });
    }
  });
</script>
