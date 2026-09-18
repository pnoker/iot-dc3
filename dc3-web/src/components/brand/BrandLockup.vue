<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -->

<template>
  <span :aria-label="t('layout.brandDescription')" :class="{'brand-lockup--compact': compact}" class="brand-lockup">
    <img :src="assetUrl('images/logo/logo.svg')" alt="" class="brand-lockup__logo" />
    <span class="brand-lockup__copy">
      <span class="brand-lockup__title">
        <strong>IoT DC3</strong>
        <svg aria-hidden="true" class="brand-lockup__spark" focusable="false" viewBox="0 0 12 12">
          <path d="M6 .8c.35 3.05 2.15 4.85 5.2 5.2C8.15 6.35 6.35 8.15 6 11.2 5.65 8.15 3.85 6.35.8 6 3.85 5.65 5.65 3.85 6 .8Z" />
        </svg>
        <span class="brand-lockup__agentic">AGENTIC</span>
      </span>
      <span class="brand-lockup__signal">{{ t('layout.brandSignal') }}</span>
    </span>
  </span>
</template>

<script lang="ts" setup>
import {useI18n} from 'vue-i18n';

import {assetUrl} from '@/utils/assetUrl';

withDefaults(defineProps<{compact?: boolean}>(), {compact: false});

const {t} = useI18n();
</script>

<style lang="scss" scoped>
.brand-lockup {
  display: inline-flex;
  min-width: 0;
  align-items: center;
  font-family: "Avenir", Helvetica, Arial, sans-serif;
}

.brand-lockup__logo {
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  filter: drop-shadow(0 4px 7px rgba(13, 82, 157, 0.2));
}

.brand-lockup__copy {
  display: flex;
  min-width: 0;
  margin-left: 8px;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
}

.brand-lockup__title {
  display: flex;
  min-width: 0;
  align-items: baseline;
  gap: 4px;
  font-size: 15px;
  line-height: 1;
  white-space: nowrap;

  strong {
    background: linear-gradient(105deg, #5656c8, #009fc9);
    background-clip: text;
    color: transparent;
    font-weight: 740;
    letter-spacing: -0.01em;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.brand-lockup__spark {
  display: block;
  width: 10px;
  height: 10px;
  overflow: visible;
  flex: none;
  align-self: center;
  fill: #5656c8;
  filter: drop-shadow(0 0 3px rgba(0, 159, 201, 0.42));
}

.brand-lockup__agentic {
  flex: none;
  align-self: center;
  color: #5656c8;
  font-size: 9px;
  font-weight: 680;
  letter-spacing: 0.04em;
  line-height: 1;
}

.brand-lockup__signal {
  min-width: 0;
  overflow: hidden;
  color: var(--dc3-text-muted);
  font-size: 9px;
  font-weight: 520;
  letter-spacing: 0.02em;
  line-height: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.brand-lockup--compact .brand-lockup__copy {
  display: none;
}
</style>
