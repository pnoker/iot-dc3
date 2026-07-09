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
  <div class="things-card cursor-pointer" @click="$emit('select', data)">
    <el-card :shadow="data.shadow">
      <div class="things-card-content">
        <div :class="['things-card__header', isConfig ? 'header-enable' : 'header-disable']">
          <div class="things-card-header-icon">
            <img :alt="data.attributeName" :src="isSelect"/>
          </div>
          <div class="things-card-header-name nowrap-name">{{ data.pointName }}</div>
        </div>
        <div class="things-card__body">
          <div class="things-card-body-content">
            <ul>
              <li v-for="attribute in attributes" :key="attribute.id" class="nowrap-item">
                <el-icon>
                  <Goblet/>
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
