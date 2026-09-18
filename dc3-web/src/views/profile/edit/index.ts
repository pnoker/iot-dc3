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

import {computed, defineComponent, onBeforeUnmount, onMounted, reactive, watch} from 'vue';
import type {FormRules} from 'element-plus';
import {ElMessageBox} from 'element-plus';
import {nameRules, remarkRules} from '@/utils/formRuleUtil';
import {useI18n} from 'vue-i18n';

import router from '@/config/router';
import {onBeforeRouteLeave, useRoute} from 'vue-router';

import {getProfileById, updateProfile} from '@/api/profile';

import baseCard from '@/components/card/base/BaseCard.vue';
import InfoCard from '@/components/card/info/InfoCard.vue';
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import point from '@/views/point/Point.vue';
import CommandList from '@/views/settings/command/CommandList.vue';
import EventList from '@/views/settings/event/definition/EventList.vue';
import type {ProfileRecord} from '@/config/types';
import {failMessage, successMessage} from '@/utils/notificationUtil';

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
      id: String(route.query.id || ''),
      active: resolveTab(route.query.active),
      oldProfileFormData: {},
      profileFormData: {} as any,
      loading: false,
      status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
      loadError: null as unknown | null,
      saving: false,
    });

    let requestToken = 0;
    let saveToken = 0;
    let routeChangeToken = 0;
    let disposed = false;

    const clone = <T,>(value: T): T => {
      if (value == null) return value;
      return JSON.parse(JSON.stringify(value)) as T;
    };

    const profileDirty = computed(() => {
      const fields: Array<keyof ProfileRecord> = ['profileName', 'enableFlag', 'remark'];
      return fields.some(
        (field) => String(reactiveData.profileFormData[field] ?? '') !== String((reactiveData.oldProfileFormData as any)[field] ?? ''),
      );
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

    const profile = async (id = reactiveData.id) => {
      const requestedId = String(id || '');
      if (!requestedId) {
        reactiveData.status = 'idle';
        reactiveData.loading = false;
        reactiveData.profileFormData = {};
        reactiveData.oldProfileFormData = {};
        return;
      }
      const token = ++requestToken;
      reactiveData.loading = true;
      reactiveData.status = 'loading';
      reactiveData.loadError = null;
      try {
        const res = await getProfileById(requestedId);
        if (disposed || token !== requestToken || requestedId !== reactiveData.id) return;
        const profileData = clone(res || {});
        reactiveData.profileFormData = profileData;
        reactiveData.oldProfileFormData = clone(profileData);
        reactiveData.status = 'success';
      } catch (error) {
        if (disposed || token !== requestToken || requestedId !== reactiveData.id) return;
        reactiveData.loadError = error;
        reactiveData.status = 'error';
      } finally {
        if (!disposed && token === requestToken) reactiveData.loading = false;
      }
    };

    const profileSave = async () => {
      if (reactiveData.saving || reactiveData.loading || !reactiveData.id || !profileDirty.value) return;
      const token = ++saveToken;
      const requestedId = reactiveData.id;
      reactiveData.saving = true;
      try {
        const res = await updateProfile(reactiveData.profileFormData);
        if (disposed || token !== saveToken || requestedId !== reactiveData.id) return;
        const profileData = clone(res || reactiveData.profileFormData);
        reactiveData.profileFormData = profileData;
        reactiveData.oldProfileFormData = clone(profileData);
        successMessage();
      } catch (error) {
        if (!disposed && token === saveToken && requestedId === reactiveData.id) {
          failMessage(undefined, undefined, error);
        }
      } finally {
        if (!disposed && token === saveToken) reactiveData.saving = false;
      }
    };

    const profileReset = () => {
      if (reactiveData.saving) return;
      reactiveData.profileFormData = clone(reactiveData.oldProfileFormData);
    };

    const changeActive = (tab: any) => {
      reactiveData.active = tab.props.name;
      const query = route.query;
      router.push({query: {...query, active: tab.props.name}}).catch(() => {
        // handled globally
      });
    };

    const confirmDiscard = async () => {
      if (!profileDirty.value) return true;
      try {
        await ElMessageBox.confirm(
          t('common.discardConfirm'),
          t('common.confirm'),
          {
            type: 'warning',
            confirmButtonText: t('common.confirm'),
            cancelButtonText: t('common.cancel'),
          },
        );
        return true;
      } catch {
        return false;
      }
    };

    watch(
      () => [route.query.id, route.query.active],
      async ([id, active]) => {
        const changeToken = ++routeChangeToken;
        const nextId = String(id || '');
        const nextActive = resolveTab(active);

        if (reactiveData.active !== nextActive) {
          reactiveData.active = nextActive;
        }

        if (nextId !== reactiveData.id) {
          if (!(await confirmDiscard())) {
            if (disposed || changeToken !== routeChangeToken) return;
            await router.replace({query: {...route.query, id: reactiveData.id, active: reactiveData.active}}).catch(() => {
              // handled globally
            });
            return;
          }
          if (disposed || changeToken !== routeChangeToken) return;
          reactiveData.id = nextId;
          reactiveData.profileFormData = {};
          reactiveData.oldProfileFormData = {};
          void profile(nextId);
        }
      }
    );

    const warnBeforeUnload = (event: BeforeUnloadEvent) => {
      if (!profileDirty.value || reactiveData.saving) return;
      event.preventDefault();
      event.returnValue = '';
    };

    onMounted(() => window.addEventListener('beforeunload', warnBeforeUnload));
    onBeforeUnmount(() => {
      disposed = true;
      requestToken += 1;
      saveToken += 1;
      routeChangeToken += 1;
      window.removeEventListener('beforeunload', warnBeforeUnload);
    });

    onBeforeRouteLeave(async (_to, _from, next) => {
      if (reactiveData.saving || (await confirmDiscard())) {
        next();
      } else {
        next(false);
      }
    });

    void profile();

    return {
      reactiveData,
      formRule,
      profileSave,
      profileReset,
      changeActive,
      profile,
      profileDirty,
    };
  },
});
