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
  <div>
    <blank-card>
      <el-tabs v-model="reactiveData.active">
        <el-tab-pane :label="activeConfig.detailTitle" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item v-for="field in activeConfig.fields" :key="field.prop" :label="field.label">
                <el-tag v-if="field.kind === 'tag'" :type="tagType(reactiveData.data[field.prop], field.prop)">
                  {{ formatDetail(field) }}
                </el-tag>
                <code v-else-if="field.kind === 'code'" class="alarm-detail__inline-code">
                  {{ formatDetail(field) }}
                </code>
                <span v-else>{{ formatDetail(field) }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </detail-card>
        </el-tab-pane>

        <el-tab-pane v-for="prop in activeConfig.extProps" :key="prop" :label="extLabel(prop)" :name="prop">
          <detail-card>
            <pre class="alarm-detail__json">{{ prettyJson(reactiveData.data[prop], '{}') }}</pre>
          </detail-card>
        </el-tab-pane>
      </el-tabs>
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
  import {computed, onMounted, reactive} from 'vue';
  import {useI18n} from 'vue-i18n';
  import {useRoute} from 'vue-router';

  import {
    getMessageById,
    getNotifyById,
    getNotifyChannelBindById,
    getNotifyChannelById,
    getNotifyHistoryById,
    getRuleById,
    getRuleStateById,
  } from '@/api/alarm';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import DetailCard from '@/components/card/detail/DetailCard.vue';
  import type {AlarmEntity as AlarmEntityData} from '@/config/types';
  import {timestampLabel} from '@/utils/dateUtil';
  import {prettyJson} from '@/utils/jsonUtil';

  type AlarmEntity = 'rule' | 'notify' | 'message' | 'channel' | 'bind' | 'state' | 'history';
  type FieldKind = 'text' | 'tag' | 'time' | 'code';

  interface DetailField {
    prop: string;
    label: string;
    kind?: FieldKind;
  }

  interface DetailConfig {
    entity: AlarmEntity;
    detailTitle: string;
    fields: DetailField[];
    extProps: string[];
    load: (id: string) => Promise<R>;
  }

  const props = withDefaults(
    defineProps<{
      entity?: AlarmEntity;
    }>(),
    {
      entity: 'rule',
    }
  );

  const route = useRoute();
  const {t} = useI18n();

  const reactiveData = reactive({
    id: route.query.id as string,
    active: (route.query.active as string) || 'detail',
    data: {} as AlarmEntityData,
  });

  const commonFields = (): DetailField[] => [
    {prop: 'enableFlag', label: t('common.enableFlag'), kind: 'tag'},
    {prop: 'remark', label: t('common.remark')},
    {prop: 'creatorName', label: t('common.creatorName')},
    {prop: 'createTime', label: t('common.createTime'), kind: 'time'},
    {prop: 'operatorName', label: t('common.operatorName')},
    {prop: 'operateTime', label: t('common.operationTime'), kind: 'time'},
  ];

  const configs = computed<DetailConfig[]>(() => [
    {
      entity: 'rule',
      detailTitle: t('nav.settingsAlarmRuleDetail'),
      fields: [
        {prop: 'ruleName', label: t('settings.alarm.ruleName')},
        {prop: 'ruleCode', label: t('settings.alarm.ruleCode'), kind: 'code'},
        {prop: 'alarmTargetTypeFlag', label: t('settings.alarm.targetType'), kind: 'tag'},
        {prop: 'entityId', label: t('settings.alarm.entityId'), kind: 'code'},
        {prop: 'notifyId', label: t('settings.alarm.notifyId'), kind: 'code'},
        {prop: 'messageId', label: t('settings.alarm.messageId'), kind: 'code'},
        ...commonFields(),
      ],
      extProps: ['ruleExt'],
      load: getRuleById,
    },
    {
      entity: 'notify',
      detailTitle: t('nav.settingsAlarmNotifyDetail'),
      fields: [
        {prop: 'notifyName', label: t('settings.alarm.notifyName')},
        {prop: 'notifyCode', label: t('settings.alarm.notifyCode'), kind: 'code'},
        {prop: 'autoConfirmFlag', label: t('settings.alarm.autoConfirm'), kind: 'tag'},
        {prop: 'notifyInterval', label: t('settings.alarm.notifyInterval')},
        ...commonFields(),
      ],
      extProps: ['notifyExt'],
      load: getNotifyById,
    },
    {
      entity: 'message',
      detailTitle: t('nav.settingsAlarmMessageDetail'),
      fields: [
        {prop: 'messageName', label: t('settings.alarm.messageName')},
        {prop: 'messageCode', label: t('settings.alarm.messageCode'), kind: 'code'},
        {prop: 'messageLevel', label: t('settings.alarm.messageLevel'), kind: 'tag'},
        ...commonFields(),
      ],
      extProps: ['messageExt'],
      load: getMessageById,
    },
    {
      entity: 'channel',
      detailTitle: t('nav.settingsAlarmChannelDetail'),
      fields: [
        {prop: 'channelName', label: t('settings.alarm.channelName')},
        {prop: 'channelCode', label: t('settings.alarm.channelCode'), kind: 'code'},
        {prop: 'channelTypeFlag', label: t('settings.alarm.channelType'), kind: 'tag'},
        {prop: 'credentialRef', label: t('settings.alarm.credentialRef'), kind: 'code'},
        ...commonFields(),
      ],
      extProps: ['channelExt'],
      load: getNotifyChannelById,
    },
    {
      entity: 'bind',
      detailTitle: t('nav.settingsAlarmBindDetail'),
      fields: [
        {prop: 'notifyId', label: t('settings.alarm.notifyId'), kind: 'code'},
        {prop: 'channelId', label: t('settings.alarm.channelId'), kind: 'code'},
        ...commonFields(),
      ],
      extProps: ['bindExt'],
      load: getNotifyChannelBindById,
    },
    {
      entity: 'state',
      detailTitle: t('nav.settingsAlarmStateDetail'),
      fields: [
        {prop: 'ruleId', label: t('settings.alarm.ruleId'), kind: 'code'},
        {prop: 'alarmTargetTypeFlag', label: t('settings.alarm.targetType'), kind: 'tag'},
        {prop: 'entityId', label: t('settings.alarm.entityId'), kind: 'code'},
        {prop: 'entityStateFlag', label: t('settings.alarm.state'), kind: 'tag'},
        {prop: 'fingerprint', label: 'Fingerprint', kind: 'code'},
        {prop: 'triggerCount', label: t('settings.alarm.triggerCount')},
        {prop: 'alarmId', label: 'Alarm ID', kind: 'code'},
        {prop: 'firstTriggerTime', label: 'First Trigger', kind: 'time'},
        {prop: 'lastTriggerTime', label: t('settings.alarm.lastTriggerTime'), kind: 'time'},
        {prop: 'lastRecoverTime', label: 'Last Recovery', kind: 'time'},
        {prop: 'lastNotifyTime', label: t('settings.alarm.lastNotifyTime'), kind: 'time'},
        ...commonFields(),
      ],
      extProps: ['entityStateExt'],
      load: getRuleStateById,
    },
    {
      entity: 'history',
      detailTitle: t('nav.settingsAlarmHistoryDetail'),
      fields: [
        {prop: 'ruleId', label: t('settings.alarm.ruleId'), kind: 'code'},
        {prop: 'notifyId', label: t('settings.alarm.notifyId'), kind: 'code'},
        {prop: 'messageId', label: t('settings.alarm.messageId'), kind: 'code'},
        {prop: 'channelId', label: t('settings.alarm.channelId'), kind: 'code'},
        {prop: 'alarmId', label: 'Alarm ID', kind: 'code'},
        {prop: 'channelTypeFlag', label: t('settings.alarm.channelType'), kind: 'tag'},
        {prop: 'target', label: t('settings.alarm.target')},
        {prop: 'statusFlag', label: t('settings.alarm.status'), kind: 'tag'},
        {prop: 'retryCount', label: t('settings.alarm.retryCount')},
        {prop: 'errorMessage', label: t('settings.alarm.errorMessage')},
        ...commonFields(),
      ],
      extProps: ['requestExt', 'responseExt'],
      load: getNotifyHistoryById,
    },
  ]);

  const activeEntity = computed<AlarmEntity>(() => props.entity || 'rule');
  const activeConfig = computed<DetailConfig>(() => {
    return configs.value.find((config) => config.entity === activeEntity.value) || configs.value[0]!;
  });

  const enumLabel = (value: unknown) => {
    const text = String(value || '');
    const map: Record<string, string> = {
      ENABLE: t('common.enable'),
      DISABLE: t('common.disable'),
      AUTO: t('settings.alarm.auto'),
      MANUAL: t('settings.alarm.manual'),
      POINT: t('settings.alarm.point'),
      DEVICE: t('settings.alarm.device'),
      DRIVER: t('settings.alarm.driver'),
      NORMAL: t('settings.alarm.normal'),
      FIRING: t('settings.alarm.firing'),
      RECOVERED: t('settings.alarm.recovered'),
      PENDING: t('settings.alarm.pending'),
      SUCCESS: t('settings.alarm.success'),
      FAILED: t('settings.alarm.failed'),
      RETRYING: t('settings.alarm.retrying'),
      SKIPPED: t('settings.alarm.skipped'),
      P0: 'P0',
      P1: 'P1',
      P2: 'P2',
      P3: 'P3',
      FEISHU_BOT: 'Feishu Bot',
      WEBHOOK: 'Webhook',
      EMAIL: 'Email',
    };
    return map[text] || text || '-';
  };

  const tagType = (value: unknown, prop: string) => {
    const text = String(value || '');
    if (text === 'ENABLE' || text === 'SUCCESS' || text === 'NORMAL' || text === 'AUTO') return 'success';
    if (text === 'DISABLE' || text === 'FAILED' || text === 'FIRING') return 'danger';
    if (text === 'PENDING' || text === 'RETRYING' || text === 'RECOVERED' || prop === 'channelTypeFlag') {
      return 'warning';
    }
    return 'info';
  };

  const formatDetail = (field: DetailField) => {
    const value = reactiveData.data[field.prop];
    if (field.kind === 'time' || field.prop.endsWith('Time')) return timestampLabel(value);
    if (field.kind === 'tag') return enumLabel(value);
    if (value == null || value === '') return '-';
    return String(value);
  };

  const extLabel = (prop: string) => {
    const map: Record<string, string> = {
      ruleExt: t('settings.alarm.ruleExt'),
      notifyExt: t('settings.alarm.notifyExt'),
      messageExt: t('settings.alarm.messageExt'),
      channelExt: t('settings.alarm.channelExt'),
      bindExt: t('settings.alarm.bindExt'),
      entityStateExt: t('settings.alarm.entityStateExt'),
      requestExt: t('settings.alarm.requestExt'),
      responseExt: t('settings.alarm.responseExt'),
    };
    return map[prop] || prop;
  };

  const load = () => {
    if (!reactiveData.id) return;
    activeConfig.value
      .load(reactiveData.id)
      .then((res: any) => {
        reactiveData.data = res.data || {};
      })
      .catch(() => {
        // handled globally
      });
  };

  onMounted(() => {
    load();
  });
</script>

<style lang="scss" scoped>
  .alarm-detail__inline-code {
    padding: 2px 5px;
    border-radius: 4px;
    color: var(--el-text-color-regular);
    background: var(--el-fill-color-light);
    font-size: 12px;
  }

  .alarm-detail__json {
    box-sizing: border-box;
    max-width: 100%;
    overflow: auto;
    margin: 0;
    padding: 12px;
    border: 1px solid var(--el-border-color-lighter);
    border-radius: 4px;
    color: var(--el-text-color-primary);
    background: var(--el-fill-color-lighter);
    font-size: 12px;
    line-height: 1.55;
    white-space: pre-wrap;
    word-break: break-word;
  }
</style>
