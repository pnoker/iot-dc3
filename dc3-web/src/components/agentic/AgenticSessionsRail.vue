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
  <nav :aria-label="t('agentic.sessionsTitle')" :class="{'is-collapsed': collapsed}" class="agentic-sessions">
    <!-- Top row: search box + new-conversation button. Collapsed, the pair
         becomes the icon rail's leading icons. -->
    <div class="agentic-sessions__top">
      <template v-if="!collapsed">
        <el-input
          ref="searchInputRef"
          v-model="searchQuery"
          :placeholder="t('agentic.searchSessions')"
          :prefix-icon="Search"
          class="agentic-sessions__search"
          clearable
        />
        <el-tooltip :content="t('agentic.headerNew')" placement="right">
          <el-button
            :aria-label="t('agentic.headerNew')"
            :disabled="disabled"
            circle
            class="agentic-tool"
            size="small"
            @click="emit('new')"
          >
            <el-icon>
              <Plus/>
            </el-icon>
          </el-button>
        </el-tooltip>
      </template>
      <template v-else>
        <el-tooltip :content="t('agentic.searchSessions')" placement="right">
          <el-button
            :aria-label="t('agentic.searchSessions')"
            circle
            class="agentic-tool"
            size="small"
            @click="focusSearch"
          >
            <el-icon>
              <Search/>
            </el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip :content="t('agentic.headerNew')" placement="right">
          <el-button
            :aria-label="t('agentic.headerNew')"
            :disabled="disabled"
            circle
            class="agentic-tool"
            size="small"
            @click="emit('new')"
          >
            <el-icon>
              <Plus/>
            </el-icon>
          </el-button>
        </el-tooltip>
      </template>
    </div>
    <el-scrollbar class="agentic-sessions__list">
      <div class="agentic-sessions__view">
        <div
          v-for="session in filteredSessions"
          :key="session.conversationId"
          :class="{'is-active': session.conversationId === activeId}"
          class="agentic-sessions__item"
        >
          <button
            :disabled="Boolean(sessionActionLoading[session.conversationId])"
            :title="session.title"
            class="agentic-sessions__item-main"
            type="button"
            @click="emit('select', session.conversationId)"
          >
            <el-icon class="agentic-sessions__item-icon">
              <ChatDotRound/>
            </el-icon>
            <span class="agentic-sessions__item-text">
              <strong>{{ session.title }}</strong>
              <small v-if="session.summary">{{ session.summary }}</small>
            </span>
          </button>
          <!-- Per-item actions: rename / archive / delete behind a hover-revealed ⋯ -->
          <el-dropdown
            :disabled="disabled || Boolean(sessionActionLoading[session.conversationId])"
            popper-class="agentic-item-menu"
            trigger="click"
            @command="(command) => handleItemCommand(command, session)"
          >
            <button
              :aria-label="t('agentic.itemActions')"
              class="agentic-sessions__item-more"
              type="button"
              @click.stop
            >
              <el-icon>
                <MoreFilled/>
              </el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">
                  <el-icon><EditPen/></el-icon>
                  {{ t('agentic.headerRename') }}
                </el-dropdown-item>
                <el-dropdown-item :command="session.sessionExt?.archived ? 'unarchive' : 'archive'">
                  <el-icon><FolderOpened v-if="session.sessionExt?.archived"/><Box v-else/></el-icon>
                  {{ session.sessionExt?.archived ? t('agentic.unarchive') : t('agentic.archive') }}
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided>
                  <el-icon><Delete/></el-icon>
                  {{ t('agentic.headerDelete') }}
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
        <div v-if="filteredSessions.length === 0" class="agentic-sessions__empty">
          {{ searchQuery ? t('agentic.searchEmpty') : t('agentic.headerNoHistory') }}
        </div>
        <div v-else-if="truncated" class="agentic-sessions__hint">
          {{ t('agentic.sessionsTruncated') }}
        </div>

        <div v-if="filteredArchived.length" class="agentic-sessions__archived">
          <button
            :aria-expanded="archivedOpen"
            class="agentic-sessions__archived-toggle"
            type="button"
            @click="archivedOpen = !archivedOpen"
          >
            <el-icon>
              <Expand v-if="archivedOpen"/>
              <Fold v-else/>
            </el-icon>
            <span>{{ t('agentic.archivedSection') }} ({{ filteredArchived.length }})</span>
          </button>
          <template v-if="archivedOpen">
            <div
              v-for="session in filteredArchived"
              :key="session.conversationId"
              :class="{'is-active': session.conversationId === activeId}"
              class="agentic-sessions__item"
            >
              <button
                :disabled="Boolean(sessionActionLoading[session.conversationId])"
                :title="session.title"
                class="agentic-sessions__item-main"
                type="button"
                @click="emit('select', session.conversationId)"
              >
                <el-icon class="agentic-sessions__item-icon">
                  <ChatDotRound/>
                </el-icon>
                <span class="agentic-sessions__item-text">
                  <strong>{{ session.title }}</strong>
                  <small v-if="session.summary">{{ session.summary }}</small>
                </span>
              </button>
              <el-dropdown
                :disabled="disabled || Boolean(sessionActionLoading[session.conversationId])"
                popper-class="agentic-item-menu"
                trigger="click"
                @command="(command) => handleItemCommand(command, session)"
              >
                <button
                  :aria-label="t('agentic.itemActions')"
                  class="agentic-sessions__item-more"
                  type="button"
                  @click.stop
                >
                  <el-icon>
                    <MoreFilled/>
                  </el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="unarchive">
                      <el-icon><FolderOpened/></el-icon>
                      {{ t('agentic.unarchive') }}
                    </el-dropdown-item>
                    <el-dropdown-item command="delete" divided>
                      <el-icon><Delete/></el-icon>
                      {{ t('agentic.headerDelete') }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </div>
      </div>
    </el-scrollbar>

    <!-- Footer: the collapse toggle only — search/new live in the top row. -->
    <div class="agentic-sessions__footer">
      <el-tooltip :content="collapsed ? t('agentic.railExpand') : t('agentic.railCollapse')" placement="right">
        <el-button
          :aria-label="collapsed ? t('agentic.railExpand') : t('agentic.railCollapse')"
          circle
          class="agentic-tool agentic-sessions__collapse"
          size="small"
          @click="emit('toggle-collapse')"
        >
          <el-icon>
            <Expand v-if="collapsed"/>
            <Fold v-else/>
          </el-icon>
        </el-button>
      </el-tooltip>
    </div>

    <!-- Rename dialog targets whichever item opened it (not only the active one). -->
    <el-dialog
      v-model="renameDialogVisible"
      :title="t('agentic.headerRename')"
      append-to-body
      width="360px"
    >
      <el-input
        v-model="renameDraft"
        :maxlength="80"
        :placeholder="t('agentic.dialogConversationTitle')"
        clearable
        @keydown.enter.prevent="confirmRename"
      />
      <template #footer>
        <el-button size="small" @click="renameDialogVisible = false">{{ t('agentic.dialogCancel') }}</el-button>
        <el-button
          :disabled="!renameDraft.trim() || disabled"
          size="small"
          type="primary"
          @click="confirmRename"
        >
          {{ t('agentic.dialogSave') }}
        </el-button>
      </template>
    </el-dialog>
  </nav>
</template>

<script lang="ts" setup>
import {Box, ChatDotRound, Delete, EditPen, Expand, Fold, FolderOpened, MoreFilled, Plus, Search} from '@element-plus/icons-vue';
import {computed, nextTick, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import type {AgenticSession} from '@/config/types';

const props = defineProps<{
  sessions: Array<AgenticSession & {title: string}>;
  activeId: string;
  sessionActionLoading: Record<string, boolean>;
  disabled: boolean;
  collapsed: boolean;
  /** The store caps the list at 50; surface that instead of silently hiding. */
  truncated?: boolean;
}>();
const emit = defineEmits<{
  select: [conversationId: string];
  new: [];
  delete: [conversationId: string];
  rename: [conversationId: string, title: string];
  /** Toggle the archive mark of a session (true = archive, false = restore). */
  archive: [conversationId: string, archived: boolean];
  'toggle-collapse': [];
}>();

const {t} = useI18n();
const archivedOpen = ref(false);
const searchQuery = ref('');
const searchInputRef = ref<{focus?: () => void}>();

const isArchived = (session: AgenticSession) => Boolean(session.sessionExt?.archived);
const matchesSearch = (session: AgenticSession & {title: string}) => {
  const needle = searchQuery.value.trim().toLowerCase();
  if (!needle) return true;
  return `${session.title} ${session.summary || ''}`.toLowerCase().includes(needle);
};
const filteredSessions = computed(() => props.sessions.filter((session) => !isArchived(session) && matchesSearch(session)));
const filteredArchived = computed(() => props.sessions.filter((session) => isArchived(session) && matchesSearch(session)));

const focusSearch = () => {
  if (props.collapsed) emit('toggle-collapse');
  void nextTick(() => searchInputRef.value?.focus?.());
};

const renameDialogVisible = ref(false);
const renameTargetId = ref('');
const renameDraft = ref('');

const handleItemCommand = (command: string, session: AgenticSession & {title: string}) => {
  if (command === 'rename') {
    renameTargetId.value = session.conversationId;
    renameDraft.value = session.title;
    renameDialogVisible.value = true;
    return;
  }
  if (command === 'archive') return emit('archive', session.conversationId, true);
  if (command === 'unarchive') return emit('archive', session.conversationId, false);
  if (command === 'delete') return emit('delete', session.conversationId);
};

const confirmRename = () => {
  const title = renameDraft.value.trim();
  if (!title || !renameTargetId.value || props.disabled) return;
  renameDialogVisible.value = false;
  emit('rename', renameTargetId.value, title);
};
</script>

<style lang="scss" scoped>
.agentic-sessions {
  display: flex;
  flex-direction: column;
  flex: 0 0 264px;
  min-width: 0;
  border-right: 1px solid var(--dc3-border-base);
  background: var(--dc3-bg-canvas);

  &__top {
    display: flex;
    align-items: center;
    gap: var(--dc3-space-2);
    padding: var(--dc3-space-3) var(--dc3-space-3) var(--dc3-space-1);

    // Element Plus adds margin-left between sibling buttons — in a vertical
    // stack that shifts every button off the rail's center axis.
    :deep(.el-button) {
      margin-left: 0;
    }
  }

  &__search {
    flex: 1;
    min-width: 0;
  }

  &__list {
    flex: 1;
    min-height: 0;
  }

  &__view {
    display: flex;
    flex-direction: column;
    gap: var(--dc3-space-2);
    padding: var(--dc3-space-2);
  }

  &__item {
    display: flex;
    align-items: center;
    min-width: 0;
    border-radius: var(--dc3-radius-lg);

    &:hover,
    &.is-active {
      background: var(--dc3-bg-interactive);
    }

    &.is-active {
      box-shadow: inset 0 0 0 1px var(--dc3-border-strong);
    }
  }

  &__item-main {
    display: flex;
    flex: 1;
    align-items: center;
    gap: var(--dc3-space-2);
    min-width: 0;
    padding: var(--dc3-space-2) var(--dc3-space-3);
    border: 0;
    border-radius: var(--dc3-radius-lg);
    background: transparent;
    color: var(--dc3-text-regular);
    cursor: pointer;
    text-align: left;

    &:hover {
      color: var(--el-color-primary);
    }
  }

  // ⋯ stays out of the way until the row is hovered / focused.
  &__item-more {
    display: inline-flex;
    flex: 0 0 28px;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    margin-right: 4px;
    padding: 0;
    border: 0;
    border-radius: var(--dc3-radius-sm);
    color: var(--dc3-text-muted);
    background: transparent;
    cursor: pointer;
    opacity: 0;
    transition: opacity var(--dc3-duration-fast) var(--dc3-ease-standard);

    &:hover {
      background: var(--dc3-bg-interactive-active);
      color: var(--dc3-text-brand);
    }
  }

  &__item:hover .agentic-sessions__item-more,
  &__item:focus-within .agentic-sessions__item-more,
  .agentic-sessions__item-more:focus-visible {
    opacity: 1;
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

  &__hint {
    padding: var(--dc3-space-2) var(--dc3-space-3);
    color: var(--dc3-text-muted);
    font-size: 11px;
  }

  &__archived {
    display: flex;
    flex-direction: column;
    gap: var(--dc3-space-1);
    margin-top: var(--dc3-space-2);
    padding-top: var(--dc3-space-2);
    border-top: 1px solid var(--dc3-border-base);
  }

  &__archived-toggle {
    display: flex;
    align-items: center;
    gap: var(--dc3-space-1);
    width: 100%;
    padding: var(--dc3-space-1) var(--dc3-space-3);
    border: 0;
    border-radius: var(--dc3-radius-md);
    background: transparent;
    color: var(--dc3-text-muted);
    font-size: 11px;
    font-weight: 600;
    cursor: pointer;
    text-align: left;

    &:hover {
      background: var(--dc3-bg-interactive);
      color: var(--dc3-text-brand);
    }
  }

  // Footer: the collapse toggle, centred on the same axis as the top row.
  &__footer {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--dc3-space-2);
    padding: var(--dc3-space-2) var(--dc3-space-3) var(--dc3-space-3);
    border-top: 1px solid var(--dc3-border-base);

    :deep(.el-button) {
      margin-left: 0;
    }
  }
}

.agentic-sessions.is-collapsed {
  flex: 0 0 64px;
  width: 64px;

  .agentic-sessions__view {
    padding: var(--dc3-space-2) var(--dc3-space-1);
  }

  .agentic-sessions__item-main {
    justify-content: center;
    min-height: var(--dc3-touch-target);
    padding-right: var(--dc3-space-2);
    padding-left: var(--dc3-space-2);
  }

  // Icon rail only — text rows and the archive section need full width.
  .agentic-sessions__item-text,
  .agentic-sessions__item-more,
  .agentic-sessions__hint,
  .agentic-sessions__empty,
  .agentic-sessions__archived {
    display: none;
  }

  .agentic-sessions__top {
    flex-direction: column;
    gap: var(--dc3-space-2);
    padding: var(--dc3-space-2) 0 var(--dc3-space-1);
  }

  .agentic-sessions__footer {
    gap: var(--dc3-space-2);
    padding-right: 0;
    padding-bottom: var(--dc3-space-3);
    padding-left: 0;
    border-top: 0;
    flex-direction: column;
  }
}

// Neutral tool chip shared with the header action buttons — in the rail
// every control is the same 32px disc so the stack reads as one axis.
.agentic-tool {
  width: 32px;
  height: 32px;
  padding: 0;
  border: 1px solid var(--dc3-border-base);
  background: var(--dc3-bg-interactive);
  color: var(--dc3-text-regular);

  .el-icon {
    font-size: 16px;
  }

  &:hover,
  &:focus-visible {
    background: var(--dc3-bg-interactive-active);
    color: var(--dc3-text-brand);
  }
}
</style>
