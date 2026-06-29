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
  <div class="things-card cursor-pointer" @click="$emit('select', data)">
    <el-card :shadow="data.shadow">
      <div class="things-card-content">
        <div :class="['things-card__header', isConfig ? 'header-enable' : 'header-disable']">
          <div class="things-card-header-icon">
            <img :alt="data.attributeName" :src="isSelect" />
          </div>
          <div class="things-card-header-name nowrap-name">{{ data.pointName }}</div>
        </div>
        <div class="things-card__body">
          <div class="things-card-body-content">
            <ul>
              <li v-for="attribute in attributes" :key="attribute.id" class="nowrap-item">
                <el-icon>
                  <Goblet />
                </el-icon>
                {{ attribute.attributeName }}: {{ displayConfigValue(attribute.attributeCode) }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
  import type {PropType} from 'vue';
  import {computed} from 'vue';
  import {Goblet} from '@element-plus/icons-vue';
  import type {Attribute} from '@/config/types';

  const props = defineProps({
    data: {
      type: Object as PropType<Record<string, any>>,
      default: () => ({}),
    },
    attributes: {
      type: Array as PropType<Attribute[]>,
      default: () => [],
    },
    icon: {
      type: String,
      default: 'images/common/point-info-disable.png',
    },
  });

  defineEmits(['select']);

  const hasConfigValue = (value: unknown) => value !== '' && value !== null && value !== undefined;

  const displayConfigValue = (attributeCode: string) => {
    const value = props.data[attributeCode]?.configValue;
    return hasConfigValue(value) ? value : '-';
  };

  const isConfig = computed(() =>
    props.attributes.every((attr: any) => hasConfigValue(props.data[attr.attributeCode]?.configValue))
  );

  const isSelect = computed(() =>
    props.data.shadow === 'always' ? 'images/common/point-info.png' : 'images/common/point-info-disable.png'
  );
</script>

<style lang="scss" scoped>
  // PointInfoCard 内联了一个简化的 header(无状态标签),不使用 ThingsCardHeader,在此补齐样式。

  .cursor-pointer {
    cursor: pointer;
  }

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
    }
  }

  .header-enable {
    border-bottom: 1px solid var(--el-color-success-light-5);
  }

  .header-disable {
    border-bottom: 1px solid var(--el-color-danger-light-5);
  }
</style>
