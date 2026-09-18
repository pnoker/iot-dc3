/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
import i18n from '@/config/i18n';

const MESSAGE_STORAGE_KEY = 'dc3-agentic-messages';
const defaultSessionTitle = () => i18n.global.t('agentic.defaultSessionTitle');
const DEFAULT_MODEL: AgenticModel = {
  model: 'dc3-agentic',
  label: 'DC3 Agentic',
  stream: true,
  toolCall: true,
  vision: false,
  reasoning: false,
};

/** Pinia store for the agentic chat panel: sessions, messages, attachments, models, and streaming state. */
export const useAgenticStore = defineStore('agentic', () => {
  const visible = ref(false);
  const bootstrapped = ref(false);
  const loading = ref(false);
  const sessionsLoading = ref(false);
  const sessionsError = ref<unknown | null>(null);
  const streaming = ref(false);
  const streamingConversationId = ref('');
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
  const pendingAttachmentIdsByConversation = ref<Record<string, string[]>>({});
  const pendingActionsByConversation = ref<Record<string, AgenticAction[]>>({});
  const traceEventsByConversation = ref<Record<string, AgenticTraceEvent[]>>({});
  const sessionActionLoading = ref<Record<string, boolean>>({});
  const actionLoading = ref<Record<string, boolean>>({});
  const actionErrors = ref<Record<string, boolean>>({});
  const messagesLoadingByConversation = ref<Record<string, boolean>>({});
  const messagesErrorByConversation = ref<Record<string, boolean>>({});
  const attachmentsLoadingByConversation = ref<Record<string, boolean>>({});
  const attachmentsErrorByConversation = ref<Record<string, boolean>>({});
  const pendingActionsLoadingByConversation = ref<Record<string, boolean>>({});
  const pendingActionsErrorByConversation = ref<Record<string, boolean>>({});
  let sessionsRequestToken = 0;
  let selectionRequestToken = 0;
  let lifecycleToken = 0;
  const messagesRequestTokens = new Map<string, number>();
  const attachmentsRequestTokens = new Map<string, number>();
  const pendingActionsRequestTokens = new Map<string, number>();
  const sessionModelRequestTokens = new Map<string, number>();
  const sessionPreferenceRequestTokens = new Map<string, number>();

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

  const currentMessagesLoading = computed(() => Boolean(messagesLoadingByConversation.value[activeConversationId.value]));
  const currentMessagesError = computed(() => Boolean(messagesErrorByConversation.value[activeConversationId.value]));
  const currentAttachmentsLoading = computed(() =>
    Boolean(attachmentsLoadingByConversation.value[activeConversationId.value])
  );
  const currentAttachmentsError = computed(() => Boolean(attachmentsErrorByConversation.value[activeConversationId.value]));
  const currentPendingActionsLoading = computed(() =>
    Boolean(pendingActionsLoadingByConversation.value[activeConversationId.value])
  );
  const currentPendingActionsError = computed(() =>
    Boolean(pendingActionsErrorByConversation.value[activeConversationId.value])
  );

  const setSessionActionLoading = (conversationId: string, busy: boolean) => {
    const next = {...sessionActionLoading.value};
    if (busy) next[conversationId] = true;
    else delete next[conversationId];
    sessionActionLoading.value = next;
  };

  const setActionLoading = (actionId: string, busy: boolean) => {
    const next = {...actionLoading.value};
    if (busy) next[actionId] = true;
    else delete next[actionId];
    actionLoading.value = next;
  };

  const setActionError = (actionId: string, failed: boolean) => {
    const next = {...actionErrors.value};
    if (failed) next[actionId] = true;
    else delete next[actionId];
    actionErrors.value = next;
  };

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
    const currentLifecycle = lifecycleToken;
    if (bootstrapped.value) {
      const conversationId = ensureActiveSession();
      restoreSessionModel(currentSession.value);
      await Promise.all([
        loadMessages(conversationId),
        loadAttachments(conversationId),
        loadPendingActions(conversationId),
      ]);
      return currentLifecycle === lifecycleToken;
    }

    loading.value = true;
    try {
      await loadModels();
      if (currentLifecycle !== lifecycleToken) return false;
      await loadSessions();
      if (currentLifecycle !== lifecycleToken) return false;
      bootstrapped.value = true;
      const conversationId = ensureActiveSession();
      restoreSessionModel(currentSession.value);
      await Promise.all([
        loadMessages(conversationId),
        loadAttachments(conversationId),
        loadPendingActions(conversationId),
      ]);
      return currentLifecycle === lifecycleToken;
    } finally {
      if (currentLifecycle === lifecycleToken) loading.value = false;
    }
  };

  const loadModels = async () => {
    const currentLifecycle = lifecycleToken;
    try {
      const response = await listAgenticModels();
      if (currentLifecycle !== lifecycleToken) return;
      models.value = response?.length ? response : [DEFAULT_MODEL];
    } catch (error) {
      if (currentLifecycle !== lifecycleToken) return;
      models.value = [DEFAULT_MODEL];
      warnMessage(i18n.global.t('agentic.failedModels'), 'Agentic', error);
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
    const currentLifecycle = lifecycleToken;
    const requestToken = ++sessionsRequestToken;
    sessionsLoading.value = true;
    sessionsError.value = null;
    try {
      const response = await listAgenticSessions({offset: 0, limit: 50});
      if (currentLifecycle !== lifecycleToken || requestToken !== sessionsRequestToken) return true;
      sessions.value = (response?.items || []).map(normalizeSession);
      sessionsError.value = null;
      if (activeConversationId.value) {
        restoreSessionModel(sessions.value.find((session) => session.conversationId === activeConversationId.value));
      }
      return true;
    } catch (error) {
      if (currentLifecycle !== lifecycleToken || requestToken !== sessionsRequestToken) return false;
      sessionsError.value = error;
      warnMessage(i18n.global.t('agentic.failedSessions'), 'Agentic', error);
      return false;
    } finally {
      if (currentLifecycle === lifecycleToken && requestToken === sessionsRequestToken) sessionsLoading.value = false;
    }
  };

  const retrySessions = async () => {
    const currentLifecycle = lifecycleToken;
    const loaded = await loadSessions();
    if (!loaded || currentLifecycle !== lifecycleToken) return false;
    const conversationId = ensureActiveSession();
    restoreSessionModel(currentSession.value);
    await Promise.all([
      loadMessages(conversationId),
      loadAttachments(conversationId),
      loadPendingActions(conversationId),
    ]);
    return true;
  };

  const newSession = () => {
    if (streaming.value) return false;
    selectionRequestToken += 1;
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
          title: defaultSessionTitle(),
          sessionExt: buildCurrentSessionExt(model),
        },
        ...sessions.value,
      ];
    }
    persistMessages();
    return true;
  };

  const selectSession = async (conversationId?: string) => {
    if (!conversationId || streaming.value) {
      return false;
    }
    const currentLifecycle = lifecycleToken;
    const requestToken = ++selectionRequestToken;
    const selectedConversationId = String(conversationId);
    activeConversationId.value = selectedConversationId;
    restoreSessionModel(sessions.value.find((session) => session.conversationId === activeConversationId.value));
    if (!messagesByConversation.value[activeConversationId.value]) {
      messagesByConversation.value[activeConversationId.value] = [];
      persistMessages();
    }
    await loadMessages(selectedConversationId);
    if (currentLifecycle !== lifecycleToken || requestToken !== selectionRequestToken) return false;
    await Promise.all([loadAttachments(selectedConversationId), loadPendingActions(selectedConversationId)]);
    return currentLifecycle === lifecycleToken && requestToken === selectionRequestToken;
  };

  const deleteSession = async (conversationId: string) => {
    if (!conversationId) {
      return false;
    }
    if (streamingConversationId.value === conversationId) {
      warnMessage(i18n.global.t('agentic.sessionStreaming'), 'Agentic');
      return false;
    }
    if (sessionActionLoading.value[conversationId]) {
      return false;
    }

    const currentLifecycle = lifecycleToken;
    const session = sessions.value.find((item) => item.conversationId === conversationId);
    const persisted = Boolean(session?.createTime || session?.operateTime);
    setSessionActionLoading(conversationId, true);
    let remoteDeleted = true;
    try {
      await deleteAgenticSession(conversationId);
      if (currentLifecycle !== lifecycleToken) return false;
    } catch (error) {
      if (currentLifecycle !== lifecycleToken) return false;
      warnMessage(i18n.global.t('agentic.failedSessionDelete'), 'Agentic', error);
      remoteDeleted = false;
      if (persisted) {
        setSessionActionLoading(conversationId, false);
        return false;
      }
    } finally {
      if (currentLifecycle === lifecycleToken) setSessionActionLoading(conversationId, false);
    }

    if (currentLifecycle !== lifecycleToken) return false;

    delete messagesByConversation.value[conversationId];
    delete attachmentsByConversation.value[conversationId];
    delete pendingAttachmentIdsByConversation.value[conversationId];
    delete pendingActionsByConversation.value[conversationId];
    delete traceEventsByConversation.value[conversationId];
    delete messagesLoadingByConversation.value[conversationId];
    delete messagesErrorByConversation.value[conversationId];
    delete attachmentsLoadingByConversation.value[conversationId];
    delete attachmentsErrorByConversation.value[conversationId];
    delete pendingActionsLoadingByConversation.value[conversationId];
    delete pendingActionsErrorByConversation.value[conversationId];
    messagesRequestTokens.set(conversationId, (messagesRequestTokens.get(conversationId) || 0) + 1);
    attachmentsRequestTokens.set(conversationId, (attachmentsRequestTokens.get(conversationId) || 0) + 1);
    pendingActionsRequestTokens.set(conversationId, (pendingActionsRequestTokens.get(conversationId) || 0) + 1);
    sessionModelRequestTokens.set(conversationId, (sessionModelRequestTokens.get(conversationId) || 0) + 1);
    sessionPreferenceRequestTokens.set(
      conversationId,
      (sessionPreferenceRequestTokens.get(conversationId) || 0) + 1
    );
    sessions.value = sessions.value.filter((session) => session.conversationId !== conversationId);
    if (activeConversationId.value === conversationId) {
      const nextSession = sessions.value[0];
      if (nextSession) {
        await selectSession(nextSession.conversationId);
      } else {
        newSession();
      }
    }
    persistMessages();
    return remoteDeleted || !persisted;
  };

  const renameSession = async (
    conversationId: string,
    title: string,
    options: {allowStreaming?: boolean} = {}
  ) => {
    if (
      !conversationId ||
      (!options.allowStreaming && streamingConversationId.value === conversationId) ||
      sessionActionLoading.value[conversationId]
    ) return false;
    const currentLifecycle = lifecycleToken;
    const normalizedTitle = normalizeTitle(title);
    const previous = sessions.value.find((session) => session.conversationId === conversationId)?.title;
    sessions.value = sessions.value.map((session) =>
      session.conversationId === conversationId ? {...session, title: normalizedTitle} : session
    );

    setSessionActionLoading(conversationId, true);
    try {
      const response = await updateAgenticSession(conversationId, {
        title: normalizedTitle,
      });
      if (currentLifecycle !== lifecycleToken) return false;
      sessions.value = sessions.value.map((session) =>
          session.conversationId === conversationId ? normalizeSession({...session, ...(response || {})}) : session
      );
      return true;
    } catch (error) {
      if (currentLifecycle === lifecycleToken && previous !== undefined) {
        sessions.value = sessions.value.map((session) =>
          session.conversationId === conversationId ? {...session, title: previous} : session
        );
      }
      if (currentLifecycle === lifecycleToken) {
        warnMessage(i18n.global.t('agentic.failedSessionUpdate'), 'Agentic', error);
      }
      return false;
    } finally {
      if (currentLifecycle === lifecycleToken) setSessionActionLoading(conversationId, false);
    }
  };

  const sendMessage = async (content: string): Promise<boolean> => {
    const text = content.trim();
    if (!text || streaming.value) {
      return false;
    }

    const currentLifecycle = lifecycleToken;

    const conversationId = ensureActiveSession();
    if (!conversationId) {
      failMessage('Missing conversation context.', 'Agentic');
      return false;
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
    streamingConversationId.value = conversationId;
    streaming.value = true;

    let completed = false;
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
          onEvent: (event) => {
            if (currentLifecycle === lifecycleToken) appendTraceEvent(conversationId, event);
          },
          onVisualization: (visualization) =>
            currentLifecycle === lifecycleToken &&
            appendAssistantVisualization(conversationId, assistantMessage.id, visualization),
          onDelta: (delta) => {
            if (currentLifecycle === lifecycleToken) appendAssistantDelta(conversationId, assistantMessage.id, delta);
          },
          onReasoning: (reasoning) => {
            if (currentLifecycle === lifecycleToken) {
              appendAssistantReasoning(conversationId, assistantMessage.id, reasoning);
            }
          },
          onFinish: (reason) => {
            if (currentLifecycle === lifecycleToken) setAssistantFinishReason(conversationId, assistantMessage.id, reason);
          },
        });
      } else {
        const response = await completeAgenticChatCompletion(request, abortController.signal);
        if (currentLifecycle !== lifecycleToken) return false;
        const responseMessage = response.choices?.[0]?.message;
        appendAssistantDelta(conversationId, assistantMessage.id, responseMessage?.content || '');
        for (const chart of responseMessage?.contentExt?.charts || []) {
          appendAssistantVisualization(conversationId, assistantMessage.id, chart);
        }
        const finishReason = response.choices?.[0]?.finishReason ?? response.choices?.[0]?.finish_reason;
        if (finishReason) {
          setAssistantFinishReason(conversationId, assistantMessage.id, finishReason);
        }
      }
      if (currentLifecycle !== lifecycleToken) return false;
      markAssistantComplete(conversationId, assistantMessage.id);
      await syncSessionAfterMessage(conversationId, text);
      if (currentLifecycle !== lifecycleToken) return false;
      await Promise.all([loadMessages(conversationId), loadPendingActions(conversationId)]);
      if (currentLifecycle !== lifecycleToken) return false;
      completed = true;
      return true;
    } catch (error) {
      if (currentLifecycle !== lifecycleToken) return false;
      if ((error as Error).name === 'AbortError') {
        appendAssistantDelta(conversationId, assistantMessage.id, `\n\n${i18n.global.t('agentic.canceled')}`);
      } else {
        failMessage('Agentic chat failed.', 'Agentic', error);
        appendAssistantDelta(conversationId, assistantMessage.id, `\n\n${i18n.global.t('agentic.requestFailed')}`);
        setAssistantFinishReason(conversationId, assistantMessage.id, 'error');
      }
      markAssistantComplete(conversationId, assistantMessage.id);
      return false;
    } finally {
      if (currentLifecycle === lifecycleToken) {
        if (completed) pendingAttachmentIdsByConversation.value[conversationId] = [];
        if (streamingConversationId.value === conversationId) {
          streaming.value = false;
          streamingConversationId.value = '';
        }
        if (currentAbortController.value === abortController) currentAbortController.value = undefined;
      }
    }
  };

  const stopStreaming = () => {
    currentAbortController.value?.abort();
  };

  const loadMessages = async (conversationId: string) => {
    if (!conversationId) {
      return;
    }
    const currentLifecycle = lifecycleToken;
    const requestToken = (messagesRequestTokens.get(conversationId) || 0) + 1;
    messagesRequestTokens.set(conversationId, requestToken);
    messagesLoadingByConversation.value[conversationId] = true;
    messagesErrorByConversation.value[conversationId] = false;
    try {
      const response = await listAgenticMessages(conversationId);
      if (currentLifecycle !== lifecycleToken || messagesRequestTokens.get(conversationId) !== requestToken) return;
      // While this conversation is streaming, the local optimistic messages are
      // newer than this snapshot (its request predates them). Writing it back
      // would drop them until the post-stream reload, so skip and let that
      // reload — which runs once the server has persisted the turn — land.
      if (streamingConversationId.value === conversationId) return;
      if (response) {
        const previousMessages = messagesByConversation.value[conversationId] || [];
        const loadedMessages = response.map((message) => ({
          id: String(message.id || createMessageId(message.role)),
          role: message.role,
          content: message.content || '',
          contentExt: message.contentExt,
          messageIndex: message.messageIndex,
          status: message.status,
          reasoning: message.reasoning || message.contentExt?.reasoningContent,
        }));
        messagesByConversation.value[conversationId] = mergeEphemeralAssistantState(previousMessages, loadedMessages);
        persistMessages();
      }
    } catch (error) {
      if (currentLifecycle !== lifecycleToken || messagesRequestTokens.get(conversationId) !== requestToken) return;
      messagesErrorByConversation.value[conversationId] = true;
      warnMessage(i18n.global.t('agentic.failedMessages'), 'Agentic', error);
    } finally {
      if (currentLifecycle === lifecycleToken && messagesRequestTokens.get(conversationId) === requestToken) {
        messagesLoadingByConversation.value[conversationId] = false;
      }
    }
  };

  const loadAttachments = async (conversationId: string) => {
    if (!conversationId) {
      return;
    }
    const currentLifecycle = lifecycleToken;
    const requestToken = (attachmentsRequestTokens.get(conversationId) || 0) + 1;
    attachmentsRequestTokens.set(conversationId, requestToken);
    attachmentsLoadingByConversation.value[conversationId] = true;
    attachmentsErrorByConversation.value[conversationId] = false;
    try {
      const response = await listAgenticAttachments(conversationId);
      if (currentLifecycle !== lifecycleToken || attachmentsRequestTokens.get(conversationId) !== requestToken) return;
      attachmentsByConversation.value[conversationId] = response || [];
    } catch (error) {
      if (currentLifecycle !== lifecycleToken || attachmentsRequestTokens.get(conversationId) !== requestToken) return;
      attachmentsErrorByConversation.value[conversationId] = true;
      attachmentsByConversation.value[conversationId] = attachmentsByConversation.value[conversationId] || [];
      warnMessage(i18n.global.t('agentic.failedAttachments'), 'Agentic', error);
    } finally {
      if (currentLifecycle === lifecycleToken && attachmentsRequestTokens.get(conversationId) === requestToken) {
        attachmentsLoadingByConversation.value[conversationId] = false;
      }
    }
  };

  const uploadAttachment = async (file: File) => {
    const conversationId = ensureActiveSession();
    const currentLifecycle = lifecycleToken;
    const response = await uploadAgenticAttachment(conversationId, file);
    if (currentLifecycle !== lifecycleToken || activeConversationId.value !== conversationId) return false;
    attachmentsByConversation.value[conversationId] = [
      response,
      ...(attachmentsByConversation.value[conversationId] || []),
    ];
    pendingAttachmentIdsByConversation.value[conversationId] = [
      response.id,
      ...(pendingAttachmentIdsByConversation.value[conversationId] || []),
    ];
    return true;
  };

  const removeLocalAttachment = (attachmentId: string) => {
    const conversationId = activeConversationId.value;
    pendingAttachmentIdsByConversation.value[conversationId] = (
      pendingAttachmentIdsByConversation.value[conversationId] || []
    ).filter((id) => id !== attachmentId);
  };

  const loadPendingActions = async (conversationId: string) => {
    if (!conversationId) {
      return;
    }
    const currentLifecycle = lifecycleToken;
    const requestToken = (pendingActionsRequestTokens.get(conversationId) || 0) + 1;
    pendingActionsRequestTokens.set(conversationId, requestToken);
    pendingActionsLoadingByConversation.value[conversationId] = true;
    pendingActionsErrorByConversation.value[conversationId] = false;
    try {
      const response = await listPendingAgenticActions(conversationId);
      if (currentLifecycle !== lifecycleToken || pendingActionsRequestTokens.get(conversationId) !== requestToken) return;
      pendingActionsByConversation.value[conversationId] = response?.items || [];
    } catch (error) {
      if (currentLifecycle !== lifecycleToken || pendingActionsRequestTokens.get(conversationId) !== requestToken) return;
      pendingActionsErrorByConversation.value[conversationId] = true;
      pendingActionsByConversation.value[conversationId] = [];
      warnMessage(i18n.global.t('agentic.failedActions'), 'Agentic', error);
    } finally {
      if (currentLifecycle === lifecycleToken && pendingActionsRequestTokens.get(conversationId) === requestToken) {
        pendingActionsLoadingByConversation.value[conversationId] = false;
      }
    }
  };

  const confirmAction = async (actionId: string) => {
    if (!actionId || actionLoading.value[actionId]) return false;
    const currentLifecycle = lifecycleToken;
    const conversationId = activeConversationId.value;
    setActionError(actionId, false);
    setActionLoading(actionId, true);
    try {
      await confirmAgenticAction(actionId);
      if (currentLifecycle !== lifecycleToken) return false;
      await loadPendingActions(conversationId);
      if (currentLifecycle !== lifecycleToken) return false;
      return true;
    } catch (error) {
      if (currentLifecycle !== lifecycleToken) return false;
      setActionError(actionId, true);
      throw error;
    } finally {
      if (currentLifecycle === lifecycleToken) setActionLoading(actionId, false);
    }
  };

  const rejectAction = async (actionId: string) => {
    if (!actionId || actionLoading.value[actionId]) return false;
    const currentLifecycle = lifecycleToken;
    const conversationId = activeConversationId.value;
    setActionError(actionId, false);
    setActionLoading(actionId, true);
    try {
      await rejectAgenticAction(actionId);
      if (currentLifecycle !== lifecycleToken) return false;
      await loadPendingActions(conversationId);
      if (currentLifecycle !== lifecycleToken) return false;
      return true;
    } catch (error) {
      if (currentLifecycle !== lifecycleToken) return false;
      setActionError(actionId, true);
      throw error;
    } finally {
      if (currentLifecycle === lifecycleToken) setActionLoading(actionId, false);
    }
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
    const currentLifecycle = lifecycleToken;
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
        await renameSession(conversationId, title, {allowStreaming: true});
        return;
      }
      if (normalizeTitle(session.title || defaultSessionTitle()) !== title) {
        await renameSession(conversationId, title, {allowStreaming: true});
      }
    }
    if (currentLifecycle !== lifecycleToken) return;
  };

  const setSelectedModel = async (model: string): Promise<boolean> => {
    if (streaming.value) return false;
    const currentLifecycle = lifecycleToken;
    const nextModel = resolveModelName(String(model || ''));
    selectedModel.value = nextModel;
    applyModelCapabilities();

    const conversationId = activeConversationId.value;
    if (!conversationId) {
      return true;
    }
    updateSessionLocally(conversationId, {sessionExt: {model: nextModel}});
    const session = sessions.value.find((item) => item.conversationId === conversationId);
    if (!session?.createTime && !session?.operateTime) {
      return true;
    }
    const requestToken = (sessionModelRequestTokens.get(conversationId) || 0) + 1;
    sessionModelRequestTokens.set(conversationId, requestToken);
    try {
      const response = await updateAgenticSession(conversationId, {sessionExt: {model: nextModel}});
      if (currentLifecycle !== lifecycleToken || sessionModelRequestTokens.get(conversationId) !== requestToken) return false;
      sessions.value = sessions.value.map((session) =>
        session.conversationId === conversationId ? normalizeSession({...session, ...(response || {})}) : session
      );
      return true;
    } catch (error) {
      if (currentLifecycle === lifecycleToken && sessionModelRequestTokens.get(conversationId) === requestToken) {
        warnMessage(i18n.global.t('agentic.failedSessionModel'), 'Agentic', error);
      }
      return false;
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
    if (!conversationId || streamingConversationId.value === conversationId) return;
    const currentLifecycle = lifecycleToken;
    const sessionExt = buildCurrentSessionExt();
    updateSessionLocally(conversationId, {sessionExt});
    const session = sessions.value.find((item) => item.conversationId === conversationId);
    if (!session?.createTime && !session?.operateTime) {
      return;
    }
    const requestToken = (sessionPreferenceRequestTokens.get(conversationId) || 0) + 1;
    sessionPreferenceRequestTokens.set(conversationId, requestToken);
    try {
      await updateAgenticSession(conversationId, {
        sessionExt,
      });
    } catch (error) {
      if (currentLifecycle === lifecycleToken && sessionPreferenceRequestTokens.get(conversationId) === requestToken) {
        warnMessage(i18n.global.t('agentic.failedSessionPreferences'), 'Agentic', error);
      }
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
    sessionsLoading,
    sessionsError,
    streaming,
    streamingConversationId,
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
    sessionActionLoading,
    actionLoading,
    actionErrors,
    messagesLoadingByConversation,
    messagesErrorByConversation,
    attachmentsLoadingByConversation,
    attachmentsErrorByConversation,
    pendingActionsLoadingByConversation,
    pendingActionsErrorByConversation,
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
    open,
    close,
    toggle,
    bootstrap,
    loadSessions,
    retrySessions,
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
      lifecycleToken += 1;
      currentAbortController.value?.abort();
      visible.value = false;
      bootstrapped.value = false;
      loading.value = false;
      sessionsLoading.value = false;
      sessionsError.value = null;
      streaming.value = false;
      streamingConversationId.value = '';
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
      sessionActionLoading.value = {};
      actionLoading.value = {};
      actionErrors.value = {};
      messagesLoadingByConversation.value = {};
      messagesErrorByConversation.value = {};
      attachmentsLoadingByConversation.value = {};
      attachmentsErrorByConversation.value = {};
      pendingActionsLoadingByConversation.value = {};
      pendingActionsErrorByConversation.value = {};
      sessionsRequestToken += 1;
      selectionRequestToken += 1;
      messagesRequestTokens.clear();
      attachmentsRequestTokens.clear();
      pendingActionsRequestTokens.clear();
      sessionModelRequestTokens.clear();
      sessionPreferenceRequestTokens.clear();
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
  return trimmed.length > 32 ? `${trimmed.slice(0, 32)}...` : trimmed || defaultSessionTitle();
};

const shouldGenerateSessionTitle = (title?: string) => {
  return !title || normalizeTitle(title) === defaultSessionTitle();
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

const normalizeBoolean = (value: unknown): boolean | undefined => {
  if (value === undefined || value === null || value === '') {
    return undefined;
  }
  if (typeof value === 'boolean') {
    return value;
  }
  if (typeof value === 'number') {
    return value !== 0;
  }
  const normalized = String(value).trim().toLowerCase();
  if (['true', '1', 'yes', 'y', 'on', 'enable', 'enabled'].includes(normalized)) {
    return true;
  }
  if (['false', '0', 'no', 'n', 'off', 'disable', 'disabled'].includes(normalized)) {
    return false;
  }
  return undefined;
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
    reasoningEnabled: normalizeBoolean(sessionExt.reasoningEnabled ?? sessionExt.reasoning_enabled),
    temperature: sessionExt.temperature,
    maxTokens: sessionExt.maxTokens ?? sessionExt.max_tokens,
    icon: sessionExt.icon,
    category: sessionExt.category,
  };
};

const sessionExtOf = (session?: AgenticSession): AgenticSessionExt | undefined => {
  return normalizeSessionExt((session as RawAgenticSession | undefined)?.sessionExt);
};

const sessionModel = (session?: AgenticSession) => {
  return sessionExtOf(session)?.model;
};
