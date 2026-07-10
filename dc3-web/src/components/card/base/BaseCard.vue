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
  <div class="base-card">
    <el-card class="base-card__body" shadow="never">
      <div class="base-card__container">
        <slot/>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup></script>

<style lang="scss" scoped>
.base-card {
  box-sizing: border-box;
  min-width: 0;

  ul {
    list-style: none;

    li {
      font-size: 13px;
      margin-top: 8px;
    }
  }

  :deep(.el-card) {
    width: 100%;
    box-sizing: border-box;
    min-width: 0;
  }

  :deep(.el-card.base-card__body) {
    border: 0;
  }

  :deep(.el-tabs__nav) {
    margin: 0 5px;
  }

  .base-card__container {
    min-width: 0;

    :deep(.el-tabs__header) {
      margin-bottom: 0;

      .el-tabs__nav-wrap:after {
        height: 1px;
      }
    }

    :deep(.el-tab-pane) {
      background: #f6f7f9;
    }
  }
}
</style>
