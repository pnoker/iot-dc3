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
  </div>
</template>

<script lang="ts" setup>
  import { useI18n } from 'vue-i18n';

  import pkg from '../../../../package.json';

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
</script>

<style lang="scss" scoped>
  .about {
    display: flex;
    flex-direction: column;
    // Stay in sync with Home / Event Overview — 8px rhythm under the
    // 4px el-card radius, plus a 4px right pad to balance the aside
    // gap on the left (About is a board-style info page like Overview,
    // not a form/table like the other settings sub-pages).
    gap: 8px;
    padding-right: 4px;

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
  }
</style>
