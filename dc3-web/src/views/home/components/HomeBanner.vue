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
    const res: any = await systemHealth();
    const data = res?.data;
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
  background: linear-gradient(135deg, #1296db 0%, #6a86ff 55%, #9059f6 100%);
  color: #ffffff;

  :deep(.el-card__body) {
    padding: 16px 24px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 24px;
    min-height: 94px;
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
    padding: 0 14px;
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
    font-size: 12px;
  }

  .home-banner__group {
    display: flex;
    align-items: center;
    gap: 8px;
    flex-wrap: wrap;
  }

  .home-banner__group-label {
    width: 48px;
    opacity: 0.7;
    flex-shrink: 0;
  }

  .home-banner__service {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    opacity: 0.92;
    min-width: 64px;
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

    &--partial {
      background: #e6a23c;
      box-shadow: 0 0 6px rgba(230, 162, 60, 0.6);
    }
  }
}
</style>
