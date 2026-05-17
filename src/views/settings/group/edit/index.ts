/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type { PropType } from 'vue';
import { computed, defineComponent, reactive, ref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useI18n } from 'vue-i18n';

import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import { ENTITY_TYPE_OPTIONS } from '@/config/constant/enums';
import type { GroupRecord } from '@/config/types/manager';

type FormMode = 'add' | 'edit';

const createEmptyForm = () => ({
  id: '' as string,
  parentGroupId: null as number | string | null,
  groupTypeFlag: 'DEVICE' as string,
  groupName: '',
  groupCode: '',
  groupIndex: 0 as number,
  enableFlag: 'ENABLE' as string,
  remark: '',
});

const buildTree = (rows: GroupRecord[]): GroupRecord[] => {
  const byId = new Map<string, GroupRecord>();
  rows.forEach((row) => byId.set(String(row.id), { ...row, children: [] }));

  const roots: GroupRecord[] = [];
  byId.forEach((node) => {
    const parentId = node.parentGroupId ? String(node.parentGroupId) : '';
    const parent = parentId ? byId.get(parentId) : undefined;
    if (parent) {
      parent.children = parent.children || [];
      parent.children.push(node);
    } else {
      roots.push(node);
    }
  });

  const sort = (nodes: GroupRecord[]) => {
    nodes.sort((a, b) => Number(a.groupIndex ?? 0) - Number(b.groupIndex ?? 0));
    nodes.forEach((node) => sort(node.children || []));
  };
  sort(roots);
  return roots;
};

export default defineComponent({
  name: 'GroupEditForm',
  components: { EnableFlagSegmented },
  props: {
    treeData: {
      type: Array as PropType<GroupRecord[]>,
      default: () => [],
    },
  },
  emits: ['add-thing', 'update-thing'],
  setup(props, { emit }) {
    const { t } = useI18n();

    const formRef = ref<FormInstance>();

    const reactiveData = reactive({
      visible: false,
      mode: 'add' as FormMode,
      submitting: false,
      form: createEmptyForm(),
      originalForm: createEmptyForm(),
    });

    const rules: FormRules = {
      groupTypeFlag: [{ required: true, message: t('settings.common.entityTypePlaceholder'), trigger: 'change' }],
      groupName: [{ required: true, message: t('settings.group.groupNamePlaceholder'), trigger: 'blur' }],
    };

    const excludedIds = computed(() => {
      const currentId = String(reactiveData.form.id || '');
      const ids = new Set<string>();
      if (!currentId) return ids;

      const childrenByParent = new Map<string, GroupRecord[]>();
      props.treeData.forEach((row) => {
        const parentId = row.parentGroupId ? String(row.parentGroupId) : '';
        if (!childrenByParent.has(parentId)) childrenByParent.set(parentId, []);
        childrenByParent.get(parentId)?.push(row);
      });

      const visit = (id: string) => {
        ids.add(id);
        (childrenByParent.get(id) || []).forEach((child) => visit(String(child.id)));
      };
      visit(currentId);
      return ids;
    });

    const parentTreeOptions = computed(() => {
      const rows = props.treeData.filter((row) => {
        return row.groupTypeFlag === reactiveData.form.groupTypeFlag && !excludedIds.value.has(String(row.id));
      });
      return [{ id: 0, groupName: t('settings.group.rootGroup'), children: buildTree(rows) }];
    });

    const reset = () => {
      reactiveData.form = reactiveData.mode === 'edit' ? { ...reactiveData.originalForm } : createEmptyForm();
      reactiveData.submitting = false;
      formRef.value?.clearValidate();
    };

    const show = () => {
      reactiveData.mode = 'add';
      reactiveData.originalForm = createEmptyForm();
      reactiveData.form = createEmptyForm();
      reactiveData.visible = true;
    };

    const showEdit = (row: GroupRecord) => {
      reactiveData.mode = 'edit';
      const initial = {
        ...createEmptyForm(),
        ...row,
        parentGroupId: row?.parentGroupId ?? null,
        groupIndex: Number(row?.groupIndex ?? 0),
      };
      reactiveData.originalForm = { ...initial };
      reactiveData.form = { ...initial };
      reactiveData.visible = true;
    };

    const done = () => {
      reactiveData.submitting = false;
      reactiveData.visible = false;
    };

    const submit = async () => {
      const valid = await formRef.value?.validate().catch(() => false);
      if (!valid) return;
      reactiveData.submitting = true;
      const payload = { ...reactiveData.form };
      if (!payload.parentGroupId || String(payload.parentGroupId) === '0') {
        payload.parentGroupId = null;
      }
      if (reactiveData.mode === 'add') {
        emit('add-thing', payload, done);
      } else {
        emit('update-thing', payload, done);
      }
    };

    return {
      t,
      formRef,
      reactiveData,
      rules,
      parentTreeOptions,
      ENTITY_TYPE_OPTIONS,
      reset,
      show,
      showEdit,
      submit,
    };
  },
});
