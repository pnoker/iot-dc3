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

import { defineComponent, ref } from 'vue';
import { useI18n } from 'vue-i18n';

import {
  addServiceAccount,
  deleteServiceAccount,
  disableServiceAccount,
  enableServiceAccount,
  listServiceAccount,
  updateServiceAccount,
} from '@/api/serviceAccount';
import { getUserByName } from '@/api/user';
import { useAuthStore } from '@/store/modules/auth';
import { usePagedList } from '@/composables/usePagedList';
import { timestampColumn } from '@/utils/dateUtil';
import { successMessage } from '@/utils/notificationUtil';
import { isEnabledFlag } from '@/utils/thingModelFormatUtil';

import type { ServiceAccountForm, ServiceAccountRecord } from '@/config/types/auth';

import serviceAccountTool from './tool/ServiceAccountTool.vue';
import serviceAccountEditForm from './edit/ServiceAccountEditForm.vue';
import BlankCard from '@/components/card/blank/BlankCard.vue';

export default defineComponent({
  name: 'SettingsServiceAccount',
  components: {
    BlankCard,
    serviceAccountTool,
    serviceAccountEditForm,
  },
  setup() {
    const { t } = useI18n();
    const authStore = useAuthStore();

    const editRef = ref<InstanceType<typeof serviceAccountEditForm>>();

    // A service account requires an owner that is a tenant member; default to the current user.
    const ownerPrincipalId = ref('');

    const {
      state: reactiveData,
      load,
      search,
      reset,
      sort,
      sizeChange,
      currentChange,
    } = usePagedList<ServiceAccountRecord, Record<string, unknown>>({
      request: (query) => listServiceAccount(query),
    });

    const refresh = () => load();

    // Resolve the current user's principalId to use as the default owner of a new service account.
    getUserByName(String(authStore.getName || ''))
      .then((res) => {
        ownerPrincipalId.value = String(res.data?.principalId || '');
      })
      .catch(() => {
        // handled globally
      });

    const openAdd = () => editRef.value?.show(ownerPrincipalId.value);
    const openEdit = (row: ServiceAccountRecord) => editRef.value?.showEdit(row);

    const onAdd = (form: ServiceAccountForm, done: () => void) => {
      addServiceAccount(form)
        .then(() => {
          successMessage();
          load();
          done();
        })
        .catch(() => {
          // handled globally
        });
    };

    const onUpdate = (form: ServiceAccountForm, done: () => void) => {
      updateServiceAccount(form)
        .then(() => {
          successMessage();
          load();
          done();
        })
        .catch(() => {
          // handled globally
        });
    };

    const remove = (id: string) => {
      deleteServiceAccount(id)
        .then(() => {
          successMessage();
          load();
        })
        .catch(() => {
          // handled globally
        });
    };

    const toggleEnable = (row: ServiceAccountRecord) => {
      const disable = isEnabledFlag(row.enableFlag);
      (disable ? disableServiceAccount : enableServiceAccount)(row.id)
        .then(() => {
          successMessage();
          load();
        })
        .catch(() => {
          // handled globally
        });
    };

    load();

    return {
      t,
      editRef,
      reactiveData,
      search,
      reset,
      refresh,
      sort,
      openAdd,
      openEdit,
      onAdd,
      onUpdate,
      remove,
      toggleEnable,
      sizeChange,
      currentChange,
      timestampColumn,
      isEnabledFlag,
    };
  },
});
