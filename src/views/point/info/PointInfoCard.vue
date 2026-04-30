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
                <el-icon><Goblet /></el-icon>
                {{ attribute.attributeName }}: {{ data[attribute.attributeCode].configValue }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
  import type { PropType } from 'vue';
  import { computed } from 'vue';
  import { Goblet } from '@element-plus/icons-vue';
  import type { Attribute } from '@/config/entity';

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

  const isConfig = computed(() =>
    props.attributes.every((attr: any) => props.data[attr.attributeCode]?.configValue !== '')
  );

  const isSelect = computed(() =>
    props.data.shadow === 'always' ? 'images/common/point-info.png' : 'images/common/point-info-disable.png'
  );
</script>

<style lang="scss" scoped>
  @use '@/components/card/styles/things-card.scss';
</style>
