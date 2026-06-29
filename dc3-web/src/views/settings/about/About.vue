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
    <!-- Platform uses BlankCard instead of an el-card-with-header like
         the others — BlankCard already enforces `border: 0`, so under
         the breadcrumb (where any border reads as extra breathing
         room) the first card sits visually flush. Title moves into
         el-descriptions' built-in title prop so we don't need a
         separate card-header slot. -->
    <blank-card>
      <el-descriptions :column="2" border>
        <el-descriptions-item :label="t('settings.about.name')">IoT DC3</el-descriptions-item>
        <el-descriptions-item :label="t('settings.about.version')">v{{ version }}</el-descriptions-item>
        <el-descriptions-item :label="t('settings.about.license')">AGPL-3.0</el-descriptions-item>
        <el-descriptions-item :label="t('settings.about.docs')">
          <external-link href="https://iotdc3.com">iotdc3.com</external-link>
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.about.repo')">
          <external-link href="https://github.com/pnoker/iot-dc3">github.com/pnoker/iot-dc3</external-link>
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.about.copyright')">
          © 2016-present the IoT DC3 original author or authors
        </el-descriptions-item>
      </el-descriptions>
    </blank-card>

    <el-card class="about__card" shadow="never">
      <template #header>
        <span class="about__title">{{ t('settings.about.introTitle') }}</span>
      </template>
      <p class="about__intro">{{ t('settings.about.intro') }}</p>
    </el-card>

    <el-card class="about__card" shadow="never">
      <template #header>
        <span class="about__title">{{ t('settings.about.architectureTitle') }}</span>
      </template>
      <el-descriptions :column="1" border>
        <el-descriptions-item :label="t('settings.about.architectureLabels.driver')">
          {{ t('settings.about.architecture.driver') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.about.architectureLabels.data')">
          {{ t('settings.about.architecture.data') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.about.architectureLabels.management')">
          {{ t('settings.about.architecture.management') }}
        </el-descriptions-item>
        <el-descriptions-item :label="t('settings.about.architectureLabels.application')">
          {{ t('settings.about.architecture.application') }}
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card class="about__card" shadow="never">
      <template #header>
        <span class="about__title">{{ t('settings.about.objectivesTitle') }}</span>
      </template>
      <ul class="about__objectives">
        <li v-for="key in objectiveKeys" :key="key">{{ t(`settings.about.objectives.${key}`) }}</li>
      </ul>
    </el-card>

    <el-card class="about__card" shadow="never">
      <template #header>
        <span class="about__title">{{ t('settings.about.licenseTitle') }}</span>
      </template>
      <p class="about__intro">{{ t('settings.about.licenseDetail') }}</p>
      <external-link class="about__license-link" href="https://www.gnu.org/licenses/agpl-3.0.html">
        AGPL-3.0 Full Text
      </external-link>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
  import {useI18n} from 'vue-i18n';

  import blankCard from '@/components/card/blank/BlankCard.vue';
  import pkg from '../../../../package.json';

  const {t} = useI18n();
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
    // 8px rhythm matches the home / event overview pages (About is a
    // multi-card info page under the same 4px radius, so the rhythm
    // should read the same). No right-side padding — About isn't a
    // board-style page the way Overview is.
    gap: 8px;

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
