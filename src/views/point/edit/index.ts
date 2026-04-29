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

import { getPointById, getPointUpdate } from '@/api/point';

import pointList from '@/views/point/Point.vue';

export default defineComponent({
  components: { pointList },
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
      oldPointFormData: {},
      pointFormData: {} as any,
    });

    // 定义表单校验规则
    const pointFormRule = reactive<FormRules>({
      pointName: [
        {
          required: true,
          message: t('point.edit.nameRequired'),
          trigger: 'blur',
        },
        {
          min: 2,
          max: 32,
          message: t('point.edit.nameLength'),
          trigger: 'blur',
        },
        {
          pattern: /^[A-Za-z0-9一-龥][A-Za-z0-9一-龥-_]*$/,
          message: t('point.edit.nameFormat'),
        },
      ],
      enable: [
        {
          required: true,
          message: t('common.enableFlag'),
          trigger: 'change',
        },
      ],
      remark: [
        {
          max: 300,
          message: t('point.edit.remarkLength'),
          trigger: 'blur',
        },
      ],
    });

    const point = () => {
      const id = route.query.id as string;
      getPointById(id)
        .then((res) => {
          reactiveData.pointFormData = res.data;
          reactiveData.oldPointFormData = { ...res.data };
        })
        .catch(() => {
          // nothing to do
        });
    };

    const pointUpdate = () => {
      const form = unref(formDataRef);
      form?.validate((valid) => {
        if (valid) {
          getPointUpdate(reactiveData.pointFormData)
            .then((res) => {
              reactiveData.oldPointFormData = { ...res.data };
            })
            .catch(() => {
              // nothing to do
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
        pointUpdate();
      }

      reactiveData.active++;
      changeActive(reactiveData.active);
    };

    const done = () => {
      router.push({ name: 'profileEdit', query: { id: route.query.profileId, active: '1' } }).catch(() => {
        // nothing to do
      });
    };

    const pointReset = () => {
      reactiveData.pointFormData = { ...reactiveData.oldPointFormData };
    };

    const changeActive = (step: string | number) => {
      const query = route.query;
      router.push({ query: { ...query, active: step } }).catch(() => {
        // nothing to do
      });
    };

    point();

    return {
      formDataRef,
      pointFormRule,
      reactiveData,
      pre,
      next,
      done,
      pointReset,
      changeActive,
      ...Icon,
    };
  },
});
