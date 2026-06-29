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

import {defineComponent, reactive, watch} from 'vue';
import type {FormRules} from 'element-plus';
import {nameRules, remarkRules} from '@/utils/formRuleUtil';
import {useI18n} from 'vue-i18n';

import router from '@/config/router';
import {useRoute} from 'vue-router';

import {getProfileById, updateProfile} from '@/api/profile';

import baseCard from '@/components/card/base/BaseCard.vue';
import InfoCard from '@/components/card/info/InfoCard.vue';
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import point from '@/views/point/Point.vue';
import CommandList from '@/views/settings/command/CommandList.vue';
import EventList from '@/views/settings/event/definition/EventList.vue';

const PROFILE_EDIT_TABS = ['profileConfig', 'pointConfig', 'commandConfig', 'eventConfig'] as const;

function resolveTab(value: unknown): string {
  const str = String(value ?? '');
  if (PROFILE_EDIT_TABS.includes(str as any)) return str;
  return 'profileConfig';
}

export default defineComponent({
  components: {baseCard, InfoCard, EnableFlagSegmented, point, CommandList, EventList},
  setup() {
    const route = useRoute();
    const {t} = useI18n();

    const reactiveData = reactive({
      id: route.query.id,
      active: resolveTab(route.query.active),
      oldProfileFormData: {},
      profileFormData: {} as any,
    });

    const formRule = reactive<FormRules>({
      profileName: nameRules(t, t('common.entityProfile')),
      enableFlag: [
        {
          message: t('common.enableFlag'),
          trigger: 'change',
        },
      ],
      remark: remarkRules(t),
    });

    const profile = () => {
      getProfileById(reactiveData.id as string).then((res) => {
        reactiveData.profileFormData = res.data;
        reactiveData.oldProfileFormData = {...res.data};
      });
    };

    const profileSave = async () => {
      try {
        const res = await updateProfile(reactiveData.profileFormData);
        reactiveData.oldProfileFormData = {...res.data};
      } catch {
        // API error
      }
    };

    const profileReset = () => {
      reactiveData.profileFormData = {...reactiveData.oldProfileFormData};
    };

    const changeActive = (tab: any) => {
      reactiveData.active = tab.props.name;
      const query = route.query;
      router.push({query: {...query, active: tab.props.name}});
    };

    watch(
      () => [route.query.id, route.query.active],
      ([id, active]) => {
        const nextId = id as string;
        const nextActive = resolveTab(active);

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
      reactiveData,
      formRule,
      profileSave,
      profileReset,
      changeActive,
    };
  },
});
