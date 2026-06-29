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
          :enabled="enabled"
          :icon="icon"
          :name="data.pointName"
          :status-title="$t('common.name')"
          @copy-id="copy(data.id, $t('point.card.profile'))"
        />
        <div class="things-card__body">
          <div class="things-card-body-content">
            <ul class="things-body-content-item-column-2">
              <li class="nowrap-item">
                <span
                  ><el-icon><List /></el-icon> {{ $t('point.card.profile') }}:
                </span>
                {{ profile.profileName }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('point.card.ratio') }}:
                </span>
                {{ data.multiple }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('point.card.accuracy') }}:
                </span>
                {{ data.valueDecimal }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('point.card.unit') }}:
                </span>
                {{ data.unit }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Edit /></el-icon> {{ $t('common.operationTime') }}:
                </span>
                {{ timestamp(data.operateTime) }}
              </li>
            </ul>
            <ul class="things-body-content-item-column-2">
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('point.card.dataType') }}:
                </span>
                {{ $t(pointTypeKey(data.pointTypeFlag)) }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('point.card.baseValue') }}:
                </span>
                {{ data.baseValue }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('pointValue.card.processedValue') }}:
                </span>
                Y = {{ data.multiple }}X + {{ data.baseValue }}, {{ data.valueDecimal }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Location /></el-icon> {{ $t('point.card.rw') }}:
                </span>
                {{ $t(rwFlagKey(data.rwFlag)) }}
              </li>
              <li class="nowrap-item">
                <span
                  ><el-icon><Sunset /></el-icon> {{ $t('common.createTime') }}:
                </span>
                {{ timestamp(data.createTime) }}
              </li>
            </ul>
          </div>
          <div :title="$t('point.add.description')" class="things-card-body-content">
            <p class="nowrap-description">
              {{ data.remark || $t('common.noDescription') }}
            </p>
          </div>
        </div>
        <things-card-actions
          v-if="!embedded"
          :delete-title="$t('point.card.confirmDelete')"
          :disable-title="$t('point.card.confirmDisable')"
          :enable-title="$t('point.card.confirmEnable')"
          :enabled="enabled"
          @delete="emitDelete"
          @detail="emit('detail', data)"
          @disable="emitToggle('disable')"
          @edit="emit('edit', data)"
          @enable="emitToggle('enable')"
        />
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
  import {computed, type PropType} from 'vue';
  import {Edit, List, Location, Sunset} from '@element-plus/icons-vue';
  import {copy} from '@/utils/commonUtil';
  import {timestamp} from '@/utils/dateUtil';
  import {successMessage} from '@/utils/notificationUtil';
  import {pointTypeKey, rwFlagKey} from '@/utils/pointFormatUtil';
  import {isEnabledFlag} from '@/utils/thingModelFormatUtil';
  import ThingsCardHeader from '@/components/card/header/ThingsCardHeader.vue';
  import ThingsCardActions from '@/components/card/actions/ThingsCardActions.vue';

  const props = defineProps({
    embedded: {type: Boolean, default: false},
    data: {type: Object as PropType<Record<string, any>>, default: () => ({})},
    profile: {type: Object as PropType<Record<string, any>>, default: () => ({})},
    icon: {type: String, default: 'images/common/point.png'},
  });

  const emit = defineEmits(['disable', 'enable', 'delete', 'edit', 'detail']);
  const enabled = computed(() => isEnabledFlag(props.data.enableFlag));

  const emitToggle = (name: 'disable' | 'enable') => {
    emit(name, props.data.id, props.data.profileId, () => successMessage());
  };

  const emitDelete = () => {
    emit('delete', props.data.id, () => successMessage());
  };
</script>

<style lang="scss" scoped>
  // PointCard 用双栏列表展示字段,200px 固定宽度是为了和卡片尺寸匹配。
  .things-body-content-item-column-2 {
    width: 200px;
  }
</style>
