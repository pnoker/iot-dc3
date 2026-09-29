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
  <footer class="agentic-composer">
    <div v-if="currentPendingActions.length" class="agentic-actions">
      <ActionApprovalCard
        v-for="action in currentPendingActions"
        :key="action.actionId"
        :action="action"
        :error="Boolean(actionErrors[action.actionId])"
        :loading="Boolean(actionLoading[action.actionId])"
        :locked="interactionLocked"
        @confirm="handleConfirmAction"
        @reject="handleRejectAction"
      />
    </div>
    <NoticeBar v-if="currentPendingActionsError" tone="danger">
      {{ t('agentic.failedActions') }}
      <template #actions>
        <el-button :loading="currentPendingActionsLoading" link type="danger" @click="handleRetryPendingActions">
          {{ t('common.retry') }}
        </el-button>
      </template>
    </NoticeBar>

    <div class="agentic-input-shell">
      <NoticeBar v-if="currentAttachmentsError" tone="danger">
        {{ t('agentic.failedAttachments') }}
        <template #actions>
          <el-button :loading="currentAttachmentsLoading" link type="danger" @click="handleRetryAttachments">
            {{ t('common.retry') }}
          </el-button>
        </template>
      </NoticeBar>
      <QuoteCard
        v-if="quotedMessage"
        :label="quoteLabel(quotedMessage.role)"
        :remove-label="t('agentic.quoteRemove')"
        :text="quotePreview"
        removable
        @remove="emit('update:quotedMessage', undefined)"
      />

      <div v-if="currentAttachments.length" class="agentic-attachments">
        <AttachmentCard
          v-for="attachment in currentAttachments"
          :key="attachment.id"
          :name="attachment.fileName"
          :removable="!interactionLocked"
          :size="attachment.size"
          @remove="agenticStore.removeLocalAttachment(attachment.id)"
        />
      </div>

      <el-input
        :model-value="draft"
        :aria-label="t('agentic.composerPlaceholder')"
        :autosize="{minRows: 2, maxRows: 6}"
        :disabled="loading || sessionsLoading || streaming"
        :placeholder="t('agentic.composerPlaceholder')"
        class="agentic-input"
        resize="none"
        type="textarea"
        @update:model-value="emit('update:draft', $event)"
        @keydown.enter.exact.prevent="handleSubmit"
      />

      <div class="agentic-composer__bar">
        <div class="agentic-composer__left">
          <input
            ref="fileInputRef"
            :disabled="loading || sessionsLoading || streaming"
            class="agentic-file"
            multiple
            type="file"
            @change="handleFileChange"
          />
          <el-tooltip :content="t('agentic.attachFile')">
            <el-button
              :aria-label="t('agentic.attachFile')"
              :disabled="loading || sessionsLoading || streaming"
              circle
              size="small"
              @click="handlePickFile"
            >
              <el-icon>
                <Paperclip/>
              </el-icon>
            </el-button>
          </el-tooltip>

        </div>

        <div class="agentic-composer__right">
          <!-- Model chip: model + reasoning + sampling settings live
               here (Codex-style) in BOTH docked and workbench modes,
               keeping the header free of controls. -->
          <el-popover
            :width="312"
            placement="top-start"
            popper-class="agentic-model-popover"
            trigger="click"
          >
            <template #reference>
              <button
                :aria-label="t('agentic.model')"
                class="agentic-model-pill"
                type="button"
              >
                <el-icon class="agentic-model-pill__icon">
                  <MagicStick/>
                </el-icon>
                <span class="agentic-model-pill__name">{{ activeModel.label || selectedModel }}</span>
                <el-icon class="agentic-model-pill__caret">
                  <CaretBottom/>
                </el-icon>
              </button>
            </template>
            <div class="agentic-model-settings">
              <span class="agentic-model-settings__label">{{ t('agentic.model') }}</span>
              <el-select
                :model-value="selectedModel"
                :aria-label="t('agentic.model')"
                :disabled="loading || sessionsLoading || streaming"
                class="agentic-model-settings__select"
                filterable
                size="small"
                @update:model-value="handleModelChange"
              >
                <el-option
                  v-for="model in models"
                  :key="model.model"
                  :label="model.label || model.model"
                  :value="model.model"
                />
              </el-select>

              <div class="agentic-model-settings__row">
                <span class="agentic-model-settings__label">{{ t('agentic.reasoning') }}</span>
                <el-switch
                  v-model="reasoningEnabled"
                  :active-icon="Cpu"
                  :active-value="true"
                  :aria-label="t('agentic.reasoning')"
                  :disabled="!activeModel.reasoning || loading || sessionsLoading || streaming"
                  :inactive-icon="Lightning"
                  :inactive-value="false"
                  inline-prompt
                  size="small"
                  @change="handlePrefsChange"
                />
              </div>

              <div class="agentic-model-settings__row">
                <span class="agentic-model-settings__label">{{ t('agentic.temperature') }}</span>
                <el-slider
                  v-model="temperatureProxy"
                  :disabled="loading || sessionsLoading || streaming"
                  :max="2"
                  :min="0"
                  :step="0.1"
                  class="agentic-model-settings__slider"
                  size="small"
                  @change="handlePrefsChange"
                />
              </div>

              <div class="agentic-model-settings__row">
                <span class="agentic-model-settings__label">{{ t('agentic.maxTokens') }}</span>
                <el-input-number
                  v-model="maxTokensProxy"
                  :disabled="loading || sessionsLoading || streaming"
                  :min="1"
                  :step="256"
                  controls-position="right"
                  size="small"
                  @change="handlePrefsChange"
                />
              </div>

              <div class="agentic-capabilities">
                <el-tag :type="activeModel.stream ? 'success' : 'info'" size="small"
                >{{ t('agentic.capStream') }}
                </el-tag>
                <el-tag :type="activeModel.toolCall ? 'success' : 'info'" size="small"
                >{{ t('agentic.capTools') }}
                </el-tag>
                <el-tag :type="activeModel.vision ? 'success' : 'info'" size="small"
                >{{ t('agentic.capVision') }}
                </el-tag>
                <el-tag :type="activeModel.reasoning ? 'success' : 'info'" size="small"
                >{{ t('agentic.capReasoning') }}
                </el-tag>
              </div>
            </div>
          </el-popover>
          <span class="agentic-composer__divider" aria-hidden="true" />
        <el-button
          v-if="streaming"
          :aria-label="t('agentic.stop')"
          circle
          class="agentic-send"
          type="warning"
          @click="agenticStore.stopStreaming"
        >
          <el-icon>
            <VideoPause/>
          </el-icon>
        </el-button>
        <el-button
          v-else
          :aria-label="t('agentic.send')"
          :disabled="loading || sessionsLoading || (!draft.trim() && !currentAttachments.length)"
          circle
          class="agentic-send"
          type="primary"
          @click="handleSubmit"
        >
          <el-icon>
            <Promotion/>
          </el-icon>
        </el-button>
        </div>
      </div>
    </div>
  </footer>
</template>

<script lang="ts" setup>
import {
  CaretBottom,
  Cpu,
  Lightning,
  MagicStick,
  Paperclip,
  Promotion,
  VideoPause,
} from '@element-plus/icons-vue';
import {storeToRefs} from 'pinia';
import {computed, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {ElMessage, ElMessageBox} from 'element-plus';
import type {AgenticMessage} from '@/config/types';
import {useAgenticStore} from '@/store';
import {quoteExcerpt, toPlainText} from './assistantContent';
import ActionApprovalCard from './cards/ActionApprovalCard.vue';
import AttachmentCard from './cards/AttachmentCard.vue';
import NoticeBar from './cards/NoticeBar.vue';
import QuoteCard from './cards/QuoteCard.vue';

const props = defineProps<{draft: string; quotedMessage?: AgenticMessage}>();
const emit = defineEmits<{
  'update:draft': [value: string];
  'update:quotedMessage': [value: AgenticMessage | undefined];
  /** Fired when the user submits, before the request settles. */
  submit: [];
  /** Fired after a successful send (draft cleared, safe to follow the stream). */
  submitted: [];
}>();

const agenticStore = useAgenticStore();
const {t} = useI18n();
const {
  actionErrors,
  actionLoading,
  activeConversationId,
  activeModel,
  currentAttachments,
  currentAttachmentsError,
  currentAttachmentsLoading,
  currentPendingActions,
  currentPendingActionsError,
  currentPendingActionsLoading,
  loading,
  maxTokens,
  models,
  reasoningEnabled,
  selectedModel,
  sessionsLoading,
  streaming,
  temperature,
} = storeToRefs(agenticStore);

const fileInputRef = ref<HTMLInputElement>();

const interactionLocked = computed(() => loading.value || sessionsLoading.value || streaming.value);


const quotePreview = computed(() => {
  if (!props.quotedMessage) return '';
  return quoteExcerpt(props.quotedMessage.content);
});

const quoteLabel = (role: AgenticMessage['role']) => {
  return role === 'assistant' ? t('agentic.quoteAssistant') : t('agentic.quoteUser');
};

const formatQuotedMessage = (message: AgenticMessage, body: string) => {
  const quoted = toPlainText(message.content)
    .split('\n')
    .map((line) => `> ${line}`)
    .join('\n');
  return `> **${quoteLabel(message.role)}**\n${quoted}\n\n${body}`;
};

const temperatureProxy = computed({
  get: () => temperature.value ?? activeModel.value.temperature ?? 0.7,
  set: (value: number) => {
    temperature.value = value;
  },
});

const maxTokensProxy = computed({
  get: () => maxTokens.value ?? activeModel.value.maxTokens ?? 2048,
  set: (value: number) => {
    maxTokens.value = value;
  },
});

const handleModelChange = (model: string) => {
  if (interactionLocked.value) return;
  void agenticStore.setSelectedModel(model);
};

const handlePrefsChange = () => {
  if (interactionLocked.value) return;
  void agenticStore.persistCurrentSessionPrefs();
};

const handleRetryAttachments = () => {
  if (interactionLocked.value || !activeConversationId.value) return;
  void agenticStore.loadAttachments(activeConversationId.value);
};

const handleRetryPendingActions = () => {
  if (interactionLocked.value || !activeConversationId.value) return;
  void agenticStore.loadPendingActions(activeConversationId.value);
};

const handlePickFile = () => {
  if (interactionLocked.value) return;
  fileInputRef.value?.click();
};

const handleFileChange = async (event: Event) => {
  if (interactionLocked.value) return;
  const input = event.target as HTMLInputElement;
  const files = Array.from(input.files || []);
  input.value = '';
  for (const file of files) {
    try {
      await agenticStore.uploadAttachment(file);
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : t('agentic.uploadFailed'));
    }
  }
};

const handleConfirmAction = async (actionId: string) => {
  if (interactionLocked.value) return;
  try {
    await ElMessageBox.confirm(t('agentic.confirm'), t('agentic.confirmActionTitle'), {
      type: 'warning',
      confirmButtonText: t('agentic.confirm'),
      cancelButtonText: t('agentic.dialogCancel'),
    });
    if (await agenticStore.confirmAction(actionId)) ElMessage.success(t('agentic.actionConfirmed'));
  } catch {
    // User cancelled or the request failed; keep the pending action visible.
  }
};

const handleRejectAction = async (actionId: string) => {
  if (interactionLocked.value) return;
  try {
    await ElMessageBox.confirm(t('agentic.reject'), t('agentic.rejectActionTitle'), {
      type: 'warning',
      confirmButtonText: t('agentic.reject'),
      cancelButtonText: t('agentic.dialogCancel'),
    });
    if (await agenticStore.rejectAction(actionId)) ElMessage.success(t('agentic.actionRejected'));
  } catch {
    // User cancelled or the request failed; keep the pending action visible.
  }
};

const handleSubmit = async () => {
  if (loading.value || sessionsLoading.value || streaming.value) return;
  const messageBody = props.draft.trim() || (currentAttachments.value.length ? t('agentic.attachAnalyze') : '');
  if (!messageBody) {
    return;
  }
  const content = props.quotedMessage ? formatQuotedMessage(props.quotedMessage, messageBody) : messageBody;
  emit('submit');
  const sent = await agenticStore.sendMessage(content);
  if (sent) {
    emit('update:draft', '');
    emit('update:quotedMessage', undefined);
    emit('submitted');
  }
};
</script>

<style lang="scss" scoped>
// Composer content shares the reading column with the message stream.
.agentic-composer {
  > * {
    width: min(var(--agentic-read-width), 100%);
    margin-inline: auto;
  }
}

.agentic-composer__left {
  min-width: 0;
}

.agentic-model-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  max-width: 220px;
  padding: 0 10px;
  height: 32px;
  border: 1px solid var(--dc3-border-base);
  border-radius: var(--dc3-radius-full);
  background: var(--dc3-bg-interactive);
  color: var(--dc3-text-regular);
  font-size: 12px;
  cursor: pointer;
  transition:
    color var(--dc3-duration-fast) var(--dc3-ease-standard),
    background-color var(--dc3-duration-fast) var(--dc3-ease-standard);

  &:hover,
  &:focus-visible {
    background: var(--dc3-bg-interactive-active);
    color: var(--dc3-text-brand);
    outline: none;
  }

  &__icon {
    color: var(--el-color-primary);
    font-size: 14px;
  }

  &__name {
    overflow: hidden;
    min-width: 0;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__caret {
    flex-shrink: 0;
    color: var(--dc3-text-muted);
    font-size: 10px;
  }
}

.agentic-model-settings {
  display: flex;
  flex-direction: column;
  gap: var(--dc3-space-3);

  &__label {
    color: var(--dc3-text-muted);
    font-size: 12px;
    font-weight: 600;
  }

  &__select {
    width: 100%;
  }

  &__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--dc3-space-3);
  }

  &__slider {
    flex: 1;
    min-width: 0;
  }
}

.agentic-composer {
  padding: 10px 4px 12px;
  background: transparent;
}







.agentic-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.agentic-input-shell {
  box-sizing: border-box;
  padding: 10px;
  border: 1px solid var(--dc3-border-base);
  border-radius: var(--dc3-radius-xl);
  background: var(--dc3-bg-elevated);
  box-shadow: var(--dc3-shadow-sm);
}






.agentic-attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
}

.agentic-attachment {
  display: inline-flex;
  align-items: center;
  max-width: 240px;

  :deep(.el-tag__content) {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    min-width: 0;
  }

  span {
    overflow: hidden;
    max-width: 150px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  small {
    color: var(--dc3-text-regular);
    font-size: 11px;
  }
}

.agentic-file {
  display: none;
}

.agentic-input {
  :deep(.el-textarea__inner) {
    padding: 0;
    border: 0;
    box-shadow: none;
  }
}

.agentic-composer__right {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: var(--dc3-space-2);
}

.agentic-composer__divider {
  width: 1px;
  height: 24px;
  background: var(--dc3-border-base);
}

// Attach / model / send all sit on the same 32px control height.
.agentic-composer__bar :deep(.el-button) {
  &.agentic-send,
  &.el-button--small {
    width: 32px;
    height: 32px;
    padding: 0;
  }
}

.agentic-composer__bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 8px;
}

.agentic-model {
  width: 136px;
}

.agentic-send {
  width: 34px;
  height: 34px;
}

.agentic-capabilities {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.agentic-data-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--dc3-space-2);
  margin: var(--dc3-space-2) var(--dc3-space-3) 0;
  padding: var(--dc3-space-2) var(--dc3-space-3);
  border: 1px solid var(--el-color-danger-light-5);
  border-radius: var(--dc3-radius-sm);
  color: var(--el-color-danger-dark-2);
  background: var(--el-color-danger-light-9);
  font-size: var(--el-font-size-small);
}

@media (max-width: $breakpoint-sm-max) {
  .agentic-composer__bar,
  .agentic-composer__left {
    flex-wrap: wrap;
    width: 100%;
  }

  .agentic-model {
    width: 112px;
  }
}

@media (max-width: $breakpoint-xs-max) {
  .agentic-composer {
    padding-right: var(--dc3-space-2);
    padding-left: var(--dc3-space-2);
  }

  .agentic-action {
    grid-template-columns: auto minmax(0, 1fr) auto;

    .agentic-action__buttons {
      grid-column: 1 / -1;
      justify-content: flex-end;
    }
  }
}

@media (prefers-reduced-motion: reduce) {
  .agentic-quote-preview__close {
    transition: none;
  }
}
</style>
