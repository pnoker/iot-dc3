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
import { nameRules, remarkRules } from '@/utils/formRuleUtil';
import { useI18n } from 'vue-i18n';

import router from '@/config/router';
import { useRoute } from 'vue-router';

import { getPointById, updatePoint } from '@/api/point';

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
      pointName: nameRules(t, '位号'),
      enable: [
        {
          required: true,
          message: t('common.enableFlag'),
          trigger: 'change',
        },
      ],
      remark: remarkRules(t),
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

    const pointUpdate = async (): Promise<boolean> => {
      const form = unref(formDataRef);
      if (!form) {
        return false;
      }

      try {
        await form.validate();
        const res = await updatePoint(reactiveData.pointFormData);
        reactiveData.oldPointFormData = { ...res.data };
        return true;
      } catch {
        return false;
      }
    };

    const pre = () => {
      reactiveData.active--;
      changeActive(reactiveData.active);
    };

    const next = async () => {
      if (reactiveData.active === 0) {
        const ok = await pointUpdate();
        if (!ok) {
          return;
        }
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
