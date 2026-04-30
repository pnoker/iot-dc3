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
  <div class="things-card">
    <el-card shadow="hover">
      <div class="things-card-content">
        <things-card-header
          :name="data.deviceName"
          :icon="icon"
          :enabled="data.enableFlag === 'ENABLE'"
          :status-title="$t('common.enableFlag')"
          @copy-id="copy(data.id, 'Device ID')"
        >
          <el-tag v-if="status === 'ONLINE'" effect="plain" type="success">{{ $t('status.online') }}</el-tag>
          <el-tag v-else-if="status === 'MAINTAIN'" effect="plain" type="warning">{{ $t('status.maintain') }}</el-tag>
          <el-tag v-else-if="status === 'FAULT'" effect="plain" type="danger">{{ $t('status.fault') }}</el-tag>
          <el-tag v-else-if="status === 'DISABLE'" effect="plain" type="info">{{ $t('status.disable') }}</el-tag>
          <el-tag v-else effect="plain" type="info">{{ $t('status.offline') }}</el-tag>
        </things-card-header>
        <div class="things-card__body">
          <div class="things-card-body-content">
            <ul>
              <li class="nowrap-item">
                <el-icon><Promotion /></el-icon>
                {{ $t('device.card.driver') }}: {{ driver.driverName }}
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
          <div class="things-card-body-content" :title="$t('device.card.remarkTitle')">
            <p class="nowrap-description">
              {{ data.remark || $t('common.noDescription') }}
            </p>
          </div>
        </div>
        <things-card-actions
          v-if="!embedded"
          :enabled="data.enableFlag === 'ENABLE'"
          :disable-title="$t('device.card.confirmDisable')"
          :enable-title="$t('device.card.confirmEnable')"
          :delete-title="$t('device.card.confirmDelete')"
          @disable="emitToggle('disable-thing')"
          @enable="emitToggle('enable-thing')"
          @delete="emitDelete"
          @edit="edit"
          @detail="detail"
        />
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
  import type { PropType } from 'vue';
  import { Edit, Promotion, Sunset } from '@element-plus/icons-vue';
  import router from '@/config/router';
  import { copy } from '@/utils/CommonUtil';
  import { timestamp } from '@/utils/DateUtil';
  import { successMessage } from '@/utils/NotificationUtil';
  import ThingsCardHeader from '@/components/card/header/ThingsCardHeader.vue';
  import ThingsCardActions from '@/components/card/actions/ThingsCardActions.vue';

  const props = defineProps({
    embedded: { type: Boolean, default: false },
    status: { type: String, default: '' },
    data: { type: Object as PropType<Record<string, any>>, default: () => ({}) },
    driver: { type: Object as PropType<Record<string, any>>, default: () => ({}) },
    icon: { type: String, default: 'images/common/device.png' },
  });

  const emit = defineEmits(['disable-thing', 'enable-thing', 'delete-thing']);

  const emitToggle = (name: 'disable-thing' | 'enable-thing') => {
    emit(name, props.data.id, props.data.driverId, () => successMessage());
  };

  const emitDelete = () => {
    emit('delete-thing', props.data.id, () => successMessage());
  };

  const edit = () => {
    router.push({ name: 'deviceEdit', query: { id: props.data.id, active: '0' } }).catch(() => {
      // nothing to do
    });
  };

  const detail = () => {
    router.push({ name: 'deviceDetail', query: { id: props.data.id, active: 'detail' } }).catch(() => {
      // nothing to do
    });
  };
</script>

<style lang="scss" scoped>
  @use '@/styles/things-card.scss';
</style>
