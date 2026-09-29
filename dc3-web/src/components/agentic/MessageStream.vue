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
  <div v-if="loading && messages.length === 0" class="agentic-loading-state">
    <el-skeleton :rows="4" animated />
  </div>
  <div v-else-if="messages.length === 0" class="agentic-empty">
    <Welcome
      :description="t('agentic.welcomeDescription')"
      :icon="assetUrl('images/logo/logo.svg')"
      :title="t('agentic.title')"
      variant="borderless"
    />
    <Prompts :items="prompts" :wrap="true" @item-click="handlePromptClick"/>
  </div>
  <div v-else class="agentic-messages">
    <MessageItem
      v-for="(message, messageIndex) in messages"
      :key="message.id"
      :attachments-by-id="attachmentsById"
      :continued="isContinued(messageIndex)"
      :focused="message.id === focusedMessageId"
      :live-traces="liveTraces"
      :message="message"
      @copy="emit('copy', $event)"
      @focus="emit('focus-message', message.id)"
      @quote="emit('quote', $event)"
    />
  </div>
</template>

<script lang="ts" setup>
import {Prompts, Welcome} from 'vue-element-plus-x';
import {useI18n} from 'vue-i18n';
import type {AgenticAttachment, AgenticMessage, AgenticTraceEvent} from '@/config/types';
import MessageItem from './MessageItem.vue';
import {assetUrl} from '@/utils/assetUrl';

/** Prompt card shown in the empty state. */
export interface AssistantPromptItem {
  key: string | number;
  label?: string;
  description?: string;
}

const props = defineProps<{
  messages: AgenticMessage[];
  liveTraces: AgenticTraceEvent[];
  loading: boolean;
  focusedMessageId: string;
  prompts: AssistantPromptItem[];
  attachmentsById?: Record<string, AgenticAttachment>;
}>();
const emit = defineEmits<{
  'focus-message': [messageId: string];
  copy: [message: AgenticMessage];
  quote: [message: AgenticMessage];
  'prompt-click': [item: AssistantPromptItem];
}>();

const {t} = useI18n();

const isContinued = (index: number) => {
  return index > 0 && props.messages[index - 1]?.role === props.messages[index]?.role;
};

const handlePromptClick = (item: AssistantPromptItem) => {
  emit('prompt-click', item);
};
</script>

<style lang="scss" scoped>
.agentic-loading-state {
  padding: var(--dc3-space-4);
}

.agentic-empty {
  display: flex;
  flex-direction: column;
  gap: var(--dc3-space-5);
  max-width: min(var(--agentic-read-width), 100%);
  margin: var(--dc3-space-10) auto 0;
}

.agentic-messages {
  display: flex;
  flex-direction: column;
  width: 100%;
  min-width: 0;
  max-width: min(var(--agentic-read-width), 100%);
  margin: 0 auto;

  // Rhythm: role changes breathe, same-role runs group tightly.
  :deep(.agentic-message + .agentic-message) {
    margin-top: var(--dc3-space-5);
  }

  :deep(.agentic-message.is-continued) {
    margin-top: var(--dc3-space-2);
  }
}
</style>
