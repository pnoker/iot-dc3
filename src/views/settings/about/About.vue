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
  <div class="about">
    <el-card shadow="never" class="about__card">
      <template #header>
        <span class="about__title">{{ t('settings.about.platformTitle') }}</span>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('settings.about.name')">IoT DC3</el-descriptions-item>
        <el-descriptions-item :label="t('settings.about.version')">v{{ version }}</el-descriptions-item>
        <el-descriptions-item :label="t('settings.about.license')">AGPL-3.0</el-descriptions-item>
        <el-descriptions-item :label="t('settings.about.docs')">
          <el-link type="primary" href="https://iotdc3.com" target="_blank">iotdc3.com</el-link>
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.about.repo')">
          <el-link type="primary" href="https://github.com/pnoker/iot-dc3" target="_blank">
            github.com/pnoker/iot-dc3
          </el-link>
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.about.copyright')">
          © 2016-present the IoT DC3 original author or authors
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="about__card">
      <template #header>
        <span class="about__title">{{ t('settings.about.introTitle') }}</span>
      </template>
      <p class="about__intro">{{ t('settings.about.intro') }}</p>
    </el-card>

    <el-card shadow="never" class="about__card">
      <template #header>
        <span class="about__title">{{ t('settings.about.architectureTitle') }}</span>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="Driver Layer">
          {{ t('settings.about.architecture.driver') }}
        </el-descriptions-item>
        <el-descriptions-item label="Data Layer">
          {{ t('settings.about.architecture.data') }}
        </el-descriptions-item>
        <el-descriptions-item label="Management Layer">
          {{ t('settings.about.architecture.management') }}
        </el-descriptions-item>
        <el-descriptions-item label="Application Layer">
          {{ t('settings.about.architecture.application') }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card shadow="never" class="about__card">
      <template #header>
        <span class="about__title">{{ t('settings.about.objectivesTitle') }}</span>
      </template>
      <ul class="about__objectives">
        <li v-for="key in objectiveKeys" :key="key">{{ t(`settings.about.objectives.${key}`) }}</li>
      </ul>
    </el-card>

    <el-card shadow="never" class="about__card">
      <template #header>
        <span class="about__title">{{ t('settings.about.licenseTitle') }}</span>
      </template>
      <p class="about__intro">{{ t('settings.about.licenseDetail') }}</p>
      <el-link
        class="about__license-link"
        type="primary"
        href="https://www.gnu.org/licenses/agpl-3.0.html"
        target="_blank"
      >
        AGPL-3.0 Full Text
      </el-link>
    </el-card>

    <el-card shadow="never" class="about__card">
      <template #header>
        <div class="about__header">
          <span class="about__title">{{ t('settings.about.healthTitle') }}</span>
          <el-button :icon="Refresh" :loading="loading" circle size="small" @click="refresh" />
        </div>
      </template>
      <div v-loading="loading" class="about__health">
        <div class="about__health-group">
          <div class="about__health-label">{{ t('home.banner.group.center') }}</div>
          <div class="about__health-items">
            <span v-for="row in centerRows" :key="row.key" class="about__health-item">
              <span :class="['about__dot', `about__dot--${row.status}`]"></span>
              <span>{{ row.label }}</span>
              <el-tag :type="row.status === 'up' ? 'success' : 'danger'" size="small">
                {{ row.status === 'up' ? t('status.online') : t('status.offline') }}
              </el-tag>
            </span>
          </div>
        </div>
        <div class="about__health-group">
          <div class="about__health-label">{{ t('home.banner.group.infra') }}</div>
          <div class="about__health-items">
            <span v-for="row in infraRows" :key="row.key" class="about__health-item">
              <span :class="['about__dot', `about__dot--${row.status}`]"></span>
              <span>{{ row.label }}</span>
              <el-tag :type="row.status === 'up' ? 'success' : 'danger'" size="small">
                {{ row.status === 'up' ? t('status.online') : t('status.offline') }}
              </el-tag>
            </span>
          </div>
        </div>
        <div class="about__health-group">
          <div class="about__health-label">{{ t('home.banner.group.drivers') }}</div>
          <div class="about__health-items">
            <span class="about__health-item">
              <span :class="['about__dot', `about__dot--${fleetDotTone(drivers)}`]"></span>
              <span>{{ t('home.banner.group.drivers') }}</span>
              <el-tag size="small">{{ drivers.online }} / {{ drivers.total }}</el-tag>
            </span>
          </div>
        </div>
        <div class="about__health-group">
          <div class="about__health-label">{{ t('home.banner.group.devices') }}</div>
          <div class="about__health-items">
            <span class="about__health-item">
              <span :class="['about__dot', `about__dot--${fleetDotTone(devices)}`]"></span>
              <span>{{ t('home.banner.group.devices') }}</span>
              <el-tag size="small">{{ devices.online }} / {{ devices.total }}</el-tag>
            </span>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { Refresh } from '@element-plus/icons-vue';

  import { systemHealth } from '@/api/dashboard';
  import pkg from '../../../../package.json';

  interface HealthRow {
    key: string;
    label: string;
    status: 'up' | 'down';
  }

  const { t } = useI18n();
  const version = pkg.version;

  const objectiveKeys = [
    'scalability',
    'resilience',
    'performance',
    'extensibility',
    'deployment',
    'security',
    'cloudNative',
    'aiReady',
  ];

  const loading = ref(false);
  const center = ref<Record<string, string>>({});
  const infra = ref<Record<string, string>>({});
  const drivers = ref<{ total: number; online: number }>({ total: 0, online: 0 });
  const devices = ref<{ total: number; online: number }>({ total: 0, online: 0 });

  const fleetDotTone = (f: { total: number; online: number }): 'up' | 'partial' | 'down' => {
    if (f.total > 0 && f.online === f.total) return 'up';
    if (f.online > 0) return 'partial';
    return 'down';
  };

  const toRow = (key: string, labelKey: string, status: string | undefined): HealthRow => ({
    key,
    label: t(labelKey),
    status: status === 'up' ? 'up' : 'down',
  });

  const centerRows = computed<HealthRow[]>(() => [
    toRow('auth', 'home.banner.serviceAuth', center.value.auth),
    toRow('data', 'home.banner.serviceData', center.value.data),
    toRow('manager', 'home.banner.serviceManager', center.value.manager),
  ]);

  const infraRows = computed<HealthRow[]>(() => [
    toRow('database', 'home.banner.serviceDatabase', infra.value.database),
    toRow('mq', 'home.banner.serviceMq', infra.value.mq),
    toRow('gateway', 'home.banner.serviceGateway', infra.value.gateway),
  ]);

  const refresh = async () => {
    loading.value = true;
    try {
      const res: any = await systemHealth();
      const data = res?.data;
      if (data) {
        center.value = data.center || {};
        infra.value = data.infra || {};
        drivers.value = { total: data.drivers?.total ?? 0, online: data.drivers?.online ?? 0 };
        devices.value = { total: data.devices?.total ?? 0, online: data.devices?.online ?? 0 };
      }
    } catch {
      // handled globally
    } finally {
      loading.value = false;
    }
  };

  onMounted(refresh);
</script>

<style lang="scss" scoped>
  .about {
    display: flex;
    flex-direction: column;
    gap: 12px;

    .about__card {
      border-radius: 10px;
    }

    .about__header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .about__title {
      font-weight: 600;
      color: #303133;
    }

    .about__intro {
      font-size: 14px;
      color: #606266;
      line-height: 1.8;
      margin: 0;
    }

    .about__objectives {
      margin: 0;
      padding-left: 20px;

      li {
        font-size: 14px;
        color: #606266;
        line-height: 2;
      }
    }

    .about__license-link {
      margin-top: 8px;
      display: inline-block;
    }

    .about__health {
      display: flex;
      flex-direction: column;
      gap: 14px;
    }

    .about__health-group {
      display: flex;
      align-items: flex-start;
      gap: 12px;
    }

    .about__health-label {
      width: 96px;
      color: #909399;
      flex-shrink: 0;
      padding-top: 2px;
    }

    .about__health-items {
      display: flex;
      flex-wrap: wrap;
      gap: 14px 18px;
    }

    .about__health-item {
      display: inline-flex;
      align-items: center;
      gap: 6px;
      min-width: 140px;
    }

    .about__dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      display: inline-block;

      &--up {
        background: #67c23a;
      }

      &--down {
        background: #f56c6c;
      }

      &--partial {
        background: #e6a23c;
      }
    }
  }
</style>
