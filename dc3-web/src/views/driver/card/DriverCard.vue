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
          :enabled="enabled"
          :icon="icon"
          :name="data.driverName"
          :status-title="$t('common.name')"
          @copy-id="copy(data.id, 'Driver ID')"
        >
          <el-tag :type="statusTagType" effect="plain">{{ $t(statusLabelKey) }}</el-tag>
        </things-card-header>
        <div class="things-card__body">
          <div class="things-card-body-content">
            <ul>
              <li class="nowrap-item">
                <el-icon>
                  <Monitor />
                </el-icon>
                {{ $t('driver.card.host') }}: {{ data.serviceHost }}
              </li>
              <li class="nowrap-item">
                <el-icon>
                  <Promotion />
                </el-icon>
                {{ $t('driver.card.driverService') }}: {{ data.serviceName }}
              </li>
              <li class="nowrap-item">
                <el-icon>
                  <Edit />
                </el-icon>
                {{ $t('common.operationTime') }}: {{ timestamp(data.operateTime) }}
              </li>
              <li class="nowrap-item">
                <el-icon>
                  <Sunset />
                </el-icon>
                {{ $t('common.createTime') }}: {{ timestamp(data.createTime) }}
              </li>
            </ul>
          </div>
          <div :title="data.remark || $t('driver.card.remarkTitle')" class="things-card-body-content">
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
  import type {PropType} from 'vue';
  import {computed} from 'vue';
  import {Edit, Monitor, Promotion, Sunset} from '@element-plus/icons-vue';
  import router from '@/config/router';
  import {copy} from '@/utils/commonUtil';
  import {timestamp} from '@/utils/dateUtil';
  import {isEnabledFlag} from '@/utils/thingModelFormatUtil';
  import ThingsCardHeader from '@/components/card/header/ThingsCardHeader.vue';

  const props = defineProps({
    icon: {type: String, default: 'images/common/driver.png'},
    statusTable: {type: Object as PropType<Record<string, string>>, default: () => ({})},
    data: {type: Object as PropType<Record<string, any>>, default: () => ({})},
    footer: {type: Boolean, default: false},
  });

  defineEmits(['select-change']);
  const enabled = computed(() => isEnabledFlag(props.data.enableFlag));

  const status = computed(() => {
    const id = props.data.id;
    return id && props.statusTable[id] ? String(props.statusTable[id]).trim() : 'OFFLINE';
  });

  const statusTagType = computed(() => {
    if (status.value === 'ONLINE') return 'success';
    if (status.value === 'MAINTAIN') return 'warning';
    if (status.value === 'FAULT') return 'danger';
    return 'info';
  });

  const statusLabelKey = computed(() => {
    if (status.value === 'ONLINE') return 'status.online';
    if (status.value === 'MAINTAIN') return 'status.maintain';
    if (status.value === 'FAULT') return 'status.fault';
    return 'status.offline';
  });

  const detail = () => {
    const id = props.data.id;
    if (id) {
      router.push({name: 'driverDetail', query: {id, active: 'detail'}}).catch(() => {
        // nothing to do
      });
    }
  };
</script>

<style lang="scss" scoped>
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
