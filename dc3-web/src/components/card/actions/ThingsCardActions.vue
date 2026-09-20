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
  <!-- Card action order is a system-wide contract: detail (browse) → edit →
       enable/disable (state) → delete (destructive last, popconfirm-guarded).
       Keep every card footer on this ladder; see DriverCard/PointValueCard. -->
  <div :aria-busy="busy" class="things-card__footer">
    <div class="things-card-footer-operation">
      <el-button :disabled="busy || detailDisabled" link type="primary" @click="$emit('detail')">
        {{ t('common.detail') }}
      </el-button>
      <el-button :disabled="busy" link type="primary" @click="$emit('edit')">
        {{ t('common.edit') }}
      </el-button>
      <el-popconfirm
        :icon="SwitchButton"
        :disabled="busy || !enabled"
        :title="disableTitle"
        icon-color="#e6a23c"
        placement="top"
        @confirm="$emit('disable')"
      >
        <template #reference>
          <el-button :disabled="busy || !enabled" :loading="busy && enabled" link type="primary">
            {{ t('common.disable') }}
          </el-button>
        </template>
      </el-popconfirm>
      <el-popconfirm
        :icon="CircleCheck"
        :disabled="busy || enabled"
        :title="enableTitle"
        icon-color="#67c23a"
        placement="top"
        @confirm="$emit('enable')"
      >
        <template #reference>
          <el-button :disabled="busy || enabled" :loading="busy && !enabled" link type="primary">
            {{ t('common.enable') }}
          </el-button>
        </template>
      </el-popconfirm>
      <el-popconfirm
        :icon="CircleClose"
        :disabled="busy"
        :title="deleteTitle"
        icon-color="#f56c6c"
        placement="top"
        @confirm="$emit('delete')"
      >
        <template #reference>
          <el-button :disabled="busy" :loading="busy" link type="primary">{{ t('common.delete') }}</el-button>
        </template>
      </el-popconfirm>
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
  busy: {type: Boolean, default: false},
});

defineEmits(['disable', 'enable', 'delete', 'edit', 'detail']);

const {t} = useI18n();
</script>

<style lang="scss" scoped>
.things-card__footer {
  min-height: var(--dc3-touch-target);
  margin-top: 2px;
  box-sizing: border-box;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--el-border-color);
}

.things-card-footer-operation {
  min-height: var(--dc3-touch-target);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--dc3-space-1);

  :deep(.el-button) {
    min-height: var(--dc3-touch-target);
    margin: 0;
  }
}

@media (max-width: $breakpoint-xs-max) {
  .things-card__footer {
    justify-content: stretch;
  }

  .things-card-footer-operation {
    width: 100%;

    :deep(.el-button) {
      flex: 1 1 auto;
    }
  }
}
</style>
