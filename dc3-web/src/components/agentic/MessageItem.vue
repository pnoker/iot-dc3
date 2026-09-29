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
  <article
    :class="[
      `agentic-message--${message.role}`,
      {'is-continued': continued, 'is-focused': focused},
    ]"
    class="agentic-message"
    @click="emit('focus')"
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
          <QuoteCard
            v-if="userMessageParts(message.content).quote"
            :label="userMessageParts(message.content).label"
            :text="userMessageParts(message.content).quote || ''"
            tone="on-primary"
          />
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
        <NoticeBar v-if="message.role === 'assistant' && !message.streaming && truncatedReason(message)" tone="warn">
          {{ truncatedReason(message) }}
        </NoticeBar>
        <div v-if="historyAttachments.length" class="agentic-attachments-inline">
          <AttachmentCard
            v-for="attachment in historyAttachments"
            :key="attachment.id"
            :name="attachment.fileName"
            :pending="false"
            :size="attachment.size"
          />
        </div>
        <MessageDetails
          v-if="message.role === 'assistant'"
          :live-traces="liveTraces"
          :message="message"
        />
      </div>
      <!-- Fixed-height meta row: time + copy + quote fade in on hover without
           ever shifting the layout (no height/margin transitions). -->
      <div class="agentic-message__toolbar">
        <el-tag
          v-if="messageBadge"
          :type="messageBadge.tone"
          class="agentic-message__badge"
          effect="plain"
          size="small"
        >
          {{ messageBadge.label }}
        </el-tag>
        <time v-if="message.createTime" :datetime="message.createTime" class="agentic-message__time">
          {{ formatShortDateTime(message.createTime) }}
        </time>
        <el-tooltip :content="t('agentic.actionCopy')" placement="top">
          <button
            :aria-label="t('agentic.actionCopy')"
            :disabled="!canShowMessageToolbar(message)"
            class="agentic-message__action"
            type="button"
            @click="emit('copy', message)"
          >
            <el-icon>
              <DocumentCopy/>
            </el-icon>
          </button>
        </el-tooltip>
        <el-tooltip :content="t('agentic.actionQuote')" placement="top">
          <button
            :aria-label="t('agentic.actionQuote')"
            :disabled="!canShowMessageToolbar(message)"
            class="agentic-message__action"
            type="button"
            @click="emit('quote', message)"
          >
            <el-icon>
              <Tickets/>
            </el-icon>
          </button>
        </el-tooltip>
      </div>
    </div>
  </article>
</template>

<script lang="ts" setup>
import {CircleCheck, DocumentCopy, Loading, Tickets} from '@element-plus/icons-vue';
import {computed} from 'vue';
import {useI18n} from 'vue-i18n';
import type {AgenticAttachment, AgenticMessage, AgenticTraceEvent} from '@/config/types';
import {formatShortDateTime} from '@/utils/timeUtil';
import MessageDetails from './MessageDetails.vue';
import AttachmentCard from './cards/AttachmentCard.vue';
import NoticeBar from './cards/NoticeBar.vue';
import QuoteCard from './cards/QuoteCard.vue';
import RenderedAssistantMessage from './RenderedAssistantMessage.vue';
import {userMessageParts} from './assistantContent';
import {createAssistantDetails} from './assistantDetails';

const props = defineProps<{
  message: AgenticMessage;
  /** Attachment metadata for the conversation, keyed by id (history display). */
  attachmentsById?: Record<string, AgenticAttachment>;
  /** Same role as the previous message — keeps the run visually grouped. */
  continued: boolean;
  /** Currently inspected message (feeds the workbench context panel). */
  focused: boolean;
  liveTraces: AgenticTraceEvent[];
}>();
const emit = defineEmits<{
  copy: [message: AgenticMessage];
  quote: [message: AgenticMessage];
  focus: [];
}>();

const {t} = useI18n();
const {
  assistantReasoningText,
  hasReasoningPanel,
  isReasoningPanelOpen,
  reasoningPanelStatus,
  truncatedReason,
} = createAssistantDetails({t, liveTraces: () => props.liveTraces});

const canShowMessageToolbar = (message: AgenticMessage) => {
  return Boolean(message.content?.trim()) && !message.streaming;
};

// Message-level outcome badge: failures and recoveries used to be invisible
// unless the footnote happened to render.
const messageBadge = computed(() => {
  const message = props.message;
  const reason = message.finishReason?.toLowerCase();
  const failed = message.status === 'FAILED' || reason === 'error' || reason === 'failed';
  if (failed) return {label: t('agentic.statusFailed'), tone: 'danger' as const};
  const cancelled = message.status === 'CANCELLED' || reason === 'cancelled' || reason === 'canceled';
  if (cancelled) return {label: t('agentic.statusCancelled'), tone: 'warning' as const};
  if (message.contentExt?.recovered) return {label: t('agentic.recovered'), tone: 'warning' as const};
  return null;
});

const historyAttachments = computed(() =>
  (props.message.contentExt?.attachments || [])
    // legacy payloads may store whole objects instead of ids — skip junk
    .filter((id): id is string => typeof id === 'string' && id.length > 0)
    .map((id) => props.attachmentsById?.[id])
    .filter((attachment): attachment is AgenticAttachment => Boolean(attachment))
);
</script>

<style lang="scss" scoped>
.agentic-message {
  // Clicking a message retargets the context panel — deliberately WITHOUT any
  // selection chrome: hover already reveals the toolbar, and a persistent
  // "selected" look reads as noise in a conversation.
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
    max-width: 85%;
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
  padding: 12px 14px;
  border: 1px solid var(--dc3-border-base);
  border-radius: var(--dc3-radius-lg);
  color: var(--dc3-text-primary);
  line-height: 1.58;
  overflow-wrap: anywhere;
  background: var(--dc3-bg-elevated);
  box-shadow: var(--dc3-shadow-sm);
}

.agentic-text {
  overflow-wrap: anywhere;
  white-space: pre-wrap;
}

.agentic-user-content {
  min-width: 0;
}




.agentic-message__toolbar {
  // Fixed-height meta row: reserved at all times so hover reveal is a pure
  // fade — height/margin must never participate or the message below jumps.
  display: flex;
  align-items: center;
  gap: 4px;
  width: fit-content;
  height: 24px;
  opacity: 0;
  visibility: hidden;
  pointer-events: none;
  transition:
    opacity var(--dc3-duration-fast) var(--dc3-ease-standard),
    visibility var(--dc3-duration-fast) var(--dc3-ease-standard);
}

.agentic-message__time {
  color: var(--dc3-text-muted);
  font-size: 11px;
  line-height: 24px;
}

.agentic-message:hover .agentic-message__toolbar,
.agentic-message:focus-within .agentic-message__toolbar {
  opacity: 1;
  visibility: visible;
  pointer-events: auto;
}

.agentic-message__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 1px solid var(--dc3-border-base);
  border-radius: var(--dc3-radius-md);
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

.agentic-attachments-inline {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 8px;
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
  border-radius: var(--dc3-radius-md);
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
    border-radius: var(--dc3-radius-full);
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

@media (hover: none), (any-pointer: coarse) {
  .agentic-message__toolbar {
    opacity: 1;
    visibility: visible;
    pointer-events: auto;
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
