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
  <div :class="['things-card__header', enabled ? 'header-enable' : 'header-disable']">
    <div class="things-card-header-icon">
      <img :alt="name" :src="icon" />
    </div>
    <div class="things-card-header-name nowrap-name" @click.stop="$emit('copy-id')">
      {{ name }}
    </div>
    <div :title="statusTitle" class="things-card-header-status">
      <slot />
    </div>
  </div>
</template>

<script lang="ts" setup>
  defineProps({
    name: {type: String, default: ''},
    icon: {type: String, required: true},
    enabled: {type: Boolean, default: false},
    statusTitle: {type: String, default: ''},
  });

  defineEmits(['copy-id']);
</script>

<style lang="scss" scoped>
  .things-card__header {
    width: 100%;
    height: 55px;
    display: flex;

    .things-card-header-icon {
      width: 48px;
      height: 48px;
      margin-right: 12px;
      border-radius: 4px;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
      }
    }

    .things-card-header-name {
      height: 48px;
      line-height: 48px;
      font-size: 14px;
      font-weight: bold;
      color: var(--el-text-color-primary);
      cursor: pointer;
      display: block;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
      width: 200px;

      &:hover {
        color: var(--el-color-primary);
      }
    }

    .things-card-header-status {
      height: 48px;
      line-height: 48px;
      text-align: right;
      flex: 1;

      :deep(.el-tag) {
        vertical-align: middle;
      }
    }
  }

  .header-enable {
    border-bottom: 1px solid var(--el-color-success-light-5);
  }

  .header-disable {
    border-bottom: 1px solid var(--el-color-danger-light-5);
  }
</style>
