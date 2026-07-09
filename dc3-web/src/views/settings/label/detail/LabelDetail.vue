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
        <el-tab-pane :label="$t('settings.label.detailTitle')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('settings.label.labelName')">
                {{ reactiveData.data.labelName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.label.labelCode')">
                {{ reactiveData.data.labelCode || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.common.entityType')">
                {{ reactiveData.data.entityTypeFlag || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.label.labelColor')">
                <span class="label-color">
                  <span
                    :style="{backgroundColor: reactiveData.data.labelColor || '#F4F4F5'}"
                    class="label-color__swatch"
                  />
                  <span>{{ reactiveData.data.labelColor || '-' }}</span>
                </span>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <enable-tag :value="reactiveData.data.enableFlag"/>
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
import {onMounted, reactive} from 'vue';
import {useRoute} from 'vue-router';

import {getLabelById} from '@/api/label';
import BlankCard from '@/components/card/blank/BlankCard.vue';
import DetailCard from '@/components/card/detail/DetailCard.vue';
import EnableTag from '@/components/tag/EnableTag.vue';
import {timestampLabel} from '@/utils/dateUtil';

const route = useRoute();

const reactiveData = reactive({
  id: route.query.id as string,
  active: (route.query.active as string) || 'detail',
  data: {} as Record<string, any>,
});

const load = () => {
  if (!reactiveData.id) return;
  getLabelById(reactiveData.id)
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

<style lang="scss" scoped>
.label-color {
  display: inline-flex;
  align-items: center;
  gap: 8px;

  &__swatch {
    width: 16px;
    height: 16px;
    border: 1px solid var(--el-border-color);
    border-radius: 4px;
  }
}
</style>
