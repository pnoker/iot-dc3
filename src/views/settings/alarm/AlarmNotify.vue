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
  <div class="alarm-notify">
    <tool-card
      :form-model="searchForm"
      :hide-sort="false"
      :page="state.page"
      @refresh="load"
      @reset="reset"
      @search="search"
      @sort="sort"
      @size-change="sizeChange"
      @current-change="currentChange"
    >
      <template #filters>
        <el-form-item :label="activeConfig.searchLabel" prop="keyword">
          <el-input v-model="searchForm.keyword" clearable :placeholder="activeConfig.searchPlaceholder" />
        </el-form-item>
        <el-form-item v-if="activeConfig.filterProp" :label="activeConfig.filterLabel" prop="filterValue">
          <el-select v-model="searchForm.filterValue" clearable :placeholder="activeConfig.filterPlaceholder">
            <el-option v-for="option in activeConfig.filterOptions" :key="option.value" v-bind="option" />
          </el-select>
        </el-form-item>
      </template>
      <template #actions>
        <el-button v-if="activeConfig.editable" :icon="Plus" type="success" @click="openAdd">
          {{ t('common.add') }}
        </el-button>
      </template>
    </tool-card>

    <blank-card>
      <el-table v-loading="state.loading" :data="state.rows" class="alarm-notify__table" stripe>
        <el-table-column
          v-for="column in activeConfig.columns"
          :key="column.prop"
          :fixed="column.fixed"
          :label="column.label"
          :min-width="column.minWidth"
          :prop="column.prop"
          :show-overflow-tooltip="column.overflow !== false"
          :width="column.width"
        >
          <template #default="{ row }">
            <el-tag v-if="column.kind === 'tag'" :type="tagType(row[column.prop], column.prop)">
              {{ formatCell(row, column) }}
            </el-tag>
            <code v-else-if="column.kind === 'code'" class="alarm-notify__inline-code">
              {{ formatCell(row, column) }}
            </code>
            <span v-else>{{ formatCell(row, column) }}</span>
          </template>
        </el-table-column>
        <el-table-column :label="t('common.operation')" fixed="right" width="180">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ t('common.detail') }}</el-button>
            <el-button v-if="activeConfig.editable" link type="primary" @click="openEdit(row)">
              {{ t('common.edit') }}
            </el-button>
            <el-popconfirm
              v-if="activeConfig.editable"
              :cancel-button-text="t('common.cancel')"
              :confirm-button-text="t('common.confirm')"
              :title="t('settings.alarm.confirmDelete')"
              @confirm="remove(row.id)"
            >
              <template #reference>
                <el-button link type="danger">{{ t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="t('settings.alarm.empty')" />
        </template>
      </el-table>
    </blank-card>

    <el-dialog
      v-model="formVisible"
      :append-to-body="true"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      :title="dialogTitle"
      class="things-dialog"
      destroy-on-close
      draggable
      width="760px"
    >
      <el-form ref="formRef" :model="formModel" :rules="formRules" label-position="top">
        <el-row :gutter="12">
          <el-col v-for="field in activeConfig.fields" :key="field.prop" :span="field.span || 12">
            <el-form-item :label="field.label" :prop="field.prop">
              <el-select
                v-if="field.kind === 'select'"
                v-model="formModel[field.prop]"
                clearable
                filterable
                :placeholder="field.placeholder"
              >
                <el-option v-for="option in field.options || []" :key="option.value" v-bind="option" />
              </el-select>
              <el-input-number
                v-else-if="field.kind === 'number'"
                v-model="formModel[field.prop]"
                controls-position="right"
                :min="0"
                :precision="field.precision || 0"
                style="width: 100%"
              />
              <el-input
                v-else-if="field.kind === 'json' || field.kind === 'textarea'"
                v-model="formModel[field.prop]"
                :autosize="{ minRows: field.rows || 4, maxRows: 18 }"
                :placeholder="field.placeholder"
                resize="vertical"
                type="textarea"
              />
              <el-input v-else v-model="formModel[field.prop]" clearable :placeholder="field.placeholder" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="things-dialog-footer">
          <el-button @click="formVisible = false">{{ t('common.cancel') }}</el-button>
          <el-button plain type="success" @click="resetForm">{{ t('common.reset') }}</el-button>
          <el-button :loading="state.saving" type="primary" @click="submit">{{ t('common.confirm') }}</el-button>
        </div>
      </template>
    </el-dialog>

    <el-drawer v-model="detailVisible" :title="activeConfig.label" size="560px">
      <el-descriptions :column="1" border>
        <el-descriptions-item v-for="field in detailFields" :key="field.prop" :label="field.label">
          {{ formatDetail(field.prop) }}
        </el-descriptions-item>
      </el-descriptions>
      <div v-for="prop in activeConfig.extProps" :key="prop" class="alarm-notify__json-block">
        <div class="alarm-notify__json-title">{{ extLabel(prop) }}</div>
        <pre>{{ prettyJson(detailRow?.[prop]) }}</pre>
      </div>
    </el-drawer>
  </div>
</template>

<script lang="ts" setup>
  import type { FormInstance, FormRules } from 'element-plus';
  import { computed, reactive, ref, watch } from 'vue';
  import { useI18n } from 'vue-i18n';
  import { Plus } from '@element-plus/icons-vue';

  import {
    addMessage,
    addNotify,
    addNotifyChannel,
    addNotifyChannelBind,
    addRule,
    deleteMessage,
    deleteNotify,
    deleteNotifyChannel,
    deleteNotifyChannelBind,
    deleteRule,
    getMessageList,
    getNotifyChannelBindList,
    getNotifyChannelList,
    getNotifyList,
    getNotifyRecordList,
    getRuleList,
    getRuleStateList,
    updateMessage,
    updateNotify,
    updateNotifyChannel,
    updateNotifyChannelBind,
    updateRule,
  } from '@/api/alarm';
  import BlankCard from '@/components/card/blank/BlankCard.vue';
  import ToolCard from '@/components/card/tool/ToolCard.vue';
  import type { AlarmEntityRecord, Order, PageQuery } from '@/config/types';
  import { timestamp } from '@/utils/dateUtil';
  import { failMessage, successMessage } from '@/utils/notificationUtil';
  import { cleanSearchParams, resetSearchForm } from '@/utils/searchParamUtil';

  type TabKey = 'rule' | 'notify' | 'message' | 'channel' | 'bind' | 'state' | 'record';
  type FieldKind = 'input' | 'number' | 'select' | 'textarea' | 'json';
  type ColumnKind = 'text' | 'tag' | 'code' | 'time' | 'json';

  interface Option {
    label: string;
    value: string;
  }

  interface FieldConfig {
    prop: string;
    label: string;
    kind?: FieldKind;
    options?: Option[];
    placeholder?: string;
    required?: boolean;
    span?: number;
    rows?: number;
    precision?: number;
  }

  interface ColumnConfig {
    prop: string;
    label: string;
    kind?: ColumnKind;
    width?: number | string;
    minWidth?: number | string;
    fixed?: boolean | 'left' | 'right';
    overflow?: boolean;
  }

  interface EntityConfig {
    key: TabKey;
    label: string;
    editable: boolean;
    searchProp?: string;
    searchLabel: string;
    searchPlaceholder: string;
    filterProp?: string;
    filterLabel?: string;
    filterPlaceholder?: string;
    filterOptions: Option[];
    extProps: string[];
    columns: ColumnConfig[];
    fields: FieldConfig[];
    defaultForm: () => Record<string, unknown>;
    list: (query: PageQuery) => Promise<R>;
    add?: (payload: Record<string, unknown>) => Promise<R>;
    update?: (payload: Record<string, unknown>) => Promise<R>;
    remove?: (id: string) => Promise<R>;
  }

  const { t } = useI18n();
  const props = defineProps<{
    entity: TabKey;
  }>();

  const enableOptions: Option[] = [
    { label: t('common.enable'), value: 'ENABLE' },
    { label: t('common.disable'), value: 'DISABLE' },
  ];
  const autoConfirmOptions: Option[] = [
    { label: t('settings.alarm.auto'), value: 'AUTO' },
    { label: t('settings.alarm.manual'), value: 'MANUAL' },
  ];
  const targetOptions: Option[] = [
    { label: t('settings.alarm.point'), value: 'POINT' },
    { label: t('settings.alarm.device'), value: 'DEVICE' },
    { label: t('settings.alarm.driver'), value: 'DRIVER' },
  ];
  const channelTypeOptions: Option[] = [
    { label: 'Feishu Bot', value: 'FEISHU_BOT' },
    { label: 'Webhook', value: 'WEBHOOK' },
    { label: 'Email', value: 'EMAIL' },
  ];
  const messageLevelOptions: Option[] = [
    { label: 'P0', value: 'P0' },
    { label: 'P1', value: 'P1' },
    { label: 'P2', value: 'P2' },
    { label: 'P3', value: 'P3' },
  ];
  const ruleStateOptions: Option[] = [
    { label: t('settings.alarm.normal'), value: 'NORMAL' },
    { label: t('settings.alarm.firing'), value: 'FIRING' },
    { label: t('settings.alarm.recovered'), value: 'RECOVERED' },
  ];
  const recordStatusOptions: Option[] = [
    { label: t('settings.alarm.pending'), value: 'PENDING' },
    { label: t('settings.alarm.success'), value: 'SUCCESS' },
    { label: t('settings.alarm.failed'), value: 'FAILED' },
    { label: t('settings.alarm.retrying'), value: 'RETRYING' },
    { label: t('settings.alarm.skipped'), value: 'SKIPPED' },
  ];

  const structuredExt = (type: string, content: Record<string, unknown>, remark = '') => ({
    type,
    version: 1,
    remark,
    content,
  });

  const defaultRuleExt = () =>
    structuredExt('alarm-rule', {
      condition: { field: 'numValue', operator: '>', threshold: 80, unit: '' },
      window: { mode: 'LAST', minSamples: 1 },
      recovery: { enabled: true, operator: '<=', threshold: 75, duration: 'PT5M' },
      severity: 'P2',
      eventType: 'ALARM',
      labels: [],
    });

  const defaultNotifyExt = () =>
    structuredExt('alarm-notify-policy', {
      dedup: { enabled: true, key: '${tenantId}:${ruleCode}:${entityId}' },
      rateLimit: { intervalMs: 300000, maxCount: 1 },
      repeat: { enabled: false },
      recovery: { enabled: true, sendRecoveryMessage: true, autoConfirmOnRecovery: false },
    });

  const defaultMessageExt = () =>
    structuredExt('alarm-message-template', {
      variables: ['severity', 'device', 'point', 'value', 'unit', 'threshold', 'triggerTime'],
      templates: [
        {
          channelType: 'FEISHU_BOT',
          payloadType: 'CARD',
          template: {
            title: '${severity} ${device} alarm',
            summary: '${point} is ${value}${unit}, threshold ${threshold}${unit}.',
          },
        },
      ],
    });

  const defaultChannelExt = () =>
    structuredExt('notify-channel', {
      signEnabled: true,
      cardVersion: 'interactive-card-v1',
      atAllAllowed: false,
      testMessageEnabled: true,
      options: { locale: 'zh-CN' },
    });

  const defaultBindExt = () =>
    structuredExt('notify-channel-bind', {
      levels: ['P0', 'P1', 'P2'],
      sendRecovery: true,
      rateLimitOverrideMs: 300000,
    });

  const commonColumns = (): ColumnConfig[] => [
    { prop: 'remark', label: t('common.remark'), minWidth: 180 },
    { prop: 'createTime', label: t('common.createTime'), kind: 'time', width: 180 },
    { prop: 'operateTime', label: t('common.operationTime'), kind: 'time', width: 180 },
  ];

  const commonFields = (): FieldConfig[] => [
    { prop: 'enableFlag', label: t('common.enableFlag'), kind: 'select', options: enableOptions, span: 12 },
    { prop: 'remark', label: t('common.remark'), kind: 'textarea', span: 24, rows: 3 },
  ];

  const configs: EntityConfig[] = [
    {
      key: 'rule',
      label: t('settings.alarm.rules'),
      editable: true,
      searchProp: 'ruleName',
      searchLabel: t('settings.alarm.ruleName'),
      searchPlaceholder: t('settings.alarm.searchRule'),
      filterProp: 'enableFlag',
      filterLabel: t('common.enableFlag'),
      filterPlaceholder: t('common.enableFlag'),
      filterOptions: enableOptions,
      extProps: ['ruleExt'],
      columns: [
        { prop: 'ruleName', label: t('settings.alarm.ruleName'), minWidth: 210 },
        { prop: 'ruleCode', label: t('settings.alarm.ruleCode'), kind: 'code', minWidth: 220 },
        { prop: 'alarmTargetTypeFlag', label: t('settings.alarm.targetType'), kind: 'tag', width: 110 },
        { prop: 'entityId', label: t('settings.alarm.entityId'), kind: 'code', minWidth: 150 },
        { prop: 'notifyId', label: t('settings.alarm.notifyId'), kind: 'code', minWidth: 150 },
        { prop: 'messageId', label: t('settings.alarm.messageId'), kind: 'code', minWidth: 150 },
        { prop: 'enableFlag', label: t('common.enableFlag'), kind: 'tag', width: 90 },
        ...commonColumns(),
      ],
      fields: [
        { prop: 'ruleName', label: t('settings.alarm.ruleName'), required: true },
        { prop: 'ruleCode', label: t('settings.alarm.ruleCode') },
        {
          prop: 'alarmTargetTypeFlag',
          label: t('settings.alarm.targetType'),
          kind: 'select',
          options: targetOptions,
          required: true,
        },
        { prop: 'entityId', label: t('settings.alarm.entityId'), required: true },
        { prop: 'notifyId', label: t('settings.alarm.notifyId'), required: true },
        { prop: 'messageId', label: t('settings.alarm.messageId'), required: true },
        { prop: 'ruleExt', label: t('settings.alarm.ruleExt'), kind: 'json', span: 24, rows: 10, required: true },
        ...commonFields(),
      ],
      defaultForm: () => ({
        ruleName: '',
        ruleCode: '',
        alarmTargetTypeFlag: 'POINT',
        entityId: '',
        notifyId: '',
        messageId: '',
        ruleExt: defaultRuleExt(),
        enableFlag: 'ENABLE',
        remark: '',
      }),
      list: getRuleList,
      add: addRule,
      update: updateRule,
      remove: deleteRule,
    },
    {
      key: 'notify',
      label: t('settings.alarm.notifies'),
      editable: true,
      searchProp: 'notifyName',
      searchLabel: t('settings.alarm.notifyName'),
      searchPlaceholder: t('settings.alarm.searchNotify'),
      filterProp: 'enableFlag',
      filterLabel: t('common.enableFlag'),
      filterPlaceholder: t('common.enableFlag'),
      filterOptions: enableOptions,
      extProps: ['notifyExt'],
      columns: [
        { prop: 'notifyName', label: t('settings.alarm.notifyName'), minWidth: 220 },
        { prop: 'notifyCode', label: t('settings.alarm.notifyCode'), kind: 'code', minWidth: 220 },
        { prop: 'autoConfirmFlag', label: t('settings.alarm.autoConfirm'), kind: 'tag', width: 120 },
        { prop: 'notifyInterval', label: t('settings.alarm.notifyInterval'), width: 130 },
        { prop: 'enableFlag', label: t('common.enableFlag'), kind: 'tag', width: 90 },
        ...commonColumns(),
      ],
      fields: [
        { prop: 'notifyName', label: t('settings.alarm.notifyName'), required: true },
        { prop: 'notifyCode', label: t('settings.alarm.notifyCode') },
        {
          prop: 'autoConfirmFlag',
          label: t('settings.alarm.autoConfirm'),
          kind: 'select',
          options: autoConfirmOptions,
          required: true,
        },
        { prop: 'notifyInterval', label: t('settings.alarm.notifyInterval'), kind: 'number', required: true },
        { prop: 'notifyExt', label: t('settings.alarm.notifyExt'), kind: 'json', span: 24, rows: 10, required: true },
        ...commonFields(),
      ],
      defaultForm: () => ({
        notifyName: '',
        notifyCode: '',
        autoConfirmFlag: 'MANUAL',
        notifyInterval: 300000,
        notifyExt: defaultNotifyExt(),
        enableFlag: 'ENABLE',
        remark: '',
      }),
      list: getNotifyList,
      add: addNotify,
      update: updateNotify,
      remove: deleteNotify,
    },
    {
      key: 'message',
      label: t('settings.alarm.messages'),
      editable: true,
      searchProp: 'messageName',
      searchLabel: t('settings.alarm.messageName'),
      searchPlaceholder: t('settings.alarm.searchMessage'),
      filterProp: 'enableFlag',
      filterLabel: t('common.enableFlag'),
      filterPlaceholder: t('common.enableFlag'),
      filterOptions: enableOptions,
      extProps: ['messageExt'],
      columns: [
        { prop: 'messageName', label: t('settings.alarm.messageName'), minWidth: 240 },
        { prop: 'messageCode', label: t('settings.alarm.messageCode'), kind: 'code', minWidth: 240 },
        { prop: 'messageLevel', label: t('settings.alarm.messageLevel'), kind: 'tag', width: 110 },
        { prop: 'enableFlag', label: t('common.enableFlag'), kind: 'tag', width: 90 },
        ...commonColumns(),
      ],
      fields: [
        { prop: 'messageName', label: t('settings.alarm.messageName'), required: true },
        { prop: 'messageCode', label: t('settings.alarm.messageCode') },
        {
          prop: 'messageLevel',
          label: t('settings.alarm.messageLevel'),
          kind: 'select',
          options: messageLevelOptions,
          required: true,
        },
        { prop: 'messageExt', label: t('settings.alarm.messageExt'), kind: 'json', span: 24, rows: 10, required: true },
        ...commonFields(),
      ],
      defaultForm: () => ({
        messageName: '',
        messageCode: '',
        messageLevel: 'P2',
        messageExt: defaultMessageExt(),
        enableFlag: 'ENABLE',
        remark: '',
      }),
      list: getMessageList,
      add: addMessage,
      update: updateMessage,
      remove: deleteMessage,
    },
    {
      key: 'channel',
      label: t('settings.alarm.channels'),
      editable: true,
      searchProp: 'channelName',
      searchLabel: t('settings.alarm.channelName'),
      searchPlaceholder: t('settings.alarm.searchChannel'),
      filterProp: 'channelTypeFlag',
      filterLabel: t('settings.alarm.channelType'),
      filterPlaceholder: t('settings.alarm.channelType'),
      filterOptions: channelTypeOptions,
      extProps: ['channelExt'],
      columns: [
        { prop: 'channelName', label: t('settings.alarm.channelName'), minWidth: 230 },
        { prop: 'channelCode', label: t('settings.alarm.channelCode'), kind: 'code', minWidth: 230 },
        { prop: 'channelTypeFlag', label: t('settings.alarm.channelType'), kind: 'tag', width: 130 },
        { prop: 'credentialRef', label: t('settings.alarm.credentialRef'), kind: 'code', minWidth: 230 },
        { prop: 'enableFlag', label: t('common.enableFlag'), kind: 'tag', width: 90 },
        ...commonColumns(),
      ],
      fields: [
        { prop: 'channelName', label: t('settings.alarm.channelName'), required: true },
        { prop: 'channelCode', label: t('settings.alarm.channelCode') },
        {
          prop: 'channelTypeFlag',
          label: t('settings.alarm.channelType'),
          kind: 'select',
          options: channelTypeOptions,
          required: true,
        },
        { prop: 'credentialRef', label: t('settings.alarm.credentialRef'), required: true },
        { prop: 'channelExt', label: t('settings.alarm.channelExt'), kind: 'json', span: 24, rows: 8, required: true },
        ...commonFields(),
      ],
      defaultForm: () => ({
        channelName: '',
        channelCode: '',
        channelTypeFlag: 'FEISHU_BOT',
        credentialRef: 'secret:feishu:',
        channelExt: defaultChannelExt(),
        enableFlag: 'ENABLE',
        remark: '',
      }),
      list: getNotifyChannelList,
      add: addNotifyChannel,
      update: updateNotifyChannel,
      remove: deleteNotifyChannel,
    },
    {
      key: 'bind',
      label: t('settings.alarm.bindings'),
      editable: true,
      searchProp: 'notifyId',
      searchLabel: t('settings.alarm.notifyId'),
      searchPlaceholder: t('settings.alarm.searchBinding'),
      filterProp: 'enableFlag',
      filterLabel: t('common.enableFlag'),
      filterPlaceholder: t('common.enableFlag'),
      filterOptions: enableOptions,
      extProps: ['bindExt'],
      columns: [
        { prop: 'notifyId', label: t('settings.alarm.notifyId'), kind: 'code', minWidth: 170 },
        { prop: 'channelId', label: t('settings.alarm.channelId'), kind: 'code', minWidth: 170 },
        { prop: 'enableFlag', label: t('common.enableFlag'), kind: 'tag', width: 90 },
        ...commonColumns(),
      ],
      fields: [
        { prop: 'notifyId', label: t('settings.alarm.notifyId'), required: true },
        { prop: 'channelId', label: t('settings.alarm.channelId'), required: true },
        { prop: 'bindExt', label: t('settings.alarm.bindExt'), kind: 'json', span: 24, rows: 8, required: true },
        ...commonFields(),
      ],
      defaultForm: () => ({
        notifyId: '',
        channelId: '',
        bindExt: defaultBindExt(),
        enableFlag: 'ENABLE',
        remark: '',
      }),
      list: getNotifyChannelBindList,
      add: addNotifyChannelBind,
      update: updateNotifyChannelBind,
      remove: deleteNotifyChannelBind,
    },
    {
      key: 'state',
      label: t('settings.alarm.states'),
      editable: false,
      searchProp: 'ruleId',
      searchLabel: t('settings.alarm.ruleId'),
      searchPlaceholder: t('settings.alarm.searchState'),
      filterProp: 'stateFlag',
      filterLabel: t('settings.alarm.state'),
      filterPlaceholder: t('settings.alarm.state'),
      filterOptions: ruleStateOptions,
      extProps: ['stateExt'],
      columns: [
        { prop: 'ruleId', label: t('settings.alarm.ruleId'), kind: 'code', minWidth: 160 },
        { prop: 'alarmTargetTypeFlag', label: t('settings.alarm.targetType'), kind: 'tag', width: 110 },
        { prop: 'entityId', label: t('settings.alarm.entityId'), kind: 'code', minWidth: 150 },
        { prop: 'stateFlag', label: t('settings.alarm.state'), kind: 'tag', width: 110 },
        { prop: 'triggerCount', label: t('settings.alarm.triggerCount'), width: 110 },
        { prop: 'lastTriggerTime', label: t('settings.alarm.lastTriggerTime'), kind: 'time', width: 180 },
        { prop: 'lastNotifyTime', label: t('settings.alarm.lastNotifyTime'), kind: 'time', width: 180 },
        ...commonColumns(),
      ],
      fields: [],
      defaultForm: () => ({}),
      list: getRuleStateList,
    },
    {
      key: 'record',
      label: t('settings.alarm.records'),
      editable: false,
      searchProp: 'target',
      searchLabel: t('settings.alarm.target'),
      searchPlaceholder: t('settings.alarm.searchRecord'),
      filterProp: 'statusFlag',
      filterLabel: t('settings.alarm.status'),
      filterPlaceholder: t('settings.alarm.status'),
      filterOptions: recordStatusOptions,
      extProps: ['requestExt', 'responseExt'],
      columns: [
        { prop: 'ruleId', label: t('settings.alarm.ruleId'), kind: 'code', minWidth: 150 },
        { prop: 'channelId', label: t('settings.alarm.channelId'), kind: 'code', minWidth: 150 },
        { prop: 'channelTypeFlag', label: t('settings.alarm.channelType'), kind: 'tag', width: 130 },
        { prop: 'target', label: t('settings.alarm.target'), minWidth: 180 },
        { prop: 'statusFlag', label: t('settings.alarm.status'), kind: 'tag', width: 110 },
        { prop: 'retryCount', label: t('settings.alarm.retryCount'), width: 100 },
        { prop: 'errorMessage', label: t('settings.alarm.errorMessage'), minWidth: 220 },
        ...commonColumns(),
      ],
      fields: [],
      defaultForm: () => ({}),
      list: getNotifyRecordList,
    },
  ];

  const formVisible = ref(false);
  const detailVisible = ref(false);
  const editing = ref(false);
  const detailRow = ref<AlarmEntityRecord | null>(null);
  const formRef = ref<FormInstance>();
  const formModel = reactive<Record<string, any>>({});
  const searchForm = reactive<Record<string, any>>({
    keyword: '',
    filterValue: '',
  });

  const state = reactive({
    loading: false,
    saving: false,
    rows: [] as AlarmEntityRecord[],
    page: {
      total: 0,
      size: 12,
      current: 1,
      orders: [{ column: 'create_time', asc: false }] as Order[],
    },
  });

  const defaultConfig = configs[0] as EntityConfig;
  const activeConfig = computed<EntityConfig>(
    () => configs.find((config) => config.key === props.entity) || defaultConfig
  );
  const dialogTitle = computed(() =>
    editing.value ? `${t('common.edit')} ${activeConfig.value.label}` : `${t('common.add')} ${activeConfig.value.label}`
  );
  const detailFields = computed(() => [
    ...activeConfig.value.columns.filter((column) => !activeConfig.value.extProps.includes(column.prop)),
    { prop: 'creatorName', label: t('common.creatorName') },
    { prop: 'operatorName', label: t('common.operatorName') },
  ]);
  const formRules = computed<FormRules>(() => {
    const rules: FormRules = {};
    activeConfig.value.fields
      .filter((field) => field.required)
      .forEach((field) => {
        rules[field.prop] = [{ required: true, message: t('settings.alarm.required'), trigger: 'blur' }];
      });
    return rules;
  });

  const query = (): PageQuery => {
    const config = activeConfig.value;
    const params = cleanSearchParams(searchForm);
    const result: PageQuery = {
      page: {
        current: state.page.current,
        size: state.page.size,
        orders: state.page.orders,
      },
    };
    if (config.searchProp && params.keyword) {
      result[config.searchProp] = String(params.keyword).trim();
    }
    if (config.filterProp && params.filterValue) {
      result[config.filterProp] = params.filterValue;
    }
    return result;
  };

  const load = () => {
    state.loading = true;
    activeConfig.value
      .list(query())
      .then((res: R) => {
        const page = res.data || {};
        state.rows = page.records || [];
        state.page.total = Number(page.total || 0);
      })
      .catch(() => {
        // handled globally
      })
      .finally(() => {
        state.loading = false;
      });
  };

  const search = (params: Record<string, any>) => {
    Object.assign(searchForm, params || {});
    state.page.current = 1;
    load();
  };

  const reset = () => {
    resetSearchForm(searchForm, { keyword: '', filterValue: '' });
    state.page.current = 1;
    load();
  };

  const sort = () => {
    const currentOrder = state.page.orders[0];
    const asc = currentOrder ? !currentOrder.asc : true;
    state.page.orders = [{ column: 'create_time', asc }];
    load();
  };

  const sizeChange = (size: number) => {
    state.page.size = size;
    state.page.current = 1;
    load();
  };

  const currentChange = (current: number) => {
    state.page.current = current;
    load();
  };

  const assignForm = (value: Record<string, unknown>) => {
    Object.keys(formModel).forEach((key) => delete formModel[key]);
    Object.assign(formModel, value);
    activeConfig.value.fields
      .filter((field) => field.kind === 'json')
      .forEach((field) => {
        formModel[field.prop] = prettyJson(formModel[field.prop]);
      });
  };

  const openAdd = () => {
    editing.value = false;
    assignForm(activeConfig.value.defaultForm());
    formVisible.value = true;
  };

  const resetForm = () => {
    if (!editing.value) {
      assignForm(activeConfig.value.defaultForm());
    }
    formRef.value?.clearValidate();
  };

  const openEdit = (row: AlarmEntityRecord) => {
    editing.value = true;
    const value = activeConfig.value.defaultForm();
    value.id = row.id;
    if (row.version) value.version = row.version;
    activeConfig.value.fields.forEach((field) => {
      value[field.prop] = row[field.prop] ?? value[field.prop];
    });
    assignForm(value);
    formVisible.value = true;
  };

  const openDetail = (row: AlarmEntityRecord) => {
    detailRow.value = row;
    detailVisible.value = true;
  };

  const payload = () => {
    const result: Record<string, unknown> = {};
    if (formModel.id) result.id = formModel.id;
    if (formModel.version) result.version = formModel.version;
    activeConfig.value.fields.forEach((field) => {
      const value = formModel[field.prop];
      if (field.kind === 'json') {
        result[field.prop] = value ? JSON.parse(value) : undefined;
      } else if (field.kind === 'number') {
        result[field.prop] = value === '' || value == null ? undefined : Number(value);
      } else {
        result[field.prop] = value;
      }
    });
    return result;
  };

  const submit = () => {
    const addRequest = activeConfig.value.add;
    const updateRequest = activeConfig.value.update;
    if (!addRequest || !updateRequest) return;
    formRef.value?.validate((valid) => {
      if (!valid) return;
      let data: Record<string, unknown>;
      try {
        data = payload();
      } catch (error) {
        failMessage(t('settings.alarm.invalidJson'), undefined, error);
        return;
      }
      state.saving = true;
      const request = editing.value ? updateRequest(data) : addRequest(data);
      request
        .then(() => {
          successMessage();
          formVisible.value = false;
          load();
        })
        .catch(() => {
          // handled globally
        })
        .finally(() => {
          state.saving = false;
        });
    });
  };

  const remove = (id: string) => {
    const removeRequest = activeConfig.value.remove;
    if (!removeRequest) return;
    removeRequest(id)
      .then(() => {
        successMessage();
        load();
      })
      .catch(() => {
        // handled globally
      });
  };

  const formatTime = (value: unknown) => {
    if (!value) return '-';
    return timestamp(String(value)) || '-';
  };

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
    if (text === 'PENDING' || text === 'RETRYING' || text === 'RECOVERED' || prop === 'channelTypeFlag')
      return 'warning';
    return 'info';
  };

  const formatCell = (row: AlarmEntityRecord, column: ColumnConfig) => {
    const value = row[column.prop];
    if (column.kind === 'time') return formatTime(value);
    if (column.kind === 'tag') return enumLabel(value);
    if (value == null || value === '') return '-';
    return String(value);
  };

  const formatDetail = (prop: string) => {
    const value = detailRow.value?.[prop];
    const column = activeConfig.value.columns.find((item) => item.prop === prop);
    if (column?.kind === 'time' || prop.endsWith('Time')) return formatTime(value);
    if (column?.kind === 'tag') return enumLabel(value);
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
      stateExt: t('settings.alarm.stateExt'),
      requestExt: t('settings.alarm.requestExt'),
      responseExt: t('settings.alarm.responseExt'),
    };
    return map[prop] || prop;
  };

  const prettyJson = (value: unknown) => {
    if (value == null || value === '') return '{}';
    if (typeof value === 'string') {
      try {
        return JSON.stringify(JSON.parse(value), null, 2);
      } catch {
        return value;
      }
    }
    return JSON.stringify(value, null, 2);
  };

  watch(
    () => props.entity,
    () => {
      resetSearchForm(searchForm, { keyword: '', filterValue: '' });
      state.page.current = 1;
      detailVisible.value = false;
      formVisible.value = false;
      load();
    }
  );

  load();
</script>

<style lang="scss" scoped>
  @use '@/styles/things-dialog.scss';

  .alarm-notify {
    display: flex;
    flex-direction: column;
    gap: 8px;

    &__table {
      margin-top: 1px;
      border-radius: 4px;
    }

    &__inline-code {
      padding: 2px 5px;
      border-radius: 4px;
      color: var(--el-text-color-regular);
      background: var(--el-fill-color-light);
      font-size: 12px;
    }

    &__json-block {
      margin-top: 12px;

      pre {
        box-sizing: border-box;
        max-width: 100%;
        overflow: auto;
        margin: 6px 0 0;
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
    }

    &__json-title {
      font-size: 13px;
      font-weight: 600;
      color: var(--el-text-color-regular);
    }
  }
</style>
