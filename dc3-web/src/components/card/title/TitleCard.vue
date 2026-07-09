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
  <div class="title-card">
    <el-card class="title-card__body" shadow="hover">
      <div class="title-card__container">
        <slot name="header">
          <span class="title-card__header">{{ props.title }}</span>
        </slot>
        <slot/>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
const props = defineProps({
  title: {
    type: String,
    default: '',
  },
});
</script>

<style lang="scss" scoped>
.title-card {
  box-sizing: border-box;

  :deep(.el-card) {
    width: 100%;
    box-sizing: border-box;
  }

  :deep(.el-card__header) {
    padding: 12px 16px;
  }

  :deep(.el-card__body) {
    padding: 16px;
  }

  :deep(.el-tabs__nav) {
    margin: 0 5px;
  }

  .title-card__container {
    .title-card__header {
      font-size: 14px;
      font-weight: bold;
    }

    &:first-child {
      padding-top: 0;
      padding-left: 0;
    }
  }
}
</style>
