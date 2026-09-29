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
  <transition name="agentic-panel">
  <aside
    v-if="visible"
    ref="panelRef"
    :aria-label="t('agentic.title')"
    :aria-modal="isMobile || undefined"
    :class="['agentic-panel', {'agentic-panel--expanded': expanded}]"
    :role="isMobile ? 'dialog' : 'complementary'"
    :style="expanded ? undefined : panelStyle"
    tabindex="-1"
    @keydown="handlePanelKeydown"
  >
    <button
      v-if="!expanded"
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
    <div class="agentic-workspace">
      <!-- Workbench sessions rail — visible only in expanded (full-screen)
           mode. The same list the history dropdown shows, laid out as a
           permanent column so switching conversations is one tap. -->
      <AgenticSessionsRail
        v-if="expanded"
        :active-id="activeConversationId"
        :collapsed="agenticRailCollapsed"
        :disabled="interactionLocked"
        :session-action-loading="sessionActionLoading"
        :sessions="conversationItems"
        :truncated="conversationItems.length >= 50"
        @delete="handleDeleteSession"
        @new="handleNewSession"
        @archive="handleArchiveSession"
        @rename="handleRenameSession"
        @select="handleSelectSession"
        @toggle-collapse="appStore.toggleAgenticRailCollapsed()"
      />

      <section v-loading="loading" class="agentic-shell">
      <header class="agentic-header">
        <div class="agentic-header__top">
          <div class="agentic-title">
            <div class="agentic-mark entity-icon-tile">
              <svg aria-hidden="true" class="agentic-mark__sparkle" viewBox="0 0 24 24">
                  <path
                    d="M12 1.5c1 8.4 2.1 11.6 10.5 12.5-8.4.9-9.5 4.1-10.5 12.5-1-8.4-2.1-11.6-10.5-12.5C9.9 13.1 11 9.9 12 1.5Z"
                  />
                  <path
                    d="M19.5 2c.3 2.3.85 3.3 3.1 3.65-2.25.35-2.8 1.35-3.1 3.65-.3-2.3-.85-3.3-3.1-3.65 2.25-.35 2.8-1.35 3.1-3.65Z"
                  />
              </svg>
            </div>
            <div>
              <strong>{{ currentSession?.title || t('agentic.newConversation') }}</strong>
              <span>{{ sessionSubtitle }}</span>
              <code v-if="activeConversationId" class="agentic-title__sid" :title="activeConversationId"
                >{{ activeConversationId }}</code
              >
            </div>
          </div>

          <div class="agentic-header__primary-actions">
            <el-tooltip v-if="expanded" :content="t('agentic.contextPanel')">
              <el-button
                :aria-label="t('agentic.contextPanel')"
                circle
                class="agentic-tool"
                size="small"
                @click="toggleContextPanel"
              >
                <el-icon>
                  <DataAnalysis/>
                </el-icon>
              </el-button>
            </el-tooltip>
            <!-- Docked mode: every session control lives in ONE header row
                 (history / rename / delete / new / expand / close). In workbench
                 mode they move to the sessions rail instead. -->
            <template v-if="!expanded">
<el-dropdown
              :disabled="loading || sessionsLoading || streaming"
              popper-class="agentic-history-popover"
              trigger="click"
              @command="handleHistoryCommand"
            >
              <el-button
                :aria-label="t('agentic.headerHistory')"
                :disabled="loading || sessionsLoading || streaming"
                :title="t('agentic.headerHistory')"
                circle
                class="agentic-tool"
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
                  class="agentic-tool"
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
                class="agentic-tool"
                size="small"
                @click="handleDeleteCurrent"
              >
                <el-icon>
                  <Delete/>
                </el-icon>
              </el-button>
            </el-tooltip>
            </template>
            <el-tooltip v-if="!expanded" :content="t('agentic.headerNew')">
              <el-button
                :aria-label="t('agentic.headerNew')"
                :disabled="loading || sessionsLoading || streaming"
                circle
                class="agentic-tool"
                size="small"
                @click="handleNewSession"
              >
                <el-icon>
                  <Plus/>
                </el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip
              v-if="!isMobile"
              :content="expanded ? t('agentic.headerCollapse') : t('agentic.headerExpand')"
            >
              <el-button
                :aria-label="expanded ? t('agentic.headerCollapse') : t('agentic.headerExpand')"
                circle
                class="agentic-tool"
                size="small"
                @click="toggleExpanded"
              >
                <el-icon>
                  <Aim v-if="expanded"/>
                  <FullScreen v-else/>
                </el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip :content="t('agentic.headerClose')">
              <el-button
                :aria-label="t('agentic.headerClose')"
                circle
                class="agentic-tool"
                size="small"
                @click="handleClosePanel"
              >
                <el-icon>
                  <Close/>
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

      <main ref="bodyRef" class="agentic-body" @scroll.passive="handleBodyScroll">
        <MessageStream
          :focused-message-id="focusedMessageId"
          :loading="currentMessagesLoading"
          :live-traces="currentTraceEvents"
          :messages="currentMessages"
          :attachments-by-id="attachmentsById"
        :prompts="promptItems"
          @copy="handleCopyMessage"
          @focus-message="focusedMessageId = $event"
          @prompt-click="handlePromptClick"
          @quote="handleQuoteMessage"
        />
      </main>

      <AgenticComposer
        v-model:draft="draft"
        v-model:quoted-message="quotedMessage"
        @submit="handleComposerSubmit"
        @submitted="handleComposerSubmitted"
      />
    </section>
      <AgenticContextPanel
        v-if="expanded && contextPanelOpen"
        :live-traces="currentTraceEvents"
        :message="focusedMessage"
        :model-label="activeModel.label || selectedModel"
        :session="contextSession"
        @toggle="toggleContextPanel"
      />
    </div>
  </aside>
  </transition>
</template>

<script lang="ts" setup>
import 'vue-element-plus-x/styles/index.css';

import {
  Aim,
  Clock,
  Close,
  DataAnalysis,
  Delete,
  EditPen,
  FullScreen,
  Plus,
} from '@element-plus/icons-vue';
import {ElMessage, ElMessageBox} from 'element-plus';
import {storeToRefs} from 'pinia';
import {useI18n} from 'vue-i18n';
import {computed, inject, nextTick, onBeforeUnmount, onMounted, ref, unref, watch} from 'vue';
import {useBreakpoint} from '@/composables/useBreakpoint';
import {useMediaQuery} from '@/composables/useMediaQuery';
import type {AgenticAttachment, AgenticMessage} from '@/config/types';
import {useAgenticStore, useAppStore} from '@/store';
import MessageStream from './MessageStream.vue';
import AgenticContextPanel from './AgenticContextPanel.vue';
import AgenticComposer from './AgenticComposer.vue';
import AgenticSessionsRail from './AgenticSessionsRail.vue';
import {toPlainText} from './assistantContent';
import {formatShortDateTime} from '@/utils/timeUtil';
import {sessionIcon} from './sessionIcons';
import {routeLocationKey, routerKey} from 'vue-router';
import {agenticUiQuery, readAgenticUiState, writeAgenticUiState} from './agenticUiState';

interface AssistantPromptItem {
  key: string | number;
  label?: string;
  description?: string;
}


const agenticStore = useAgenticStore();
const appStore = useAppStore();
const {agenticRailCollapsed} = storeToRefs(appStore);

const {t, locale} = useI18n();
const {isDesktop, isMobile, is: isTier} = useBreakpoint();
const prefersReducedMotion = useMediaQuery('(prefers-reduced-motion: reduce)');
const {
  visible,
  loading,
  sessionsLoading,
  sessionsError,
  streaming,
  sessions,
  activeConversationId,
  activeModel,
  selectedModel,
  currentMessages,
  currentSession,
  currentTraceEvents,
  currentMessagesLoading,
  currentMessagesError,
  sessionActionLoading,
  workbenchExpanded: expanded,
} = storeToRefs(agenticStore);


const draft = ref('');
const quotedMessage = ref<AgenticMessage>();
const bodyRef = ref<HTMLElement>();
// Scroll bookkeeping: streaming increments must not yank the view back to
// the bottom while the user is reading history (see scheduleScrollToBottom).
const isAtBottom = ref(true);
const userJustSent = ref(false);
const BOTTOM_THRESHOLD_PX = 80;
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
// The sm tier keeps the docked panel compact so the page behind stays
// readable; the cap lives here instead of a CSS !important rule so that
// drag-resize keeps working and the workbench width is never caught by it.
const TABLET_PANEL_WIDTH_MAX = 420;

const interactionLocked = computed(() => loading.value || sessionsLoading.value || streaming.value);

const storedPanelWidth = Number(localStorage.getItem(ASSISTANT_WIDTH_STORAGE_KEY) || '');
if (Number.isFinite(storedPanelWidth) && storedPanelWidth > 0) {
  panelWidth.value = Math.min(Math.max(storedPanelWidth, MIN_PANEL_WIDTH), MAX_PANEL_WIDTH);
}

const effectivePanelWidth = computed(() => clamp(panelWidth.value || 340, MIN_PANEL_WIDTH, MAX_PANEL_WIDTH));
const panelStyle = computed(() => {
  if (isMobile.value) {
    return {width: '100%'};
  }
  const tablet = isTier.sm.value;
  const viewportCap = tablet ? '40vw' : '45vw';
  if (panelWidth.value) {
    const widthCap = tablet ? TABLET_PANEL_WIDTH_MAX : MAX_PANEL_WIDTH;
    return {width: `min(${Math.min(effectivePanelWidth.value, widthCap)}px, ${viewportCap})`};
  }
  return {width: tablet ? 'clamp(320px, 40vw, 420px)' : 'clamp(340px, 30vw, 520px)'};
});

// Publish the dock width so Layout's .body-main can slide over (the panel is
// position:fixed, so the page reserves space through this custom property).
// clamp()/min() expressions must live in the custom property itself — the
// spacing guardrail forbids them in margin declarations.
watch(
  [panelStyle, visible, expanded, isMobile],
  () => {
    const active = visible.value && !expanded.value && !isMobile.value;
    const width = active ? `max(${panelStyle.value.width}, ${MIN_PANEL_WIDTH}px)` : '0px';
    document.documentElement.style.setProperty('--dc3-agentic-dock-width', width);
  },
  {immediate: true}
);

onBeforeUnmount(() => {
  document.documentElement.style.removeProperty('--dc3-agentic-dock-width');
});

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

// ---- expanded (workbench) mode -------------------------------------------
// Full-screen takes over the region below the header and splits into a
// sessions rail + chat column; the docked side panel stays as-is.
// The flag lives in the agentic store (workbenchExpanded): it outlives route
// changes while the panel is open and is cleared by close()/reset(), so it
// cannot leak into the next open.
const toggleExpanded = () => {
  agenticStore.toggleWorkbench();
};
const handleClosePanel = () => {
  agenticStore.close();
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

const handleSelectSession = async (conversationId: string) => {
  if (interactionLocked.value) return;
  await agenticStore.selectSession(conversationId);
};

const handleArchiveSession = (conversationId: string, archived: boolean) => {
  void agenticStore.setSessionArchived(conversationId, archived);
};

const handleRenameSession = async (conversationId: string, title: string) => {
  if (interactionLocked.value) return;
  if (!conversationId) return;
  await agenticStore.renameSession(conversationId, title);
};

// Header subtitle: conversation stats instead of a static product name.
const sessionSubtitle = computed(() => {
  const rounds = currentMessages.value.filter((message) => message.role === 'user').length;
  const parts: string[] = [];
  if (rounds > 0) parts.push(t('agentic.sessionRounds', {n: rounds}));
  const updated = currentSession.value?.operateTime || currentSession.value?.createTime;
  if (updated) parts.push(t('agentic.sessionUpdated', {time: formatShortDateTime(updated)}));
  return parts.join(' · ');
});

// ---- UI state persistence (refresh / shareable URL) --------------------
// The panel mode (closed / docked / workbench) and the selected conversation
// survive a reload: written into the URL query and mirrored to localStorage,
// restored with URL taking precedence (see agenticUiState.ts).
// Optional router access: tests mount this component without a router, and
// missing injections must stay silent (the suite fails on Vue warnings).
const router = inject(routerKey, undefined);
const routeRef = inject(routeLocationKey, undefined);
const routeQuery = () => (routeRef ? unref(routeRef).query : {});
const routePath = () => (routeRef ? unref(routeRef).fullPath : '');

const currentUiState = () => ({
  mode: !visible.value ? ('closed' as const) : expanded.value ? ('workbench' as const) : ('open' as const),
  conversationId: activeConversationId.value || undefined,
});

const syncUiState = () => {
  const state = currentUiState();
  writeAgenticUiState(state);
  if (!router || !routeRef) return;
  const current = unref(routeRef);
  const query = {...current.query, ...agenticUiQuery(state)};
  const unchanged =
    String(query.agentic ?? '') === String(current.query.agentic ?? '') &&
    String(query.session ?? '') === String(current.query.session ?? '');
  if (!unchanged) void router.replace({query});
};

const restoreUiState = async () => {
  const saved = readAgenticUiState(routeQuery() as Record<string, unknown>);
  if (!saved) return;
  if (saved.mode === 'closed') {
    writeAgenticUiState(saved);
    return;
  }
  await agenticStore.open();
  if (saved.conversationId && agenticStore.sessions.some((item) => item.conversationId === saved.conversationId)) {
    await agenticStore.selectSession(saved.conversationId);
  }
  if (saved.mode === 'workbench') agenticStore.setWorkbenchExpanded(true);
};

onMounted(() => {
  void restoreUiState();
});

watch([visible, expanded, activeConversationId], syncUiState);
// In-app navigation drops the query — re-assert it so the URL keeps recording.
watch(routePath, syncUiState);

// Composer submit bookkeeping: follow the stream right after a send, but
// leave the reading position alone for background streaming increments.
// Workbench context panel: session meta + the focused message's run details.
const contextPanelOpen = computed(() => appStore.agenticContextPanelOpen ?? isDesktop.value);
const toggleContextPanel = () => {
  appStore.setAgenticContextPanelOpen(!contextPanelOpen.value);
};

// Which message the context panel inspects; defaults to the last assistant
// message so the panel is useful right after opening the workbench.
const focusedMessageId = ref('');
const effectiveFocusedMessageId = computed(() => {
  if (focusedMessageId.value) return focusedMessageId.value;
  const last = [...currentMessages.value].reverse().find((message) => message.role === 'assistant');
  return last?.id || '';
});
const focusedMessage = computed(() =>
  currentMessages.value.find((message) => message.id === effectiveFocusedMessageId.value)
);
const attachmentsById = computed<Record<string, AgenticAttachment>>(() =>
  Object.fromEntries(
    (agenticStore.attachmentsByConversation[activeConversationId.value] || []).map((attachment) => [
      String(attachment.id),
      attachment,
    ])
  )
);

const contextSession = computed(() =>
  conversationItems.value.find((session) => session.conversationId === activeConversationId.value)
);

const handleComposerSubmit = () => {
  userJustSent.value = true;
};

const handleComposerSubmitted = () => {
  void nextTick(() => {
    userJustSent.value = false;
  });
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

const handleDeleteCurrent = () => handleDeleteSession(activeConversationId.value);

const handleDeleteSession = async (conversationId: string) => {
  if (interactionLocked.value || !conversationId) {
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

const handleRetryMessages = () => {
  if (interactionLocked.value || !activeConversationId.value) return;
  void agenticStore.loadMessages(activeConversationId.value);
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
  if (event.key === 'Escape') {
    event.preventDefault();
    // Graded exit: the first Escape leaves the workbench but keeps the
    // docked panel, the second one closes the panel. Docked mode has no
    // workbench to leave, so Escape closes right away (e2e relies on this).
    if (expanded.value && !isMobile.value) {
      toggleExpanded();
      return;
    }
    agenticStore.close();
    return;
  }
  if (!isMobile.value) return;
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

const clamp = (value: number, min: number, max: number) => {
  return Math.min(Math.max(value, min), max);
};

const handleBodyScroll = () => {
  const body = bodyRef.value;
  if (!body) return;
  isAtBottom.value = body.scrollHeight - body.scrollTop - body.clientHeight < BOTTOM_THRESHOLD_PX;
};

const scheduleScrollToBottom = () => {
  // Follow the stream only when the reader is already at the bottom or has
  // just sent a message; otherwise leave the reading position alone.
  if (!isAtBottom.value && !userJustSent.value) {
    return;
  }
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

</script>

<style lang="scss" scoped>
// The panel lives in ONE coordinate system for both modes: fixed under
// the header, anchored right. Docked width comes from panelStyle; the
// workbench just widens to 100% — because both states are fixed, the
// width transition animates continuously instead of snapping between
// layout flow and fixed positioning (the old "slide projector" cut).
.agentic-panel {
  // One reading column for messages, the empty state and the composer -
  // docked the panel is narrower so this is a no-op there; the workbench
  // grows around this column instead of stretching text across the screen.
  --agentic-read-width: 760px;
  --agentic-chart-height: clamp(200px, 28vh, 320px);
  --agentic-context-width: 300px;

  position: fixed;
  top: var(--dc3-header-height);
  right: 0;
  bottom: 0;
  display: flex;
  z-index: calc(var(--dc3-z-floating-action) - 1);
  min-width: 320px;
  max-width: 520px;
  min-height: 0;
  border-left: 1px solid var(--dc3-border-base);
  // Sit on the shared canvas instead of a white slab: the sidebar is part
  // of the page, and its content follows the platform's card grammar.
  background: transparent;
  box-shadow: none;
  transition: width var(--dc3-duration-slow) var(--dc3-ease-standard);

  // No left: 0 here: with right: 0 already anchoring the panel, adding a
  // left anchor over-constrains the box and made the width transition snap
  // the left edge to x=0 before growing (the slide-projector cut). The
  // panel now grows leftward from the right edge in one continuous motion.
  &.agentic-panel--expanded {
    min-width: 0;
    max-width: none;
    width: 100%;
    border-left: none;
    box-shadow: none;
    // Docked mode sits transparent on the shared canvas; the workbench
    // covers the page and needs an opaque backdrop of its own.
    background: var(--dc3-bg-canvas);
  }
}
// Width tracking must stay 1:1 with the pointer during a drag.
body.agentic-resizing .agentic-panel {
  transition: none;
}
// Open / close: slide in from the right edge with a soft fade.
.agentic-panel-enter-active {
  // Plain fade: the panel is a real sidebar column, not a floating layer.
  transition: opacity 280ms ease-out;
}
.agentic-panel-leave-active {
  transition: opacity 200ms ease-in;
}
.agentic-panel-enter-from,
.agentic-panel-leave-to {
  opacity: 0;
}
// Sessions rail (expanded mode only) — same list as the history dropdown,
// laid out as a permanent column.
.agentic-sessions {
  display: flex;
  flex-direction: column;
  flex: 0 0 264px;
  min-width: 0;
  border-right: 1px solid var(--dc3-border-base);
  background: var(--dc3-bg-canvas);

  &__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--dc3-space-3) var(--dc3-space-3) var(--dc3-space-2);
    color: var(--dc3-text-muted);
    font-size: 12px;
    font-weight: 600;
  }

  &__list {
    flex: 1;
    min-height: 0;
  }

  &__item {
    display: flex;
    align-items: center;
    gap: var(--dc3-space-2);
    width: 100%;
    padding: var(--dc3-space-2) var(--dc3-space-3);
    border: 0;
    border-radius: var(--dc3-radius-md);
    background: transparent;
    color: var(--dc3-text-regular);
    cursor: pointer;
    text-align: left;

    &:hover {
      background: var(--dc3-bg-interactive);
      color: var(--el-color-primary);
    }

    &.is-active {
      background: var(--dc3-brand-gradient-soft);
      box-shadow: inset 0 0 0 1px var(--dc3-border-strong);
      color: var(--el-color-primary);

      strong {
        color: inherit;
      }
    }
  }

  &__item-icon {
    flex-shrink: 0;
    color: var(--el-color-primary);
    font-size: 16px;
  }

  &__item-text {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;

    strong {
      overflow: hidden;
      font-size: 13px;
      font-weight: 600;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    small {
      overflow: hidden;
      color: var(--dc3-text-muted);
      font-size: 11px;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  &__empty {
    padding: var(--dc3-space-3);
    color: var(--dc3-text-muted);
    font-size: 12px;
  }
}
.agentic-workspace {
  position: relative;
  display: flex;
  flex: 1 1 auto;
  min-width: 0;
  min-height: 0;
}
// Neutral tool chip for the header actions (new / expand / close) — the
// old success/danger circles fought the theme; this matches the utility
// buttons used across the app header.
.agentic-tool {
  border: 1px solid var(--dc3-border-base);
  background: var(--dc3-bg-interactive);
  color: var(--dc3-text-regular);

  &:hover,
  &:focus-visible {
    background: var(--dc3-bg-interactive-active);
    color: var(--dc3-text-brand);
  }
}
// Sessions rail header actions (rename / delete / new for the active
// conversation) — grouped so the rail owns all conversation management in
// workbench mode.
.agentic-sessions__header-actions {
  display: flex;
  align-items: center;
  gap: 4px;
}
// Model chip in the composer bar (Codex-style): the current model rides
// with the input, and its popover carries model + reasoning + sampling
// settings so the header stays free of controls in every mode.
.agentic-model-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  max-width: 220px;
  padding: 0 10px;
  height: 28px;
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
  flex: 1 1 auto;
  height: 100%;
  min-height: 0;
  min-width: 0;
  background: transparent;
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
  background: transparent;
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
.agentic-title,
.agentic-header__top,
.agentic-header__actions,
.agentic-header__actions-right,
.agentic-header__primary-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.agentic-header__top {
  justify-content: space-between;
  width: 100%;
}
.agentic-header__primary-actions {
  // Docked mode packs every session control into this single row — the
  // title absorbs the squeeze, the buttons never wrap.
  flex: 0 0 auto;
  flex-wrap: nowrap;
  gap: var(--dc3-space-1);
}
.agentic-header {
  :deep(.el-button + .el-button) {
    margin-left: 0;
  }
}
// Brand tile on the header — the shared entity-icon-tile family (see
// global.scss), sized down to fit the compact header.
.agentic-mark.entity-icon-tile {
  .agentic-mark__sparkle {
    width: 21px;
    height: 21px;
    fill: var(--dc3-text-brand);
  }

  
  flex: 0 0 36px;
  width: 36px;
  height: 36px;
}
.agentic-title {
  overflow: hidden;

  // Conversation id for debugging — muted mono, never competes with the title.
  .agentic-title__sid {
    max-width: 100%;
    margin-top: 1px;
    overflow: hidden;
    color: var(--dc3-text-muted);
    font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
    font-size: 10px;
    letter-spacing: 0.02em;
    text-overflow: ellipsis;
    white-space: nowrap;
    user-select: all;
  }

  min-width: 0;
  flex: 1 1 auto;

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
  border-radius: var(--dc3-radius-md);

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
  background: transparent;
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
    border-radius: var(--dc3-radius-md);
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
    border-radius: var(--dc3-radius-sm);
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
    border-radius: var(--dc3-radius-md);
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
  // Docked panel width on this tier is resolved in panelStyle (no !important),
  // so the inline drag width and the workbench width stay authoritative.
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

// Width tracking must stay 1:1 with the pointer during a drag — the main
// column slides through --dc3-agentic-dock-width and must not lag either.
body.agentic-resizing .body-main {
  transition: none !important;
}

// Teleported poppers live outside the panel's stacking context (the panel is
// z-index 999); style them globally so they are neither clipped nor out-z'd.
.agentic-model-popover.el-popper {
  padding: var(--dc3-space-4);
  border-radius: var(--dc3-radius-lg);
  box-shadow: var(--dc3-shadow-md);
}

.agentic-history-popover {
  max-height: 360px;
  overflow-y: auto;
}

// Rename popper content (shared by the docked header and the sessions rail;
// both poppers teleport here, outside either component's scoped styles).
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

</style>
