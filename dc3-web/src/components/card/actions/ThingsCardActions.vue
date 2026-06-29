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
  <div class="things-card__footer">
    <div class="things-card-footer-operation">
      <el-popconfirm
        :icon="SwitchButton"
        :title="disableTitle"
        icon-color="#e6a23c"
        placement="top"
        @confirm="$emit('disable')"
      >
        <template #reference>
          <el-button :disabled="!enabled" link type="primary">
            {{ t('common.disable') }}
          </el-button>
        </template>
      </el-popconfirm>
      <el-popconfirm
        :icon="CircleCheck"
        :title="enableTitle"
        icon-color="#67c23a"
        placement="top"
        @confirm="$emit('enable')"
      >
        <template #reference>
          <el-button :disabled="enabled" link type="primary">
            {{ t('common.enable') }}
          </el-button>
        </template>
      </el-popconfirm>
      <el-popconfirm
        :icon="CircleClose"
        :title="deleteTitle"
        icon-color="#f56c6c"
        placement="top"
        @confirm="$emit('delete')"
      >
        <template #reference>
          <el-button link type="primary">{{ t('common.delete') }}</el-button>
        </template>
      </el-popconfirm>
      <el-button link type="primary" @click="$emit('edit')">
        {{ t('common.edit') }}
      </el-button>
      <el-button :disabled="detailDisabled" link type="primary" @click="$emit('detail')">
        {{ t('common.detail') }}
      </el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import {CircleCheck, CircleClose, SwitchButton} from '@element-plus/icons-vue';
  import {useI18n} from 'vue-i18n';

  defineProps({
    enabled: {type: Boolean, required: true},
    disableTitle: {type: String, required: true},
    enableTitle: {type: String, required: true},
    deleteTitle: {type: String, required: true},
    detailDisabled: {type: Boolean, default: false},
  });

  defineEmits(['disable', 'enable', 'delete', 'edit', 'detail']);

  const {t} = useI18n();
</script>

<style lang="scss" scoped>
  .things-card__footer {
    height: 35px;
    margin-top: 2px;
    display: flex;
    justify-content: flex-end;
    border-top: 1px solid var(--el-border-color);
  }

  .things-card-footer-operation {
    height: 35px;
    display: flex;
  }
</style>
