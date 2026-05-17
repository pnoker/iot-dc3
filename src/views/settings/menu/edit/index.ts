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
import { MENU_LEVEL_OPTIONS, MENU_TYPE_OPTIONS } from '@/config/constant/enums';
import { iconMap, iconNames, resolveIcon } from '@/config/constant/icons';

type FormMode = 'add' | 'edit';

const createEmptyForm = () => ({
  id: '' as string,
  parentMenuId: 0 as number | string,
  menuName: '',
  menuCode: '',
  menuTypeFlag: 'COMMON' as string,
  menuLevel: 'C1' as string,
  menuIndex: 0 as number,
  titleZh: '',
  titleEn: '',
  icon: '',
  url: '',
  enableFlag: 'ENABLE' as string,
  remark: '',
});

export default defineComponent({
  name: 'MenuEditForm',
  components: { EnableFlagSegmented },
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
      menuName: [{ required: true, message: t('settings.menu.menuNamePlaceholder'), trigger: 'blur' }],
      menuCode: [{ required: true, message: t('settings.menu.menuCodePlaceholder'), trigger: 'blur' }],
      titleZh: [{ required: true, message: t('settings.menu.titleZhPlaceholder'), trigger: 'blur' }],
      titleEn: [{ required: true, message: t('settings.menu.titleEnPlaceholder'), trigger: 'blur' }],
      menuTypeFlag: [{ required: true, trigger: 'change' }],
      menuLevel: [{ required: true, trigger: 'change' }],
    };

    const parentTreeOptions = computed(() => [{ id: 0, menuName: 'Root', children: props.treeData || [] }]);

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
      const content = row?.menuExt?.content || {};
      const titles = content.titles || {};
      // Legacy rows may still carry content.title (an i18n key like
      // "nav.home"). Keep the form usable on old data by falling back to
      // the resolved i18n text, then to menuName, so editors never face a
      // blank title input.
      const legacyTitle =
        typeof content.title === 'string' && content.title ? t(content.title, content.title) : row?.menuName || '';
      const initial = {
        ...createEmptyForm(),
        ...row,
        parentMenuId: row?.parentMenuId ?? 0,
        menuIndex: row?.menuIndex ?? 0,
        titleZh: titles.zh || legacyTitle,
        titleEn: titles.en || legacyTitle,
        icon: content.icon || '',
        url: content.url || '',
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
      const { icon, url, titleZh, titleEn, ...rest } = reactiveData.form;
      const payload: Record<string, any> = {
        ...rest,
        menuExt: {
          content: {
            // Authoritative locale → display-name map. Picked up by the
            // sidebar / breadcrumb / header via content.titles[currentLocale]
            // with fallback to content.titles.en.
            titles: {
              zh: titleZh,
              en: titleEn,
            },
            icon: icon || '',
            url: url || '',
            remark: rest.remark || '',
          },
        },
      };
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
      MENU_TYPE_OPTIONS,
      MENU_LEVEL_OPTIONS,
      iconNames,
      iconMap,
      resolveIcon,
      reset,
      show,
      showEdit,
      submit,
    };
  },
});
