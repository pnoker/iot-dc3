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
import {computed, reactive, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';

import type {Order, PageQuery} from '@/config/types';
import type {EntityColumnConfig, EntityListConfig, EntityOption} from '@/config/types/entityList';
import {timestampLabel} from '@/utils/dateUtil';
import {prettyJson} from '@/utils/jsonUtil';
import {successMessage} from '@/utils/notificationUtil';
import {cleanSearchParams, resetSearchForm} from '@/utils/searchParamUtil';

export const useEntityListPage = (rawConfig: EntityListConfig) => {
  const {t} = useI18n();
  const router = useRouter();

  const config = ref(rawConfig);

  const formVisible = ref(false);
  const editing = ref(false);
  const formRef = ref<FormInstance>();
  const setFormRef = (instance: unknown) => {
    formRef.value = (instance || undefined) as FormInstance | undefined;
  };
  const formModel = reactive<Record<string, any>>({});

  const defaultSearchForm = (): Record<string, any> => {
    const form: Record<string, any> = {};
    config.value.searchFields.forEach((field) => {
      form[field.prop] = field.multiple ? [] : '';
    });
    return form;
  };

  const searchForm = reactive<Record<string, any>>(defaultSearchForm());

  const relations = reactive<Record<string, Record<string, string>>>({});

  const state = reactive({
    loading: false,
    saving: false,
    rows: [] as Record<string, any>[],
    page: {
      total: 0,
      size: config.value.pageSize || 12,
      current: 1,
      orders: [{column: config.value.defaultOrderColumn || 'create_time', asc: false}] as Order[],
    },
  });

  const dialogTitle = computed(() => {
    const entity = config.value.title || config.value.name;
    return editing.value ? `${t('common.edit')} ${entity}` : `${t('common.add')} ${entity}`;
  });

  const formRules = computed<FormRules>(() => {
    const rules: FormRules = {};
    config.value.fields.forEach((field) => {
      const fieldRules: FormItemRule[] = [];
      if (field.required) {
        fieldRules.push({
          required: true,
          message: t('common.required'),
          trigger: field.kind === 'select' || field.kind === 'treeSelect' ? 'change' : 'blur',
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
              callback(new Error(t('common.invalidJson')));
            }
          },
          trigger: 'blur',
        });
      }
      if (field.rules && field.rules.length > 0) {
        fieldRules.push(...field.rules);
      }
      if (fieldRules.length > 0) {
        rules[field.prop] = fieldRules;
      }
    });
    return rules;
  });

  const query = (): PageQuery => {
    const params = cleanSearchParams(searchForm);
    const result: PageQuery = {};
    config.value.searchFields.forEach((sf) => {
      const value = params[sf.prop];
      if (value === undefined || value === null) return;
      if (sf.multiple && Array.isArray(value) && value.length === 0) return;
      result[sf.prop] = value;
    });
    if (config.value.mode !== 'tree') {
      result.page = {
        current: state.page.current,
        size: state.page.size,
        orders: state.page.orders,
      };
    }
    return result;
  };

  // Depth-first flatten of a tree (over `children`) so relation loaders can
  // resolve names for every node, not just the roots.
  const flattenRows = (rows: Record<string, any>[]): Record<string, any>[] => {
    const out: Record<string, any>[] = [];
    const walk = (nodes: Record<string, any>[]) => {
      for (const node of nodes || []) {
        out.push(node);
        if (node.children && node.children.length > 0) walk(node.children);
      }
    };
    walk(rows || []);
    return out;
  };

  const loadRelations = async (rows: Record<string, any>[]): Promise<void> => {
    if (!config.value.relations || config.value.relations.length === 0) return;
    await Promise.all(
      config.value.relations.map((r) =>
        r.load(rows).then((result) => {
          relations[r.key] = result;
        })
      )
    );
  };

  const load = () => {
    state.loading = true;
    config.value
      .list(query())
      .then((res: R) => {
        if (config.value.mode === 'tree') {
          state.rows = (res.data as Record<string, any>[]) || [];
        } else {
          const page = res.data || {};
          state.rows = page.records || [];
          state.page.total = Number(page.total || 0);
        }
        // Relations resolve after rows arrive so loaders can act on them.
        return loadRelations(config.value.mode === 'tree' ? flattenRows(state.rows) : state.rows);
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
    resetSearchForm(searchForm, defaultSearchForm());
    state.page.current = 1;
    load();
  };

  const sort = () => {
    const currentOrder = state.page.orders[0];
    const asc = currentOrder ? !currentOrder.asc : true;
    state.page.orders = [{column: config.value.defaultOrderColumn || 'create_time', asc}];
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
    config.value.fields
      .filter((field) => field.kind === 'json')
      .forEach((field) => {
        formModel[field.prop] = prettyJson(formModel[field.prop], '{}');
      });
  };

  const openAdd = () => {
    editing.value = false;
    assignForm(config.value.defaultForm());
    formVisible.value = true;
  };

  const resetForm = () => {
    if (!editing.value) {
      assignForm(config.value.defaultForm());
    }
    formRef.value?.clearValidate();
  };

  const openEdit = (row: Record<string, any>) => {
    editing.value = true;
    const value = config.value.defaultForm();
    value.id = row.id;
    if (row.version) value.version = row.version;
    config.value.fields.forEach((field) => {
      value[field.prop] = row[field.prop] ?? value[field.prop];
    });
    Object.assign(value, config.value.fromRow?.(row) || {});
    assignForm(value);
    formVisible.value = true;
  };

  const openDetail = (row: Record<string, any>) => {
    if (!config.value.detail) return;
    router.push({name: config.value.detail.routeName, query: {id: String(row.id)}}).catch(() => {
      // handled globally
    });
  };

  const payload = () => {
    if (config.value.toPayload) return config.value.toPayload(formModel);
    const result: Record<string, unknown> = {};
    if (formModel.id) result.id = formModel.id;
    if (formModel.version) result.version = formModel.version;
    config.value.fields.forEach((field) => {
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
    const addRequest = config.value.add;
    const updateRequest = config.value.update;
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
    const removeRequest = config.value.remove;
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

  const optionLabel = (options: EntityOption[] | undefined, value: unknown) => {
    const text = String(value ?? '');
    const hit = options?.find((o) => String(o.value) === text);
    return hit ? hit.label : text || '-';
  };

  const tagType = (value: unknown) => {
    const text = String(value ?? '');
    if (
      text === 'ENABLE' ||
      text === 'SUCCESS' ||
      text === 'NORMAL' ||
      text === 'AUTO' ||
      text === 'LOW' ||
      text === 'ACTIVE'
    )
      return 'success';
    if (
      text === 'DISABLE' ||
      text === 'FAILED' ||
      text === 'FAILURE' ||
      text === 'ERROR' ||
      text === 'DENIED' ||
      text === 'FIRING' ||
      text === 'HIGH'
    )
      return 'danger';
    if (text === 'PENDING' || text === 'RETRYING' || text === 'RECOVERED' || text === 'MEDIUM' || text === 'SUSPENDED')
      return 'warning';
    return 'info';
  };

  const formatCell = (row: Record<string, any>, column: EntityColumnConfig) => {
    const value = column.prop.split('.').reduce((obj: any, key) => (obj != null ? obj[key] : undefined), row);
    if (column.formatter) return column.formatter(row, {t, relations});
    if (column.kind === 'time') return timestampLabel(value);
    if (column.kind === 'tag') return optionLabel(column.options, value);
    if (value == null || value === '') return '-';
    return String(value);
  };

  const canEdit = (row: Record<string, any>) => (config.value.rowEditable ? config.value.rowEditable(row) : true);

  const canDelete = (row: Record<string, any>) => (config.value.rowDeletable ? config.value.rowDeletable(row) : true);

  load();

  return {
    t,
    config,
    state,
    searchForm,
    formVisible,
    editing,
    setFormRef,
    formModel,
    formRules,
    dialogTitle,
    relations,
    load,
    search,
    reset,
    sort,
    sizeChange,
    currentChange,
    openAdd,
    openEdit,
    openDetail,
    resetForm,
    submit,
    remove,
    formatCell,
    tagType,
    optionLabel,
    canEdit,
    canDelete,
  };
};
