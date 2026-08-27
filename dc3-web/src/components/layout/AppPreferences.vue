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
  <div
    :aria-label="t('layout.preferences')"
    :class="{'app-preferences--compact': compact, 'app-preferences--surface': surface}"
    class="app-preferences"
    role="group"
  >
    <div :aria-label="t('layout.language')" class="app-preferences__language" role="group">
      <button
        :aria-pressed="locale === 'en'"
        :class="{active: locale === 'en'}"
        type="button"
        @click="setLocale('en')"
      >
        EN
      </button>
      <button
        :aria-pressed="locale === 'zh'"
        :class="{active: locale === 'zh'}"
        type="button"
        @click="setLocale('zh')"
      >
        中
      </button>
    </div>
    <span aria-hidden="true" class="app-preferences__divider" />
    <div :aria-label="t('layout.theme')" class="app-preferences__theme" role="group">
      <el-tooltip :content="t('layout.themeLight')" placement="bottom">
        <button
          :aria-label="t('layout.themeLight')"
          :aria-pressed="appStore.themeMode === 'light'"
          :class="{active: appStore.themeMode === 'light'}"
          type="button"
          @click="appStore.setTheme('light')"
        >
          <el-icon><Sunny /></el-icon>
        </button>
      </el-tooltip>
      <el-tooltip :content="t('layout.themeSystem')" placement="bottom">
        <button
          :aria-label="t('layout.themeSystem')"
          :aria-pressed="appStore.themeMode === 'auto'"
          :class="{active: appStore.themeMode === 'auto'}"
          type="button"
          @click="appStore.setTheme('auto')"
        >
          <el-icon><Monitor /></el-icon>
        </button>
      </el-tooltip>
      <el-tooltip :content="t('layout.themeDark')" placement="bottom">
        <button
          :aria-label="t('layout.themeDark')"
          :aria-pressed="appStore.themeMode === 'dark'"
          :class="{active: appStore.themeMode === 'dark'}"
          type="button"
          @click="appStore.setTheme('dark')"
        >
          <el-icon><Moon /></el-icon>
        </button>
      </el-tooltip>
    </div>
  </div>
</template>

<script lang="ts" setup>
import {Monitor, Moon, Sunny} from '@element-plus/icons-vue';
import {useI18n} from 'vue-i18n';

import {useAppStore} from '@/store';

withDefaults(
  defineProps<{
    compact?: boolean;
    surface?: boolean;
  }>(),
  {
    compact: false,
    surface: false,
  }
);

const {t, locale} = useI18n();
const appStore = useAppStore();

const setLocale = (value: 'en' | 'zh') => {
  locale.value = value;
};
</script>

<style lang="scss" scoped>
.app-preferences {
  display: flex;
  align-items: center;
  gap: 5px;
  box-sizing: border-box;
  height: 40px;
  padding: 4px 5px;
  border-radius: var(--dc3-radius-full);

  &--surface {
    border: 1px solid var(--dc3-border-strong);
    background: color-mix(in srgb, var(--dc3-bg-elevated) 88%, transparent);
    box-shadow:
      var(--dc3-shadow-sm),
      inset 0 1px 0 var(--dc3-highlight-sheen);
    backdrop-filter: blur(16px) saturate(1.2);
    -webkit-backdrop-filter: blur(16px) saturate(1.2);
  }

  &--compact {
    height: 34px;
    padding: 2px 3px;

    button {
      height: 28px;
    }

    .app-preferences__language {
      width: 62px;
    }

    .app-preferences__theme {
      width: 88px;
    }
  }
}

.app-preferences__language,
.app-preferences__theme {
  display: grid;
  gap: 2px;

  button {
    display: grid;
    place-items: center;
    min-width: 0;
    height: 30px;
    padding: 0;
    border: 0;
    border-radius: var(--dc3-radius-full);
    background: transparent;
    color: var(--dc3-text-muted);
    cursor: pointer;
    font-family: inherit;
    font-size: 11px;
    font-weight: 680;
    transition:
      color var(--dc3-duration-fast) var(--dc3-ease-standard),
      background-color var(--dc3-duration-fast) var(--dc3-ease-standard),
      box-shadow var(--dc3-duration-fast) var(--dc3-ease-standard);

    &:hover {
      background: var(--dc3-bg-interactive);
      color: var(--dc3-text-brand);
    }

    &:focus-visible {
      outline: none;
      box-shadow: var(--dc3-focus-ring);
    }

    &.active {
      background: var(--dc3-bg-elevated-strong);
      box-shadow:
        var(--dc3-shadow-active),
        inset 0 0 0 1px var(--dc3-border-base);
      color: var(--dc3-text-brand);
    }
  }
}

.app-preferences__language {
  grid-template-columns: repeat(2, 1fr);
  width: 66px;
}

.app-preferences__theme {
  grid-template-columns: repeat(3, 1fr);
  width: 94px;

  button {
    font-size: 15px;
  }
}

.app-preferences__divider {
  width: 1px;
  height: 20px;
  background: var(--dc3-border-base);
}
</style>
