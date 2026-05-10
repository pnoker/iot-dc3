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
  <el-tooltip v-if="!visible" content="AI Assistant" placement="left">
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
            <strong>DC3 Assistant</strong>
            <span>{{ currentSession?.title || 'New Conversation' }}</span>
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
                <el-dropdown-item v-if="conversationItems.length === 0" disabled>No history</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-tooltip content="Rename">
            <el-button size="small" circle :disabled="!activeConversationId" @click="handleRenameCurrent">
              <el-icon>
                <EditPen />
              </el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="Delete">
            <el-button size="small" circle :disabled="!activeConversationId" @click="handleDeleteCurrent">
              <el-icon>
                <Delete />
              </el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="Close">
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
            title="DC3 Assistant"
            description="Ask about devices, drivers, points, events, and platform operations."
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
              <span v-if="message.streaming && !message.content" class="agentic-cursor">Thinking...</span>
              <details v-if="message.role === 'assistant' && hasAssistantDetails(message)" class="agentic-details">
                <summary>
                  <el-icon>
                    <Cpu />
                  </el-icon>
                  <span>{{ assistantDetailSummary(message) }}</span>
                </summary>

                <div class="agentic-details__body">
                  <div v-if="assistantSkills(message).length" class="agentic-details__row">
                    <span class="agentic-details__label">Skills</span>
                    <div class="agentic-details__tags">
                      <el-tag v-for="skill in assistantSkills(message)" :key="skill" size="small">{{ skill }}</el-tag>
                    </div>
                  </div>

                  <div v-if="assistantReasoning(message)" class="agentic-details__row">
                    <span class="agentic-details__label">Reasoning</span>
                    <el-tag size="small" type="warning">enabled</el-tag>
                  </div>

                  <div v-if="assistantTools(message).length" class="agentic-details__row">
                    <span class="agentic-details__label">Tools</span>
                    <div class="agentic-details__tags">
                      <el-tag v-for="tool in assistantTools(message)" :key="tool" size="small" type="success">
                        {{ tool }}
                      </el-tag>
                    </div>
                  </div>

                  <div
                    v-if="assistantContexts(message).length"
                    class="agentic-details__row agentic-details__row--stack"
                  >
                    <span class="agentic-details__label">Contexts</span>
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

                  <div v-if="assistantTokens(message)" class="agentic-details__row">
                    <span class="agentic-details__label">Tokens</span>
                    <div class="agentic-token-list">
                      <span v-for="item in assistantTokenItems(message)" :key="item.label">
                        {{ item.label }} {{ item.value }}
                      </span>
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
            <el-tag type="warning" size="small">Pending</el-tag>
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
            placeholder="Message DC3 Assistant..."
            :disabled="streaming"
            @keydown.enter.exact.prevent="handleSubmit"
          />

          <div class="agentic-composer__bar">
            <div class="agentic-composer__left">
              <input ref="fileInputRef" class="agentic-file" type="file" multiple @change="handleFileChange" />
              <el-tooltip content="Attach file">
                <el-button circle size="small" :disabled="streaming" @click="handlePickFile">
                  <el-icon>
                    <Paperclip />
                  </el-icon>
                </el-button>
              </el-tooltip>

              <el-select v-model="selectedModel" class="agentic-model" size="small" filterable>
                <el-option
                  v-for="model in models"
                  :key="model.model"
                  :label="model.label || model.model"
                  :value="model.model"
                />
              </el-select>

              <el-tooltip content="Reasoning">
                <el-switch
                  v-model="reasoningEnabled"
                  :disabled="!activeModel.reasoning"
                  size="small"
                  inline-prompt
                  active-text="Think"
                  inactive-text="Fast"
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
                    <span>Temperature</span>
                    <el-slider v-model="temperatureProxy" :min="0" :max="2" :step="0.1" size="small" />
                  </div>
                  <div class="agentic-setting">
                    <span>Max Tokens</span>
                    <el-input-number
                      v-model="maxTokensProxy"
                      :min="1"
                      :step="256"
                      size="small"
                      controls-position="right"
                    />
                  </div>
                  <div class="agentic-setting agentic-setting--row">
                    <span>Confirm Actions</span>
                    <el-switch v-model="requireConfirmation" size="small" />
                  </div>
                  <div class="agentic-capabilities">
                    <el-tag :type="activeModel.stream ? 'success' : 'info'" size="small">Stream</el-tag>
                    <el-tag :type="activeModel.toolCall ? 'success' : 'info'" size="small">Tools</el-tag>
                    <el-tag :type="activeModel.vision ? 'success' : 'info'" size="small">Vision</el-tag>
                    <el-tag :type="activeModel.reasoning ? 'success' : 'info'" size="small">Reasoning</el-tag>
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
  import { computed, ref } from 'vue';
  import type { AgenticMessage, AgenticMessageContext, AgenticMessageTokens } from '@/config/types';
  import { useAgenticStore } from '@/store';

  interface AssistantPromptItem {
    key: string | number;
    label?: string;
    description?: string;
  }

  const agenticStore = useAgenticStore();
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

  const promptItems: AssistantPromptItem[] = [
    { key: 'device-status', label: 'Device Status', description: 'Show abnormal devices and recent events.' },
    { key: 'point-trend', label: 'Point Trend', description: 'Analyze recent point value changes.' },
    { key: 'driver-health', label: 'Driver Health', description: 'Check driver connectivity and errors.' },
    { key: 'operation-plan', label: 'Operation Plan', description: 'Draft a safe maintenance workflow.' },
  ];

  const conversationItems = computed(() => {
    return sessions.value.map((session) => ({
      ...session,
      title: session.title || 'New Conversation',
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
    const result = await ElMessageBox.prompt('Conversation title', 'Rename', {
      inputValue: currentSession.value?.title || 'New Conversation',
      confirmButtonText: 'Save',
      cancelButtonText: 'Cancel',
    });
    await agenticStore.renameSession(conversationId, result.value);
  };

  const handleDeleteCurrent = async () => {
    const conversationId = activeConversationId.value;
    if (!conversationId) {
      return;
    }
    await ElMessageBox.confirm('Delete this conversation?', 'Delete', {
      type: 'warning',
      confirmButtonText: 'Delete',
      cancelButtonText: 'Cancel',
    });
    await agenticStore.deleteSession(conversationId);
  };

  const handlePromptClick = (item: AssistantPromptItem) => {
    const description = item.description ? ` ${item.description}` : '';
    draft.value = `${item.label || ''}${description}`.trim();
  };

  const handleSubmit = async () => {
    const content = draft.value.trim() || (currentAttachments.value.length ? 'Please analyze the attached files.' : '');
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
    await ElMessageBox.confirm('Confirm this operation?', 'Confirm Action', {
      type: 'warning',
      confirmButtonText: 'Confirm',
      cancelButtonText: 'Cancel',
    });
    await agenticStore.confirmAction(actionId);
    ElMessage.success('Action confirmed');
  };

  const handleRejectAction = async (actionId: string) => {
    await agenticStore.rejectAction(actionId);
    ElMessage.success('Action rejected');
  };

  const renderMarkdown = (message: AgenticMessage) => {
    return sanitizeHtml(String(marked.parse(message.content || (message.streaming ? 'Thinking...' : ''))));
  };

  const hasAssistantDetails = (message: AgenticMessage) => {
    return (
      assistantSkills(message).length > 0 ||
      assistantReasoning(message) ||
      assistantTools(message).length > 0 ||
      assistantContexts(message).length > 0 ||
      Boolean(assistantTokens(message)) ||
      Boolean(message.streaming)
    );
  };

  const assistantDetailSummary = (message: AgenticMessage) => {
    const parts: string[] = [];
    const skills = assistantSkills(message);
    const tools = assistantTools(message);
    const contexts = assistantContexts(message);
    if (message.streaming && !currentTraceEvents.value.length) {
      parts.push('Thinking');
    }
    if (skills.length) {
      parts.push(`${skills.length} skill${skills.length > 1 ? 's' : ''}`);
    }
    if (tools.length) {
      parts.push(`${tools.length} tool${tools.length > 1 ? 's' : ''}`);
    }
    if (contexts.length) {
      parts.push(`${contexts.length} context${contexts.length > 1 ? 's' : ''}`);
    }
    if (assistantReasoning(message)) {
      parts.push('reasoning');
    }
    return parts.length ? parts.join(' · ') : 'Thinking';
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
      message.contentExt?.reasoning || messageTraceEvents(message).some((event) => event.type === 'reasoning')
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

  const assistantTokenItems = (message: AgenticMessage) => {
    const tokens = assistantTokens(message);
    if (!tokens) {
      return [];
    }
    return Object.entries(tokens)
      .filter(([, value]) => typeof value === 'number')
      .map(([label, value]) => ({ label, value }));
  };

  const messageTraceEvents = (message: AgenticMessage) => {
    return message.streaming ? currentTraceEvents.value : [];
  };

  const uniqueStrings = (values: string[]) => {
    return Array.from(new Set(values.filter(Boolean)));
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
    gap: 8px;
    padding-top: 4px;
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
