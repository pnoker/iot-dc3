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
  <div class="error">
    <el-result :sub-title="t('error.notFound')" icon="warning" title="404">
      <template #extra>
        <router-link :to="{path: '/'}">
          <el-button plain type="primary">{{ t('error.backHome') }}</el-button>
        </router-link>
      </template>
    </el-result>
  </div>
</template>

<script lang="ts" setup>
import {useI18n} from 'vue-i18n';

const {t} = useI18n();
</script>

<style lang="scss" scoped>
.error {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
}
</style>
