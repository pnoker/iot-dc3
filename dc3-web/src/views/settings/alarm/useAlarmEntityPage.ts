/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import {ElMessageBox} from 'element-plus';
import type {FormInstance, FormItemRule, FormRules} from 'element-plus';
import {computed, getCurrentInstance, onUnmounted, reactive, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';

import type {
  AlarmEntity,
  Order,
  PageQuery,
  ResponsiveListCellKind,
  ResponsiveListColumn,
  ResponsiveListTagType,
} from '@/config/types';
import {timestampLabel} from '@/utils/dateUtil';
import {prettyJson} from '@/utils/jsonUtil';
import {successMessage} from '@/utils/notificationUtil';
import {cleanSearchParams, resetSearchForm} from '@/utils/searchParamUtil';

import {
  ALARM_DETAIL_ROUTE_MAP,
  type AlarmColumnConfig,
  type AlarmEntityConfig,
  type AlarmTabKey,
  createAlarmEntityConfigs,
} from './alarmEntityConfig';

/**
 * AlarmEntityPageProps data contract.
 */
export interface AlarmEntityPageProps {
  entity: AlarmTabKey;
}

/**
 * Fetch the use alarm entity page.
 * @param props - props entries
 * @returns the composable handle
 */
export const useAlarmEntityPage = (props: AlarmEntityPageProps) => {
  const {t} = useI18n();
  const router = useRouter();
  const {configs} = createAlarmEntityConfigs((key) => t(key));

  const formVisible = ref(false);
  const editing = ref(false);
  const formRef = ref<FormInstance>();
  const setFormRef = (instance: unknown) => {
    formRef.value = (instance || undefined) as FormInstance | undefined;
  };
  const formModel = reactive<Record<string, any>>({});
  const initialFormModel = ref('');
  let formSessionId = 0;
  let latestSaveId = 0;
  const searchForm = reactive<Record<string, any>>({
    keyword: '',
    filterValue: '',
  });

  const state = reactive({
    loading: false,
    saving: false,
    saveError: null as unknown | null,
    error: null as unknown | null,
    status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
    lastUpdated: null as number | null,
    rows: [] as AlarmEntity[],
    page: {
      total: 0,
      size: 12,
      current: 1,
      orders: [{column: 'createTime', asc: false}] as Order[],
    },
  });
  let latestLoadId = 0;
  const deletingIds = ref(new Set<string>());
  let disposed = false;

  const defaultConfig = configs[0] as AlarmEntityConfig;
  const activeConfig = computed<AlarmEntityConfig>(
    () => configs.find((config) => config.key === props.entity) || defaultConfig
  );
  const dialogTitle = computed(() =>
    editing.value ? `${t('common.edit')} ${activeConfig.value.label}` : `${t('common.add')} ${activeConfig.value.label}`
  );
  const formRules = computed<FormRules>(() => {
    const rules: FormRules = {};
    activeConfig.value.fields.forEach((field) => {
      const fieldRules: FormItemRule[] = [];
      if (field.required) {
        fieldRules.push({
          required: true,
          message: t('settings.alarm.required'),
          trigger: field.kind === 'select' || field.kind === 'remoteSelect' ? 'change' : 'blur',
        });
      }
      if (field.kind === 'json') {
        fieldRules.push({
          validator: (_rule, value, callback) => {
            if (!value) {
              callback();
              return;
            }
            try {
              JSON.parse(String(value));
              callback();
            } catch {
              callback(new Error(t('settings.alarm.invalidJson')));
            }
          },
          trigger: 'blur',
        });
      }
      if (fieldRules.length > 0) {
        rules[field.prop] = fieldRules;
      }
    });
    return rules;
  });

  const query = (config = activeConfig.value): PageQuery => {
    const params = cleanSearchParams(searchForm);
    const result: PageQuery = {
      offset: (state.page.current - 1) * state.page.size,
      limit: state.page.size,
      sort: state.page.orders.map((order) => ({field: order.column, direction: order.asc ? 'ASC' : 'DESC'})),
    };
    if (config.searchProp && params.keyword) {
      result[config.searchProp] = String(params.keyword).trim();
    }
    if (config.filterProp && params.filterValue) {
      result[config.filterProp] = params.filterValue;
    }
    return result;
  };

  const load = async () => {
    if (disposed) return;
    const loadId = ++latestLoadId;
    const config = activeConfig.value;
    state.loading = true;
    state.status = 'loading';
    state.error = null;
    try {
      const page = await config.list(query(config));
      if (loadId !== latestLoadId || config.key !== activeConfig.value.key) return;
      state.rows = page.items || [];
      state.page.total = page.total || 0;
      state.status = 'success';
      state.lastUpdated = Date.now();
    } catch (error) {
      if (loadId !== latestLoadId || config.key !== activeConfig.value.key) return;
      state.error = error;
      state.status = 'error';
    } finally {
      if (loadId === latestLoadId) {
        state.loading = false;
        if (state.status === 'loading') state.status = 'success';
      }
    }
  };

  const search = (params: Record<string, any>) => {
    Object.assign(searchForm, params || {});
    state.page.current = 1;
    load();
  };

  const reset = () => {
    resetSearchForm(searchForm, {keyword: '', filterValue: ''});
    state.page.current = 1;
    load();
  };

  const sort = () => {
    const currentOrder = state.page.orders[0];
    const asc = currentOrder ? !currentOrder.asc : true;
    state.page.orders = [{column: 'createTime', asc}];
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
        formModel[field.prop] = prettyJson(formModel[field.prop], '{}');
      });
  };

  const captureInitialForm = () => {
    initialFormModel.value = JSON.stringify(formModel);
    state.saveError = null;
  };

  const formDirty = computed(() => formVisible.value && JSON.stringify(formModel) !== initialFormModel.value);

  const beginFormSession = () => {
    formSessionId += 1;
    latestSaveId += 1;
    state.saving = false;
    state.saveError = null;
  };

  const openAdd = () => {
    beginFormSession();
    editing.value = false;
    assignForm(activeConfig.value.defaultForm());
    captureInitialForm();
    formVisible.value = true;
  };

  const resetForm = () => {
    assignForm(JSON.parse(initialFormModel.value || '{}') as Record<string, unknown>);
    state.saveError = null;
    formRef.value?.clearValidate();
  };

  const openEdit = (row: AlarmEntity) => {
    beginFormSession();
    editing.value = true;
    const value = activeConfig.value.defaultForm();
    value.id = row.id;
    if (row.version !== undefined && row.version !== null) value.version = row.version;
    activeConfig.value.fields.forEach((field) => {
      value[field.prop] = row[field.prop] ?? value[field.prop];
    });
    assignForm(value);
    captureInitialForm();
    formVisible.value = true;
  };

  const finishCloseForm = (done?: () => void) => {
    formSessionId += 1;
    latestSaveId += 1;
    state.saving = false;
    state.saveError = null;
    if (done) {
      done();
      return;
    }
    formVisible.value = false;
  };

  const requestCloseForm = async (done?: () => void) => {
    if (state.saving) return;
    const sessionId = formSessionId;
    if (!formDirty.value) {
      finishCloseForm(done);
      return;
    }
    try {
      await ElMessageBox.confirm(t('common.discardConfirm'), {
        confirmButtonText: t('common.confirm'),
        cancelButtonText: t('common.cancel'),
        type: 'warning',
      });
      if (disposed || sessionId !== formSessionId || !formVisible.value) return;
      finishCloseForm(done);
    } catch {
      // Keep the draft open when the user cancels the confirmation.
    }
  };

  const openDetail = (row: AlarmEntity) => {
    router.push({name: ALARM_DETAIL_ROUTE_MAP[activeConfig.value.key], query: {id: String(row.id)}}).catch(() => {
      // handled globally
    });
  };

  const payload = () => {
    const result: Record<string, unknown> = {};
    if (formModel.id) result.id = formModel.id;
    if (formModel.version !== undefined && formModel.version !== null) result.version = formModel.version;
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

  const submit = async () => {
    if (state.saving) return;
    const addRequest = activeConfig.value.add;
    const updateRequest = activeConfig.value.update;
    if (!addRequest || !updateRequest) return;
    const sessionId = formSessionId;
    state.saving = true;
    state.saveError = null;
    const valid = await formRef.value?.validate().catch(() => false);
    if (disposed || sessionId !== formSessionId || !formVisible.value) {
      if (sessionId === formSessionId) state.saving = false;
      return;
    }
    if (!valid) {
      state.saving = false;
      return;
    }
    let data: Record<string, unknown>;
    try {
      data = payload();
    } catch {
      await formRef.value?.validate().catch(() => undefined);
      if (sessionId === formSessionId) state.saving = false;
      return;
    }
    const saveId = ++latestSaveId;
    try {
      await (editing.value ? updateRequest(data) : addRequest(data));
      if (saveId !== latestSaveId || sessionId !== formSessionId) return;
      successMessage();
      captureInitialForm();
      state.saving = false;
      finishCloseForm();
      await load();
    } catch (error) {
      if (saveId === latestSaveId && sessionId === formSessionId) state.saveError = error;
    } finally {
      if (saveId === latestSaveId && sessionId === formSessionId) state.saving = false;
    }
  };

  const remove = async (row: AlarmEntity) => {
    const removeRequest = activeConfig.value.remove;
    const id = String(row.id || '');
    if (!removeRequest || !id || deletingIds.value.has(id)) return;
    deletingIds.value.add(id);
    try {
      await removeRequest(id);
      if (disposed) return;
      successMessage();
      await load();
    } catch {
      // handled globally
    } finally {
      deletingIds.value.delete(id);
    }
  };

  const isDeleting = (row: AlarmEntity) => deletingIds.value.has(String(row.id));

  if (getCurrentInstance()) {
    onUnmounted(() => {
      disposed = true;
      latestLoadId += 1;
      latestSaveId += 1;
      formSessionId += 1;
      deletingIds.value.clear();
    });
  }

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

  const tagType = (value: unknown, prop: string): ResponsiveListTagType => {
    const text = String(value || '');
    if (text === 'ENABLE' || text === 'SUCCESS' || text === 'NORMAL' || text === 'AUTO') return 'success';
    if (text === 'DISABLE' || text === 'FAILED' || text === 'FIRING') return 'danger';
    if (text === 'PENDING' || text === 'RETRYING' || text === 'RECOVERED' || prop === 'channelTypeFlag')
      return 'warning';
    return 'info';
  };

  const formatCell = (row: AlarmEntity, column: AlarmColumnConfig) => {
    const value = row[column.prop];
    if (column.kind === 'time') return timestampLabel(value);
    if (column.kind === 'tag') return enumLabel(value);
    if (value == null || value === '') return '-';
    return String(value);
  };

  const columns = computed<ResponsiveListColumn<AlarmEntity>[]>(() =>
    activeConfig.value.columns.map((column) => ({
      key: column.prop,
      prop: column.prop,
      label: column.label,
      kind: (column.kind === 'json' ? 'text' : column.kind || 'text') as ResponsiveListCellKind,
      width: column.width,
      minWidth: column.minWidth,
      fixed: column.fixed,
      overflow: column.overflow,
      mobile: column.mobile,
      formatter: (row) => formatCell(row, column),
      tagType: (row) => tagType(row[column.prop], column.prop),
    }))
  );

  watch(
    () => props.entity,
    () => {
      beginFormSession();
      resetSearchForm(searchForm, {keyword: '', filterValue: ''});
      state.page.current = 1;
      formVisible.value = false;
      void load();
    }
  );

  void load();

  return {
    t,
    formVisible,
    setFormRef,
    formModel,
    searchForm,
    state,
    activeConfig,
    columns,
    dialogTitle,
    formRules,
    formDirty,
    load,
    search,
    reset,
    sort,
    sizeChange,
    currentChange,
    openAdd,
    resetForm,
    openEdit,
    requestCloseForm,
    openDetail,
    submit,
    remove,
    isDeleting,
    tagType,
    formatCell,
  };
};
