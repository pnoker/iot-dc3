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
  <el-skeleton :loading="props.loading" :throttle="{leading: 500}" animated>
    <template #template>
      <el-row>
        <el-col v-for="data in 12" :key="data" :lg="6" :md="8" :sm="12" :xl="4" :xs="24">
          <div class="skeleton-card">
            <el-card class="skeleton-card__body" shadow="hover">
              <div class="skeleton-card__container">
                <div class="skeleton-card__header">
                  <el-skeleton-item class="skeleton-card-icon" variant="image"/>
                  <el-skeleton-item class="skeleton-card-name" variant="text"/>
                </div>
                <div class="skeleton-card__body">
                  <div class="skeleton-card-content">
                    <ul>
                      <el-skeleton-item class="skeleton-card-text" variant="text"/>
                      <el-skeleton-item class="skeleton-card-text" variant="text"/>
                      <el-skeleton-item class="skeleton-card-text" variant="text"/>
                      <el-skeleton-item class="skeleton-card-text" variant="text"/>
                    </ul>
                  </div>
                  <div class="skeleton-card-content">
                    <el-skeleton-item class="skeleton-card-description" variant="text"/>
                  </div>
                </div>
                <div v-if="props.footer" class="skeleton-card__footer">
                  <div class="skeleton-card-operation">
                    <el-skeleton-item class="skeleton-card-button" variant="button"/>
                    <el-skeleton-item class="skeleton-card-button" variant="button"/>
                    <el-skeleton-item class="skeleton-card-button" variant="button"/>
                    <el-skeleton-item class="skeleton-card-button" variant="button"/>
                    <el-skeleton-item class="skeleton-card-button" variant="button"/>
                  </div>
                </div>
              </div>
            </el-card>
          </div>
        </el-col>
      </el-row>
    </template>
    <template #default>
      <slot/>
    </template>
  </el-skeleton>
</template>

<script lang="ts" setup>
const props = defineProps({
  loading: {
    type: Boolean,
    default: () => {
      return false;
    },
  },
  footer: {
    type: Boolean,
    default: () => {
      return true;
    },
  },
});
</script>

<style lang="scss" scoped>
.skeleton-card {
  border-radius: 4px;
  box-sizing: border-box;
  margin: 0 3px 6px;
}

.skeleton-card__container {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.skeleton-card__header {
  width: 100%;
  height: 55px;
  display: flex;
  border-bottom: 1px solid var(--el-border-color);
}

.skeleton-card-icon {
  width: 55px;
  height: 48px;
  margin-right: 12px;
  border-radius: 4px;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
  }
}

.skeleton-card-name {
  height: 28px;
  margin-top: 10px;
  line-height: 48px;
}

.skeleton-card__body {
  display: flex;
  flex-direction: column;
}

.skeleton-card-content {
  display: flex;
  justify-content: space-around;

  ul {
    width: 100%;
    padding-inline-start: 25px;
    list-style: none;

    li {
      font-size: 13px;
      margin-top: 8px;
    }
  }
}

.skeleton-card-text {
  margin-top: 2px;
}

.skeleton-card-description {
  margin-top: 10px;
}

.skeleton-card__footer {
  height: 35px;
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  border-top: 1px solid var(--el-border-color);
}

.skeleton-card-operation {
  display: flex;
}

.skeleton-card-button {
  width: 28px;
  height: 20px;
  margin-right: 10px;
}
</style>
