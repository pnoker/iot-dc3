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
  <div class="things-card" @click="$emit('select-change', data)">
    <el-card shadow="hover">
      <div class="things-card-content">
        <things-card-header
          :name="data.driverName"
          :icon="icon"
          :enabled="data.enableFlag === 'ENABLE'"
          :status-title="$t('common.name')"
          @copy-id="copy(data.id, 'Driver ID')"
        >
          <el-tag v-if="status === 'ONLINE'" effect="plain" type="success">{{ $t('status.online') }}</el-tag>
          <el-tag v-else-if="status === 'MAINTAIN'" effect="plain" type="warning">{{ $t('status.maintain') }}</el-tag>
          <el-tag v-else-if="status === 'FAULT'" effect="plain" type="danger">{{ $t('status.fault') }}</el-tag>
          <el-tag v-else-if="status === 'DISABLE'" effect="plain" type="info">{{ $t('status.disable') }}</el-tag>
          <el-tag v-else-if="status === 'REGISTERING'" effect="plain" type="info">
            {{ $t('status.registering') }}
          </el-tag>
          <el-tag v-else effect="plain" type="info">{{ $t('status.offline') }}</el-tag>
        </things-card-header>
        <div class="things-card__body">
          <div class="things-card-body-content">
            <ul>
              <li class="nowrap-item">
                <el-icon><Monitor /></el-icon>
                {{ $t('driver.card.host') }}: {{ data.serviceHost }}
              </li>
              <li class="nowrap-item">
                <el-icon><Promotion /></el-icon>
                {{ $t('driver.card.driverService') }}: {{ data.serviceName }}
              </li>
              <li class="nowrap-item">
                <el-icon><Edit /></el-icon>
                {{ $t('common.operationTime') }}: {{ timestamp(data.operateTime) }}
              </li>
              <li class="nowrap-item">
                <el-icon><Sunset /></el-icon>
                {{ $t('common.createTime') }}: {{ timestamp(data.createTime) }}
              </li>
            </ul>
          </div>
          <div class="things-card-body-content" :title="data.remark || $t('driver.card.remarkTitle')">
            <p class="nowrap-description">
              {{ data.remark || $t('common.noDescription') }}
            </p>
          </div>
        </div>
        <div v-if="!footer" class="things-card__footer">
          <div class="things-card-footer-operation">
            <el-button link type="primary" @click.stop="detail">{{ $t('common.detail') }}</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
  import type { PropType } from 'vue';
  import { computed } from 'vue';
  import { Edit, Monitor, Promotion, Sunset } from '@element-plus/icons-vue';
  import router from '@/config/router';
  import { copy } from '@/utils/CommonUtil';
  import { timestamp } from '@/utils/DateUtil';
  import ThingsCardHeader from '@/components/card/header/ThingsCardHeader.vue';

  const props = defineProps({
    icon: { type: String, default: 'images/common/driver.png' },
    statusTable: { type: Object as PropType<Record<string, string>>, default: () => ({}) },
    data: { type: Object as PropType<Record<string, any>>, default: () => ({}) },
    footer: { type: Boolean, default: false },
  });

  defineEmits(['select-change']);

  const status = computed(() => {
    const id = props.data.id;
    return id && props.statusTable[id] ? props.statusTable[id] : '';
  });

  const detail = () => {
    const id = props.data.id;
    if (id) {
      router.push({ name: 'driverDetail', query: { id, active: 'detail' } }).catch(() => {
        // nothing to do
      });
    }
  };
</script>

<style lang="scss" scoped>
  @use '@/styles/things-card.scss';
  @use '@/views/driver/card/style.scss';

  // DriverCard 的 footer 只有单个 detail 按钮,不使用 ThingsCardActions,在此补齐样式。
  .things-card__footer {
    height: 35px;
    margin-top: 2px;
    display: flex;
    justify-content: flex-end;
    border-top: 1px solid #dcdfe6;

    .things-card-footer-operation {
      height: 35px;
      display: flex;
    }
  }
</style>
