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
  <div class="things-card">
    <el-card shadow="hover">
      <div class="things-card-content">
        <things-card-header
          :enabled="enabled"
          :icon="icon"
          :name="data.deviceName"
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
                <el-icon>
                  <Promotion/>
                </el-icon>
                {{ $t('device.card.driver') }}: {{ driver.driverName }}
              </li>
              <li class="nowrap-item">
                <el-icon>
                  <Edit/>
                </el-icon>
                {{ $t('common.operationTime') }}: {{ timestamp(data.operateTime) }}
              </li>
              <li class="nowrap-item">
                <el-icon>
                  <Sunset/>
                </el-icon>
                {{ $t('common.createTime') }}: {{ timestamp(data.createTime) }}
              </li>
            </ul>
          </div>
          <div :title="$t('device.card.remarkTitle')" class="things-card-body-content">
            <p class="nowrap-description">
              {{ data.remark || $t('common.noDescription') }}
            </p>
          </div>
        </div>
        <things-card-actions
          v-if="!embedded"
          :delete-title="$t('device.card.confirmDelete')"
          :disable-title="$t('device.card.confirmDisable')"
          :enable-title="$t('device.card.confirmEnable')"
          :enabled="enabled"
          @delete="emitDelete"
          @detail="detail"
          @disable="emitToggle('disable')"
          @edit="edit"
          @enable="emitToggle('enable')"
        />
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, type PropType} from 'vue';
import {Edit, Promotion, Sunset} from '@element-plus/icons-vue';
import router from '@/config/router';
import {copy} from '@/utils/commonUtil';
import {timestamp} from '@/utils/dateUtil';
import {successMessage} from '@/utils/notificationUtil';
import {isEnabledFlag} from '@/utils/thingModelFormatUtil';
import ThingsCardHeader from '@/components/card/header/ThingsCardHeader.vue';
import ThingsCardActions from '@/components/card/actions/ThingsCardActions.vue';

const props = defineProps({
  embedded: {type: Boolean, default: false},
  status: {type: String, default: ''},
  data: {type: Object as PropType<Record<string, any>>, default: () => ({})},
  driver: {type: Object as PropType<Record<string, any>>, default: () => ({})},
  icon: {type: String, default: 'images/common/device.png'},
});

const emit = defineEmits(['disable', 'enable', 'delete']);
const enabled = computed(() => isEnabledFlag(props.data.enableFlag));

const emitToggle = (name: 'disable' | 'enable') => {
  emit(name, props.data.id, props.data.driverId, () => successMessage());
};

const emitDelete = () => {
  emit('delete', props.data.id, () => successMessage());
};

const edit = () => {
  router.push({name: 'deviceEdit', query: {id: props.data.id, active: 'deviceConfig'}}).catch(() => {
    // nothing to do
  });
};

const detail = () => {
  router.push({name: 'deviceDetail', query: {id: props.data.id, active: 'detail'}}).catch(() => {
    // nothing to do
  });
};
</script>
