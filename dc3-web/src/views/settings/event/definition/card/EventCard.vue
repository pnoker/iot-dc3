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
          :name="data.eventName"
          :status-title="$t('eventDefinition.card.level')"
          @copy-id="copy(data.id, 'Event ID')"
        >
          <el-tag v-if="!embedded" :type="eventLevelTag(data.eventLevelFlag)">
            {{ eventLevelLabel(data.eventLevelFlag) }}
          </el-tag>
        </things-card-header>
        <div class="things-card__body">
          <div class="things-card-body-content">
            <ul class="things-body-content-item-column-2">
              <li class="nowrap-item">
                <span>
                  <el-icon><List/></el-icon> {{ $t('eventDefinition.card.code') }}:
                </span>
                {{ data.eventCode || '-' }}
              </li>
              <li class="nowrap-item">
                <span>
                  <el-icon><Location/></el-icon> {{ $t('eventDefinition.card.type') }}:
                </span>
                {{ eventTypeLabel(data.eventTypeFlag) }}
              </li>
              <li class="nowrap-item">
                <span>
                  <el-icon><Location/></el-icon> {{ $t('eventDefinition.card.level') }}:
                </span>
                {{ eventLevelLabel(data.eventLevelFlag) }}
              </li>
            </ul>
            <ul class="things-body-content-item-column-2">
              <li class="nowrap-item">
                <span>
                  <el-icon><Location/></el-icon> {{ $t('common.enableFlag') }}:
                </span>
                {{ enabled ? $t('common.enable') : $t('common.disable') }}
              </li>
              <li class="nowrap-item">
                <span>
                  <el-icon><Edit/></el-icon> {{ $t('common.operationTime') }}:
                </span>
                {{ timestamp(data.operateTime || '') }}
              </li>
              <li class="nowrap-item">
                <span>
                  <el-icon><Sunset/></el-icon> {{ $t('common.createTime') }}:
                </span>
                {{ timestamp(data.createTime || '') }}
              </li>
            </ul>
          </div>
          <div :title="$t('eventDefinition.card.remarkTitle')" class="things-card-body-content">
            <p class="nowrap-description">
              {{ data.remark || $t('common.noDescription') }}
            </p>
          </div>
        </div>
        <things-card-actions
          v-if="!embedded"
          :delete-title="$t('eventDefinition.card.confirmDelete')"
          :disable-title="$t('eventDefinition.card.confirmDisable')"
          :enable-title="$t('eventDefinition.card.confirmEnable')"
          :enabled="enabled"
          @delete="emitDelete"
          @detail="$emit('detail-thing', data)"
          @disable="emitToggle('disable-thing')"
          @edit="$emit('edit-thing', data)"
          @enable="emitToggle('enable-thing')"
        />
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {computed} from 'vue';
import {Edit, List, Location, Sunset} from '@element-plus/icons-vue';
import {copy} from '@/utils/commonUtil';
import {timestamp} from '@/utils/dateUtil';
import {successMessage} from '@/utils/notificationUtil';
import {eventLevelLabel, eventLevelTag, eventTypeLabel, isEnabledFlag} from '@/utils/thingModelFormatUtil';
import ThingsCardHeader from '@/components/card/header/ThingsCardHeader.vue';
import ThingsCardActions from '@/components/card/actions/ThingsCardActions.vue';
import type {EventRecord} from '@/config/types';

const props = defineProps({
  embedded: {type: Boolean, default: false},
  data: {type: Object as PropType<EventRecord>, default: () => ({})},
  icon: {type: String, default: 'images/common/event.png'},
});

const emit = defineEmits(['disable-thing', 'enable-thing', 'delete-thing', 'edit-thing', 'detail-thing']);

const enabled = computed(() => isEnabledFlag(props.data.enableFlag));

const emitToggle = (name: 'disable-thing' | 'enable-thing') => {
  emit(name, props.data.id, props.data.profileId, () => successMessage());
};

const emitDelete = () => {
  emit('delete-thing', props.data.id, () => successMessage());
};
</script>

<style lang="scss" scoped>
.things-body-content-item-column-2 {
  max-width: 200px;
}
</style>
