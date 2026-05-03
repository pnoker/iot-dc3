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

import { computed, defineComponent, reactive, ref } from 'vue';
import type { PropType } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useI18n } from 'vue-i18n';

type FormMode = 'add' | 'edit';

// Ordered so the Parent picker always shows the same type ordering across
// tenants — missing types are skipped in the computed.
const TYPE_ORDER = ['MENU', 'API', 'DATA', 'DEVICE', 'POINT', 'PROFILE', 'DRIVER'];

// Flatten the backend resource tree so we can regroup by type.
const flattenTree = (nodes: any[]): any[] => {
  const out: any[] = [];
  const walk = (ns: any[]) => {
    for (const n of ns || []) {
      out.push(n);
      if (n.children) walk(n.children);
    }
  };
  walk(nodes || []);
  return out;
};

const createEmptyForm = () => ({
  id: '' as string,
  parentResourceId: 0 as number | string,
  resourceName: '',
  resourceCode: '',
  resourceTypeFlag: '' as string,
  entityId: '' as string | number,
  enableFlag: 'ENABLE' as string,
  remark: '',
});

export default defineComponent({
  name: 'ResourceEditForm',
  props: {
    treeData: {
      type: Array as PropType<any[]>,
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
      resourceName: [{ required: true, message: t('settings.resource.resourceNamePlaceholder'), trigger: 'blur' }],
      parentResourceId: [
        { required: true, message: t('settings.resource.parentResourceIdPlaceholder'), trigger: 'change' },
      ],
      entityId: [{ required: true, message: t('settings.resource.entityIdPlaceholder'), trigger: 'blur' }],
    };

    // Parent picker layout: a virtual "Root" (id=0) that commits top-level,
    // plus one disabled group node per resource type with that type's
    // resources nested underneath. Mixing all 7 types into one flat tree
    // made it impossible to tell MENU/API/DATA apart — grouping gives the
    // same structure the role-assign dialog uses.
    //
    // Cross-type parent/child links in the source tree are dropped: a DATA
    // node parented to a MENU becomes a root inside the DATA group. That
    // matches the role-assign behavior and keeps each group's hierarchy
    // to same-type nodes only.
    const parentTreeOptions = computed(() => {
      const flat = flattenTree(props.treeData || []);
      const buckets: Record<string, any[]> = {};
      for (const n of flat) {
        const type = String(n.resourceTypeFlag || 'OTHER');
        if (!buckets[type]) buckets[type] = [];
        buckets[type].push({
          id: n.id,
          parentResourceId: n.parentResourceId,
          resourceName: n.resourceName,
          resourceCode: n.resourceCode,
          resourceTypeFlag: type,
          children: [],
        });
      }
      const treesByType: Record<string, any[]> = {};
      for (const [type, nodes] of Object.entries(buckets)) {
        const byId = new Map<string, any>();
        for (const n of nodes) byId.set(String(n.id), n);
        const roots: any[] = [];
        for (const n of nodes) {
          const pid = n.parentResourceId != null ? String(n.parentResourceId) : null;
          const parent = pid && byId.get(pid);
          if (parent) parent.children.push(n);
          else roots.push(n);
        }
        treesByType[type] = roots;
      }
      const present = Object.keys(treesByType);
      const ordered = TYPE_ORDER.filter((x) => present.includes(x));
      for (const x of present) if (!ordered.includes(x)) ordered.push(x);

      const groups = ordered.map((type) => ({
        // Prefix the id so it can't collide with a real BIGINT resource id
        // coming back from the server. Disabled marks it unselectable — the
        // user only picks real resources underneath.
        id: `__group_${type}`,
        resourceName: type,
        disabled: true,
        children: treesByType[type],
      }));
      return [{ id: 0, resourceName: t('settings.resource.rootResource') }, ...groups];
    });

    // Auto-expand all group nodes so resources surface on open. Users
    // otherwise see a list of seven type folders and have to click each.
    const defaultExpandedKeys = computed(() => {
      return (parentTreeOptions.value || [])
        .filter((n: any) => typeof n.id === 'string' && n.id.startsWith('__group_'))
        .map((n: any) => n.id);
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

    const showEdit = (row: any) => {
      reactiveData.mode = 'edit';
      const initial = {
        ...createEmptyForm(),
        ...row,
        parentResourceId: row?.parentResourceId ?? 0,
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
      if (!payload.parentResourceId) {
        payload.parentResourceId = 0;
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
      defaultExpandedKeys,
      reset,
      show,
      showEdit,
      submit,
    };
  },
});
