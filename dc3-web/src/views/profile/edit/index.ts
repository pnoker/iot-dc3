/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
