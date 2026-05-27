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

import { defineComponent, reactive, ref, unref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { Back, Edit, RefreshLeft, Right } from '@element-plus/icons-vue';
import { nameRules, remarkRules } from '@/utils/formRuleUtil';
import { useI18n } from 'vue-i18n';

import router from '@/config/router';
import { useRoute } from 'vue-router';

import { getProfileById, updateProfile } from '@/api/profile';

import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import point from '@/views/point/Point.vue';
import CommandList from '@/views/settings/command/CommandList.vue';
import EventList from '@/views/settings/event/definition/EventList.vue';

function activeStep(value: unknown): number {
  const step = Number(value ?? 0);
  if (!Number.isFinite(step)) {
    return 0;
  }
  return Math.min(Math.max(Math.trunc(step), 0), 4);
}

export default defineComponent({
  components: { EnableFlagSegmented, point, CommandList, EventList },
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
      active: activeStep(route.query.active),
      oldProfileFormData: {},
      profileFormData: {} as any,
    });

    // 定义表单校验规则
    const formRule = reactive<FormRules>({
      profileName: nameRules(t, t('common.entityProfile')),
      enableFlag: [
        {
          required: true,
          message: t('common.enableFlag'),
          trigger: 'change',
        },
      ],
      remark: remarkRules(t),
    });

    const profile = () => {
      getProfileById(reactiveData.id as string).then((res) => {
        reactiveData.profileFormData = res.data;
        reactiveData.oldProfileFormData = { ...res.data };
      });
    };

    const profileUpdate = async (): Promise<boolean> => {
      const form = unref(formDataRef);
      if (!form) {
        return false;
      }

      try {
        await form.validate();
        const res = await updateProfile(reactiveData.profileFormData);
        reactiveData.oldProfileFormData = { ...res.data };
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
        const ok = await profileUpdate();
        if (!ok) {
          return;
        }
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

    watch(
      () => [route.query.id, route.query.active],
      ([id, active]) => {
        const nextId = id as string;
        const nextActive = activeStep(active);

        if (reactiveData.active !== nextActive) {
          reactiveData.active = nextActive;
        }

        if (nextId && nextId !== reactiveData.id) {
          reactiveData.id = nextId;
          reactiveData.profileFormData = {};
          reactiveData.oldProfileFormData = {};
          profile();
        }
      }
    );

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
