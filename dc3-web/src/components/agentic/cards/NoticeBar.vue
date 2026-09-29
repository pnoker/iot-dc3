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

<!-- Unified inline notice: truncation warnings, load failures, action errors.
     Keeps one tone language instead of per-surface yellow/red strips. -->
<template>
  <div :class="`chat-notice--${tone}`" class="chat-notice" role="alert">
    <el-icon class="chat-notice__icon">
      <Warning v-if="tone === 'warn'"/>
      <CircleCloseFilled v-else-if="tone === 'danger'"/>
      <InfoFilled v-else/>
    </el-icon>
    <span class="chat-notice__text"><slot/></span>
    <span v-if="$slots.actions" class="chat-notice__actions">
      <slot name="actions"/>
    </span>
  </div>
</template>

<script lang="ts" setup>
import {CircleCloseFilled, InfoFilled, Warning} from '@element-plus/icons-vue';

withDefaults(defineProps<{tone?: 'info' | 'warn' | 'danger'}>(), {tone: 'warn'});
</script>

<style lang="scss" scoped>
.chat-notice {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
  padding: 6px 8px;
  border: 1px solid var(--dc3-border-base);
  border-radius: var(--dc3-radius-md);
  font-size: 12px;
  line-height: 1.5;
}

.chat-notice__icon {
  flex-shrink: 0;
  font-size: 14px;
}

.chat-notice__text {
  min-width: 0;
  overflow-wrap: anywhere;
}

.chat-notice__actions {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  margin-left: auto;
}

.chat-notice--warn {
  border-color: var(--el-color-warning-light-5);
  color: var(--el-color-warning-dark-2);
  background: var(--el-color-warning-light-9);

  .chat-notice__icon {
    color: var(--el-color-warning);
  }
}

.chat-notice--danger {
  border-color: var(--el-color-danger-light-5);
  color: var(--el-color-danger-dark-2);
  background: var(--el-color-danger-light-9);

  .chat-notice__icon {
    color: var(--el-color-danger);
  }
}

.chat-notice--info {
  border-color: var(--el-color-primary-light-5);
  color: var(--dc3-text-primary);
  background: var(--dc3-bg-interactive);

  .chat-notice__icon {
    color: var(--el-color-primary);
  }
}
</style>
