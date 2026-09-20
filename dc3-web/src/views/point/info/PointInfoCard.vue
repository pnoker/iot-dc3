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
  <div
    :aria-label="String(data.pointName || data.attributeName || '')"
    class="things-card cursor-pointer"
    role="button"
    tabindex="0"
    @click="$emit('select', data)"
    @keydown.enter.prevent="$emit('select', data)"
    @keydown.space.prevent="$emit('select', data)"
  >
    <el-card :shadow="data.shadow">
      <div class="things-card-content">
        <div :class="['things-card__header', isConfig ? 'header-enable' : 'header-disable']">
          <div :class="['things-card-header-icon', {'is-inactive': !isSelected}]">
            <el-icon :size="24">
              <component :is="icon"/>
            </el-icon>
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
    default: 'SetUp',
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

// Selected config tiles carry the brand accent; the rest stay neutral —
// replaces the old selected/unselected PNG pair.
const isSelected = computed(() => props.data.shadow === 'always');
</script>

<style lang="scss" scoped>
// PointInfoCard has a simplified header without a status tag, so it supplies its own header styles.

.cursor-pointer {
  cursor: pointer;

  &:focus-visible {
    border-radius: var(--dc3-radius-xl);
    outline: none;
    box-shadow: var(--dc3-focus-ring);
  }
}

.things-card__header {
  width: 100%;
  height: 55px;
  display: flex;
  align-items: center;

  // Same tile family as ThingsCardHeader; selection flips the accent.
  .things-card-header-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 44px;
    height: 44px;
    margin-right: var(--dc3-space-3);
    border: 1px solid color-mix(in srgb, var(--el-color-primary) 18%, transparent);
    border-radius: var(--dc3-radius-lg);
    background: var(--el-color-primary-light-9);
    color: var(--el-color-primary);

    &.is-inactive {
      border-color: var(--el-border-color-lighter);
      background: var(--el-fill-color-light);
      color: var(--el-text-color-secondary);
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
