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
import {computed, getCurrentInstance, onUnmounted, reactive, ref} from 'vue';
import {useI18n} from 'vue-i18n';
import {useRouter} from 'vue-router';

import type {CursorPageResult, Order, PageQuery, SortSpec, PageResult} from '@/config/types';
import type {EntityColumnConfig, EntityListConfig, EntityOption} from '@/config/types/entityList';
import {ENUM_TAG_TYPE_MAP} from '@/config/constant/enums';
import {timestampLabel} from '@/utils/dateUtil';
import {prettyJson} from '@/utils/jsonUtil';
import {logger} from '@/utils/log';
import {successMessage} from '@/utils/notificationUtil';
import {cleanSearchParams, resetSearchForm} from '@/utils/searchParamUtil';

/**
 * Reusable entity-list page state: search form, pagination, and dialog CRUD wiring
 * driven by an {@link EntityListConfig}.
 *
 * @param rawConfig column/search/dialog configuration of the page
 * @returns the composable handle
 */
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
  const initialFormModel = ref('');
  let formSessionId = 0;
  let latestSaveId = 0;

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
    saveError: null as unknown | null,
    error: null as unknown | null,
    status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
    lastUpdated: null as number | null,
    rows: [] as Record<string, any>[],
    page: {
      total: 0,
      size: config.value.pageSize || 12,
      current: 1,
      hasNext: false,
      orders: [{column: config.value.defaultOrderColumn || 'create_time', asc: false}] as Order[],
    },
  });
  let latestLoadId = 0;
  const cursorStack: Array<string | undefined> = [undefined];
  const removingIds = reactive(new Set<string>());
  let disposed = false;

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
    if (config.value.pagination === 'cursor') {
      result.cursor = cursorStack[state.page.current - 1];
      result.limit = state.page.size;
    } else if (config.value.mode !== 'tree') {
      result.offset = (state.page.current - 1) * state.page.size;
      result.limit = state.page.size;
      result.sort = state.page.orders.map((order): SortSpec => ({
        field: order.column,
        direction: order.asc ? 'ASC' : 'DESC',
      }));
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

  const loadRelations = async (
    rows: Record<string, any>[]
  ): Promise<Array<{key: string; value: Record<string, string>}>> => {
    if (!config.value.relations || config.value.relations.length === 0) return [];
    return Promise.all(
      config.value.relations.map(async (relation) => ({
        key: relation.key,
        value: await relation.load(rows),
      }))
    );
  };

  const load = () => {
    if (disposed) return Promise.resolve();
    const loadId = ++latestLoadId;
    state.loading = true;
    state.status = 'loading';
    state.error = null;
    return config.value
      .list(query())
      .then(async (data: PageResult<Record<string, any>> | CursorPageResult<Record<string, any>> | Record<string, any>[]) => {
        if (loadId !== latestLoadId) return;
        if (config.value.mode === 'tree') {
          state.rows = (data as unknown as Record<string, any>[]) || [];
        } else {
          const page = data as PageResult<Record<string, any>>;
          state.rows = page.items || [];
          if (config.value.pagination === 'cursor') {
            const cursorPage = data as CursorPageResult<Record<string, any>>;
            state.page.hasNext = cursorPage.hasNext;
            cursorStack[state.page.current] = cursorPage.nextCursor ?? undefined;
          } else {
            state.page.total = page.total;
            state.page.hasNext = page.hasNext;
          }
        }
        const relationValues = await loadRelations(
          config.value.mode === 'tree' ? flattenRows(state.rows) : state.rows
        );
        if (loadId !== latestLoadId) return;
        relationValues.forEach(({key, value}) => {
          relations[key] = value;
        });
        state.status = 'success';
        state.lastUpdated = Date.now();
      })
      .catch((error: unknown) => {
        if (loadId !== latestLoadId) return;
        state.error = error;
        state.status = 'error';
      })
      .finally(() => {
        if (loadId === latestLoadId) {
          state.loading = false;
          if (state.status === 'loading') state.status = 'success';
        }
      });
  };

  const search = (params: Record<string, any>) => {
    Object.assign(searchForm, params || {});
    state.page.current = 1;
    cursorStack.splice(0, cursorStack.length, undefined);
    state.page.hasNext = false;
    load();
  };

  const reset = () => {
    resetSearchForm(searchForm, defaultSearchForm());
    state.page.current = 1;
    cursorStack.splice(0, cursorStack.length, undefined);
    state.page.hasNext = false;
    load();
  };

  const sort = () => {
    const currentOrder = state.page.orders[0];
    const asc = currentOrder ? !currentOrder.asc : true;
    state.page.orders = [{column: config.value.defaultOrderColumn || 'create_time', asc}];
    cursorStack.splice(0, cursorStack.length, undefined);
    state.page.current = 1;
    load();
  };

  const sizeChange = (size: number) => {
    state.page.size = size;
    state.page.current = 1;
    cursorStack.splice(0, cursorStack.length, undefined);
    state.page.hasNext = false;
    load();
  };

  const currentChange = (current: number) => {
    if (config.value.pagination === 'cursor') return;
    state.page.current = current;
    load();
  };

  const cursorNext = () => {
    if (config.value.pagination !== 'cursor' || !state.page.hasNext) return;
    state.page.current += 1;
    load();
  };

  const cursorPrevious = () => {
    if (config.value.pagination !== 'cursor' || state.page.current <= 1) return;
    state.page.current -= 1;
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
    assignForm(config.value.defaultForm());
    captureInitialForm();
    formVisible.value = true;
  };

  const resetForm = () => {
    assignForm(JSON.parse(initialFormModel.value || '{}') as Record<string, unknown>);
    state.saveError = null;
    formRef.value?.clearValidate();
  };

  const openEdit = (row: Record<string, any>) => {
    beginFormSession();
    editing.value = true;
    const value = config.value.defaultForm();
    value.id = row.id;
    if (row.version !== undefined && row.version !== null) value.version = row.version;
    config.value.fields.forEach((field) => {
      value[field.prop] = row[field.prop] ?? value[field.prop];
    });
    Object.assign(value, config.value.fromRow?.(row) || {});
    assignForm(value);
    captureInitialForm();
    formVisible.value = true;
  };

  const finishCloseForm = (done?: () => void) => {
    formSessionId += 1;
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
    if (formModel.version !== undefined && formModel.version !== null) result.version = formModel.version;
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

  const submit = async () => {
    if (state.saving) return;
    const addRequest = config.value.add;
    const updateRequest = config.value.update;
    if (!addRequest || !updateRequest) {
      logger.warn('Entity list action not configured', {add: Boolean(addRequest), update: Boolean(updateRequest)});
      return;
    }
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
      if (saveId === latestSaveId && sessionId === formSessionId) {
        state.saveError = error;
      }
    } finally {
      if (saveId === latestSaveId && sessionId === formSessionId) {
        state.saving = false;
      }
    }
  };

  const isRemoving = (id: string) => removingIds.has(String(id));

  const remove = async (id: string) => {
    const removeRequest = config.value.remove;
    const key = String(id);
    if (!removeRequest || removingIds.has(key)) return;
    removingIds.add(key);
    try {
      await removeRequest(id);
      if (disposed) return;
      successMessage();
      await load();
    } catch {
      // handled globally
    } finally {
      removingIds.delete(key);
    }
  };

  const optionLabel = (options: EntityOption[] | undefined, value: unknown) => {
    const text = String(value ?? '');
    const hit = options?.find((o) => String(o.value) === text);
    return hit ? hit.label : text || '-';
  };

  const tagType = (value: unknown) => {
    const text = String(value ?? '');
    return ENUM_TAG_TYPE_MAP[text] || 'info';
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

  if (getCurrentInstance()) {
    onUnmounted(() => {
      disposed = true;
      latestLoadId += 1;
      latestSaveId += 1;
      formSessionId += 1;
      removingIds.clear();
    });
  }

  load();

  return {
    t,
    config,
    state,
    searchForm,
    formVisible,
    formDirty,
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
    cursorNext,
    cursorPrevious,
    openAdd,
    openEdit,
    requestCloseForm,
    openDetail,
    resetForm,
    submit,
    remove,
    formatCell,
    tagType,
    optionLabel,
    canEdit,
    canDelete,
    isRemoving,
  };
};
