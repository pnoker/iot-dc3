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

<!-- One quote card for both surfaces that quote something: the user message's
     embedded quote and the composer's pending-quote preview. The "quoted
     content" icon stays ChatLineSquare here; the toolbar's quote *action*
     uses Tickets so the two meanings stop colliding. -->
<template>
  <div :class="`chat-quote--${tone}`" class="chat-quote">
    <div class="chat-quote__header">
      <el-icon>
        <ChatLineSquare/>
      </el-icon>
      <span>{{ label }}</span>
      <button
        v-if="removable"
        :aria-label="removeLabel || t('agentic.quoteRemove')"
        :title="removeLabel || t('agentic.quoteRemove')"
        class="chat-quote__close"
        type="button"
        @click="emit('remove')"
      >
        <el-icon>
          <Close/>
        </el-icon>
      </button>
    </div>
    <p class="chat-quote__text">{{ text }}</p>
  </div>
</template>

<script lang="ts" setup>
import {ChatLineSquare, Close} from '@element-plus/icons-vue';
import {useI18n} from 'vue-i18n';

withDefaults(
  defineProps<{
    label?: string;
    text: string;
    /** on-primary renders on the user bubble; surface renders on the composer. */
    tone?: 'on-primary' | 'surface';
    removable?: boolean;
    removeLabel?: string;
  }>(),
  {tone: 'surface', removable: false}
);
const emit = defineEmits<{remove: []}>();
const {t} = useI18n();
</script>

<style lang="scss" scoped>
.chat-quote {
  position: relative;
  max-width: 100%;
  min-width: 0;
  overflow: hidden;
  border-radius: var(--dc3-radius-md);
}

.chat-quote__header {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  font-weight: 700;
}

.chat-quote__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  margin-left: auto;
  padding: 0;
  border: 1px solid transparent;
  border-radius: var(--dc3-radius-sm);
  background: transparent;
  cursor: pointer;

  &:hover {
    background: var(--dc3-bg-interactive);
    color: var(--dc3-text-brand);
  }
}

.chat-quote__text {
  display: -webkit-box;
  margin: 4px 0 0;
  overflow: hidden;
  font-size: 12px;
  line-height: 1.5;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.chat-quote--surface {
  padding: 8px 10px;
  border: 1px solid var(--dc3-border-base);
  border-left: 3px solid var(--el-color-primary);
  background: var(--dc3-bg-muted);

  .chat-quote__header {
    color: var(--dc3-text-muted);
  }

  .chat-quote__text {
    color: var(--dc3-text-regular);
  }
}

.chat-quote--on-primary {
  margin-bottom: 8px;
  padding: 8px 10px;
  border: 1px solid color-mix(in srgb, var(--el-color-white) 28%, transparent);
  border-left: 3px solid color-mix(in srgb, var(--el-color-white) 82%, transparent);
  background: color-mix(in srgb, var(--el-color-white) 13%, transparent);

  .chat-quote__header {
    color: var(--el-color-white);
  }

  .chat-quote__text {
    color: color-mix(in srgb, var(--el-color-white) 88%, transparent);
  }

  .chat-quote__close {
    color: var(--el-color-white);

    &:hover {
      background: color-mix(in srgb, var(--el-color-white) 22%, transparent);
      color: var(--el-color-white);
    }
  }
}
</style>
