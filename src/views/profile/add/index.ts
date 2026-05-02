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

import { defineComponent, reactive, ref, unref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { useI18n } from 'vue-i18n';

import { successMessage } from '@/utils/NotificationUtil';

export default defineComponent({
  name: 'ProfileAddForm',
  emits: ['add-thing'],
  setup(_props, { emit }) {
    const { t } = useI18n();

    // 定义表单引用
    const formDataRef = ref<FormInstance>();

    // 定义表单校验规则
    const formRule = reactive<FormRules>({
      profileName: [
        {
          required: true,
          message: t('common.nameRequired', { name: '模板' }),
          trigger: 'blur',
        },
        {
          min: 2,
          max: 32,
          message: t('common.nameLength'),
          trigger: 'blur',
        },
        {
          pattern: /^[A-Za-z0-9\u4e00-\u9fa5][A-Za-z0-9\u4e00-\u9fa5-_]*$/,
          message: t('common.nameFormat'),
        },
      ],
      remark: [
        {
          max: 300,
          message: t('common.remarkLength'),
          trigger: 'blur',
        },
      ],
    });

    // 定义响应式数据
    const reactiveData = reactive({
      formData: {} as any,
      formVisible: false,
    });

    const show = () => {
      reactiveData.formVisible = true;
    };

    const cancel = () => {
      reactiveData.formVisible = false;
    };

    const reset = () => {
      const form = unref(formDataRef);
      form?.resetFields();
    };

    const addThing = () => {
      const form = unref(formDataRef);
      form?.validate((valid) => {
        if (valid) {
          emit('add-thing', reactiveData.formData, () => {
            cancel();
            reset();
            successMessage();
          });
        }
      });
    };

    return {
      formDataRef,
      formRule,
      reactiveData,
      show,
      cancel,
      reset,
      addThing,
    };
  },
});
