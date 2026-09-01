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
      <div class="home-banner__group">
        <span class="home-banner__group-label">{{ $t('home.banner.group.center') }}</span>
        <span v-for="s in centerServices" :key="s.key" :title="s.label" class="home-banner__service">
          <span :class="['home-banner__dot', `home-banner__dot--${s.status}`]"></span>
          <span>{{ s.label }}</span>
        </span>
      </div>
      <div class="home-banner__group">
        <span class="home-banner__group-label">{{ $t('home.banner.group.infra') }}</span>
        <span v-for="s in infraServices" :key="s.key" :title="s.label" class="home-banner__service">
          <span :class="['home-banner__dot', `home-banner__dot--${s.status}`]"></span>
          <span>{{ s.label }}</span>
        </span>
      </div>
      <div class="home-banner__group">
        <span class="home-banner__group-label">{{ $t('home.banner.group.drivers') }}</span>
        <span class="home-banner__service">
          <span :class="['home-banner__dot', `home-banner__dot--${fleetDotTone(drivers)}`]"></span>
          <span>{{ drivers.online }} / {{ drivers.total }}</span>
        </span>
      </div>
      <div class="home-banner__group">
        <span class="home-banner__group-label">{{ $t('home.banner.group.devices') }}</span>
        <span class="home-banner__service">
          <span :class="['home-banner__dot', `home-banner__dot--${fleetDotTone(devices)}`]"></span>
          <span>{{ devices.online }} / {{ devices.total }}</span>
        </span>
      </div>
    </div>
  </el-card>
</template>

<script lang="ts" setup>
import {computed, onMounted, onUnmounted, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {useAuthStore} from '@/store/modules/auth';
import {systemHealth} from '@/api/dashboard';
import type {SystemHealth} from '@/config/types/dashboard';

interface ServiceRow {
  key: string;
  label: string;
  status: 'up' | 'down';
}

const {t, locale} = useI18n();
const authStore = useAuthStore();

// Map vue-i18n locale strings to browser BCP-47 tags; fall back to the
// browser default if the app is in an unexpected locale.
const bcp47 = () => (locale.value === 'zh' ? 'zh-CN' : locale.value === 'en' ? 'en-US' : undefined);

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

// Defaults to all-up so the banner doesn't flash "down" during the first
// request. If /system/health fails we leave everything up (probably the
// browser's offline — the user can tell from the broken requests below).
const center = ref<Record<string, string>>({auth: 'up', data: 'up', manager: 'up'});
const infra = ref<Record<string, string>>({database: 'up', mq: 'up', gateway: 'up'});
const drivers = ref<{ total: number; online: number }>({total: 0, online: 0});
const devices = ref<{ total: number; online: number }>({total: 0, online: 0});

const fleetDotTone = (f: { total: number; online: number }): 'up' | 'partial' | 'down' => {
  if (f.total > 0 && f.online === f.total) return 'up';
  if (f.online > 0) return 'partial';
  return 'down';
};

const buildRow = (key: string, labelKey: string, value: string | undefined): ServiceRow => ({
  key,
  label: t(labelKey),
  status: value === 'up' ? 'up' : 'down',
});

const centerServices = computed<ServiceRow[]>(() => [
  buildRow('auth', 'home.banner.serviceAuth', center.value.auth),
  buildRow('data', 'home.banner.serviceData', center.value.data),
  buildRow('manager', 'home.banner.serviceManager', center.value.manager),
]);

const infraServices = computed<ServiceRow[]>(() => [
  buildRow('database', 'home.banner.serviceDatabase', infra.value.database),
  buildRow('mq', 'home.banner.serviceMq', infra.value.mq),
  buildRow('gateway', 'home.banner.serviceGateway', infra.value.gateway),
]);

const tick = () => {
  const d = new Date();
  const tag = bcp47();
  now.value = d.toLocaleTimeString(tag, {hour12: false});
  date.value = d.toLocaleDateString(tag, {year: 'numeric', month: 'long', day: 'numeric', weekday: 'long'});
};

const refreshHealth = async () => {
  try {
    const data: SystemHealth = await systemHealth();
    if (!data) return;
    if (data.center) center.value = data.center;
    if (data.infra) infra.value = data.infra;
    if (data.drivers) drivers.value = {total: data.drivers.total ?? 0, online: data.drivers.online ?? 0};
    if (data.devices) devices.value = {total: data.devices.total ?? 0, online: data.devices.online ?? 0};
  } catch {
    // handled globally
  }
};

let clockTimer: ReturnType<typeof setInterval> | null = null;
let healthTimer: ReturnType<typeof setInterval> | null = null;
onMounted(() => {
  tick();
  clockTimer = setInterval(tick, 1000);
  refreshHealth();
  // Poll every 30s so the banner reflects dependency failures without a reload.
  healthTimer = setInterval(refreshHealth, 30_000);
});
onUnmounted(() => {
  if (clockTimer) clearInterval(clockTimer);
  if (healthTimer) clearInterval(healthTimer);
});
</script>

<style lang="scss" scoped>
.home-banner {
  position: relative;
  isolation: isolate;
  overflow: hidden;
  border-color: var(--dc3-border-strong);
  background:
    radial-gradient(circle at 78% -80%, var(--dc3-ambient-secondary), transparent 46%),
    radial-gradient(circle at 10% 120%, var(--dc3-ambient-primary), transparent 42%),
    var(--dc3-bg-elevated);
  color: var(--dc3-text-primary);
  box-shadow: var(--dc3-shadow-md);

  &::before {
    position: absolute;
    z-index: -1;
    top: -100px;
    left: 22%;
    width: 360px;
    height: 220px;
    border: 1px solid var(--dc3-border-base);
    border-radius: 50%;
    content: '';
    transform: rotate(-12deg);
  }

  :deep(.el-card__body) {
    display: grid;
    grid-template-columns: minmax(210px, 1fr) auto minmax(420px, 1.55fr);
    align-items: center;
    gap: clamp(16px, 2vw, 28px);
    min-height: 108px;
    padding: var(--dc3-space-4) clamp(18px, 2vw, 28px);
  }

  .home-banner__left {
    flex: 1;
    min-width: 0;
  }

  .home-banner__greeting {
    background: var(--dc3-brand-gradient);
    background-clip: text;
    color: transparent;
    font-size: clamp(19px, 1.5vw, 24px);
    font-weight: 720;
    letter-spacing: -0.02em;
    line-height: 1.25;
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .home-banner__user {
    font-weight: 620;
  }

  .home-banner__sub {
    font-size: 13px;
    color: var(--dc3-text-muted);
    margin-top: 4px;
    line-height: 1.5;
  }

  .home-banner__middle {
    text-align: center;
    padding: 10px var(--dc3-space-5);
    border: 1px solid var(--dc3-border-base);
    border-radius: var(--dc3-radius-lg);
    background: var(--dc3-bg-muted);

    .home-banner__time {
      font-family: 'Menlo', monospace;
      font-size: 24px;
      font-weight: 680;
      color: var(--dc3-text-primary);
      letter-spacing: -0.03em;
      line-height: 1.1;
      white-space: nowrap;
    }

    .home-banner__date {
      font-size: 12px;
      color: var(--dc3-text-muted);
      margin-top: 2px;
      white-space: nowrap;
    }
  }

  .home-banner__right {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--dc3-space-2);
    min-width: 0;
    font-size: 12px;
  }

  .home-banner__group {
    display: flex;
    align-items: center;
    gap: var(--dc3-space-2);
    flex-wrap: wrap;
    min-width: 0;
    padding: 7px 9px;
    border: 1px solid var(--dc3-border-base);
    border-radius: var(--dc3-radius-md);
    background: color-mix(in srgb, var(--dc3-bg-elevated-strong) 62%, transparent);
  }

  .home-banner__group-label {
    width: 48px;
    color: var(--dc3-text-muted);
    flex-shrink: 0;
    font-weight: 600;
  }

  .home-banner__service {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    color: var(--dc3-text-regular);
    min-width: 64px;
  }

  .home-banner__dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    display: inline-block;
    flex-shrink: 0;

    &--up {
      background: var(--el-color-success);
      box-shadow: 0 0 6px color-mix(in srgb, var(--el-color-success) 60%, transparent);
    }

    &--down {
      background: var(--el-color-danger);
      box-shadow: 0 0 6px color-mix(in srgb, var(--el-color-danger) 60%, transparent);
    }

    &--partial {
      background: var(--el-color-warning);
      box-shadow: 0 0 6px color-mix(in srgb, var(--el-color-warning) 60%, transparent);
    }
  }

  @media (max-width: $breakpoint-md-max) {
    :deep(.el-card__body) {
      grid-template-columns: minmax(180px, 1fr) auto;
    }

    .home-banner__right {
      grid-column: 1 / -1;
      grid-template-columns: repeat(4, minmax(0, 1fr));
    }

    .home-banner__group {
      align-content: flex-start;
    }
  }

  @media (max-width: $breakpoint-sm-max) {
    .home-banner__right {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: $breakpoint-xs-max) {
    :deep(.el-card__body) {
      grid-template-columns: minmax(0, 1fr);
      gap: var(--dc3-space-3);
      padding: var(--dc3-space-4);
    }

    .home-banner__greeting {
      font-size: 19px;
      overflow-wrap: break-word;
    }

    .home-banner__middle {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--dc3-space-3);
      padding: 9px var(--dc3-space-3);
      text-align: left;
    }

    .home-banner__right {
      grid-column: auto;
      grid-template-columns: 1fr;
    }

    .home-banner__group {
      flex-wrap: nowrap;
      overflow-x: auto;
    }
  }
}
</style>
