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
    <el-button class="agentic-launcher" type="primary" circle @click="agenticStore.toggle">
      <el-icon>
        <ChatDotRound />
      </el-icon>
    </el-button>
  </el-tooltip>

  <el-drawer
    v-model="visible"
    custom-class="agentic-drawer"
    size="900px"
    :with-header="false"
    :close-on-click-modal="false"
    append-to-body
  >
    <section v-loading="loading" class="agentic-shell">
      <header class="agentic-header">
        <div class="agentic-title">
          <div class="agentic-mark">AI</div>
          <div>
            <strong>{{ t('agentic.title') }}</strong>
            <span>{{ currentSession?.title || t('agentic.newConversation') }}</span>
          </div>
        </div>

        <div class="agentic-header__actions">
          <el-button size="small" @click="handleNewSession">
            <el-icon>
              <Plus />
            </el-icon>
            New
          </el-button>
          <el-dropdown trigger="click" max-height="360" @command="handleHistoryCommand">
            <el-button size="small">
              <el-icon>
                <Clock />
              </el-icon>
              History
              <el-icon class="el-icon--right">
                <ArrowDown />
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
                <el-dropdown-item v-if="conversationItems.length === 0" disabled>{{
                  t('agentic.headerNoHistory')
                }}</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-tooltip :content="t('agentic.headerRename')">
            <el-button size="small" circle :disabled="!activeConversationId" @click="handleRenameCurrent">
              <el-icon>
                <EditPen />
              </el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip :content="t('agentic.headerDelete')">
            <el-button size="small" circle :disabled="!activeConversationId" @click="handleDeleteCurrent">
              <el-icon>
                <Delete />
              </el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip :content="t('agentic.headerClose')">
            <el-button size="small" circle @click="agenticStore.close">
              <el-icon>
                <Close />
              </el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </header>

      <main class="agentic-body">
        <div v-if="currentMessages.length === 0" class="agentic-empty">
          <Welcome
            icon="/images/logo/logo.png"
            :title="t('agentic.title')"
            :description="t('agentic.welcomeDescription')"
            variant="borderless"
          />
          <Prompts :items="promptItems" :wrap="true" @item-click="handlePromptClick" />
        </div>

        <div v-else class="agentic-messages">
          <article
            v-for="message in currentMessages"
            :key="message.id"
            class="agentic-message"
            :class="`agentic-message--${message.role}`"
          >
            <div class="agentic-message__avatar">
              <img :src="message.role === 'user' ? '/images/common/avatar.png' : '/images/common/llm.svg'" alt="" />
            </div>
            <div class="agentic-message__content">
              <div v-if="message.role === 'assistant'" class="agentic-markdown" v-html="renderMarkdown(message)" />
              <div v-else class="agentic-text">{{ message.content }}</div>
              <span v-if="message.streaming && !message.content" class="agentic-cursor">{{
                t('agentic.thinking')
              }}</span>
              <div v-if="message.reasoning" class="agentic-reasoning">
                <div class="agentic-reasoning__content" v-html="renderReasoning(message.reasoning)" />
              </div>
              <details
                v-if="message.role === 'assistant' && hasAssistantDetails(message)"
                class="agentic-details"
                :open="message.streaming"
              >
                <summary>
                  <el-icon>
                    <Cpu />
                  </el-icon>
                  <span>{{ assistantDetailSummary(message) }}</span>
                </summary>

                <div class="agentic-details__body">
                  <div class="agentic-run-overview">
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

                  <section v-if="assistantSkillSteps(message).length" class="agentic-trace-section">
                    <div class="agentic-trace-section__header">
                      <span>{{ t('agentic.detailSkillChain') }}</span>
                    </div>
                    <ol class="agentic-chain">
                      <li v-for="step in assistantSkillSteps(message)" :key="step.id">
                        <span class="agentic-chain__index">{{ step.index }}</span>
                        <div class="agentic-chain__content">
                          <strong>{{ step.label }}</strong>
                          <small v-if="step.detail">{{ step.detail }}</small>
                        </div>
                      </li>
                    </ol>
                  </section>

                  <section
                    v-if="assistantToolSteps(message).length || assistantAvailableTools(message).length"
                    class="agentic-trace-section"
                  >
                    <div class="agentic-trace-section__header">
                      <span>{{ t('agentic.detailToolChain') }}</span>
                    </div>
                    <div v-if="assistantAvailableTools(message).length" class="agentic-tool-scope">
                      <span>{{ t('agentic.detailScope') }}</span>
                      <div class="agentic-details__tags">
                        <el-tag v-for="tool in assistantAvailableTools(message)" :key="tool" size="small" type="info">
                          {{ tool }}
                        </el-tag>
                      </div>
                    </div>
                    <ol v-if="assistantToolSteps(message).length" class="agentic-chain">
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
            <el-tag type="warning" size="small">{{ t('agentic.pending') }}</el-tag>
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
            class="agentic-input"
            type="textarea"
            :autosize="{ minRows: 2, maxRows: 6 }"
            resize="none"
            :placeholder="t('agentic.composerPlaceholder')"
            :disabled="streaming"
            @keydown.enter.exact.prevent="handleSubmit"
          />

          <div class="agentic-composer__bar">
            <div class="agentic-composer__left">
              <input ref="fileInputRef" class="agentic-file" type="file" multiple @change="handleFileChange" />
              <el-tooltip :content="t('agentic.attachFile')">
                <el-button circle size="small" :disabled="streaming" @click="handlePickFile">
                  <el-icon>
                    <Paperclip />
                  </el-icon>
                </el-button>
              </el-tooltip>

              <el-select
                :model-value="selectedModel"
                class="agentic-model"
                size="small"
                filterable
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
                  :disabled="!activeModel.reasoning"
                  size="small"
                  inline-prompt
                  :active-text="t('agentic.think')"
                  :inactive-text="t('agentic.fast')"
                />
              </el-tooltip>

              <el-popover placement="top-start" trigger="click" width="300">
                <template #reference>
                  <el-button circle size="small">
                    <el-icon>
                      <Setting />
                    </el-icon>
                  </el-button>
                </template>
                <div class="agentic-settings">
                  <div class="agentic-setting">
                    <span>{{ t('agentic.temperature') }}</span>
                    <el-slider v-model="temperatureProxy" :min="0" :max="2" :step="0.1" size="small" />
                  </div>
                  <div class="agentic-setting">
                    <span>{{ t('agentic.maxTokens') }}</span>
                    <el-input-number
                      v-model="maxTokensProxy"
                      :min="1"
                      :step="256"
                      size="small"
                      controls-position="right"
                    />
                  </div>
                  <div class="agentic-setting agentic-setting--row">
                    <span>{{ t('agentic.confirmActions') }}</span>
                    <el-switch v-model="requireConfirmation" size="small" />
                  </div>
                  <div class="agentic-capabilities">
                    <el-tag :type="activeModel.stream ? 'success' : 'info'" size="small">{{
                      t('agentic.capStream')
                    }}</el-tag>
                    <el-tag :type="activeModel.toolCall ? 'success' : 'info'" size="small">{{
                      t('agentic.capTools')
                    }}</el-tag>
                    <el-tag :type="activeModel.vision ? 'success' : 'info'" size="small">{{
                      t('agentic.capVision')
                    }}</el-tag>
                    <el-tag :type="activeModel.reasoning ? 'success' : 'info'" size="small">{{
                      t('agentic.capReasoning')
                    }}</el-tag>
                  </div>
                </div>
              </el-popover>
            </div>

            <el-button v-if="streaming" class="agentic-send" type="warning" circle @click="agenticStore.stopStreaming">
              <el-icon>
                <VideoPause />
              </el-icon>
            </el-button>
            <el-button
              v-else
              class="agentic-send"
              type="primary"
              circle
              :disabled="!draft.trim() && !currentAttachments.length"
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
  </el-drawer>
</template>

<script lang="ts" setup>
  import 'vue-element-plus-x/styles/index.css';

  import { Prompts, Welcome } from 'vue-element-plus-x';
  import {
    ArrowDown,
    ChatDotRound,
    Check,
    CircleClose,
    Clock,
    Close,
    Cpu,
    Delete,
    Document,
    EditPen,
    Paperclip,
    Plus,
    Promotion,
    Setting,
    VideoPause,
  } from '@element-plus/icons-vue';
  import { ElMessage, ElMessageBox } from 'element-plus';
  import { marked } from 'marked';
  import { storeToRefs } from 'pinia';
  import { useI18n } from 'vue-i18n';
  import { computed, ref } from 'vue';
  import type { AgenticMessage, AgenticMessageContext, AgenticMessageTokens, AgenticTraceEvent } from '@/config/types';
  import { useAgenticStore } from '@/store';

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
  }

  interface AssistantTokenItem {
    label: string;
    value: string;
  }

  const agenticStore = useAgenticStore();

  const { t } = useI18n();
  const {
    visible,
    loading,
    streaming,
    sessions,
    models,
    selectedModel,
    reasoningEnabled,
    requireConfirmation,
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

  const promptItems = computed<AssistantPromptItem[]>(() => [
    { key: 'device-status', label: t('agentic.promptDeviceStatus'), description: t('agentic.promptDeviceStatusDesc') },
    { key: 'point-trend', label: t('agentic.promptPointTrend'), description: t('agentic.promptPointTrendDesc') },
    { key: 'driver-health', label: t('agentic.promptDriverHealth'), description: t('agentic.promptDriverHealthDesc') },
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

  const handleNewSession = () => {
    agenticStore.newSession();
  };

  const handleHistoryCommand = async (command: string) => {
    if (command.startsWith('select:')) {
      await agenticStore.selectSession(command.replace('select:', ''));
    }
  };

  const handleRenameCurrent = async () => {
    const conversationId = activeConversationId.value;
    if (!conversationId) {
      return;
    }
    const result = await ElMessageBox.prompt(t('agentic.dialogConversationTitle'), t('agentic.dialogRename'), {
      inputValue: currentSession.value?.title || t('agentic.newConversation'),
      confirmButtonText: t('agentic.dialogSave'),
      cancelButtonText: t('agentic.dialogCancel'),
    });
    await agenticStore.renameSession(conversationId, result.value);
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
        ElMessage.error(error instanceof Error ? error.message : 'Upload failed');
      }
    }
  };

  const handleConfirmAction = async (actionId: string) => {
    await ElMessageBox.confirm(t('agentic.confirm'), t('agentic.confirmActions'), {
      type: 'warning',
      confirmButtonText: t('agentic.confirm'),
      cancelButtonText: t('agentic.dialogCancel'),
    });
    await agenticStore.confirmAction(actionId);
    ElMessage.success('Action confirmed');
  };

  const handleRejectAction = async (actionId: string) => {
    await agenticStore.rejectAction(actionId);
    ElMessage.success('Action rejected');
  };

  const renderMarkdown = (message: AgenticMessage) => {
    return sanitizeHtml(String(marked.parse(message.content || (message.streaming ? t('agentic.thinking') : ''))));
  };

  const renderReasoning = (reasoning: string) => {
    if (!reasoning) return '';
    return sanitizeHtml(String(marked.parse(reasoning)));
  };

  const hasAssistantDetails = (message: AgenticMessage) => {
    return (
      assistantRunOverview(message).length > 0 ||
      assistantThinkingItems(message).length > 0 ||
      assistantSkillSteps(message).length > 0 ||
      assistantToolSteps(message).length > 0 ||
      assistantAvailableTools(message).length > 0 ||
      assistantReasoning(message) ||
      assistantContexts(message).length > 0 ||
      assistantTokenItems(message).length > 0
    );
  };

  const assistantDetailSummary = (message: AgenticMessage) => {
    const parts: string[] = [];
    const skills = assistantSkillSteps(message);
    const tools = assistantToolSteps(message);
    const contexts = assistantContexts(message);
    const tokenTotal = assistantTokenTotal(message);
    if (message.streaming && !currentTraceEvents.value.length) {
      parts.push(t('agentic.detailSummaryThinking'));
    }
    if (assistantReasoning(message)) {
      parts.push(t('agentic.thinkingReasoningMode').toLowerCase());
    }
    if (skills.length) {
      parts.push(t('agentic.detailSummarySkills', { n: skills.length }));
    }
    if (tools.length) {
      parts.push(t('agentic.detailSummaryTools', { n: tools.length }));
    }
    if (contexts.length) {
      parts.push(t('agentic.detailContexts'));
    }
    if (typeof tokenTotal === 'number') {
      parts.push(t('agentic.detailSummaryTokens', { n: formatCount(tokenTotal) }));
    }
    return parts.length ? parts.join(' · ') : t('agentic.detailSummaryThinking');
  };

  const assistantRunOverview = (message: AgenticMessage): AssistantRunStat[] => {
    const stats: AssistantRunStat[] = [];
    const skills = assistantSkillSteps(message).length;
    const tools = assistantToolSteps(message).length;
    const tokenTotal = assistantTokenTotal(message);
    stats.push({
      label: t('agentic.statusLabel'),
      value: message.streaming
        ? message.content
          ? t('agentic.statusStreaming')
          : t('agentic.statusThinking')
        : t('agentic.statusDone'),
    });
    if (assistantReasoning(message)) {
      stats.push({ label: t('agentic.thinkingReasoningMode'), value: t('agentic.statusEnabled') });
    }
    if (skills > 0) {
      stats.push({ label: t('agentic.detailSkillChain'), value: String(skills) });
    }
    if (tools > 0) {
      stats.push({ label: t('agentic.detailToolChain'), value: String(tools) });
    }
    if (typeof tokenTotal === 'number') {
      stats.push({ label: t('agentic.detailTokenUsage'), value: formatCount(tokenTotal) });
    }
    return stats;
  };

  const assistantThinkingItems = (message: AgenticMessage): AssistantThinkingItem[] => {
    const items: AssistantThinkingItem[] = [];
    const traces = assistantTraceEvents(message);
    const tools = assistantToolSteps(message).length;
    const skillSteps = assistantSkillSteps(message);
    if (message.streaming) {
      items.push({
        label: message.content ? t('agentic.thinkingGenerating') : t('agentic.thinkingPreparing'),
        detail: traces.length ? t('agentic.thinkingTraceCollecting') : t('agentic.thinkingWaitingChunk'),
      });
    }
    if (assistantReasoning(message)) {
      items.push({
        label: t('agentic.thinkingReasoningMode'),
        detail: t('agentic.thinkingReasoningDetail'),
      });
    }
    if (skillSteps.length) {
      items.push({
        label: t('agentic.thinkingSkillRouting'),
        detail: t('agentic.thinkingSkillDetail', { skill: skillSteps[0]?.label || 'general' }),
      });
    }
    if (tools > 0) {
      items.push({
        label: t('agentic.thinkingToolExec'),
        detail: t('agentic.thinkingToolDetail', { n: tools }),
      });
    }
    return uniqueThinkingItems(items);
  };

  const assistantSkills = (message: AgenticMessage) => {
    const persisted = message.contentExt?.skills || message.skills || [];
    const streaming = messageTraceEvents(message)
      .filter((event) => event.type === 'skill')
      .map((event) => event.name || event.title)
      .filter(Boolean);
    return uniqueStrings([...persisted, ...streaming]);
  };

  const assistantReasoning = (message: AgenticMessage) => {
    return Boolean(
      message.contentExt?.reasoning || assistantTraceEvents(message).some((event) => event.type === 'reasoning')
    );
  };

  const assistantTools = (message: AgenticMessage) => {
    const persisted = message.contentExt?.tools || [];
    const streaming = messageTraceEvents(message)
      .filter((event) => event.type === 'tool' && event.title !== 'Backend context loaded')
      .map((event) => event.name || event.title)
      .filter(Boolean);
    return uniqueStrings([...persisted, ...streaming]);
  };

  const assistantTraceEvents = (message: AgenticMessage): AgenticTraceEvent[] => {
    return uniqueTraceEvents([...(message.contentExt?.traces || []), ...messageTraceEvents(message)]);
  };

  const assistantSkillSteps = (message: AgenticMessage): AssistantChainStep[] => {
    const traceSteps = assistantTraceEvents(message)
      .filter((event) => event.type === 'skill')
      .map((event) => ({
        id: traceKey(event),
        index: 0,
        label: event.name || event.title || 'general',
        detail: event.detail || event.title,
      }));
    const fallbackSteps = assistantSkills(message).map((skill) => ({
      id: `skill-${skill}`,
      index: 0,
      label: skill,
      detail: '',
    }));
    return indexChainSteps(uniqueChainSteps([...traceSteps, ...fallbackSteps]));
  };

  const assistantAvailableTools = (message: AgenticMessage) => {
    return uniqueStrings(
      assistantTraceEvents(message)
        .filter((event) => event.type === 'tools')
        .flatMap((event) => (event.detail || '').split(','))
        .map((tool) => tool.trim())
        .filter(Boolean)
    );
  };

  const assistantToolSteps = (message: AgenticMessage): AssistantChainStep[] => {
    const traceSteps = assistantTraceEvents(message)
      .filter((event) => event.type === 'tool')
      .map((event) => {
        const backendContext = event.title === 'Backend context loaded';
        const label = backendContext ? 'Backend context' : event.name || event.title || 'tool';
        const detail = backendContext ? event.detail : event.title;
        const meta = backendContext ? event.name : event.detail;
        return {
          id: traceKey(event),
          index: 0,
          label,
          detail,
          meta: meta && meta !== detail ? meta : undefined,
        };
      });
    const fallbackSteps = assistantTools(message).map((tool) => ({
      id: `tool-${tool}`,
      index: 0,
      label: tool,
      detail: '',
    }));
    return indexChainSteps(uniqueChainSteps([...traceSteps, ...fallbackSteps]));
  };

  const assistantContexts = (message: AgenticMessage): AgenticMessageContext[] => {
    const persisted = message.contentExt?.contexts || [];
    const streamingContexts = messageTraceEvents(message)
      .filter((event) => event.title === 'Backend context loaded')
      .map((event) => ({
        type: 'backend' as const,
        content: event.detail || event.title,
      }));
    return [...persisted, ...streamingContexts];
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
    return typeof total === 'number' ? t('agentic.detailTotal', { n: formatCount(total) }) : t('agentic.detailPending');
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
      .map(([key, label]) => ({ label, value: formatCount(tokens[key] || 0) }));
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
    return [event.type, event.name || '', event.title || '', event.detail || ''].join('|');
  };

  const uniqueChainSteps = (steps: AssistantChainStep[]) => {
    const seen = new Set<string>();
    return steps.filter((step) => {
      const key = [step.label, step.detail || '', step.meta || ''].join('|');
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

  const sanitizeHtml = (html: string) => {
    return html
      .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
      .replace(/\son\w+="[^"]*"/gi, '')
      .replace(/\son\w+='[^']*'/gi, '')
      .replace(/javascript:/gi, '');
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

  .agentic-shell {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
    background: #f5f7fb;
  }

  .agentic-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    min-height: 64px;
    padding: 12px 18px;
    border-bottom: 1px solid #dfe5ee;
    background: #ffffff;
  }

  .agentic-title,
  .agentic-header__actions,
  .agentic-composer__left {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .agentic-mark {
    display: flex;
    flex: 0 0 34px;
    align-items: center;
    justify-content: center;
    width: 34px;
    height: 34px;
    border-radius: 8px;
    color: #ffffff;
    font-size: 12px;
    font-weight: 700;
    background: #2563eb;
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
      max-width: 320px;
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
    padding: 18px;
    overflow-y: auto;
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
    gap: 18px;
    max-width: 820px;
    margin: 0 auto;
  }

  .agentic-message {
    display: flex;
    gap: 10px;
    align-items: flex-start;
  }

  .agentic-message--user {
    justify-content: flex-end;

    .agentic-message__avatar {
      order: 2;
    }

    .agentic-message__content {
      max-width: 72%;
      color: #ffffff;
      background: #2563eb;
    }
  }

  .agentic-message__avatar {
    display: flex;
    flex: 0 0 30px;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    border-radius: 8px;
    overflow: hidden;
    background: #f8fafc;
    border: 1px solid #e5e7eb;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }
  }

  .agentic-message__content {
    max-width: 78%;
    padding: 10px 12px;
    border-radius: 8px;
    color: #1f2937;
    line-height: 1.58;
    background: #ffffff;
    box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
  }

  .agentic-text {
    white-space: pre-wrap;
  }

  .agentic-cursor {
    color: #64748b;
    font-size: 12px;
  }

  .agentic-reasoning {
    margin-top: 8px;
    padding: 8px 10px;
    border-left: 3px solid #93c5fd;
    border-radius: 4px;
    color: #475569;
    font-size: 13px;
    background: #f0f7ff;
  }

  .agentic-reasoning__content {
    :deep(p) {
      margin: 0 0 6px;
    }

    :deep(p:last-child) {
      margin-bottom: 0;
    }
  }

  .agentic-details {
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
    padding: 14px 18px 18px;
    border-top: 1px solid #dfe5ee;
    background: #ffffff;
  }

  .agentic-actions {
    display: flex;
    flex-direction: column;
    gap: 8px;
    max-width: 820px;
    margin: 0 auto 10px;
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
    max-width: 820px;
    margin: 0 auto;
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
    width: 190px;
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

    code {
      padding: 2px 4px;
      border-radius: 4px;
      background: #eef2f7;
      font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
      font-size: 12px;
    }

    pre {
      overflow: auto;
      padding: 10px;
      border-radius: 6px;
      background: #0f172a;

      code {
        padding: 0;
        color: #e5e7eb;
        background: transparent;
      }
    }

    table {
      width: 100%;
      border-collapse: collapse;
    }

    th,
    td {
      padding: 6px 8px;
      border: 1px solid #dfe5ee;
    }
  }

  @media (max-width: 900px) {
    .agentic-header {
      align-items: flex-start;
      flex-direction: column;
    }

    .agentic-header__actions,
    .agentic-composer__bar,
    .agentic-composer__left {
      flex-wrap: wrap;
      width: 100%;
    }

    .agentic-message__content,
    .agentic-message--user .agentic-message__content {
      max-width: calc(100% - 44px);
    }

    .agentic-model {
      width: 100%;
    }
  }
</style>

<style lang="scss">
  .agentic-drawer {
    .el-drawer__body {
      padding: 0;
    }
  }
</style>
