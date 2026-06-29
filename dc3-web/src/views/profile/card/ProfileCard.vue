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
          :name="data.profileName"
          :status-title="$t('common.name')"
          @copy-id="copy(data.id, $t('profile.card.profileId'))"
        />
        <div class="things-card__body">
          <div class="things-card-body-content">
            <ul>
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
          <div :title="$t('profile.card.remarkTitle')" class="things-card-body-content">
            <p class="nowrap-description">
              {{ data.remark || $t('common.noDescription') }}
            </p>
          </div>
        </div>
        <things-card-actions
          v-if="!embedded"
          :delete-title="$t('profile.card.confirmDelete')"
          :disable-title="$t('profile.card.confirmDisable')"
          :enable-title="$t('profile.card.confirmEnable')"
          :enabled="enabled"
          @delete="emitAction('delete-thing')"
          @detail="detail"
          @disable="emitAction('disable-thing')"
          @edit="edit"
          @enable="emitAction('enable-thing')"
        />
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
  import {computed, type PropType} from 'vue';
  import {Edit, Sunset} from '@element-plus/icons-vue';
  import router from '@/config/router';
  import {copy} from '@/utils/commonUtil';
  import {timestamp} from '@/utils/dateUtil';
  import {successMessage} from '@/utils/notificationUtil';
  import {isEnabledFlag} from '@/utils/thingModelFormatUtil';
  import ThingsCardHeader from '@/components/card/header/ThingsCardHeader.vue';
  import ThingsCardActions from '@/components/card/actions/ThingsCardActions.vue';

  const props = defineProps({
    embedded: {type: Boolean, default: false},
    data: {type: Object as PropType<Record<string, any>>, default: () => ({})},
    icon: {type: String, default: 'images/common/profile.png'},
  });

  const emit = defineEmits(['disable-thing', 'enable-thing', 'delete-thing']);
  const enabled = computed(() => isEnabledFlag(props.data.enableFlag));

  const emitAction = (name: 'disable-thing' | 'enable-thing' | 'delete-thing') => {
    emit(name, props.data.id, () => successMessage());
  };

  const edit = () => {
    router.push({name: 'profileEdit', query: {id: props.data.id, active: 'profileConfig'}}).catch(() => {
      // nothing to do
    });
  };

  const detail = () => {
    router.push({name: 'profileDetail', query: {id: props.data.id, active: 'detail'}}).catch(() => {
      // nothing to do
    });
  };
</script>
