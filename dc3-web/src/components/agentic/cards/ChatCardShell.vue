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

<!-- Shared shell for the assistant card family (tool steps, stats, quotes,
     attachments, actions, contexts). One border/radius/typography contract so
     the chat surfaces stop hand-rolling the same box. Message bubbles are
     deliberately NOT cards and do not use this shell. -->
<template>
  <section :class="['chat-card', `chat-card--${tone}`, {'is-active': active}]" class="chat-card">
    <header v-if="title || $slots.icon || $slots.status || $slots.actions" class="chat-card__head">
      <span v-if="$slots.icon" class="chat-card__icon">
        <slot name="icon"/>
      </span>
      <span v-if="title" class="chat-card__title">{{ title }}</span>
      <span v-if="$slots.status" class="chat-card__status">
        <slot name="status"/>
      </span>
      <span v-if="$slots.actions" class="chat-card__actions">
        <slot name="actions"/>
      </span>
    </header>
    <div class="chat-card__body">
      <slot/>
    </div>
    <footer v-if="$slots.footer" class="chat-card__foot">
      <slot name="footer"/>
    </footer>
  </section>
</template>

<script lang="ts" setup>
withDefaults(
  defineProps<{
    title?: string;
    tone?: 'default' | 'info' | 'success' | 'warn' | 'danger';
    active?: boolean;
  }>(),
  {tone: 'default', active: false}
);
</script>

<style lang="scss" scoped>
.chat-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  padding: 10px;
  border: 1px solid var(--dc3-border-base);
  border-radius: var(--dc3-radius-lg);
  background: var(--dc3-bg-elevated);
  box-shadow: var(--dc3-shadow-sm);
}

.chat-card--info {
  border-color: var(--el-color-primary-light-5);
  background: var(--dc3-bg-interactive);
}

.chat-card--success {
  border-color: var(--el-color-success-light-5);
  background: var(--el-color-success-light-9);
}

.chat-card--warn {
  border-color: var(--el-color-warning-light-5);
  background: var(--el-color-warning-light-9);
}

.chat-card--danger {
  border-color: var(--el-color-danger-light-5);
  background: var(--el-color-danger-light-9);
}

.chat-card.is-active {
  box-shadow: inset 0 0 0 1px var(--dc3-border-strong);
}

.chat-card__head {
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.chat-card__icon {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  color: var(--dc3-text-muted);
  font-size: 14px;
}

.chat-card__title {
  overflow: hidden;
  min-width: 0;
  color: var(--dc3-text-primary);
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-card__status {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  margin-left: auto;
}

.chat-card__actions {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 4px;
  margin-left: auto;
}

.chat-card__body {
  min-width: 0;
  font-size: 12px;
  line-height: 1.5;
}

.chat-card__foot {
  min-width: 0;
}
</style>
