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

<!--
  - Unified slot-wrapper card. Replaces the former BaseCard / TitleCard /
  - DetailCard / BlankCard quartet, which all wrapped an <el-card> around a
  - slotted container with near-identical scoped styles. The four variants now
  - live as thin compatibility wrappers (BaseCard.vue et al.) that pin the
  - props below so each consumer's rendering is preserved bit-for-bit.
  -
  - Variants:
  -   borderless        border:0 on the card body          (Base, Blank)
  -   shadow="never"    no hover/always shadow             (Base)
  -   title / #header   optional in-container header strip (Title)
  -->

<template>
  <div :class="{ 'is-borderless': props.borderless }" class="card-shell">
    <el-card :shadow="props.shadow" class="card-shell__body">
      <div class="card-shell__container">
        <slot name="header">
          <span v-if="props.title" class="card-shell__header">{{ props.title }}</span>
        </slot>
        <slot/>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
const props = withDefaults(
  defineProps<{
    shadow?: 'never' | 'hover' | 'always';
    borderless?: boolean;
    title?: string;
  }>(),
  {
    shadow: 'hover',
    borderless: false,
    title: '',
  },
);
</script>

<style lang="scss" scoped>
.card-shell {
  box-sizing: border-box;
  min-width: 0;

  ul {
    list-style: none;

    li {
      font-size: 13px;
      margin-top: 8px;
    }
  }

  :deep(.el-card) {
    width: 100%;
    box-sizing: border-box;
    min-width: 0;
    border-color: var(--dc3-border-base);
  }

  &.is-borderless :deep(.el-card.card-shell__body) {
    border: 0;
  }

  :deep(.el-card__header) {
    padding: var(--dc3-space-3) var(--dc3-space-4);
  }

  :deep(.el-card__body) {
    padding: var(--dc3-space-4);
  }

  :deep(.el-tabs__nav) {
    margin: 0 5px;
  }

  .card-shell__container {
    min-width: 0;

    .card-shell__header {
      font-size: 14px;
      font-weight: 650;
      color: var(--dc3-text-primary);
    }

    &:first-child {
      padding-top: 0;
      padding-left: 0;
    }

    :deep(.el-tabs__header) {
      margin-bottom: 0;

      .el-tabs__nav-wrap:after {
        height: 1px;
      }
    }

    :deep(.el-tab-pane) {
      background: var(--dc3-bg-muted);
      border-radius: var(--dc3-radius-lg);
    }
  }
}
</style>
