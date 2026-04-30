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

import type { Dictionary } from '@/config/entity';

import { successMessage } from '@/utils/NotificationUtil';
import { getDriverDictionary, getProfileDictionary } from '@/api/dictionary';
import { useI18n } from 'vue-i18n';

export default defineComponent({
  name: 'DeviceAddForm',
  emits: ['add-thing'],
  setup(_props, { emit }) {
    const { t } = useI18n();
    // 定义表单引用
    const formDataRef = ref<FormInstance>();

    // 定义响应式数据
    const reactiveData = reactive({
      formData: {} as any,
      formVisible: false,
      driverDictionary: [] as Dictionary[],
      driverLoading: false,
      profileDictionary: [] as Dictionary[],
      profileLoading: false,
    });

    // 定义表单校验规则
    const formRule = reactive<FormRules>({
      deviceName: [
        {
          required: true,
          message: () => t('device.add.nameRequired'),
          trigger: 'blur',
        },
        {
          min: 2,
          max: 32,
          message: () => t('device.add.nameLength'),
          trigger: 'blur',
        },
        {
          pattern: /^[A-Za-z0-9一-龥][A-Za-z0-9一-龥-_]*$/,
          message: () => t('device.add.nameFormat'),
        },
      ],
      driverId: [
        {
          required: true,
          message: () => t('device.add.driverRequired'),
          trigger: 'change',
        },
      ],
      remark: [
        {
          max: 300,
          message: () => t('device.add.remarkLength'),
          trigger: 'blur',
        },
      ],
    });

    const driverDictionary = (query?: string) => {
      reactiveData.driverLoading = true;
      getDriverDictionary({
        page: { size: 50, current: 1 },
        label: query || '',
      })
        .then((res) => {
          reactiveData.driverDictionary = res.data.records;
        })
        .catch(() => {
          // nothing to do
        })
        .finally(() => {
          reactiveData.driverLoading = false;
        });
    };

    const driverDictionaryVisible = (visible: boolean) => {
      if (visible) driverDictionary('');
    };

    const profileDictionary = (query?: string) => {
      reactiveData.profileLoading = true;
      getProfileDictionary({
        page: { size: 50, current: 1 },
        label: query || '',
      })
        .then((res) => {
          reactiveData.profileDictionary = res.data.records;
        })
        .catch(() => {
          // nothing to do
        })
        .finally(() => {
          reactiveData.profileLoading = false;
        });
    };

    const profileDictionaryVisible = (visible: boolean) => {
      if (visible) profileDictionary('');
    };

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
      driverDictionary,
      driverDictionaryVisible,
      profileDictionary,
      profileDictionaryVisible,
      show,
      cancel,
      reset,
      addThing,
    };
  },
});
