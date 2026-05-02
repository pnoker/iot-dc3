<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <el-card class="home-banner" shadow="never">
    <div class="home-banner__left">
      <div class="home-banner__greeting">
        {{ greeting }}<span v-if="username" class="home-banner__user">,&nbsp;{{ username }}</span>
      </div>
      <div class="home-banner__sub">{{ $t('home.banner.welcome') }}</div>
    </div>

    <div class="home-banner__middle">
      <div class="home-banner__time">{{ now }}</div>
      <div class="home-banner__date">{{ date }}</div>
    </div>

    <div class="home-banner__right">
      <div v-for="s in services" :key="s.key" class="home-banner__service">
        <span :class="['home-banner__dot', `home-banner__dot--${s.status}`]"></span>
        <span class="home-banner__service-label">{{ s.label }}</span>
      </div>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
  import { computed, onMounted, onUnmounted, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { useAuthStore } from '@/store/modules/auth';

  const props = defineProps({
    status: {
      type: Object as () => { auth: boolean; data: boolean; manager: boolean },
      default: () => ({ auth: true, data: true, manager: true }),
    },
  });

  const { t } = useI18n();
  const authStore = useAuthStore();

  const now = ref('');
  const date = ref('');
  const username = computed(() => authStore.getName || authStore.name || '');

  const greeting = computed(() => {
    const h = new Date().getHours();
    if (h < 6) return t('home.banner.greetingNight');
    if (h < 12) return t('home.banner.greetingMorning');
    if (h < 18) return t('home.banner.greetingAfternoon');
    return t('home.banner.greetingEvening');
  });

  const services = computed(() => [
    {
      key: 'auth',
      label: t('home.banner.serviceAuth'),
      status: props.status.auth ? 'up' : 'down',
    },
    {
      key: 'data',
      label: t('home.banner.serviceData'),
      status: props.status.data ? 'up' : 'down',
    },
    {
      key: 'manager',
      label: t('home.banner.serviceManager'),
      status: props.status.manager ? 'up' : 'down',
    },
  ]);

  const tick = () => {
    const d = new Date();
    now.value = d.toLocaleTimeString('zh-CN', { hour12: false });
    date.value = d.toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' });
  };

  let timer: ReturnType<typeof setInterval> | null = null;
  onMounted(() => {
    tick();
    timer = setInterval(tick, 1000);
  });
  onUnmounted(() => {
    if (timer) clearInterval(timer);
  });
</script>

<style lang="scss" scoped>
  .home-banner {
    border-radius: 10px;
    background: linear-gradient(135deg, #409eff 0%, #6a86ff 55%, #9059f6 100%);
    color: #ffffff;

    :deep(.el-card__body) {
      padding: 16px 24px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 24px;
      min-height: 86px;
    }

    .home-banner__left {
      flex: 1;
      min-width: 0;
    }

    .home-banner__greeting {
      font-size: 20px;
      font-weight: 600;
      line-height: 1.2;
    }

    .home-banner__user {
      font-weight: 500;
    }

    .home-banner__sub {
      font-size: 13px;
      opacity: 0.86;
      margin-top: 4px;
    }

    .home-banner__middle {
      text-align: center;
      padding: 0 12px;
      border-left: 1px solid rgba(255, 255, 255, 0.25);
      border-right: 1px solid rgba(255, 255, 255, 0.25);

      .home-banner__time {
        font-family: 'Menlo', monospace;
        font-size: 24px;
        font-weight: 600;
        line-height: 1.1;
      }

      .home-banner__date {
        font-size: 12px;
        opacity: 0.86;
        margin-top: 2px;
      }
    }

    .home-banner__right {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .home-banner__service {
      display: flex;
      align-items: center;
      font-size: 12px;
      gap: 6px;
    }

    .home-banner__dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      display: inline-block;
      flex-shrink: 0;

      &--up {
        background: #67c23a;
        box-shadow: 0 0 6px rgba(103, 194, 58, 0.6);
      }

      &--down {
        background: #f56c6c;
        box-shadow: 0 0 6px rgba(245, 108, 108, 0.6);
      }
    }

    .home-banner__service-label {
      opacity: 0.92;
    }
  }
</style>
