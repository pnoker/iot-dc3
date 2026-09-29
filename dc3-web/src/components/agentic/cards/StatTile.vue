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

<!-- One label/value tile shared by the run-overview grid, the token grid and
     the chart stat grid (three hand-rolled lookalikes before this). -->
<template>
  <div :class="{'chat-stat--row': layout === 'row'}" class="chat-stat">
    <span>{{ label }}</span>
    <strong>{{ value }}</strong>
  </div>
</template>

<script lang="ts" setup>
withDefaults(
  defineProps<{
    label: string;
    value: string | number;
    /** stacked (label above value) or row (label left, value right). */
    layout?: 'stack' | 'row';
  }>(),
  {layout: 'stack'}
);
</script>

<style lang="scss" scoped>
.chat-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  padding: 6px 8px;
  border: 1px solid var(--dc3-border-base);
  border-radius: var(--dc3-radius-md);
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
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

.chat-stat--row {
  flex-direction: row;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
</style>
