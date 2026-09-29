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

<!-- Attachment card for the composer (pending uploads) and future message
     history display. Download/preview are not wired yet — this is the display
     contract both surfaces share. -->
<template>
  <ChatCardShell class="chat-attachment">
    <template #icon>
      <el-icon><Document/></el-icon>
    </template>
    <template #status>
      <el-tag :type="pending ? 'warning' : 'success'" size="small">
        {{ pending ? t('agentic.attachmentPending') : t('agentic.attachmentReady') }}
      </el-tag>
    </template>
    <template v-if="removable" #actions>
      <button
        :aria-label="t('agentic.quoteRemove')"
        :title="t('agentic.quoteRemove')"
        class="chat-attachment__remove"
        type="button"
        @click="emit('remove')"
      >
        <el-icon>
          <Close/>
        </el-icon>
      </button>
    </template>
    <div class="chat-attachment__body">
      <strong>{{ name }}</strong>
      <small>{{ formatFileSize(size) }}</small>
    </div>
  </ChatCardShell>
</template>

<script lang="ts" setup>
import {Close, Document} from '@element-plus/icons-vue';
import {useI18n} from 'vue-i18n';
import ChatCardShell from './ChatCardShell.vue';

withDefaults(
  defineProps<{
    name: string;
    size?: number;
    pending?: boolean;
    removable?: boolean;
  }>(),
  {size: 0, pending: true, removable: false}
);
const emit = defineEmits<{remove: []}>();
const {t} = useI18n();

const formatFileSize = (size = 0) => {
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
};
</script>

<style lang="scss" scoped>
.chat-attachment__body {
  display: flex;
  align-items: baseline;
  gap: 6px;
  min-width: 0;

  strong {
    overflow: hidden;
    color: var(--dc3-text-primary);
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    flex-shrink: 0;
    color: var(--dc3-text-muted);
    font-size: 11px;
  }
}

.chat-attachment__remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: var(--dc3-radius-sm);
  color: var(--dc3-text-muted);
  background: transparent;
  cursor: pointer;

  &:hover {
    background: var(--dc3-bg-interactive);
    color: var(--dc3-text-brand);
  }
}
</style>
