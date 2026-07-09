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
  <div class="blank-card">
    <el-card class="blank-card__body" shadow="hover">
      <div class="blank-card__container">
        <slot/>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup></script>

<style lang="scss" scoped>
.blank-card {
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

  :deep(.el-card.blank-card__body) {
    border: 0;
  }

  :deep(.el-card__body) {
    padding: 16px;
    overflow: hidden;
  }

  .blank-card__container {
    min-width: 0;
  }

  :deep(.el-tabs__nav) {
    margin: 0 5px;
  }
}
</style>
