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
import { Back, Edit, RefreshLeft, Right } from '@element-plus/icons-vue';
import { useI18n } from 'vue-i18n';

import router from '@/config/router';
import { useRoute } from 'vue-router';

import { getProfileById, updateProfile } from '@/api/profile';

import point from '@/views/point/Point.vue';

export default defineComponent({
  components: { point },
  setup() {
    const route = useRoute();
    const { t } = useI18n();

    // 定义表单引用
    const formDataRef = ref<FormInstance>();

    // 图标
    const Icon = {
      Edit,
      RefreshLeft,
      Right,
      Back,
    };

    // 定义响应式数据
    const reactiveData = reactive({
      id: route.query.id,
      active: +(route.query.active || 0),
      oldProfileFormData: {},
      profileFormData: {} as any,
    });

    // 定义表单校验规则
    const formRule = reactive<FormRules>({
      profileName: [
        {
          required: true,
          message: t('profile.edit.nameRequired'),
          trigger: 'blur',
        },
        {
          min: 2,
          max: 32,
          message: t('profile.edit.nameLength'),
          trigger: 'blur',
        },
        {
          pattern: /^[A-Za-z0-9一-龥][A-Za-z0-9一-龥-_]*$/,
          message: t('profile.edit.nameFormat'),
        },
      ],
      enableFlag: [
        {
          required: true,
          message: t('common.enableFlag'),
          trigger: 'change',
        },
      ],
      remark: [
        {
          max: 300,
          message: t('profile.edit.remarkLength'),
          trigger: 'blur',
        },
      ],
    });

    const profile = () => {
      const id = route.query.id as string;
      getProfileById(id).then((res) => {
        reactiveData.profileFormData = res.data;
        reactiveData.oldProfileFormData = { ...res.data };
      });
    };

    const profileUpdate = () => {
      const form = unref(formDataRef);
      form?.validate((valid) => {
        if (valid) {
          updateProfile(reactiveData.profileFormData).then((res) => {
            reactiveData.oldProfileFormData = { ...res.data };
          });
        }
      });
    };

    const pre = () => {
      reactiveData.active--;
      changeActive(reactiveData.active);
    };

    const next = () => {
      if (reactiveData.active === 0) {
        profileUpdate();
      }

      reactiveData.active++;
      changeActive(reactiveData.active);
    };

    const done = () => {
      router.push({ name: 'profile' }).catch(() => {
        // nothing to do
      });
    };

    const profileReset = () => {
      reactiveData.profileFormData = { ...reactiveData.oldProfileFormData };
    };

    const changeActive = (step: number) => {
      const query = route.query;
      router.push({ query: { ...query, active: step } });
    };

    profile();

    return {
      formDataRef,
      reactiveData,
      formRule,
      pre,
      next,
      done,
      profileReset,
      changeActive,
      ...Icon,
    };
  },
});
