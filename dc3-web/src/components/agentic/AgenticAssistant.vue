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
  <el-tooltip v-if="!visible" :content="t('agentic.tooltip')" placement="left">
    <el-button
      :aria-label="t('agentic.tooltip')"
      circle
      class="agentic-launcher"
      type="primary"
      @click="handleLauncherClick"
    >
      <el-icon>
        <ChatDotRound/>
      </el-icon>
    </el-button>
  </el-tooltip>

  <aside
    v-if="visible"
    ref="panelRef"
    :aria-label="t('agentic.title')"
    :aria-modal="isMobile || undefined"
    :role="isMobile ? 'dialog' : 'complementary'"
    :style="panelStyle"
    class="agentic-panel"
    tabindex="-1"
    @keydown="handlePanelKeydown"
  >
    <button
      :aria-label="t('agentic.resize')"
      :aria-valuemax="MAX_PANEL_WIDTH"
      :aria-valuemin="MIN_PANEL_WIDTH"
      :aria-valuenow="effectivePanelWidth"
      :tabindex="isMobile ? -1 : 0"
      aria-orientation="vertical"
      class="agentic-resizer"
      role="separator"
      type="button"
      @keydown.left.prevent="handleResizeKeydown(16)"
      @keydown.right.prevent="handleResizeKeydown(-16)"
      @mousedown="handleResizeStart"
    />
    <section v-loading="loading" class="agentic-shell">
      <header class="agentic-header">
        <div class="agentic-header__top">
          <div class="agentic-title">
            <div class="agentic-mark">
              <img :src="assetUrl('images/common/llm.svg')" alt=""/>
            </div>
            <div>
              <strong>{{ t('agentic.title') }}</strong>
              <span>{{ currentSession?.title || t('agentic.newConversation') }}</span>
            </div>
          </div>

          <div class="agentic-header__primary-actions">
            <el-tooltip :content="t('agentic.headerNew')">
              <el-button
                :aria-label="t('agentic.headerNew')"
                :disabled="loading || sessionsLoading || streaming"
                circle
                size="small"
                type="success"
                @click="handleNewSession"
              >
                <el-icon>
                  <Plus/>
                </el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip :content="t('agentic.headerClose')">
              <el-button
                :aria-label="t('agentic.headerClose')"
                circle
                size="small"
                type="danger"
                @click="agenticStore.close"
              >
                <el-icon>
                  <Close/>
                </el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>

        <div class="agentic-header__actions">
          <div class="agentic-header__actions-left">
            <el-select
              :model-value="selectedModel"
              :aria-label="t('agentic.model')"
              class="agentic-model agentic-model--toolbar"
              :disabled="loading || sessionsLoading || streaming"
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
                :active-value="true"
                :active-icon="Cpu"
                :aria-label="t('agentic.reasoning')"
                :disabled="!activeModel.reasoning || loading || sessionsLoading || streaming"
                :inactive-value="false"
                :inactive-icon="Lightning"
                class="agentic-reasoning-switch"
                inline-prompt
                size="small"
                @change="handlePrefsChange"
              />
            </el-tooltip>
          </div>

          <div class="agentic-header__actions-right">
            <el-popover :teleported="false" placement="bottom-end" trigger="click" width="300">
              <template #reference>
                <el-button
                  :aria-label="t('agentic.headerSettings')"
                  :disabled="loading || sessionsLoading || streaming"
                  :title="t('agentic.headerSettings')"
                  circle
                  size="small"
                >
                  <el-icon>
                    <Setting/>
                  </el-icon>
                </el-button>
              </template>
              <div class="agentic-settings">
                <div class="agentic-setting">
                  <span>{{ t('agentic.temperature') }}</span>
                  <el-slider
                    v-model="temperatureProxy"
                    :disabled="loading || sessionsLoading || streaming"
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

            <el-dropdown
              :disabled="loading || sessionsLoading || streaming"
              max-height="360"
              :teleported="false"
              trigger="click"
              @command="handleHistoryCommand"
            >
              <el-button
                :aria-label="t('agentic.headerHistory')"
                :disabled="loading || sessionsLoading || streaming"
                :title="t('agentic.headerHistory')"
                circle
                size="small"
              >
                <el-icon>
                  <Clock/>
                </el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="session in conversationItems"
                    :key="session.conversationId"
                    :command="`select:${session.conversationId}`"
                    :disabled="Boolean(sessionActionLoading[session.conversationId])"
                  >
                    <span :class="`is-${session.sessionExt?.icon || 'monitor'}`" class="agentic-history-icon">
                      <el-icon><component :is="sessionIcon(session.sessionExt?.icon)"/></el-icon>
                    </span>
                    <span class="agentic-history-item">
                      <strong>{{ session.title }}</strong>
                      <small v-if="session.summary">{{ session.summary }}</small>
                    </span>
                  </el-dropdown-item>
                  <el-dropdown-item v-if="conversationItems.length === 0" disabled
                  >{{ t('agentic.headerNoHistory') }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-popover
              v-model:visible="renamePopoverVisible"
              :teleported="false"
              placement="bottom-end"
              trigger="click"
              width="280"
            >
              <template #reference>
                <el-button
                  :aria-label="t('agentic.headerRename')"
                  :disabled="
                    !activeConversationId ||
                    loading ||
                    sessionsLoading ||
                    streaming ||
                    Boolean(sessionActionLoading[activeConversationId])
                  "
                  :title="t('agentic.headerRename')"
                  circle
                  size="small"
                  @click="prepareRenameTitle"
                >
                  <el-icon>
                    <EditPen/>
                  </el-icon>
                </el-button>
              </template>
              <div class="agentic-rename-form">
                <span class="agentic-popover-title">{{ t('agentic.dialogConversationTitle') }}</span>
                <el-input
                  v-model="renameTitle"
                  :disabled="loading || sessionsLoading || streaming || Boolean(sessionActionLoading[activeConversationId])"
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
                  <el-button
                    :disabled="
                      !renameTitle.trim() ||
                      loading ||
                      sessionsLoading ||
                      streaming ||
                      Boolean(sessionActionLoading[activeConversationId])
                    "
                    :loading="Boolean(sessionActionLoading[activeConversationId])"
                    size="small"
                    type="primary"
                    @click="handleRenameCurrent"
                  >
                    {{ t('agentic.dialogSave') }}
                  </el-button>
                </div>
              </div>
            </el-popover>
            <el-tooltip :content="t('agentic.headerDelete')">
              <el-button
                :aria-label="t('agentic.headerDelete')"
                :disabled="
                  !activeConversationId ||
                  loading ||
                  sessionsLoading ||
                  streaming ||
                  Boolean(sessionActionLoading[activeConversationId])
                "
                circle
                size="small"
                @click="handleDeleteCurrent"
              >
                <el-icon>
                  <Delete/>
                </el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>
      </header>

      <div v-if="sessionsError" class="agentic-session-error" role="alert">
        <span>{{ t('agentic.failedSessions') }}</span>
        <el-button :loading="sessionsLoading" link type="danger" @click="handleRetrySessions">
          {{ t('common.retry') }}
        </el-button>
      </div>

      <div v-if="currentMessagesError" class="agentic-data-error" role="alert">
        <span>{{ t('agentic.failedMessages') }}</span>
        <el-button :loading="currentMessagesLoading" link type="danger" @click="handleRetryMessages">
          {{ t('common.retry') }}
        </el-button>
      </div>

      <main ref="bodyRef" class="agentic-body">
        <div v-if="currentMessagesLoading && currentMessages.length === 0" class="agentic-loading-state">
          <el-skeleton :rows="4" animated />
        </div>
        <div v-else-if="currentMessages.length === 0" class="agentic-empty">
          <Welcome
            :description="t('agentic.welcomeDescription')"
            :icon="assetUrl('images/logo/logo.svg')"
            :title="t('agentic.title')"
            variant="borderless"
          />
          <Prompts :items="promptItems" :wrap="true" @item-click="handlePromptClick"/>
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
                      <CircleCheck v-if="!message.streaming"/>
                      <Loading v-else/>
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
                        <i/>
                        <i/>
                        <i/>
                      </span>
                    </div>
                  </div>
                </details>
                <RenderedAssistantMessage
                  v-if="message.role === 'assistant'"
                  :charts="message.contentExt?.charts || []"
                  :content="message.content"
                />
                <div v-else class="agentic-user-content">
                  <div v-if="userMessageParts(message.content).quote" class="agentic-user-quote">
                    <div class="agentic-user-quote__header">
                      <el-icon>
                        <ChatLineSquare/>
                      </el-icon>
                      <span>{{ userMessageParts(message.content).label }}</span>
                    </div>
                    <p>{{ userMessageParts(message.content).quote }}</p>
                  </div>
                  <div v-if="userMessageParts(message.content).body" class="agentic-text">
                    {{ userMessageParts(message.content).body }}
                  </div>
                </div>
                <span
                  v-if="message.streaming && !message.content && !hasReasoningPanel(message)"
                  class="agentic-cursor"
                >
                  <span>{{ t('agentic.thinking') }}</span>
                  <span aria-hidden="true" class="agentic-thinking-dots">
                    <i/>
                    <i/>
                    <i/>
                  </span>
                </span>
                <div
                  v-if="message.role === 'assistant' && !message.streaming && truncatedReason(message)"
                  class="agentic-message__warning"
                >
                  <el-icon>
                    <Warning/>
                  </el-icon>
                  <span>{{ truncatedReason(message) }}</span>
                </div>
                <details v-if="message.role === 'assistant' && hasAssistantDetails(message)" class="agentic-details">
                  <summary>
                    <el-icon>
                      <Cpu/>
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
                  <button
                    :aria-label="t('agentic.actionCopy')"
                    class="agentic-message__action"
                    type="button"
                    @click="handleCopyMessage(message)"
                  >
                    <el-icon>
                      <DocumentCopy/>
                    </el-icon>
                  </button>
                </el-tooltip>
                <el-tooltip :content="t('agentic.actionQuote')" placement="top">
                  <button
                    :aria-label="t('agentic.actionQuote')"
                    class="agentic-message__action"
                    type="button"
                    @click="handleQuoteMessage(message)"
                  >
                    <el-icon>
                      <ChatLineSquare/>
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
            <span class="agentic-action__icon">
              <el-icon><component :is="actionIcon(action.actionType)"/></el-icon>
            </span>
            <div class="agentic-action__content">
              <strong>{{ action.title }}</strong>
              <span>{{ action.description }}</span>
              <span v-if="actionErrors[action.actionId]" class="agentic-action__error" role="alert">
                {{ t('agentic.actionFailed') }}
              </span>
            </div>
            <el-tag size="small" type="warning">{{ t('agentic.pending') }}</el-tag>
            <div class="agentic-action__buttons">
              <el-button
                :disabled="interactionLocked || Boolean(actionLoading[action.actionId])"
                :loading="Boolean(actionLoading[action.actionId])"
                size="small"
                type="primary"
                @click="handleConfirmAction(action.actionId)"
              >
                <el-icon>
                  <Check/>
                </el-icon>
                {{ t('agentic.confirm') }}
              </el-button>
              <el-button
                :disabled="interactionLocked || Boolean(actionLoading[action.actionId])"
                size="small"
                @click="handleRejectAction(action.actionId)"
              >
                <el-icon>
                  <CircleClose/>
                </el-icon>
                {{ t('agentic.reject') }}
              </el-button>
            </div>
          </div>
        </div>
        <div v-if="currentPendingActionsError" class="agentic-data-error" role="alert">
          <span>{{ t('agentic.failedActions') }}</span>
          <el-button :loading="currentPendingActionsLoading" link type="danger" @click="handleRetryPendingActions">
            {{ t('common.retry') }}
          </el-button>
        </div>

        <div class="agentic-input-shell">
          <div v-if="currentAttachmentsError" class="agentic-data-error" role="alert">
            <span>{{ t('agentic.failedAttachments') }}</span>
            <el-button :loading="currentAttachmentsLoading" link type="danger" @click="handleRetryAttachments">
              {{ t('common.retry') }}
            </el-button>
          </div>
          <div v-if="quotedMessage" class="agentic-quote-preview">
            <span class="agentic-quote-preview__icon">
              <el-icon><ChatLineSquare/></el-icon>
            </span>
            <div class="agentic-quote-preview__content">
              <span class="agentic-quote-preview__meta">
                {{ t('agentic.quotePreview') }}
                <strong>{{ quoteLabel(quotedMessage.role) }}</strong>
              </span>
              <p>{{ quotePreview }}</p>
            </div>
            <button
              :aria-label="t('agentic.quoteRemove')"
              :title="t('agentic.quoteRemove')"
              class="agentic-quote-preview__close"
              type="button"
              @click="quotedMessage = undefined"
            >
              <el-icon>
                <Close/>
              </el-icon>
            </button>
          </div>

          <div v-if="currentAttachments.length" class="agentic-attachments">
            <el-tag
              v-for="attachment in currentAttachments"
              :key="attachment.id"
              class="agentic-attachment"
              :closable="!interactionLocked"
              @close="agenticStore.removeLocalAttachment(attachment.id)"
            >
              <el-icon>
                <Document/>
              </el-icon>
              <span>{{ attachment.fileName }}</span>
              <small>{{ formatFileSize(attachment.size) }}</small>
            </el-tag>
          </div>

          <el-input
            v-model="draft"
            :aria-label="t('agentic.composerPlaceholder')"
            :autosize="{minRows: 2, maxRows: 6}"
            :disabled="loading || sessionsLoading || streaming"
            :placeholder="t('agentic.composerPlaceholder')"
            class="agentic-input"
            resize="none"
            type="textarea"
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
  Connection,
  Cpu,
  DataAnalysis,
  Delete,
  Document,
  DocumentCopy,
  EditPen,
  Lightning,
  Loading,
  Monitor,
  Odometer,
  Operation,
  Paperclip,
  Plus,
  Promotion,
  Setting,
  Tools,
  TrendCharts,
  VideoPause,
  Warning,
  WarningFilled,
} from '@element-plus/icons-vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import {storeToRefs} from 'pinia';
import {useI18n} from 'vue-i18n';
import {computed, nextTick, onBeforeUnmount, ref, watch} from 'vue';
import {useBreakpoint} from '@/composables/useBreakpoint';
import {useMediaQuery} from '@/composables/useMediaQuery';
import type {
  AgenticMessage,
  AgenticMessageContext,
  AgenticMessageTokens,
  AgenticSessionExt,
  AgenticTraceEvent,
} from '@/config/types';
import {useAgenticStore} from '@/store';
import RenderedAssistantMessage from './RenderedAssistantMessage.vue';
import {toPlainText} from './assistantContent';
import {assetUrl} from '@/utils/assetUrl';

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

interface UserMessageParts {
  body: string;
  label?: string;
  quote?: string;
}

const agenticStore = useAgenticStore();

const {t, locale} = useI18n();
const {isMobile} = useBreakpoint();
const prefersReducedMotion = useMediaQuery('(prefers-reduced-motion: reduce)');
const {
  visible,
  loading,
  sessionsLoading,
  sessionsError,
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
  currentMessagesLoading,
  currentMessagesError,
  currentAttachmentsLoading,
  currentAttachmentsError,
  currentPendingActionsLoading,
  currentPendingActionsError,
  sessionActionLoading,
  actionLoading,
  actionErrors,
} = storeToRefs(agenticStore);

const draft = ref('');
const quotedMessage = ref<AgenticMessage>();
const fileInputRef = ref<HTMLInputElement>();
const bodyRef = ref<HTMLElement>();
const panelRef = ref<HTMLElement>();
const panelWidth = ref<number>();
const resizeFrame = ref<number>();
const obscuredElements = ref<Array<{element: HTMLElement; wasInert: boolean}>>([]);
const lastFocusElement = ref<HTMLElement>();
const renamePopoverVisible = ref(false);
const renameTitle = ref('');
let localeReloadToken = 0;
const ASSISTANT_WIDTH_STORAGE_KEY = 'dc3-agentic-panel-width';
const MIN_PANEL_WIDTH = 320;
const MAX_PANEL_WIDTH = 520;

const interactionLocked = computed(() => loading.value || sessionsLoading.value || streaming.value);

const storedPanelWidth = Number(localStorage.getItem(ASSISTANT_WIDTH_STORAGE_KEY) || '');
if (Number.isFinite(storedPanelWidth) && storedPanelWidth > 0) {
  panelWidth.value = Math.min(Math.max(storedPanelWidth, MIN_PANEL_WIDTH), MAX_PANEL_WIDTH);
}

const effectivePanelWidth = computed(() => clamp(panelWidth.value || 340, MIN_PANEL_WIDTH, MAX_PANEL_WIDTH));
const panelStyle = computed(() => ({
  width: isMobile.value
    ? '100%'
    : panelWidth.value
      ? `min(${effectivePanelWidth.value}px, 45vw)`
      : 'clamp(340px, 30vw, 520px)',
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

const quoteExcerpt = (content: string) => {
  return toPlainText(content)
    .replace(/^\s{0,3}#{1,6}\s+/gm, '')
    .replace(/^\s*(?:[-*+] |\d+\.\s+)/gm, '')
    .replace(/\[([^\]]+)]\([^)]+\)/g, '$1')
    .replace(/[*_`~]+/g, '')
    .replace(/\s+/g, ' ')
    .trim();
};

const quotePreview = computed(() => {
  if (!quotedMessage.value) return '';
  return quoteExcerpt(quotedMessage.value.content);
});

const conversationItems = computed(() => {
  return sessions.value.map((session) => ({
    ...session,
    title: session.title || t('agentic.newConversation'),
  }));
});

const sessionIcons = {
  monitor: Monitor,
  warning: WarningFilled,
  trend: TrendCharts,
  connection: Connection,
  odometer: Odometer,
  tools: Tools,
  operation: Operation,
  lightning: Lightning,
} as const;

const sessionIcon = (icon?: AgenticSessionExt['icon']) => sessionIcons[icon || 'monitor'] || Monitor;

const actionIcon = (actionType: string) => {
  if (actionType.includes('WRITE')) return Lightning;
  if (actionType.includes('CONFIG')) return Setting;
  if (actionType.includes('WORK_ORDER')) return Tools;
  return DataAnalysis;
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
      if (!lastFocusElement.value || !document.contains(lastFocusElement.value)) {
        const activeElement = document.activeElement;
        lastFocusElement.value = activeElement instanceof HTMLElement ? activeElement : undefined;
      }
      void nextTick(() => {
        scrollToBottom('auto');
        if (isMobile.value) {
          obscureBackground();
          panelRef.value?.focus();
        }
      });
    } else {
      restoreMainContent();
      void nextTick(() => {
        const target = lastFocusElement.value || document.querySelector<HTMLElement>('.agentic-launcher');
        if (target && document.contains(target)) target.focus();
        lastFocusElement.value = undefined;
      });
    }
  }
);

watch(isMobile, (mobile) => {
  if (!visible.value) return;
  if (mobile) {
    void nextTick(() => {
      obscureBackground();
      panelRef.value?.focus();
    });
  } else {
    restoreMainContent();
  }
});

watch(activeConversationId, () => {
  quotedMessage.value = undefined;
});

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

watch(
  () => locale.value,
  async () => {
    if (!visible.value) return;
    const token = ++localeReloadToken;
    const conversationId = activeConversationId.value;
    await agenticStore.loadSessions();
    if (token !== localeReloadToken || conversationId !== activeConversationId.value) return;
    if (conversationId) await agenticStore.selectSession(conversationId);
  }
);

onBeforeUnmount(() => {
  localeReloadToken += 1;
  if (resizeFrame.value) {
    cancelAnimationFrame(resizeFrame.value);
  }
  window.removeEventListener('mousemove', handleResizeMove);
  window.removeEventListener('mouseup', handleResizeEnd);
  document.body.classList.remove('agentic-resizing');
  restoreMainContent();
  if (streaming.value) agenticStore.stopStreaming();
});

const handleNewSession = () => {
  if (interactionLocked.value) return;
  agenticStore.newSession();
};

const handleRetrySessions = async () => {
  if (interactionLocked.value) return;
  await agenticStore.retrySessions();
};

const handleHistoryCommand = async (command: string) => {
  if (interactionLocked.value) return;
  if (command.startsWith('select:')) {
    await agenticStore.selectSession(command.replace('select:', ''));
  }
};

const prepareRenameTitle = () => {
  renameTitle.value = currentSession.value?.title || t('agentic.newConversation');
};

const handleRenameCurrent = async () => {
  if (interactionLocked.value) return;
  const conversationId = activeConversationId.value;
  const title = renameTitle.value.trim();
  if (!conversationId || !title) {
    return;
  }
  const updated = await agenticStore.renameSession(conversationId, title);
  if (updated) renamePopoverVisible.value = false;
};

const handleDeleteCurrent = async () => {
  if (interactionLocked.value) return;
  const conversationId = activeConversationId.value;
  if (!conversationId) {
    return;
  }
  try {
    await ElMessageBox.confirm(t('agentic.dialogDeleteConfirm'), t('agentic.dialogDeleteTitle'), {
      type: 'warning',
      confirmButtonText: t('agentic.dialogDeleteTitle'),
      cancelButtonText: t('agentic.dialogCancel'),
    });
    await agenticStore.deleteSession(conversationId);
  } catch {
    // User cancelled or the operation failed; the store keeps the session intact.
  }
};

const handlePromptClick = (item: AssistantPromptItem) => {
  if (interactionLocked.value) return;
  const description = item.description ? ` ${item.description}` : '';
  draft.value = `${item.label || ''}${description}`.trim();
};

const handleModelChange = (model: string) => {
  if (interactionLocked.value) return;
  void agenticStore.setSelectedModel(model);
};

const handlePrefsChange = () => {
  if (interactionLocked.value) return;
  void agenticStore.persistCurrentSessionPrefs();
};

const handleRetryMessages = () => {
  if (interactionLocked.value || !activeConversationId.value) return;
  void agenticStore.loadMessages(activeConversationId.value);
};

const handleRetryAttachments = () => {
  if (interactionLocked.value || !activeConversationId.value) return;
  void agenticStore.loadAttachments(activeConversationId.value);
};

const handleRetryPendingActions = () => {
  if (interactionLocked.value || !activeConversationId.value) return;
  void agenticStore.loadPendingActions(activeConversationId.value);
};

const handleResizeStart = (event: MouseEvent) => {
  if (isMobile.value) return;
  event.preventDefault();
  window.addEventListener('mousemove', handleResizeMove);
  window.addEventListener('mouseup', handleResizeEnd);
  document.body.classList.add('agentic-resizing');
};

const setPanelWidth = (width: number) => {
  const containerWidth = panelRef.value?.parentElement?.getBoundingClientRect().width || window.innerWidth;
  const maxWidth = Math.min(MAX_PANEL_WIDTH, Math.floor(containerWidth * 0.45));
  panelWidth.value = clamp(width, MIN_PANEL_WIDTH, Math.max(MIN_PANEL_WIDTH, maxWidth));
};

const handleResizeMove = (event: MouseEvent) => {
  const container = panelRef.value?.parentElement;
  const rect = container?.getBoundingClientRect();
  if (!rect) {
    return;
  }
  setPanelWidth(Math.round(rect.right - event.clientX));
};

const handleResizeKeydown = (delta: number) => {
  if (isMobile.value) return;
  setPanelWidth(effectivePanelWidth.value + delta);
  if (panelWidth.value) localStorage.setItem(ASSISTANT_WIDTH_STORAGE_KEY, String(panelWidth.value));
};

const handleResizeEnd = () => {
  window.removeEventListener('mousemove', handleResizeMove);
  window.removeEventListener('mouseup', handleResizeEnd);
  document.body.classList.remove('agentic-resizing');
  if (panelWidth.value) {
    localStorage.setItem(ASSISTANT_WIDTH_STORAGE_KEY, String(panelWidth.value));
  }
};

const handlePanelKeydown = (event: KeyboardEvent) => {
  if (!isMobile.value) return;
  if (event.key === 'Escape') {
    event.preventDefault();
    agenticStore.close();
    return;
  }
  if (event.key !== 'Tab') return;
  const focusable = Array.from(
    panelRef.value?.querySelectorAll<HTMLElement>(
      'button:not([disabled]), [href], input:not([disabled]), textarea:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
    ) || []
  ).filter((element) => element.getAttribute('aria-hidden') !== 'true');
  if (focusable.length === 0) {
    event.preventDefault();
    panelRef.value?.focus();
    return;
  }
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && (document.activeElement === first || document.activeElement === panelRef.value)) {
    event.preventDefault();
    last?.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first?.focus();
  }
};

function obscureBackground() {
  restoreMainContent();
  const elements = [
    document.querySelector<HTMLElement>('.header'),
    panelRef.value?.parentElement?.querySelector<HTMLElement>('.body-main'),
  ].filter((element): element is HTMLElement => Boolean(element));
  obscuredElements.value = elements.map((element) => ({element, wasInert: element.hasAttribute('inert')}));
  for (const {element} of obscuredElements.value) element.setAttribute('inert', '');
}

function restoreMainContent() {
  for (const {element, wasInert} of obscuredElements.value) {
    if (!wasInert) element.removeAttribute('inert');
  }
  obscuredElements.value = [];
}

const handleLauncherClick = () => {
  const activeElement = document.activeElement;
  lastFocusElement.value = activeElement instanceof HTMLElement ? activeElement : undefined;
  void agenticStore.toggle();
};

const handleSubmit = async () => {
  if (loading.value || sessionsLoading.value || streaming.value) return;
  const messageBody = draft.value.trim() || (currentAttachments.value.length ? t('agentic.attachAnalyze') : '');
  if (!messageBody) {
    return;
  }
  const content = quotedMessage.value ? formatQuotedMessage(quotedMessage.value, messageBody) : messageBody;
  const sent = await agenticStore.sendMessage(content);
  if (sent) {
    draft.value = '';
    quotedMessage.value = undefined;
  }
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
    if (await agenticStore.rejectAction(actionId)) ElMessage.success(t('agentic.actionRejected'));
  } catch {
    // Keep the action visible so it can be retried.
  }
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
  quotedMessage.value = message;
};

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

const userMessageParts = (content: string): UserMessageParts => {
  const lines = content.split('\n');
  const labelMatch = lines[0]?.match(/^> \*\*(.+)\*\*$/);
  if (!labelMatch) {
    return {body: content};
  }

  const quoteLines: string[] = [];
  let index = 1;
  while (index < lines.length) {
    const line = lines[index];
    if (!line?.startsWith('>')) break;
    quoteLines.push(line.replace(/^> ?/, ''));
    index += 1;
  }

  return {
    label: labelMatch[1],
    quote: quoteExcerpt(quoteLines.join('\n')),
    body: lines.slice(index).join('\n').trim(),
  };
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
  if (message.status === 'CANCELLED') {
    return t('agentic.statusCancelled');
  }
  if (message.status === 'FAILED') {
    return t('agentic.statusFailed');
  }
  const reason = message.finishReason?.toLowerCase();
  if (reason === 'cancelled' || reason === 'canceled') {
    return t('agentic.statusCancelled');
  }
  if (reason === 'error' || reason === 'failed') {
    return t('agentic.statusFailed');
  }
  if (message.contentExt?.recovered) {
    return t('agentic.statusDone');
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
    void nextTick(() => scrollToBottom(prefersReducedMotion.value ? 'auto' : 'smooth'));
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
  right: var(--dc3-space-4);
  bottom: var(--dc3-space-4);
  box-sizing: border-box;
  z-index: var(--dc3-z-floating-action);
  width: var(--dc3-touch-target);
  height: var(--dc3-touch-target);
  box-shadow: var(--dc3-shadow-md);
}

.agentic-panel {
  position: relative;
  flex: 0 0 auto;
  min-width: 320px;
  max-width: 520px;
  height: 100%;
  min-height: 0;
  border-left: 1px solid var(--dc3-border-base);
  background: var(--dc3-bg-elevated-strong);
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
    background: var(--el-color-primary);
  }
}

.agentic-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  background: var(--dc3-bg-elevated-strong);
}

.agentic-header {
  display: flex;
  align-items: stretch;
  flex-direction: column;
  justify-content: space-between;
  gap: 10px;
  min-height: 0;
  padding: 10px 10px 9px;
  border-bottom: 1px solid var(--dc3-border-base);
  background: var(--dc3-bg-elevated-strong);
}

.agentic-session-error,
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

.agentic-data-error {
  flex: 0 0 auto;
}

.agentic-loading-state {
  padding: var(--dc3-space-4);
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
  border: 1px solid var(--dc3-border-base);
  border-radius: 8px;
  background: var(--dc3-bg-muted);

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
    color: var(--dc3-text-primary);
    font-size: 15px;
  }

  span {
    overflow: hidden;
    max-width: 100%;
    color: var(--dc3-text-regular);
    font-size: 12px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.agentic-history-item {
  display: inline-flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
  max-width: 300px;

  strong,
  small {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: var(--el-text-color-primary);
    font-size: 13px;
    font-weight: 600;
  }

  small {
    margin-top: 2px;
    color: var(--el-text-color-secondary);
    font-size: 11px;
  }
}

.agentic-history-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  margin-right: 9px;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  border-radius: 9px;

  &.is-warning {
    color: var(--el-color-danger);
    background: var(--el-color-danger-light-9);
  }

  &.is-trend {
    color: var(--el-color-success);
    background: var(--el-color-success-light-9);
  }

  &.is-connection {
    color: var(--dc3-color-purple);
    background: var(--dc3-color-purple-soft);
  }

  &.is-odometer {
    color: var(--dc3-text-brand);
    background: var(--dc3-bg-interactive);
  }

  &.is-tools {
    color: var(--el-color-warning-dark-2);
    background: var(--el-color-warning-light-9);
  }

  &.is-operation {
    color: var(--dc3-color-purple);
    background: var(--dc3-color-purple-soft);
  }

  &.is-lightning {
    color: var(--el-color-danger-dark-2);
    background: var(--el-color-danger-light-9);
  }
}

.agentic-body {
  position: relative;
  flex: 1;
  min-height: 0;
  padding: 12px 4px;
  overflow-x: hidden;
  overflow-y: auto;
  background: var(--dc3-bg-elevated-strong);
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
    color: var(--el-color-white);
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
  border: 1px solid var(--dc3-border-base);
  border-radius: 8px;
  color: var(--dc3-text-primary);
  line-height: 1.58;
  overflow-wrap: anywhere;
  background: var(--dc3-bg-elevated-strong);
}

.agentic-text {
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.agentic-user-content {
  min-width: 0;
}

.agentic-user-quote {
  position: relative;
  max-width: 100%;
  margin-bottom: 8px;
  padding: 8px 10px;
  overflow: hidden;
  border: 1px solid color-mix(in srgb, var(--el-color-white) 28%, transparent);
  border-left: 3px solid color-mix(in srgb, var(--el-color-white) 82%, transparent);
  border-radius: 6px;
  background: color-mix(in srgb, var(--el-color-white) 13%, transparent);

  &::after {
    position: absolute;
    right: 0;
    bottom: 0;
    left: 0;
    height: 18px;
    pointer-events: none;
    content: '';
    background: linear-gradient(transparent, color-mix(in srgb, var(--el-color-primary) 88%, transparent));
  }

  p {
    display: -webkit-box;
    margin: 4px 0 0;
    overflow: hidden;
    color: color-mix(in srgb, var(--el-color-white) 88%, transparent);
    font-size: 12px;
    line-height: 1.5;
    white-space: pre-wrap;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 3;
  }
}

.agentic-user-quote__header {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--el-color-white);
  font-size: 11px;
  font-weight: 700;
}

.agentic-message__warning {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin-top: 8px;
  padding: 6px 8px;
  border: 1px solid var(--el-color-warning-light-5);
  border-radius: 4px;
  color: var(--el-color-warning-dark-2);
  font-size: 12px;
  line-height: 1.5;
  background: var(--el-color-warning-light-9);
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
  transition: opacity 0.16s ease,
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
  border: 1px solid var(--dc3-border-base);
  border-radius: 4px;
  color: var(--dc3-text-regular);
  font-size: 13px;
  background: var(--dc3-bg-elevated-strong);
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
  color: var(--dc3-text-regular);
  font-size: 12px;
}

.agentic-reasoning-panel {
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  margin-bottom: 8px;
  border: 1px solid var(--dc3-border-base);
  border-radius: 6px;
  background: var(--dc3-bg-muted);

  summary {
    display: flex;
    align-items: center;
    gap: 7px;
    min-height: 30px;
    padding: 0 8px;
    color: var(--dc3-text-primary);
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
      border-right: 1px solid var(--dc3-text-muted);
      border-bottom: 1px solid var(--dc3-text-muted);
      transform: rotate(45deg);
      transition: transform 0.16s ease;
    }

    small {
      overflow: hidden;
      color: var(--dc3-text-regular);
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
  color: var(--dc3-text-regular);
  font-size: 12px;
  line-height: 1.55;
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.agentic-live-thinking {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--dc3-text-regular);
  font-size: 12px;
}

.agentic-thinking-pulse {
  font-size: 14px;
  color: var(--dc3-text-muted);
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
        var(--dc3-highlight-sheen) 45%,
        var(--dc3-bg-elevated-strong) 50%,
        var(--dc3-highlight-sheen) 55%,
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
  border-top: 1px solid var(--dc3-border-base);

  summary {
    display: flex;
    align-items: center;
    gap: 6px;
    min-height: 28px;
    color: var(--dc3-text-regular);
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
      border-right: 1px solid var(--dc3-text-muted);
      border-bottom: 1px solid var(--dc3-text-muted);
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
  border: 1px solid var(--dc3-border-base);
  border-radius: 6px;
  background: var(--dc3-bg-muted);

  span {
    overflow: hidden;
    color: var(--dc3-text-regular);
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    overflow: hidden;
    color: var(--dc3-text-primary);
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
  border: 1px solid var(--dc3-border-base);
  border-radius: 6px;
  background: var(--dc3-bg-elevated-strong);
}

.agentic-trace-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;

  span {
    color: var(--dc3-text-primary);
    font-size: 12px;
    font-weight: 700;
  }

  strong {
    color: var(--el-color-primary);
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
  border-left: 2px solid var(--el-color-primary-light-5);

  span {
    color: var(--dc3-text-primary);
    font-size: 12px;
    font-weight: 600;
  }

  small {
    color: var(--dc3-text-regular);
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
  color: var(--el-color-primary-dark-2);
  font-size: 11px;
  font-weight: 700;
  background: var(--el-color-primary-light-9);
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
    color: var(--dc3-text-primary);
    font-size: 12px;
  }

  small {
    color: var(--dc3-text-regular);
    font-size: 11px;
    line-height: 1.45;
  }

  em {
    color: var(--dc3-text-regular);
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
    color: var(--dc3-text-regular);
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
  background: var(--dc3-bg-muted);

  span {
    overflow: hidden;
    color: var(--dc3-text-regular);
    font-size: 11px;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  strong {
    color: var(--dc3-text-primary);
    font-size: 12px;
    font-weight: 700;
  }
}

.agentic-details__row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  min-width: 0;
  color: var(--dc3-text-regular);
  font-size: 12px;
}

.agentic-details__row--stack {
  flex-direction: column;
}

.agentic-details__label {
  flex: 0 0 58px;
  color: var(--dc3-text-regular);
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
  color: var(--dc3-text-regular);
  background: var(--dc3-bg-muted);
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
    border: 1px solid var(--dc3-border-base);
    border-radius: 6px;
    color: var(--dc3-text-primary);
    white-space: pre-wrap;
    background: var(--dc3-bg-muted);
  }
}

.agentic-composer {
  padding: 10px 4px 12px;
  border-top: 1px solid var(--dc3-border-base);
  background: var(--dc3-bg-elevated-strong);
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
  grid-template-columns: auto minmax(0, 1fr) auto auto;
  gap: 10px;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid var(--el-color-warning-light-5);
  border-radius: 6px;
  background: var(--el-color-warning-light-9);
}

.agentic-action__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  color: var(--el-color-warning-dark-2);
  background: var(--el-color-warning-light-9);
  border-radius: 9px;
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
    color: var(--dc3-text-primary);
    font-size: 13px;
  }

  span {
    color: var(--dc3-text-regular);
    font-size: 12px;
  }
}

.agentic-action__content .agentic-action__error {
  color: var(--el-color-danger);
  overflow-wrap: anywhere;
  text-overflow: initial;
  white-space: normal;
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
  border: 1px solid var(--dc3-border-strong);
  border-radius: 8px;
  background: var(--dc3-bg-elevated-strong);
}

.agentic-quote-preview {
  position: relative;
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr) 24px;
  gap: 9px;
  align-items: start;
  min-width: 0;
  margin-bottom: 9px;
  padding: 9px 8px 9px 10px;
  overflow: hidden;
  border: 1px solid var(--el-color-primary-light-5);
  border-radius: 8px;
  background: var(--dc3-brand-gradient-soft);
  box-shadow: inset 3px 0 0 var(--el-color-primary);
}

.agentic-quote-preview__icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 8px;
  color: var(--el-color-primary);
  font-size: 15px;
  background: var(--dc3-bg-elevated);
  box-shadow: var(--dc3-shadow-sm);
}

.agentic-quote-preview__content {
  min-width: 0;

  p {
    display: -webkit-box;
    margin: 3px 0 0;
    overflow: hidden;
    color: var(--dc3-text-regular);
    font-size: 12px;
    line-height: 1.45;
    overflow-wrap: anywhere;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
  }
}

.agentic-quote-preview__meta {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--dc3-text-regular);
  font-size: 10px;
  line-height: 1.3;

  strong {
    padding: 1px 5px;
    border-radius: 999px;
    color: var(--el-color-primary-dark-2);
    font-size: 10px;
    font-weight: 700;
    background: var(--el-color-primary-light-9);
  }
}

.agentic-quote-preview__close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 0;
  border-radius: 6px;
  color: var(--dc3-text-regular);
  background: transparent;
  cursor: pointer;
  transition: color 0.16s ease, background 0.16s ease;

  &:hover,
  &:focus-visible {
    color: var(--el-color-danger);
    background: var(--el-color-danger-light-9);
    outline: none;
  }
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
  color: var(--dc3-text-primary);
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
    color: var(--dc3-text-primary);
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
    position: relative;
    box-sizing: border-box;
    max-width: 100%;
    margin: 8px 0;
    padding: 10px 12px 10px 38px;
    overflow-wrap: anywhere;
    border: 1px solid var(--el-color-primary-light-5);
    border-left: 3px solid var(--el-color-primary-light-3);
    border-radius: 7px;
    color: var(--dc3-text-primary);
    background: var(--dc3-brand-gradient-soft);
    box-shadow: var(--dc3-shadow-sm);

    &::before {
      position: absolute;
      top: 5px;
      left: 11px;
      color: var(--el-color-primary-light-3);
      content: '\201C';
      font-family: Georgia, serif;
      font-size: 30px;
      font-weight: 700;
      line-height: 1;
    }

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
    background: var(--dc3-border-base);
  }

  code {
    padding: 2px 4px;
    border-radius: 4px;
    background: var(--dc3-bg-muted);
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 12px;
  }

  pre {
    box-sizing: border-box;
    max-width: 100%;
    overflow: auto;
    padding: 10px;
    border: 1px solid var(--dc3-border-base);
    border-radius: 6px;
    background: var(--dc3-bg-muted);

    code {
      padding: 0;
      color: var(--dc3-text-primary);
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
    border: 1px solid var(--dc3-border-base);
  }
}

@media (max-width: $breakpoint-sm-max) {
  .agentic-panel {
    width: clamp(320px, 40vw, 420px) !important;
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

@media (max-width: $breakpoint-xs-max) {
  .agentic-launcher {
    right: var(--dc3-space-3);
    bottom: calc(var(--dc3-space-4) + env(safe-area-inset-bottom));
  }

  .agentic-panel {
    position: absolute;
    inset: 0;
    z-index: 20;
    width: 100% !important;
    min-width: 0;
    max-width: none;
    border-left: 0;
  }

  .agentic-resizer {
    display: none;
  }

  .agentic-header,
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

@media (hover: none), (any-pointer: coarse) {
  .agentic-message__toolbar {
    height: 24px;
    margin-top: 4px;
    overflow: visible;
    opacity: 1;
    visibility: visible;
    pointer-events: auto;
    transform: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .agentic-thinking-pulse.is-active,
  .agentic-thinking-label.is-shimmer::after,
  .agentic-thinking-dots i {
    animation: none !important;
  }

  .agentic-message__toolbar,
  .agentic-message__action,
  .agentic-quote-preview__close,
  .agentic-resizer::after,
  .agentic-reasoning-panel summary::after,
  .agentic-details summary::after {
    transition: none;
  }
}
</style>

<style lang="scss">
.agentic-resizing {
  cursor: col-resize;
  user-select: none;
}
</style>
