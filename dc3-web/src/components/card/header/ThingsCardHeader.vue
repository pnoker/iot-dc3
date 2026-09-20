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
  <div :class="['things-card__header', `things-card__header--${tone}`, enabled ? 'header-enable' : 'header-disable']">
    <div class="things-card-header-icon">
      <!-- Icon accepts an Element Plus icon name (the default for every
           entity card — one line-art glyph on a tone-tinted tile, matching
           the StatCard tiles on Home) or a legacy image URL. -->
      <el-icon v-if="!isImageUrl" :size="24">
        <component :is="icon"/>
      </el-icon>
      <img v-else :alt="name" :src="icon"/>
    </div>
    <button
      :aria-label="`${name}: ${copyLabel}`"
      class="things-card-header-name nowrap-name"
      type="button"
      @click.stop="$emit('copy-id')"
    >
      {{ name }}
    </button>
    <div :title="statusTitle" class="things-card-header-status">
      <slot/>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {computed} from 'vue';

const props = defineProps({
  name: {type: String, default: ''},
  /** Element Plus icon name, or a legacy image URL (contains "/"). */
  icon: {type: String, required: true},
  /** Tile accent — same palette family as StatCard on Home. */
  tone: {
    type: String as () => 'blue' | 'green' | 'orange' | 'purple' | 'red',
    default: 'blue',
  },
  enabled: {type: Boolean, default: false},
  statusTitle: {type: String, default: ''},
  copyLabel: {type: String, required: true},
});

defineEmits(['copy-id']);

const isImageUrl = computed(() => props.icon.includes('/'));
</script>

<style lang="scss" scoped>
.things-card__header {
  --things-card-accent: var(--el-color-primary);
  --things-card-accent-soft: var(--el-color-primary-light-9);
  width: 100%;
  height: 55px;
  display: flex;
  align-items: center;

  &--blue {
    --things-card-accent: var(--el-color-primary);
    --things-card-accent-soft: var(--el-color-primary-light-9);
  }

  &--green {
    --things-card-accent: var(--el-color-success);
    --things-card-accent-soft: var(--el-color-success-light-9);
  }

  &--orange {
    --things-card-accent: var(--el-color-warning);
    --things-card-accent-soft: var(--el-color-warning-light-9);
  }

  &--purple {
    --things-card-accent: var(--dc3-color-purple);
    --things-card-accent-soft: var(--dc3-color-purple-soft);
  }

  &--red {
    --things-card-accent: var(--el-color-danger);
    --things-card-accent-soft: var(--el-color-danger-light-9);
  }

  .things-card-header-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 44px;
    height: 44px;
    margin-right: 12px;
    border: 1px solid color-mix(in srgb, var(--things-card-accent) 18%, transparent);
    border-radius: var(--dc3-radius-lg);
    background: var(--things-card-accent-soft);
    color: var(--things-card-accent);
    overflow: hidden;

    img {
      width: 100%;
      height: 100%;
    }
  }

  .things-card-header-name {
    appearance: none;
    padding: 0;
    border: 0;
    background: transparent;
    height: 48px;
    line-height: 48px;
    font-size: 14px;
    font-weight: bold;
    color: var(--el-text-color-primary);
    cursor: pointer;
    display: block;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    width: 100%;
    max-width: 200px;
    text-align: left;

    &:hover,
    &:focus-visible {
      color: var(--el-color-primary);
    }

    &:focus-visible {
      border-radius: var(--dc3-radius-sm);
      outline: none;
      box-shadow: var(--dc3-focus-ring);
    }
  }

  .things-card-header-status {
    height: 48px;
    line-height: 48px;
    text-align: right;
    flex: 1;

    :deep(.el-tag) {
      vertical-align: middle;
    }
  }
}

.header-enable {
  border-bottom: 1px solid var(--el-color-success-light-5);
}

.header-disable {
  border-bottom: 1px solid var(--el-color-danger-light-5);
}
</style>
