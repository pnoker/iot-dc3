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
  <div class="things-card__footer">
    <div class="things-card-footer-operation">
      <el-popconfirm
        :icon="SwitchButton"
        :title="disableTitle"
        icon-color="#e6a23c"
        placement="top"
        @confirm="$emit('disable')"
      >
        <template #reference>
          <el-button :disabled="!enabled" link type="primary">
            {{ t('common.disable') }}
          </el-button>
        </template>
      </el-popconfirm>
      <el-popconfirm
        :icon="CircleCheck"
        :title="enableTitle"
        icon-color="#67c23a"
        placement="top"
        @confirm="$emit('enable')"
      >
        <template #reference>
          <el-button :disabled="enabled" link type="primary">
            {{ t('common.enable') }}
          </el-button>
        </template>
      </el-popconfirm>
      <el-popconfirm
        :icon="CircleClose"
        :title="deleteTitle"
        icon-color="#f56c6c"
        placement="top"
        @confirm="$emit('delete')"
      >
        <template #reference>
          <el-button link type="primary">{{ t('common.delete') }}</el-button>
        </template>
      </el-popconfirm>
      <el-button link type="primary" @click="$emit('edit')">
        {{ t('common.edit') }}
      </el-button>
      <el-button :disabled="detailDisabled" link type="primary" @click="$emit('detail')">
        {{ t('common.detail') }}
      </el-button>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {CircleCheck, CircleClose, SwitchButton} from '@element-plus/icons-vue';
import {useI18n} from 'vue-i18n';

defineProps({
  enabled: {type: Boolean, required: true},
  disableTitle: {type: String, required: true},
  enableTitle: {type: String, required: true},
  deleteTitle: {type: String, required: true},
  detailDisabled: {type: Boolean, default: false},
});

defineEmits(['disable', 'enable', 'delete', 'edit', 'detail']);

const {t} = useI18n();
</script>

<style lang="scss" scoped>
.things-card__footer {
  height: 35px;
  margin-top: 2px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--el-border-color);
}

.things-card-footer-operation {
  height: 35px;
  display: flex;
}
</style>
