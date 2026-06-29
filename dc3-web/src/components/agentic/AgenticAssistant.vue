<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <el-tooltip v-if="!visible" :content="t('agentic.tooltip')" placement="left">
    <el-button circle class="agentic-launcher" type="primary" @click="agenticStore.toggle">
      <el-icon>
        <ChatDotRound />
      </el-icon>
    </el-button>
  </el-tooltip>

  <aside v-if="visible" ref="panelRef" :style="panelStyle" class="agentic-panel">
    <button :aria-label="t('agentic.resize')" class="agentic-resizer" type="button" @mousedown="handleResizeStart" />
    <section v-loading="loading" class="agentic-shell">
      <header class="agentic-header">
        <div class="agentic-header__top">
          <div class="agentic-title">
            <div class="agentic-mark">
              <img alt="" src="/images/common/llm.svg" />
            </div>
            <div>
              <strong>{{ t('agentic.title') }}</strong>
              <span>{{ currentSession?.title || t('agentic.newConversation') }}</span>
            </div>
          </div>

          <div class="agentic-header__primary-actions">
            <el-tooltip :content="t('agentic.headerNew')">
              <el-button circle size="small" type="success" @click="handleNewSession">
                <el-icon>
                  <Plus />
                </el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip :content="t('agentic.headerClose')">
              <el-button circle size="small" type="danger" @click="agenticStore.close">
                <el-icon>
                  <Close />
                </el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>

        <div class="agentic-header__actions">
          <div class="agentic-header__actions-left">
            <el-select
              :model-value="selectedModel"
              class="agentic-model agentic-model--toolbar"
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

            <el-tooltip :content="t('agentic.reasoning')">
              <el-switch
                v-model="reasoningEnabled"
                :active-icon="Cpu"
                :aria-label="t('agentic.reasoning')"
                :disabled="!activeModel.reasoning"
                :inactive-icon="Lightning"
                class="agentic-reasoning-switch"
                inline-prompt
                size="small"
                @change="handlePrefsChange"
              />
            </el-tooltip>
          </div>

          <div class="agentic-header__actions-right">
            <el-popover placement="bottom-end" trigger="click" width="300">
              <template #reference>
                <el-button :title="t('agentic.headerSettings')" circle size="small">
                  <el-icon>
                    <Setting />
                  </el-icon>
                </el-button>
              </template>
              <div class="agentic-settings">
                <div class="agentic-setting">
                  <span>{{ t('agentic.temperature') }}</span>
                  <el-slider
                    v-model="temperatureProxy"
                    :max="2"
                    :min="0"
                    :step="0.1"
                    size="small"
                    @change="handlePrefsChange"
                  />
                </div>
                <div class="agentic-setting">
                  <span>{{ t('agentic.maxTokens') }}</span>
                  <el-input-number
                    v-model="maxTokensProxy"
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

            <el-dropdown max-height="360" trigger="click" @command="handleHistoryCommand">
              <el-button :title="t('agentic.headerHistory')" circle size="small">
                <el-icon>
                  <Clock />
                </el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="session in conversationItems"
                    :key="session.conversationId"
                    :command="`select:${session.conversationId}`"
                  >
                    <span class="agentic-history-item">{{ session.title }}</span>
                  </el-dropdown-item>
                  <el-dropdown-item v-if="conversationItems.length === 0" disabled
                    >{{ t('agentic.headerNoHistory') }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-popover v-model:visible="renamePopoverVisible" placement="bottom-end" trigger="click" width="280">
              <template #reference>
                <el-button
                  :disabled="!activeConversationId"
                  :title="t('agentic.headerRename')"
                  circle
                  size="small"
                  @click="prepareRenameTitle"
                >
                  <el-icon>
                    <EditPen />
                  </el-icon>
                </el-button>
              </template>
              <div class="agentic-rename-form">
                <span class="agentic-popover-title">{{ t('agentic.dialogConversationTitle') }}</span>
                <el-input
                  v-model="renameTitle"
                  :maxlength="80"
                  :placeholder="t('agentic.dialogConversationTitle')"
                  clearable
                  size="small"
                  @keydown.enter.prevent="handleRenameCurrent"
                />
                <div class="agentic-popover-actions">
                  <el-button size="small" @click="renamePopoverVisible = false">
                    {{ t('agentic.dialogCancel') }}
                  </el-button>
                  <el-button :disabled="!renameTitle.trim()" size="small" type="primary" @click="handleRenameCurrent">
                    {{ t('agentic.dialogSave') }}
                  </el-button>
                </div>
              </div>
            </el-popover>
            <el-tooltip :content="t('agentic.headerDelete')">
              <el-button :disabled="!activeConversationId" circle size="small" @click="handleDeleteCurrent">
                <el-icon>
                  <Delete />
                </el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </header>

      <main ref="bodyRef" class="agentic-body">
        <div v-if="currentMessages.length === 0" class="agentic-empty">
          <Welcome
            :description="t('agentic.welcomeDescription')"
            :title="t('agentic.title')"
            icon="/images/logo/logo.svg"
            variant="borderless"
          />
          <Prompts :items="promptItems" :wrap="true" @item-click="handlePromptClick" />
        </div>

        <div v-else class="agentic-messages">
          <article
            v-for="message in currentMessages"
            :key="message.id"
            :class="`agentic-message--${message.role}`"
            class="agentic-message"
          >
            <div class="agentic-message__main">
              <div class="agentic-message__content">
                <details
                  v-if="message.role === 'assistant' && hasReasoningPanel(message)"
                  :open="isReasoningPanelOpen(message)"
                  class="agentic-reasoning-panel"
                >
                  <summary>
                    <el-icon
                      :class="{'is-active': message.streaming, 'is-done': !message.streaming}"
                      class="agentic-thinking-pulse"
                    >
                      <CircleCheck v-if="!message.streaming" />
                      <Loading v-else />
                    </el-icon>
                    <span :class="{'is-shimmer': message.streaming}" class="agentic-thinking-label"
                      >{{ t('agentic.detailThinking') }}&ensp;{{ reasoningPanelStatus(message) }}</span
                    >
                  </summary>
                  <div class="agentic-reasoning-panel__body">
                    <div v-if="assistantReasoningText(message)" class="agentic-reasoning-panel__text">
                      {{ assistantReasoningText(message) }}
                    </div>
                    <div v-else class="agentic-live-thinking">
                      <span>{{ t('agentic.thinking') }}</span>
                      <span aria-hidden="true" class="agentic-thinking-dots">
                        <i />
                        <i />
                        <i />
                      </span>
                    </div>
                  </div>
                </details>
                <RenderedAssistantMessage
                  v-if="message.role === 'assistant'"
                  :charts="message.contentExt?.charts || []"
                  :content="message.content"
                />
                <div v-else class="agentic-text">{{ message.content }}</div>
                <span
                  v-if="message.streaming && !message.content && !hasReasoningPanel(message)"
                  class="agentic-cursor"
                >
                  <span>{{ t('agentic.thinking') }}</span>
                  <span aria-hidden="true" class="agentic-thinking-dots">
                    <i />
                    <i />
                    <i />
                  </span>
                </span>
                <div
                  v-if="message.role === 'assistant' && !message.streaming && truncatedReason(message)"
                  class="agentic-message__warning"
                >
                  <el-icon>
                    <Warning />
                  </el-icon>
                  <span>{{ truncatedReason(message) }}</span>
                </div>
                <details v-if="message.role === 'assistant' && hasAssistantDetails(message)" class="agentic-details">
                  <summary>
                    <el-icon>
                      <Cpu />
                    </el-icon>
                    <span>{{ assistantDetailSummary(message) }}</span>
                  </summary>

                  <div class="agentic-details__body">
                    <div v-if="assistantRunOverview(message).length" class="agentic-run-overview">
                      <div v-for="item in assistantRunOverview(message)" :key="item.label" class="agentic-run-stat">
                        <span>{{ item.label }}</span>
                        <strong>{{ item.value }}</strong>
                      </div>
                    </div>

                    <section v-if="assistantThinkingItems(message).length" class="agentic-trace-section">
                      <div class="agentic-trace-section__header">
                        <span>{{ t('agentic.detailThinking') }}</span>
                      </div>
                      <div class="agentic-thinking-list">
                        <div v-for="item in assistantThinkingItems(message)" :key="item.label" class="agentic-thinking">
                          <span>{{ item.label }}</span>
                          <small>{{ item.detail }}</small>
                        </div>
                      </div>
                    </section>

                    <section v-if="assistantToolSteps(message).length" class="agentic-trace-section">
                      <div class="agentic-trace-section__header">
                        <span>{{ t('agentic.detailToolChain') }}</span>
                      </div>
                      <ol class="agentic-chain">
                        <li v-for="step in assistantToolSteps(message)" :key="step.id">
                          <span class="agentic-chain__index">{{ step.index }}</span>
                          <div class="agentic-chain__content">
                            <strong>{{ step.label }}</strong>
                            <small v-if="step.detail">{{ step.detail }}</small>
                            <em v-if="step.meta">{{ step.meta }}</em>
                          </div>
                        </li>
                      </ol>
                    </section>

                    <section v-if="assistantTokenItems(message).length" class="agentic-trace-section">
                      <div class="agentic-trace-section__header">
                        <span>{{ t('agentic.detailTokenUsage') }}</span>
                        <strong>{{ assistantTokenTotalLabel(message) }}</strong>
                      </div>
                      <div class="agentic-token-grid">
                        <div v-for="item in assistantTokenItems(message)" :key="item.label" class="agentic-token-item">
                          <span>{{ item.label }}</span>
                          <strong>{{ item.value }}</strong>
                        </div>
                      </div>
                    </section>

                    <div
                      v-if="assistantContexts(message).length"
                      class="agentic-details__row agentic-details__row--stack"
                    >
                      <span class="agentic-details__label">{{ t('agentic.detailContexts') }}</span>
                      <div class="agentic-contexts">
                        <div
                          v-for="(context, index) in assistantContexts(message)"
                          :key="`${context.type}-${index}`"
                          class="agentic-context"
                        >
                          <el-tag size="small" type="info">{{ context.type }}</el-tag>
                          <pre>{{ context.content }}</pre>
                        </div>
                      </div>
                    </div>
                  </div>
                </details>
              </div>
              <div v-if="canShowMessageToolbar(message)" class="agentic-message__toolbar">
                <el-tooltip :content="t('agentic.actionCopy')" placement="top">
                  <button class="agentic-message__action" type="button" @click="handleCopyMessage(message)">
                    <el-icon>
                      <DocumentCopy />
                    </el-icon>
                  </button>
                </el-tooltip>
                <el-tooltip :content="t('agentic.actionQuote')" placement="top">
                  <button class="agentic-message__action" type="button" @click="handleQuoteMessage(message)">
                    <el-icon>
                      <ChatLineSquare />
                    </el-icon>
                  </button>
                </el-tooltip>
              </div>
            </div>
          </article>
        </div>
      </main>

      <footer class="agentic-composer">
        <div v-if="currentPendingActions.length" class="agentic-actions">
          <div v-for="action in currentPendingActions" :key="action.actionId" class="agentic-action">
            <div class="agentic-action__content">
              <strong>{{ action.title }}</strong>
              <span>{{ action.description }}</span>
            </div>
            <el-tag size="small" type="warning">{{ t('agentic.pending') }}</el-tag>
            <div class="agentic-action__buttons">
              <el-button size="small" type="primary" @click="handleConfirmAction(action.actionId)">
                <el-icon>
                  <Check />
                </el-icon>
                Confirm
              </el-button>
              <el-button size="small" @click="handleRejectAction(action.actionId)">
                <el-icon>
                  <CircleClose />
                </el-icon>
                Reject
              </el-button>
            </div>
          </div>
        </div>

        <div class="agentic-input-shell">
          <div v-if="currentAttachments.length" class="agentic-attachments">
            <el-tag
              v-for="attachment in currentAttachments"
              :key="attachment.id"
              class="agentic-attachment"
              closable
              @close="agenticStore.removeLocalAttachment(attachment.id)"
            >
              <el-icon>
                <Document />
              </el-icon>
              <span>{{ attachment.fileName }}</span>
              <small>{{ formatFileSize(attachment.size) }}</small>
            </el-tag>
          </div>

          <el-input
            v-model="draft"
            :autosize="{minRows: 2, maxRows: 6}"
            :disabled="streaming"
            :placeholder="t('agentic.composerPlaceholder')"
            class="agentic-input"
            resize="none"
            type="textarea"
            @keydown.enter.exact.prevent="handleSubmit"
          />

          <div class="agentic-composer__bar">
            <div class="agentic-composer__left">
              <input ref="fileInputRef" class="agentic-file" multiple type="file" @change="handleFileChange" />
              <el-tooltip :content="t('agentic.attachFile')">
                <el-button :disabled="streaming" circle size="small" @click="handlePickFile">
                  <el-icon>
                    <Paperclip />
                  </el-icon>
                </el-button>
              </el-tooltip>
            </div>

            <el-button v-if="streaming" circle class="agentic-send" type="warning" @click="agenticStore.stopStreaming">
              <el-icon>
                <VideoPause />
              </el-icon>
            </el-button>
            <el-button
              v-else
              :disabled="!draft.trim() && !currentAttachments.length"
              circle
              class="agentic-send"
              type="primary"
              @click="handleSubmit"
            >
              <el-icon>
                <Promotion />
              </el-icon>
            </el-button>
          </div>
        </div>
      </footer>
    </section>
  </aside>
</template>

<script lang="ts" setup>
  import 'vue-element-plus-x/styles/index.css';

  import {Prompts, Welcome} from 'vue-element-plus-x';
  import {
    ChatDotRound,
    ChatLineSquare,
    Check,
    CircleCheck,
    CircleClose,
    Clock,
    Close,
    Cpu,
    Delete,
    Document,
    DocumentCopy,
    EditPen,
    Lightning,
    Loading,
    Paperclip,
    Plus,
    Promotion,
    Setting,
    VideoPause,
    Warning,
  } from '@element-plus/icons-vue';
  import {ElMessage, ElMessageBox} from 'element-plus';
  import {storeToRefs} from 'pinia';
  import {useI18n} from 'vue-i18n';
  import {computed, nextTick, onBeforeUnmount, ref, watch} from 'vue';
  import type {AgenticMessage, AgenticMessageContext, AgenticMessageTokens, AgenticTraceEvent} from '@/config/types';
  import {useAgenticStore} from '@/store';
  import RenderedAssistantMessage from './RenderedAssistantMessage.vue';
  import {toPlainText} from './assistantContent';

  interface AssistantPromptItem {
    key: string | number;
    label?: string;
    description?: string;
  }

  interface AssistantRunStat {
    label: string;
    value: string;
  }

  interface AssistantThinkingItem {
    label: string;
    detail: string;
  }

  interface AssistantChainStep {
    id: string;
    index: number;
    label: string;
    detail?: string;
    meta?: string;
    status?: AgenticTraceEvent['status'];
  }

  interface AssistantTokenItem {
    label: string;
    value: string;
  }

  const agenticStore = useAgenticStore();

  const {t} = useI18n();
  const {
    visible,
    loading,
    streaming,
    sessions,
    models,
    selectedModel,
    reasoningEnabled,
    temperature,
    maxTokens,
    activeConversationId,
    activeModel,
    currentMessages,
    currentSession,
    currentAttachments,
    currentPendingActions,
    currentTraceEvents,
  } = storeToRefs(agenticStore);

  const draft = ref('');
  const fileInputRef = ref<HTMLInputElement>();
  const bodyRef = ref<HTMLElement>();
  const panelRef = ref<HTMLElement>();
  const panelWidth = ref<number>();
  const resizeFrame = ref<number>();
  const renamePopoverVisible = ref(false);
  const renameTitle = ref('');
  const ASSISTANT_WIDTH_STORAGE_KEY = 'dc3-agentic-panel-width';
  const MIN_PANEL_WIDTH = 320;
  const MAX_PANEL_WIDTH = 520;

  const storedPanelWidth = Number(localStorage.getItem(ASSISTANT_WIDTH_STORAGE_KEY) || '');
  if (Number.isFinite(storedPanelWidth) && storedPanelWidth > 0) {
    panelWidth.value = storedPanelWidth;
  }

  const panelStyle = computed(() => ({
    width: panelWidth.value ? `${panelWidth.value}px` : 'clamp(340px, 30vw, 520px)',
  }));

  const promptItems = computed<AssistantPromptItem[]>(() => [
    {key: 'device-status', label: t('agentic.promptDeviceStatus'), description: t('agentic.promptDeviceStatusDesc')},
    {key: 'point-trend', label: t('agentic.promptPointTrend'), description: t('agentic.promptPointTrendDesc')},
    {key: 'driver-health', label: t('agentic.promptDriverHealth'), description: t('agentic.promptDriverHealthDesc')},
    {
      key: 'operation-plan',
      label: t('agentic.promptOperationPlan'),
      description: t('agentic.promptOperationPlanDesc'),
    },
  ]);

  const conversationItems = computed(() => {
    return sessions.value.map((session) => ({
      ...session,
      title: session.title || t('agentic.newConversation'),
    }));
  });

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

  const messageScrollSignature = computed(() =>
    currentMessages.value
      .map((message) =>
        [message.id, message.content.length, message.reasoning?.length || 0, message.streaming].join(':')
      )
      .join('|')
  );

  watch(
    () => visible.value,
    (isVisible) => {
      if (isVisible) {
        void nextTick(() => scrollToBottom('auto'));
      }
    }
  );

  watch(
    () => activeConversationId.value,
    () => {
      void nextTick(() => scrollToBottom('auto'));
    }
  );

  watch(
    () => messageScrollSignature.value,
    () => scheduleScrollToBottom(),
    {flush: 'post'}
  );

  onBeforeUnmount(() => {
    if (resizeFrame.value) {
      cancelAnimationFrame(resizeFrame.value);
    }
    window.removeEventListener('mousemove', handleResizeMove);
    window.removeEventListener('mouseup', handleResizeEnd);
  });

  const handleNewSession = () => {
    agenticStore.newSession();
  };

  const handleHistoryCommand = async (command: string) => {
    if (command.startsWith('select:')) {
      await agenticStore.selectSession(command.replace('select:', ''));
    }
  };

  const prepareRenameTitle = () => {
    renameTitle.value = currentSession.value?.title || t('agentic.newConversation');
  };

  const handleRenameCurrent = async () => {
    const conversationId = activeConversationId.value;
    const title = renameTitle.value.trim();
    if (!conversationId || !title) {
      return;
    }
    await agenticStore.renameSession(conversationId, title);
    renamePopoverVisible.value = false;
  };

  const handleDeleteCurrent = async () => {
    const conversationId = activeConversationId.value;
    if (!conversationId) {
      return;
    }
    await ElMessageBox.confirm(t('agentic.dialogDeleteConfirm'), t('agentic.dialogDeleteTitle'), {
      type: 'warning',
      confirmButtonText: t('agentic.dialogDeleteTitle'),
      cancelButtonText: t('agentic.dialogCancel'),
    });
    await agenticStore.deleteSession(conversationId);
  };

  const handlePromptClick = (item: AssistantPromptItem) => {
    const description = item.description ? ` ${item.description}` : '';
    draft.value = `${item.label || ''}${description}`.trim();
  };

  const handleModelChange = (model: string | number) => {
    void agenticStore.setSelectedModel(model);
  };

  const handlePrefsChange = () => {
    void agenticStore.persistCurrentSessionPrefs();
  };

  const handleResizeStart = (event: MouseEvent) => {
    event.preventDefault();
    window.addEventListener('mousemove', handleResizeMove);
    window.addEventListener('mouseup', handleResizeEnd);
    document.body.classList.add('agentic-resizing');
  };

  const handleResizeMove = (event: MouseEvent) => {
    const container = panelRef.value?.parentElement;
    const rect = container?.getBoundingClientRect();
    if (!rect) {
      return;
    }
    const maxWidth = Math.min(MAX_PANEL_WIDTH, Math.floor(rect.width * 0.45));
    panelWidth.value = clamp(
      Math.round(rect.right - event.clientX),
      MIN_PANEL_WIDTH,
      Math.max(MIN_PANEL_WIDTH, maxWidth)
    );
  };

  const handleResizeEnd = () => {
    window.removeEventListener('mousemove', handleResizeMove);
    window.removeEventListener('mouseup', handleResizeEnd);
    document.body.classList.remove('agentic-resizing');
    if (panelWidth.value) {
      localStorage.setItem(ASSISTANT_WIDTH_STORAGE_KEY, String(panelWidth.value));
    }
  };

  const handleSubmit = async () => {
    const content = draft.value.trim() || (currentAttachments.value.length ? t('agentic.attachAnalyze') : '');
    if (!content) {
      return;
    }
    draft.value = '';
    await agenticStore.sendMessage(content);
  };

  const handlePickFile = () => {
    fileInputRef.value?.click();
  };

  const handleFileChange = async (event: Event) => {
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
    await ElMessageBox.confirm(t('agentic.confirm'), t('agentic.confirmActionTitle'), {
      type: 'warning',
      confirmButtonText: t('agentic.confirm'),
      cancelButtonText: t('agentic.dialogCancel'),
    });
    await agenticStore.confirmAction(actionId);
    ElMessage.success(t('agentic.actionConfirmed'));
  };

  const handleRejectAction = async (actionId: string) => {
    await agenticStore.rejectAction(actionId);
    ElMessage.success(t('agentic.actionRejected'));
  };

  const handleCopyMessage = async (message: AgenticMessage) => {
    const text = toPlainText(message.content);
    try {
      await navigator.clipboard.writeText(text);
      ElMessage.success(t('agentic.actionCopied'));
    } catch {
      ElMessage.error(t('agentic.actionCopyFailed'));
    }
  };

  const handleQuoteMessage = (message: AgenticMessage) => {
    const text = toPlainText(message.content);
    if (!text) return;
    const quoted = text
      .split('\n')
      .map((line) => `> ${line}`)
      .join('\n');
    draft.value = `${quoted}\n\n${draft.value || ''}`.trim();
  };

  const canShowMessageToolbar = (message: AgenticMessage) => {
    return Boolean(message.content?.trim()) && !message.streaming;
  };

  const truncatedReason = (message: AgenticMessage): string => {
    const reason = message.finishReason?.toLowerCase();
    if (!reason || reason === 'stop') {
      return '';
    }
    if (reason === 'length') {
      return t('agentic.finishLength');
    }
    if (reason === 'content_filter') {
      return t('agentic.finishContentFilter');
    }
    if (reason === 'tool_calls') {
      return t('agentic.finishToolCalls');
    }
    return t('agentic.finishOther', {reason});
  };

  const assistantStatus = (message: AgenticMessage) => {
    if (message.streaming) {
      return message.content ? t('agentic.statusStreaming') : t('agentic.statusThinking');
    }
    const reason = message.finishReason?.toLowerCase();
    if (reason === 'error' || reason === 'failed') {
      return t('agentic.statusFailed');
    }
    if (assistantTraceEvents(message).some((event) => event.type === 'error' || event.status === 'failed')) {
      return t('agentic.statusFailed');
    }
    return t('agentic.statusDone');
  };

  const hasAssistantDetails = (message: AgenticMessage) => {
    return (
      assistantRunOverview(message).length > 0 ||
      assistantThinkingItems(message).length > 0 ||
      assistantToolSteps(message).length > 0 ||
      assistantReasoning(message) ||
      assistantContexts(message).length > 0 ||
      assistantTokenItems(message).length > 0
    );
  };

  const assistantDetailSummary = (message: AgenticMessage) => {
    const parts: string[] = [];
    const tools = assistantToolSteps(message);
    const contexts = assistantContexts(message);
    const tokenTotal = assistantTokenTotal(message);
    parts.push(`${t('agentic.statusLabel')} ${assistantStatus(message)}`);
    if (assistantReasoning(message)) {
      parts.push(t('agentic.thinkingReasoningMode').toLowerCase());
    }
    if (tools.length) {
      parts.push(t('agentic.detailSummaryTools', {n: tools.length}));
    }
    if (contexts.length) {
      parts.push(t('agentic.detailContexts'));
    }
    if (typeof tokenTotal === 'number') {
      parts.push(t('agentic.detailSummaryTokens', {n: formatCount(tokenTotal)}));
    }
    return parts.join(' · ');
  };

  const assistantRunOverview = (message: AgenticMessage): AssistantRunStat[] => {
    const stats: AssistantRunStat[] = [];
    const tools = assistantToolSteps(message).length;
    const tokenTotal = assistantTokenTotal(message);
    if (assistantReasoning(message)) {
      stats.push({label: t('agentic.thinkingReasoningMode'), value: t('agentic.statusEnabled')});
    }
    if (tools > 0) {
      stats.push({label: t('agentic.detailToolChain'), value: String(tools)});
    }
    if (typeof tokenTotal === 'number') {
      stats.push({label: t('agentic.detailTokenUsage'), value: formatCount(tokenTotal)});
    }
    return stats;
  };

  const assistantThinkingItems = (message: AgenticMessage): AssistantThinkingItem[] => {
    const items: AssistantThinkingItem[] = [];
    const traces = assistantTraceEvents(message);
    const tools = assistantToolSteps(message).length;
    if (message.streaming) {
      items.push({
        label: message.content ? t('agentic.thinkingGenerating') : t('agentic.thinkingPreparing'),
        detail: traces.length ? t('agentic.thinkingTraceCollecting') : t('agentic.thinkingWaitingChunk'),
      });
    }
    if (tools > 0) {
      items.push({
        label: t('agentic.thinkingToolExec'),
        detail: t('agentic.thinkingToolDetail', {n: tools}),
      });
    }
    return uniqueThinkingItems(items);
  };

  const hasReasoningPanel = (message: AgenticMessage) => {
    return Boolean(message.streaming || assistantReasoningText(message));
  };

  const isReasoningPanelOpen = (message: AgenticMessage) => {
    return Boolean(message.streaming);
  };

  const reasoningPanelStatus = (message: AgenticMessage) => {
    if (message.streaming) {
      return assistantReasoningText(message) ? t('agentic.statusStreaming') : t('agentic.statusThinking');
    }
    return t('agentic.statusDone');
  };

  const assistantReasoning = (message: AgenticMessage) => {
    return Boolean(
      assistantReasoningText(message) ||
      message.contentExt?.reasoning ||
      assistantTraceEvents(message).some((event) => event.type === 'reasoning')
    );
  };

  const assistantReasoningText = (message: AgenticMessage) => {
    const directReasoning = [message.reasoning, message.contentExt?.reasoningContent].filter((value): value is string =>
      Boolean(value)
    );
    const traceReasoning = assistantTraceEvents(message)
      .filter((event) => event.type === 'reasoning')
      .map((event) => event.detail || event.title)
      .filter((value): value is string => Boolean(value));
    return uniqueStrings([...directReasoning, ...traceReasoning])
      .join('\n')
      .trim();
  };

  const assistantTools = (message: AgenticMessage) => {
    const persisted = message.contentExt?.tools || [];
    const streaming = messageTraceEvents(message)
      .filter((event) => event.type === 'tool')
      .map((event) => event.name || event.title)
      .filter(Boolean);
    return uniqueStrings([...persisted, ...streaming]);
  };

  const assistantTraceEvents = (message: AgenticMessage): AgenticTraceEvent[] => {
    return uniqueTraceEvents([...(message.contentExt?.traces || []), ...messageTraceEvents(message)]);
  };

  const assistantToolSteps = (message: AgenticMessage): AssistantChainStep[] => {
    const traceSteps = groupedToolTraceEvents(message).map((event) => {
      const label = event.name || event.title || 'tool';
      const detail = event.title;
      const meta = toolTraceMeta(event, detail);
      return {
        id: traceKey(event),
        index: 0,
        label,
        detail,
        meta,
        status: event.status,
      };
    });
    const tracedToolLabels = new Set(traceSteps.map((step) => step.label));
    const fallbackSteps = assistantTools(message)
      .filter((tool) => !tracedToolLabels.has(tool))
      .map((tool) => ({
        id: `tool-${tool}`,
        index: 0,
        label: tool,
        detail: '',
      }));
    return indexChainSteps(uniqueChainSteps([...traceSteps, ...fallbackSteps]));
  };

  const groupedToolTraceEvents = (message: AgenticMessage): AgenticTraceEvent[] => {
    const grouped = new Map<string, AgenticTraceEvent>();
    assistantTraceEvents(message)
      .filter((event) => event.type === 'tool')
      .forEach((event) => {
        const key = event.name || event.title || traceKey(event);
        const current = grouped.get(key);
        if (!current || toolEventRank(event) >= toolEventRank(current)) {
          grouped.set(key, event);
        }
      });
    return Array.from(grouped.values());
  };

  const toolEventRank = (event: AgenticTraceEvent) => {
    if (event.phase === 'error' || event.status === 'failed') return 4;
    if (event.phase === 'result') return 3;
    if (event.phase === 'start') return 2;
    return 1;
  };

  const toolTraceMeta = (event: AgenticTraceEvent, detail?: string) => {
    const seen = new Set<string>();
    const parts = [event.status, event.code, event.detail].filter((part): part is string => {
      if (!part || part === detail || seen.has(part)) return false;
      seen.add(part);
      return true;
    });
    return parts.length ? parts.join(' · ') : undefined;
  };

  const assistantContexts = (message: AgenticMessage): AgenticMessageContext[] => {
    return message.contentExt?.contexts || [];
  };

  const assistantTokens = (message: AgenticMessage): AgenticMessageTokens | undefined => {
    return message.contentExt?.tokens;
  };

  const assistantTokenTotal = (message: AgenticMessage) => {
    const tokens = assistantTokens(message);
    if (!tokens) {
      return undefined;
    }
    const input = typeof tokens.input === 'number' ? tokens.input : 0;
    const output = typeof tokens.output === 'number' ? tokens.output : 0;
    return input + output > 0 ? input + output : undefined;
  };

  const assistantTokenTotalLabel = (message: AgenticMessage) => {
    const total = assistantTokenTotal(message);
    return typeof total === 'number' ? t('agentic.detailTotal', {n: formatCount(total)}) : t('agentic.detailPending');
  };

  const assistantTokenItems = (message: AgenticMessage): AssistantTokenItem[] => {
    const tokens = assistantTokens(message);
    if (!tokens) {
      return [];
    }
    const tokenOrder: Array<[keyof AgenticMessageTokens, string]> = [
      ['input', t('agentic.tokenInput')],
      ['output', t('agentic.tokenOutput')],
      ['text', t('agentic.tokenText')],
      ['context', t('agentic.tokenContext')],
      ['system', t('agentic.tokenSystem')],
      ['memory', t('agentic.tokenMemory')],
    ];
    return tokenOrder
      .filter(([key]) => typeof tokens[key] === 'number')
      .map(([key, label]) => ({label, value: formatCount(tokens[key] || 0)}));
  };

  const messageTraceEvents = (message: AgenticMessage) => {
    return message.streaming ? currentTraceEvents.value : [];
  };

  const uniqueStrings = (values: string[]) => {
    return Array.from(new Set(values.filter(Boolean)));
  };

  const uniqueTraceEvents = (events: AgenticTraceEvent[]) => {
    const seen = new Set<string>();
    return events.filter((event) => {
      const key = traceKey(event);
      if (seen.has(key)) {
        return false;
      }
      seen.add(key);
      return true;
    });
  };

  const traceKey = (event: AgenticTraceEvent) => {
    return [
      event.type,
      event.name || '',
      event.phase || '',
      event.status || '',
      event.code || '',
      event.title || '',
      event.detail || '',
    ].join('|');
  };

  const uniqueChainSteps = (steps: AssistantChainStep[]) => {
    const seen = new Set<string>();
    return steps.filter((step) => {
      const key = [step.label, step.status || '', step.detail || '', step.meta || ''].join('|');
      if (seen.has(key)) {
        return false;
      }
      seen.add(key);
      return true;
    });
  };

  const indexChainSteps = (steps: AssistantChainStep[]) => {
    return steps.map((step, index) => ({
      ...step,
      id: `${step.id}-${index}`,
      index: index + 1,
    }));
  };

  const uniqueThinkingItems = (items: AssistantThinkingItem[]) => {
    const seen = new Set<string>();
    return items.filter((item) => {
      const key = `${item.label}|${item.detail}`;
      if (seen.has(key)) {
        return false;
      }
      seen.add(key);
      return true;
    });
  };

  const formatCount = (value: number) => {
    return new Intl.NumberFormat('en-US').format(value);
  };

  const scheduleScrollToBottom = () => {
    if (resizeFrame.value) {
      cancelAnimationFrame(resizeFrame.value);
    }
    resizeFrame.value = requestAnimationFrame(() => {
      void nextTick(() => scrollToBottom('smooth'));
    });
  };

  const scrollToBottom = (behavior: ScrollBehavior = 'smooth') => {
    const body = bodyRef.value;
    if (!body) {
      return;
    }
    body.scrollTo({
      top: body.scrollHeight,
      behavior,
    });
  };

  const clamp = (value: number, min: number, max: number) => {
    return Math.min(Math.max(value, min), max);
  };

  const formatFileSize = (size = 0) => {
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    return `${(size / 1024 / 1024).toFixed(1)} MB`;
  };
</script>

<style lang="scss" scoped>
  .agentic-launcher {
    position: fixed;
    right: 28px;
    bottom: 96px;
    z-index: 2100;
    width: 46px;
    height: 46px;
    box-shadow: 0 10px 26px rgba(46, 64, 88, 0.24);
  }

  .agentic-panel {
    position: relative;
    flex: 0 0 auto;
    min-width: 320px;
    max-width: 520px;
    height: 100%;
    min-height: 0;
    border-left: 1px solid #dfe5ee;
    background: #ffffff;
  }

  .agentic-resizer {
    position: absolute;
    top: 0;
    left: -5px;
    z-index: 4;
    width: 10px;
    height: 100%;
    padding: 0;
    border: 0;
    cursor: col-resize;
    background: transparent;

    &::after {
      position: absolute;
      top: 0;
      left: 4px;
      width: 2px;
      height: 100%;
      content: '';
      background: transparent;
      transition: background 0.16s ease;
    }

    &:hover::after {
      background: #2563eb;
    }
  }

  .agentic-shell {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
    background: #ffffff;
  }

  .agentic-header {
    display: flex;
    align-items: stretch;
    flex-direction: column;
    justify-content: space-between;
    gap: 10px;
    min-height: 0;
    padding: 10px 10px 9px;
    border-bottom: 1px solid #dfe5ee;
    background: #ffffff;
  }

  .agentic-title,
  .agentic-header__top,
  .agentic-header__actions,
  .agentic-header__actions-left,
  .agentic-header__actions-right,
  .agentic-header__primary-actions,
  .agentic-composer__left {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .agentic-header__top {
    justify-content: space-between;
    width: 100%;
  }

  .agentic-header__actions {
    flex-wrap: wrap;
    justify-content: space-between;
    row-gap: 6px;
    width: 100%;
  }

  .agentic-header__actions-left {
    flex: 1 1 auto;
    justify-content: flex-start;
  }

  .agentic-header__actions-right {
    flex: 0 0 auto;
    justify-content: flex-end;
    gap: 6px;
    margin-left: auto;
  }

  .agentic-header__primary-actions {
    flex: 0 0 auto;
  }

  .agentic-header {
    :deep(.el-button + .el-button) {
      margin-left: 0;
    }
  }

  .agentic-mark {
    display: flex;
    flex: 0 0 34px;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    padding: 5px;
    border: 1px solid #dbe4f0;
    border-radius: 8px;
    background: #f8fafc;

    img {
      width: 100%;
      height: 100%;
      object-fit: contain;
    }
  }

  .agentic-title {
    div:last-child {
      display: flex;
      flex-direction: column;
      min-width: 0;
    }

    strong {
      color: #1f2937;
      font-size: 15px;
    }

    span {
      overflow: hidden;
      max-width: 100%;
      color: #64748b;
      font-size: 12px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .agentic-history-item {
    display: inline-block;
    overflow: hidden;
    max-width: 280px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .agentic-body {
    position: relative;
    flex: 1;
    min-height: 0;
    padding: 12px 4px;
    overflow-x: hidden;
    overflow-y: auto;
    background: #ffffff;
  }

  .agentic-empty {
    display: flex;
    flex-direction: column;
    gap: 18px;
    max-width: 680px;
    margin: 46px auto 0;
  }

  .agentic-messages {
    display: flex;
    flex-direction: column;
    gap: 12px;
    width: 100%;
    min-width: 0;
    max-width: 100%;
    margin: 0;
  }

  .agentic-message {
    display: flex;
    align-items: flex-start;
    min-width: 0;
    max-width: 100%;
  }

  .agentic-message--user {
    justify-content: flex-end;

    .agentic-message__main {
      align-items: flex-end;
      padding-left: 0;
      padding-right: 7px;
    }

    .agentic-message__content {
      color: #ffffff;
      background: var(--el-color-primary);
      border-color: var(--el-color-primary);

      &::before,
      &::after {
        position: absolute;
        top: 13px;
        width: 0;
        height: 0;
        content: '';
        border-top: 6px solid transparent;
        border-bottom: 6px solid transparent;
      }

      &::before {
        right: -7px;
        left: auto;
        border-left: 7px solid var(--el-color-primary);
      }

      &::after {
        right: -6px;
        left: auto;
        border-left: 7px solid var(--el-color-primary);
      }
    }
  }

  .agentic-message--assistant {
    justify-content: flex-start;

    .agentic-message__content {
      width: 100%;
    }
  }

  .agentic-message__main {
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
    width: 100%;
    max-width: 100%;
    min-width: 0;
  }

  .agentic-message__content {
    position: relative;
    box-sizing: border-box;
    width: fit-content;
    min-width: 0;
    max-width: 100%;
    padding: 10px 12px;
    border: 1px solid #e7edf5;
    border-radius: 8px;
    color: #1f2937;
    line-height: 1.58;
    overflow-wrap: anywhere;
    background: #ffffff;
  }

  .agentic-text {
    overflow-wrap: anywhere;
    white-space: pre-wrap;
  }

  .agentic-message__warning {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    margin-top: 8px;
    padding: 6px 8px;
    border: 1px solid #fcd34d;
    border-radius: 4px;
    color: #92400e;
    font-size: 12px;
    line-height: 1.5;
    background: #fffbeb;
  }

  .agentic-message__toolbar {
    display: flex;
    gap: 4px;
    width: fit-content;
    height: 0;
    margin-top: 0;
    overflow: hidden;
    opacity: 0;
    visibility: hidden;
    pointer-events: none;
    transform: translateY(-2px);
    transition:
      opacity 0.16s ease,
      transform 0.16s ease,
      height 0.16s ease,
      margin-top 0.16s ease,
      visibility 0.16s ease;
  }

  .agentic-message:hover .agentic-message__toolbar,
  .agentic-message:focus-within .agentic-message__toolbar {
    height: 24px;
    margin-top: 4px;
    opacity: 1;
    visibility: visible;
    pointer-events: auto;
    transform: translateY(0);
  }

  .agentic-message__action {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    padding: 0;
    border: 1px solid #e2e8f0;
    border-radius: 4px;
    color: #64748b;
    font-size: 13px;
    background: #ffffff;
    cursor: pointer;
    transition: all 0.16s ease;

    &:hover {
      color: var(--el-color-primary);
      border-color: var(--el-color-primary-light-5);
      background: var(--el-color-primary-light-9);
    }
  }

  .agentic-cursor {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: #64748b;
    font-size: 12px;
  }

  .agentic-reasoning-panel {
    box-sizing: border-box;
    width: 100%;
    min-width: 0;
    margin-bottom: 8px;
    border: 1px solid #dfe7f1;
    border-radius: 6px;
    background: #f8fafc;

    summary {
      display: flex;
      align-items: center;
      gap: 7px;
      min-height: 30px;
      padding: 0 8px;
      color: #334155;
      font-size: 12px;
      font-weight: 700;
      cursor: pointer;
      list-style: none;

      &::-webkit-details-marker {
        display: none;
      }

      &::after {
        content: '';
        width: 6px;
        height: 6px;
        margin-left: auto;
        border-right: 1px solid #94a3b8;
        border-bottom: 1px solid #94a3b8;
        transform: rotate(45deg);
        transition: transform 0.16s ease;
      }

      small {
        overflow: hidden;
        color: #64748b;
        font-size: 11px;
        font-weight: 500;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    &[open] summary::after {
      transform: rotate(225deg);
    }
  }

  .agentic-reasoning-panel__body {
    padding: 0 8px 8px 23px;
  }

  .agentic-reasoning-panel__text {
    color: #475569;
    font-size: 12px;
    line-height: 1.55;
    overflow-wrap: anywhere;
    white-space: pre-wrap;
  }

  .agentic-live-thinking {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: #64748b;
    font-size: 12px;
  }

  .agentic-thinking-pulse {
    font-size: 14px;
    color: #94a3b8;
    vertical-align: middle;

    &.is-active {
      color: var(--el-color-primary);
      animation: agentic-spin 2s linear infinite;
    }

    &.is-done {
      color: var(--el-color-success);
    }
  }

  .agentic-thinking-label {
    position: relative;

    &.is-shimmer {
      overflow: hidden;

      &::after {
        position: absolute;
        inset: -2px -4px;
        pointer-events: none;
        content: '';
        background: linear-gradient(
          120deg,
          transparent 0%,
          transparent 25%,
          rgba(255, 255, 255, 0.9) 45%,
          #fff 50%,
          rgba(255, 255, 255, 0.9) 55%,
          transparent 75%,
          transparent 100%
        );
        background-size: 200% 100%;
        animation: agentic-shimmer 3s ease-in-out infinite;
      }
    }
  }

  .agentic-thinking-dots {
    display: inline-flex;
    gap: 3px;

    i {
      display: block;
      width: 4px;
      height: 4px;
      border-radius: 999px;
      background: currentcolor;
      opacity: 0.35;
      animation: agentic-dot 1s ease-in-out infinite;

      &:nth-child(2) {
        animation-delay: 0.14s;
      }

      &:nth-child(3) {
        animation-delay: 0.28s;
      }
    }
  }

  @keyframes agentic-pulse {
    from {
      opacity: 0.45;
      transform: scale(0.75);
    }

    to {
      opacity: 0;
      transform: scale(1.65);
    }
  }

  @keyframes agentic-spin {
    from {
      transform: rotate(0deg);
    }

    to {
      transform: rotate(360deg);
    }
  }

  @keyframes agentic-shimmer {
    from {
      background-position: 100% 0;
    }

    to {
      background-position: -100% 0;
    }
  }

  @keyframes agentic-dot {
    0%,
    80%,
    100% {
      opacity: 0.28;
      transform: translateY(0);
    }

    40% {
      opacity: 0.9;
      transform: translateY(-2px);
    }
  }

  .agentic-details {
    max-width: 100%;
    min-width: 0;
    margin-top: 8px;
    border-top: 1px solid #e5eaf2;

    summary {
      display: flex;
      align-items: center;
      gap: 6px;
      min-height: 28px;
      color: #64748b;
      font-size: 12px;
      cursor: pointer;
      list-style: none;

      &::-webkit-details-marker {
        display: none;
      }

      &::after {
        content: '';
        width: 6px;
        height: 6px;
        margin-left: auto;
        border-right: 1px solid #94a3b8;
        border-bottom: 1px solid #94a3b8;
        transform: rotate(45deg);
        transition: transform 0.16s ease;
      }
    }

    &[open] summary::after {
      transform: rotate(225deg);
    }
  }

  .agentic-details__body {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding-top: 4px;
  }

  .agentic-run-overview {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(92px, 1fr));
    gap: 6px;
  }

  .agentic-run-stat {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    padding: 6px 8px;
    border: 1px solid #dfe7f1;
    border-radius: 6px;
    background: #f8fafc;

    span {
      overflow: hidden;
      color: #64748b;
      font-size: 11px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    strong {
      overflow: hidden;
      color: #1f2937;
      font-size: 12px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .agentic-trace-section {
    display: flex;
    flex-direction: column;
    gap: 8px;
    min-width: 0;
    padding: 8px;
    border: 1px solid #e2e8f0;
    border-radius: 6px;
    background: #ffffff;
  }

  .agentic-trace-section__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    min-width: 0;

    span {
      color: #334155;
      font-size: 12px;
      font-weight: 700;
    }

    strong {
      color: #2563eb;
      font-size: 12px;
      font-weight: 700;
    }
  }

  .agentic-thinking-list,
  .agentic-chain {
    display: flex;
    flex-direction: column;
    gap: 6px;
    min-width: 0;
    margin: 0;
    padding: 0;
  }

  .agentic-thinking {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
    padding-left: 10px;
    border-left: 2px solid #93c5fd;

    span {
      color: #1f2937;
      font-size: 12px;
      font-weight: 600;
    }

    small {
      color: #64748b;
      font-size: 11px;
      line-height: 1.45;
      overflow-wrap: anywhere;
      white-space: pre-wrap;
    }
  }

  .agentic-chain {
    list-style: none;

    li {
      display: grid;
      grid-template-columns: 22px minmax(0, 1fr);
      gap: 8px;
      align-items: flex-start;
      min-width: 0;
    }
  }

  .agentic-chain__index {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    border-radius: 999px;
    color: #1d4ed8;
    font-size: 11px;
    font-weight: 700;
    background: #dbeafe;
  }

  .agentic-chain__content {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;

    strong,
    small,
    em {
      overflow-wrap: anywhere;
    }

    strong {
      color: #1f2937;
      font-size: 12px;
    }

    small {
      color: #475569;
      font-size: 11px;
      line-height: 1.45;
    }

    em {
      color: #64748b;
      font-size: 11px;
      font-style: normal;
    }
  }

  .agentic-tool-scope {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    min-width: 0;

    > span {
      flex: 0 0 auto;
      color: #64748b;
      font-size: 11px;
      font-weight: 700;
      line-height: 22px;
    }
  }

  .agentic-token-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(86px, 1fr));
    gap: 6px;
  }

  .agentic-token-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    min-width: 0;
    padding: 5px 7px;
    border-radius: 5px;
    background: #f1f5f9;

    span {
      overflow: hidden;
      color: #64748b;
      font-size: 11px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    strong {
      color: #334155;
      font-size: 12px;
      font-weight: 700;
    }
  }

  .agentic-details__row {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    min-width: 0;
    color: #475569;
    font-size: 12px;
  }

  .agentic-details__row--stack {
    flex-direction: column;
  }

  .agentic-details__label {
    flex: 0 0 58px;
    color: #64748b;
    font-weight: 600;
  }

  .agentic-details__tags,
  .agentic-token-list {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    min-width: 0;
  }

  .agentic-token-list span {
    padding: 2px 6px;
    border-radius: 4px;
    color: #475569;
    background: #f1f5f9;
  }

  .agentic-contexts {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: 100%;
  }

  .agentic-context {
    display: flex;
    flex-direction: column;
    gap: 5px;

    pre {
      max-height: 180px;
      margin: 0;
      padding: 8px;
      overflow: auto;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      color: #334155;
      white-space: pre-wrap;
      background: #f8fafc;
    }
  }

  .agentic-composer {
    padding: 10px 4px 12px;
    border-top: 1px solid #dfe5ee;
    background: #ffffff;
  }

  .agentic-actions {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: 100%;
    margin: 0 0 10px;
  }

  .agentic-action {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto auto;
    gap: 10px;
    align-items: center;
    padding: 10px 12px;
    border: 1px solid #f3c96b;
    border-radius: 6px;
    background: #fffaf0;
  }

  .agentic-action__content {
    display: flex;
    flex-direction: column;
    min-width: 0;

    strong,
    span {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    strong {
      color: #1f2937;
      font-size: 13px;
    }

    span {
      color: #64748b;
      font-size: 12px;
    }
  }

  .agentic-action__buttons {
    display: flex;
    gap: 6px;
  }

  .agentic-input-shell {
    box-sizing: border-box;
    width: 100%;
    margin: 0;
    padding: 10px;
    border: 1px solid #d6dde8;
    border-radius: 8px;
    background: #ffffff;
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
      color: #64748b;
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

  .agentic-model--toolbar {
    flex: 0 1 136px;
    min-width: 104px;
  }

  .agentic-reasoning-switch {
    flex: 0 0 auto;
  }

  .agentic-send {
    width: 34px;
    height: 34px;
  }

  .agentic-settings {
    display: flex;
    flex-direction: column;
    gap: 14px;
  }

  .agentic-rename-form {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .agentic-popover-title {
    color: #334155;
    font-size: 13px;
    font-weight: 600;
  }

  .agentic-popover-actions {
    display: flex;
    justify-content: flex-end;
    gap: 8px;

    :deep(.el-button + .el-button) {
      margin-left: 0;
    }
  }

  .agentic-setting {
    display: flex;
    flex-direction: column;
    gap: 6px;

    span {
      color: #334155;
      font-size: 13px;
      font-weight: 500;
    }
  }

  .agentic-setting--row {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }

  .agentic-capabilities {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
  }

  :deep(.agentic-markdown) {
    max-width: 100%;
    min-width: 0;
    overflow-wrap: anywhere;

    p {
      margin: 0 0 8px;
    }

    p:last-child {
      margin-bottom: 0;
    }

    ul,
    ol {
      padding-left: 20px;
      margin: 6px 0;
    }

    blockquote {
      box-sizing: border-box;
      max-width: 100%;
      margin: 8px 0;
      padding: 8px 10px 8px 12px;
      overflow-wrap: anywhere;
      border-left: 3px solid var(--el-color-primary-light-3);
      border-radius: 4px;
      color: #334155;
      background: #f8fafc;

      > :first-child {
        margin-top: 0;
      }

      > :last-child {
        margin-bottom: 0;
      }
    }

    hr {
      height: 1px;
      margin: 10px 0;
      border: 0;
      background: #dfe5ee;
    }

    code {
      padding: 2px 4px;
      border-radius: 4px;
      background: #eef2f7;
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 12px;
    }

    pre {
      box-sizing: border-box;
      max-width: 100%;
      overflow: auto;
      padding: 10px;
      border: 1px solid #e2e8f0;
      border-radius: 6px;
      background: #f8fafc;

      code {
        padding: 0;
        color: #334155;
        background: transparent;
      }
    }

    table {
      display: block;
      width: 100%;
      max-width: 100%;
      overflow-x: auto;
      border-collapse: collapse;
    }

    th,
    td {
      padding: 6px 8px;
      border: 1px solid #dfe5ee;
    }
  }

  @media (max-width: 900px) {
    .agentic-panel {
      width: min(420px, 40vw) !important;
    }

    .agentic-header__actions,
    .agentic-composer__bar,
    .agentic-composer__left {
      flex-wrap: wrap;
      width: 100%;
    }

    .agentic-model {
      width: 112px;
    }
  }
</style>

<style lang="scss">
  .agentic-resizing {
    cursor: col-resize;
    user-select: none;
  }
</style>
