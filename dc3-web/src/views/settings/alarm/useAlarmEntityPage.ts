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

import type {FormInstance, FormItemRule, FormRules} from 'element-plus';
import {computed, reactive, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';

import type {AlarmEntity, Order, PageQuery} from '@/config/types';
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

export interface AlarmEntityPageProps {
  entity: AlarmTabKey;
}

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
  const searchForm = reactive<Record<string, any>>({
    keyword: '',
    filterValue: '',
  });

  const state = reactive({
    loading: false,
    saving: false,
    rows: [] as AlarmEntity[],
    page: {
      total: 0,
      size: 12,
      current: 1,
      orders: [{column: 'create_time', asc: false}] as Order[],
    },
  });

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
          trigger: field.kind === 'select' ? 'change' : 'blur',
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
    resetSearchForm(searchForm, {keyword: '', filterValue: ''});
    state.page.current = 1;
    load();
  };

  const sort = () => {
    const currentOrder = state.page.orders[0];
    const asc = currentOrder ? !currentOrder.asc : true;
    state.page.orders = [{column: 'create_time', asc}];
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

  const openEdit = (row: AlarmEntity) => {
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

  const openDetail = (row: AlarmEntity) => {
    router.push({name: ALARM_DETAIL_ROUTE_MAP[activeConfig.value.key], query: {id: String(row.id)}}).catch(() => {
      // handled globally
    });
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
      } catch {
        formRef.value?.validate().catch(() => undefined);
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

  const formatCell = (row: AlarmEntity, column: AlarmColumnConfig) => {
    const value = row[column.prop];
    if (column.kind === 'time') return timestampLabel(value);
    if (column.kind === 'tag') return enumLabel(value);
    if (value == null || value === '') return '-';
    return String(value);
  };

  watch(
    () => props.entity,
    () => {
      resetSearchForm(searchForm, {keyword: '', filterValue: ''});
      state.page.current = 1;
      formVisible.value = false;
      load();
    }
  );

  load();

  return {
    t,
    formVisible,
    setFormRef,
    formModel,
    searchForm,
    state,
    activeConfig,
    dialogTitle,
    formRules,
    load,
    search,
    reset,
    sort,
    sizeChange,
    currentChange,
    openAdd,
    resetForm,
    openEdit,
    openDetail,
    submit,
    remove,
    tagType,
    formatCell,
  };
};
