/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {
  completeAgenticChatCompletion,
  confirmAgenticAction,
  deleteAgenticSession,
  listAgenticAttachments,
  listAgenticMessages,
  listAgenticModels,
  listAgenticSessions,
  listPendingAgenticActions,
  rejectAgenticAction,
  streamAgenticChatCompletion,
  updateAgenticSession,
  uploadAgenticAttachment,
} from '@/api/agentic';
import type {
  AgenticAction,
  AgenticAttachment,
  AgenticMessage,
  AgenticModel,
  AgenticSession,
  AgenticSessionExt,
  AgenticTraceEvent,
  AgenticVisualizationSpec,
} from '@/config/types';
import {failMessage, warnMessage} from '@/utils/notificationUtil';
import {getStorage, setStorage} from '@/utils/storageUtil';
import {defineStore} from 'pinia';
import {computed, ref} from 'vue';

const MESSAGE_STORAGE_KEY = 'dc3-agentic-messages';
const DEFAULT_SESSION_TITLE = 'New Conversation';
const DEFAULT_MODEL: AgenticModel = {
  model: 'dc3-agentic',
  label: 'DC3 Agentic',
  stream: true,
  toolCall: true,
  vision: false,
  reasoning: false,
};

export const useAgenticStore = defineStore('agentic', () => {
  const visible = ref(false);
  const bootstrapped = ref(false);
  const loading = ref(false);
  const streaming = ref(false);
  const sessions = ref<AgenticSession[]>([]);
  const models = ref<AgenticModel[]>([]);
  const selectedModel = ref('');
  const reasoningEnabled = ref(false);
  const temperature = ref<number>();
  const maxTokens = ref<number>();

  const activeConversationId = ref('');
  const currentAbortController = ref<AbortController>();
  const messagesByConversation = ref<Record<string, AgenticMessage[]>>(readCachedMessages());
  const attachmentsByConversation = ref<Record<string, AgenticAttachment[]>>({});
  const pendingAttachmentIdsByConversation = ref<Record<string, number[]>>({});
  const pendingActionsByConversation = ref<Record<string, AgenticAction[]>>({});
  const traceEventsByConversation = ref<Record<string, AgenticTraceEvent[]>>({});

  const activeModel = computed(() => {
    return models.value.find((model) => model.model === selectedModel.value) || models.value[0] || DEFAULT_MODEL;
  });

  const currentMessages = computed(() => {
    return activeConversationId.value ? messagesByConversation.value[activeConversationId.value] || [] : [];
  });

  const currentSession = computed(() => {
    return sessions.value.find((session) => session.conversationId === activeConversationId.value);
  });

  const currentAttachments = computed(() => {
    if (!activeConversationId.value) {
      return [];
    }
    const pendingIds = new Set(pendingAttachmentIdsByConversation.value[activeConversationId.value] || []);
    return (attachmentsByConversation.value[activeConversationId.value] || []).filter((attachment) =>
      pendingIds.has(attachment.id)
    );
  });

  const currentPendingActions = computed(() => {
    return activeConversationId.value ? pendingActionsByConversation.value[activeConversationId.value] || [] : [];
  });

  const currentTraceEvents = computed(() => {
    return activeConversationId.value ? traceEventsByConversation.value[activeConversationId.value] || [] : [];
  });

  const open = async () => {
    visible.value = true;
    await bootstrap();
  };

  const close = () => {
    visible.value = false;
  };

  const toggle = async () => {
    if (visible.value) {
      close();
      return;
    }
    await open();
  };

  const bootstrap = async () => {
    if (bootstrapped.value) {
      const conversationId = ensureActiveSession();
      restoreSessionModel(currentSession.value);
      await Promise.all([
        loadMessages(conversationId),
        loadAttachments(conversationId),
        loadPendingActions(conversationId),
      ]);
      return;
    }

    loading.value = true;
    try {
      await loadModels();
      await loadSessions();
      bootstrapped.value = true;
      const conversationId = ensureActiveSession();
      restoreSessionModel(currentSession.value);
      await Promise.all([
        loadMessages(conversationId),
        loadAttachments(conversationId),
        loadPendingActions(conversationId),
      ]);
    } finally {
      loading.value = false;
    }
  };

  const loadModels = async () => {
    try {
      const response = await listAgenticModels();
      models.value = response.data?.length ? response.data : [DEFAULT_MODEL];
    } catch (error) {
      models.value = [DEFAULT_MODEL];
      warnMessage('Failed to load agentic models, fallback model is used.', 'Agentic', error);
    }

    const firstModel = models.value[0] || DEFAULT_MODEL;
    selectedModel.value = resolveModelName(
      selectedModel.value || sessionModel(currentSession.value) || firstModel.model
    );
    applyModelCapabilities();
    temperature.value = temperature.value ?? firstModel.temperature;
    maxTokens.value = maxTokens.value ?? firstModel.maxTokens;
  };

  const loadSessions = async () => {
    try {
      const response = await listAgenticSessions({page: {current: 1, size: 50}});
      sessions.value = (response.data?.records || []).map(normalizeSession);
      if (activeConversationId.value) {
        restoreSessionModel(sessions.value.find((session) => session.conversationId === activeConversationId.value));
      }
    } catch (error) {
      sessions.value = [];
      warnMessage('Failed to load agentic sessions.', 'Agentic', error);
    }
  };

  const newSession = () => {
    const conversationId = createConversationId();
    const model = resolveModelName(selectedModel.value);
    selectedModel.value = model;
    applyModelCapabilities();
    activeConversationId.value = conversationId;
    if (!messagesByConversation.value[conversationId]) {
      messagesByConversation.value[conversationId] = [];
    }
    pendingAttachmentIdsByConversation.value[conversationId] = [];
    traceEventsByConversation.value[conversationId] = [];
    if (!sessions.value.some((session) => session.conversationId === conversationId)) {
      sessions.value = [
        {
          conversationId,
          title: DEFAULT_SESSION_TITLE,
          sessionExt: buildCurrentSessionExt(model),
        },
        ...sessions.value,
      ];
    }
    persistMessages();
  };

  const selectSession = async (conversationId?: string | number) => {
    if (!conversationId) {
      return;
    }
    activeConversationId.value = String(conversationId);
    restoreSessionModel(sessions.value.find((session) => session.conversationId === activeConversationId.value));
    if (!messagesByConversation.value[activeConversationId.value]) {
      messagesByConversation.value[activeConversationId.value] = [];
      persistMessages();
    }
    await loadMessages(activeConversationId.value);
    await Promise.all([loadAttachments(activeConversationId.value), loadPendingActions(activeConversationId.value)]);
  };

  const deleteSession = async (conversationId: string) => {
    if (!conversationId) {
      return;
    }

    try {
      await deleteAgenticSession(conversationId);
    } catch (error) {
      warnMessage('Failed to delete server-side agentic session, local session is removed.', 'Agentic', error);
    }

    delete messagesByConversation.value[conversationId];
    delete attachmentsByConversation.value[conversationId];
    delete pendingAttachmentIdsByConversation.value[conversationId];
    delete pendingActionsByConversation.value[conversationId];
    delete traceEventsByConversation.value[conversationId];
    sessions.value = sessions.value.filter((session) => session.conversationId !== conversationId);
    if (activeConversationId.value === conversationId) {
      const nextSession = sessions.value[0];
      if (nextSession) {
        activeConversationId.value = nextSession.conversationId;
      } else {
        newSession();
      }
    }
    persistMessages();
  };

  const renameSession = async (conversationId: string, title: string) => {
    const normalizedTitle = normalizeTitle(title);
    sessions.value = sessions.value.map((session) =>
      session.conversationId === conversationId ? {...session, title: normalizedTitle} : session
    );

    try {
      const response = await updateAgenticSession(conversationId, {
        title: normalizedTitle,
      });
      sessions.value = sessions.value.map((session) =>
        session.conversationId === conversationId ? normalizeSession({...session, ...(response.data || {})}) : session
      );
    } catch (error) {
      warnMessage('Failed to update agentic session metadata.', 'Agentic', error);
    }
  };

  const sendMessage = async (content: string) => {
    const text = content.trim();
    if (!text || streaming.value) {
      return;
    }

    const conversationId = ensureActiveSession();
    if (!conversationId) {
      failMessage('Missing conversation context.', 'Agentic');
      return;
    }
    const model = resolveModelName(selectedModel.value || sessionModel(currentSession.value));
    selectedModel.value = model;
    applyModelCapabilities();
    updateSessionLocally(conversationId, {sessionExt: buildCurrentSessionExt(model)});
    const userMessage: AgenticMessage = {
      id: createMessageId('user'),
      role: 'user',
      content: text,
    };
    const assistantMessage: AgenticMessage = {
      id: createMessageId('assistant'),
      role: 'assistant',
      content: '',
      streaming: true,
    };

    setConversationMessages(conversationId, [...currentMessages.value, userMessage, assistantMessage]);
    traceEventsByConversation.value[conversationId] = [];
    const abortController = new AbortController();
    currentAbortController.value = abortController;
    streaming.value = true;

    try {
      const request = {
        model,
        messages: [{role: 'user' as const, content: text}],
        stream: activeModel.value.stream,
        conversationId,
        temperature: temperature.value,
        maxTokens: maxTokens.value,
        attachments: currentAttachments.value.map((attachment) => attachment.id),
        reasoning: activeModel.value.reasoning && reasoningEnabled.value,
      };
      if (activeModel.value.stream) {
        await streamAgenticChatCompletion(request, {
          signal: abortController.signal,
          onEvent: (event) => appendTraceEvent(conversationId, event),
          onVisualization: (visualization) =>
            appendAssistantVisualization(conversationId, assistantMessage.id, visualization),
          onDelta: (delta) => appendAssistantDelta(conversationId, assistantMessage.id, delta),
          onReasoning: (reasoning) => appendAssistantReasoning(conversationId, assistantMessage.id, reasoning),
          onFinish: (reason) => setAssistantFinishReason(conversationId, assistantMessage.id, reason),
        });
      } else {
        const response = await completeAgenticChatCompletion(request, abortController.signal);
        const responseMessage = response.choices?.[0]?.message;
        appendAssistantDelta(conversationId, assistantMessage.id, responseMessage?.content || '');
        for (const chart of responseMessage?.contentExt?.charts || responseMessage?.content_ext?.charts || []) {
          appendAssistantVisualization(conversationId, assistantMessage.id, chart);
        }
        const finishReason = response.choices?.[0]?.finishReason ?? response.choices?.[0]?.finish_reason;
        if (finishReason) {
          setAssistantFinishReason(conversationId, assistantMessage.id, finishReason);
        }
      }
      markAssistantComplete(conversationId, assistantMessage.id);
      await syncSessionAfterMessage(conversationId, text);
      await Promise.all([loadMessages(conversationId), loadPendingActions(conversationId)]);
    } catch (error) {
      if ((error as Error).name === 'AbortError') {
        appendAssistantDelta(conversationId, assistantMessage.id, '\n\nCanceled.');
      } else {
        failMessage('Agentic chat failed.', 'Agentic', error);
        appendAssistantDelta(conversationId, assistantMessage.id, '\n\nRequest failed.');
        setAssistantFinishReason(conversationId, assistantMessage.id, 'error');
      }
      markAssistantComplete(conversationId, assistantMessage.id);
    } finally {
      pendingAttachmentIdsByConversation.value[conversationId] = [];
      streaming.value = false;
      currentAbortController.value = undefined;
    }
  };

  const stopStreaming = () => {
    currentAbortController.value?.abort();
  };

  const loadMessages = async (conversationId: string) => {
    if (!conversationId) {
      return;
    }
    try {
      const response = await listAgenticMessages(conversationId);
      if (response.data) {
        const previousMessages = messagesByConversation.value[conversationId] || [];
        const loadedMessages = response.data.map((message) => ({
          id: String(message.id || createMessageId(message.role)),
          role: message.role,
          content: message.content || '',
          contentExt: message.contentExt,
          messageIndex: message.messageIndex,
          reasoning: message.reasoning || message.contentExt?.reasoningContent,
        }));
        messagesByConversation.value[conversationId] = mergeEphemeralAssistantState(previousMessages, loadedMessages);
        persistMessages();
      }
    } catch (error) {
      warnMessage('Failed to load agentic messages, local cache is used.', 'Agentic', error);
    }
  };

  const loadAttachments = async (conversationId: string) => {
    if (!conversationId) {
      return;
    }
    try {
      const response = await listAgenticAttachments(conversationId);
      attachmentsByConversation.value[conversationId] = response.data || [];
    } catch (error) {
      attachmentsByConversation.value[conversationId] = attachmentsByConversation.value[conversationId] || [];
      warnMessage('Failed to load agentic attachments.', 'Agentic', error);
    }
  };

  const uploadAttachment = async (file: File) => {
    const conversationId = ensureActiveSession();
    const response = await uploadAgenticAttachment(conversationId, file);
    attachmentsByConversation.value[conversationId] = [
      response.data,
      ...(attachmentsByConversation.value[conversationId] || []),
    ];
    pendingAttachmentIdsByConversation.value[conversationId] = [
      response.data.id,
      ...(pendingAttachmentIdsByConversation.value[conversationId] || []),
    ];
  };

  const removeLocalAttachment = (attachmentId: number) => {
    const conversationId = activeConversationId.value;
    pendingAttachmentIdsByConversation.value[conversationId] = (
      pendingAttachmentIdsByConversation.value[conversationId] || []
    ).filter((id) => id !== attachmentId);
  };

  const loadPendingActions = async (conversationId: string) => {
    if (!conversationId) {
      return;
    }
    try {
      const response = await listPendingAgenticActions(conversationId);
      pendingActionsByConversation.value[conversationId] = response.data || [];
    } catch (error) {
      pendingActionsByConversation.value[conversationId] = [];
      warnMessage('Failed to load pending agentic actions.', 'Agentic', error);
    }
  };

  const confirmAction = async (actionId: string) => {
    await confirmAgenticAction(actionId);
    await loadPendingActions(activeConversationId.value);
  };

  const rejectAction = async (actionId: string) => {
    await rejectAgenticAction(actionId);
    await loadPendingActions(activeConversationId.value);
  };

  const ensureActiveSession = () => {
    if (!activeConversationId.value) {
      const existingSession = sessions.value[0];
      if (existingSession) {
        activeConversationId.value = existingSession.conversationId;
        restoreSessionModel(existingSession);
      } else {
        newSession();
      }
    }
    return activeConversationId.value;
  };

  const syncSessionAfterMessage = async (conversationId: string, firstUserText: string) => {
    const session = sessions.value.find((item) => item.conversationId === conversationId);
    // Guard: if session or its title is missing, always generate a new title
    // rather than crashing on the non-null assertion in the else branch.
    if (!session || !session.title || shouldGenerateSessionTitle(session.title)) {
      const title = normalizeTitle(firstUserText);
      if (!session) {
        sessions.value = [
          {
            conversationId,
            title,
            sessionExt: buildCurrentSessionExt(resolveModelName(selectedModel.value)),
          },
          ...sessions.value,
        ];
        await renameSession(conversationId, title);
        return;
      }
      if (normalizeTitle(session.title || DEFAULT_SESSION_TITLE) !== title) {
        await renameSession(conversationId, title);
      }
    }
  };

  const setSelectedModel = async (model: string | number) => {
    const nextModel = resolveModelName(String(model || ''));
    selectedModel.value = nextModel;
    applyModelCapabilities();

    const conversationId = activeConversationId.value;
    if (!conversationId) {
      return;
    }
    updateSessionLocally(conversationId, {sessionExt: {model: nextModel}});
    const session = sessions.value.find((item) => item.conversationId === conversationId);
    if (!session?.createTime && !session?.operateTime) {
      return;
    }
    try {
      const response = await updateAgenticSession(conversationId, {sessionExt: {model: nextModel}});
      sessions.value = sessions.value.map((session) =>
        session.conversationId === conversationId ? normalizeSession({...session, ...(response.data || {})}) : session
      );
    } catch (error) {
      warnMessage('Failed to update agentic session model.', 'Agentic', error);
    }
  };

  const restoreSessionModel = (session?: AgenticSession) => {
    selectedModel.value = resolveModelName(sessionModel(session) || selectedModel.value);
    const modelDefaults = models.value.find((model) => model.model === selectedModel.value) || activeModel.value;
    const sessionExt = sessionExtOf(session);
    reasoningEnabled.value = sessionExt?.reasoningEnabled ?? false;
    temperature.value = sessionExt?.temperature ?? modelDefaults.temperature;
    maxTokens.value = sessionExt?.maxTokens ?? modelDefaults.maxTokens;
    applyModelCapabilities();
  };

  const updateSessionLocally = (conversationId: string, patch: Partial<Pick<AgenticSession, 'sessionExt'>>) => {
    sessions.value = sessions.value.map((session) =>
      session.conversationId === conversationId
        ? {
            ...session,
            ...patch,
            sessionExt: patch.sessionExt ? {...(sessionExtOf(session) || {}), ...patch.sessionExt} : session.sessionExt,
          }
        : session
    );
  };

  const persistSessionPrefs = async (conversationId: string) => {
    if (!conversationId) return;
    const sessionExt = buildCurrentSessionExt();
    updateSessionLocally(conversationId, {sessionExt});
    const session = sessions.value.find((item) => item.conversationId === conversationId);
    if (!session?.createTime && !session?.operateTime) {
      return;
    }
    try {
      await updateAgenticSession(conversationId, {
        sessionExt,
      });
    } catch (error) {
      warnMessage('Failed to update agentic session preferences.', 'Agentic', error);
    }
  };

  const persistCurrentSessionPrefs = async () => {
    if (!activeConversationId.value) {
      return;
    }
    await persistSessionPrefs(activeConversationId.value);
  };

  const buildCurrentSessionExt = (model = resolveModelName(selectedModel.value)): AgenticSessionExt => ({
    model,
    reasoningEnabled: reasoningEnabled.value,
    temperature: temperature.value,
    maxTokens: maxTokens.value,
  });

  const resolveModelName = (model?: string) => {
    const candidate = model?.trim();
    if (candidate && models.value.some((item) => item.model === candidate)) {
      return candidate;
    }
    return models.value[0]?.model || DEFAULT_MODEL.model;
  };

  const applyModelCapabilities = () => {
    if (!activeModel.value.reasoning) {
      reasoningEnabled.value = false;
    }
  };

  const setConversationMessages = (conversationId: string, messages: AgenticMessage[]) => {
    messagesByConversation.value[conversationId] = messages;
    persistMessages();
  };

  const appendAssistantDelta = (conversationId: string, messageId: string, delta: string) => {
    const messages = messagesByConversation.value[conversationId];
    if (!messages) return;
    const index = messages.findIndex((message) => message.id === messageId);
    if (index < 0) return;
    const target = messages[index]!;
    messages[index] = {...target, content: target.content + delta};
    messagesByConversation.value[conversationId] = [...messages];
  };

  const appendAssistantReasoning = (conversationId: string, messageId: string, reasoning: string) => {
    const messages = messagesByConversation.value[conversationId];
    if (!messages) return;
    const index = messages.findIndex((message) => message.id === messageId);
    if (index < 0) return;
    const target = messages[index]!;
    messages[index] = {...target, reasoning: (target.reasoning || '') + reasoning};
    messagesByConversation.value[conversationId] = [...messages];
  };

  const appendAssistantVisualization = (
    conversationId: string,
    messageId: string,
    visualization: AgenticVisualizationSpec
  ) => {
    const messages = messagesByConversation.value[conversationId];
    if (!messages) return;
    const index = messages.findIndex((message) => message.id === messageId);
    if (index < 0) return;
    const target = messages[index]!;
    const contentExt = target.contentExt || {};
    messages[index] = {
      ...target,
      contentExt: {
        ...contentExt,
        charts: [...(contentExt.charts || []), visualization],
      },
    };
    messagesByConversation.value[conversationId] = [...messages];
  };

  const markAssistantComplete = (conversationId: string, messageId: string) => {
    const messages = messagesByConversation.value[conversationId] || [];
    setConversationMessages(
      conversationId,
      messages.map((message) => (message.id === messageId ? {...message, streaming: false} : message))
    );
  };

  const setAssistantFinishReason = (conversationId: string, messageId: string, reason: string) => {
    const messages = messagesByConversation.value[conversationId];
    if (!messages) return;
    const index = messages.findIndex((message) => message.id === messageId);
    if (index < 0) return;
    const target = messages[index]!;
    messages[index] = {...target, finishReason: reason};
    messagesByConversation.value[conversationId] = [...messages];
  };

  const appendTraceEvent = (conversationId: string, event: AgenticTraceEvent) => {
    traceEventsByConversation.value[conversationId] = [
      ...(traceEventsByConversation.value[conversationId] || []),
      {
        ...event,
        id: event.id || createTraceEventId(event.type),
      },
    ];
  };

  const persistMessages = () => {
    setStorage(MESSAGE_STORAGE_KEY, messagesByConversation.value);
  };

  return {
    visible,
    bootstrapped,
    loading,
    streaming,
    sessions,
    models,
    selectedModel,
    reasoningEnabled,
    temperature,
    maxTokens,
    activeConversationId,
    messagesByConversation,
    attachmentsByConversation,
    pendingAttachmentIdsByConversation,
    pendingActionsByConversation,
    traceEventsByConversation,
    activeModel,
    currentMessages,
    currentSession,
    currentAttachments,
    currentPendingActions,
    currentTraceEvents,
    open,
    close,
    toggle,
    bootstrap,
    loadSessions,
    newSession,
    selectSession,
    deleteSession,
    renameSession,
    setSelectedModel,
    persistCurrentSessionPrefs,
    sendMessage,
    stopStreaming,
    loadMessages,
    loadAttachments,
    uploadAttachment,
    removeLocalAttachment,
    loadPendingActions,
    confirmAction,
    rejectAction,
    reset() {
      visible.value = false;
      bootstrapped.value = false;
      loading.value = false;
      streaming.value = false;
      sessions.value = [];
      models.value = [];
      selectedModel.value = '';
      reasoningEnabled.value = false;
      temperature.value = undefined;
      maxTokens.value = undefined;
      activeConversationId.value = '';
      currentAbortController.value = undefined;
      messagesByConversation.value = {};
      attachmentsByConversation.value = {};
      pendingAttachmentIdsByConversation.value = {};
      pendingActionsByConversation.value = {};
      traceEventsByConversation.value = {};
      try {
        localStorage.removeItem(MESSAGE_STORAGE_KEY);
      } catch {
        // storage unavailable
      }
    },
  };
});

const readCachedMessages = (): Record<string, AgenticMessage[]> => {
  const cached = getStorage(MESSAGE_STORAGE_KEY);
  if (!cached || typeof cached !== 'object') {
    return {};
  }
  return Object.fromEntries(
    Object.entries(cached as Record<string, AgenticMessage[]>).map(([conversationId, messages]) => [
      conversationId,
      Array.isArray(messages)
        ? messages.map((message) => ({
            ...message,
            content: message.content || '',
          }))
        : [],
    ])
  );
};

const createConversationId = () => {
  return typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `conversation-${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

const createMessageId = (role: string) => {
  return `${role}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

const createTraceEventId = (type: string) => {
  return `trace-${type}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
};

const normalizeTitle = (title: string) => {
  const trimmed = title.trim().replace(/\s+/g, ' ');
  return trimmed.length > 32 ? `${trimmed.slice(0, 32)}...` : trimmed || DEFAULT_SESSION_TITLE;
};

const shouldGenerateSessionTitle = (title?: string) => {
  return !title || normalizeTitle(title) === DEFAULT_SESSION_TITLE;
};

const mergeEphemeralAssistantState = (previous: AgenticMessage[], loaded: AgenticMessage[]) => {
  const previousAssistantState = previous
    .filter((message) => message.role === 'assistant')
    .map((message) => ({
      reasoning: message.reasoning,
      finishReason: message.finishReason,
      charts: message.contentExt?.charts,
    }));
  let assistantIndex = 0;
  return loaded.map((message) => {
    if (message.role !== 'assistant') {
      return message;
    }
    const state = previousAssistantState[assistantIndex++];
    if (!state) {
      return message;
    }
    return {
      ...message,
      reasoning: message.reasoning || state.reasoning,
      finishReason: message.finishReason || state.finishReason,
      contentExt:
        message.contentExt?.charts?.length || !state.charts?.length
          ? message.contentExt
          : {...(message.contentExt || {}), charts: state.charts},
    };
  });
};

type RawAgenticSessionExt = AgenticSessionExt & {
  reasoning_enabled?: boolean;
  max_tokens?: number;
};

type RawAgenticSession = AgenticSession & {
  session_ext?: RawAgenticSessionExt;
};

const normalizeSession = (session: RawAgenticSession): AgenticSession => {
  const {session_ext: _sessionExt, sessionExt, ...rest} = session;
  const normalizedExt = normalizeSessionExt(sessionExt || _sessionExt);
  return {
    ...rest,
    conversationId: String(rest.conversationId || ''),
    sessionExt: normalizedExt,
  };
};

const normalizeSessionExt = (sessionExt?: RawAgenticSessionExt): AgenticSessionExt | undefined => {
  if (!sessionExt) {
    return undefined;
  }
  return {
    model: sessionExt.model,
    reasoningEnabled: sessionExt.reasoningEnabled ?? sessionExt.reasoning_enabled,
    temperature: sessionExt.temperature,
    maxTokens: sessionExt.maxTokens ?? sessionExt.max_tokens,
  };
};

const sessionExtOf = (session?: AgenticSession): AgenticSessionExt | undefined => {
  return normalizeSessionExt((session as RawAgenticSession | undefined)?.sessionExt);
};

const sessionModel = (session?: AgenticSession) => {
  return sessionExtOf(session)?.model;
};
