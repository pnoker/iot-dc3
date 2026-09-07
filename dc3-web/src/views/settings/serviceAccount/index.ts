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

import {computed, defineComponent, onBeforeUnmount, reactive, ref, watch} from 'vue';
import {useI18n} from 'vue-i18n';

import {
  addServiceAccount,
  deleteServiceAccount,
  disableServiceAccount,
  enableServiceAccount,
  listServiceAccount,
  updateServiceAccount,
} from '@/api/serviceAccount';
import {listPrincipalByIds} from '@/api/principal';
import {getUserByName} from '@/api/user';
import {useAuthStore} from '@/store/modules/auth';
import {usePagedList} from '@/composables/usePagedList';
import {successMessage} from '@/utils/notificationUtil';
import {isEnabledFlag} from '@/utils/thingModelFormatUtil';

import type {ResponsiveListColumn, ServiceAccountForm, ServiceAccountRecord} from '@/config/types';

import serviceAccountTool from './tool/ServiceAccountTool.vue';
import serviceAccountEditForm from './edit/ServiceAccountEditForm.vue';
import ResponsiveRecordList from '@/components/list/ResponsiveRecordList.vue';

export default defineComponent({
  name: 'SettingsServiceAccount',
  components: {
    ResponsiveRecordList,
    serviceAccountTool,
    serviceAccountEditForm,
  },
  setup() {
    const {t} = useI18n();
    const authStore = useAuthStore();

    const editRef = ref<InstanceType<typeof serviceAccountEditForm>>();

    // A service account requires an owner that is a tenant member; default to the current user.
    const ownerPrincipalId = ref('');
    let disposed = false;
    let ownerLookupRequestId = 0;

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

    // Resolve ownerPrincipalId → principal name for the table column, reusing the
    // shared listPrincipalByIds endpoint (same source as the family relations).
    const ownerNameMap = reactive<Record<string, string>>({});
    const resolveOwnerNames = async (rows: ServiceAccountRecord[]) => {
      const requestId = ++ownerLookupRequestId;
      const ids = Array.from(
        new Set(rows.map((r) => String(r.ownerPrincipalId ?? '')).filter((id) => id && id !== '0' && !ownerNameMap[id]))
      );
      if (!ids.length) return;
      try {
        const res: any = await listPrincipalByIds(ids);
        if (disposed || requestId !== ownerLookupRequestId) return;
        (res || []).forEach((p: any) => {
          ownerNameMap[String(p.id)] = p.displayName || p.principalName || String(p.id);
        });
      } catch {
        // handled globally
      }
    };
    watch(
      () => reactiveData.listData,
      (rows) => resolveOwnerNames((rows as ServiceAccountRecord[]) || []),
      {
        immediate: true,
      }
    );
    const ownerNameFor = (row: ServiceAccountRecord) =>
      ownerNameMap[String(row.ownerPrincipalId)] || String(row.ownerPrincipalId ?? '-');
    const columns = computed<ResponsiveListColumn<ServiceAccountRecord>[]>(() => [
      {
        key: 'serviceAccountName',
        label: t('settings.serviceAccount.serviceAccountName'),
        minWidth: 160,
        mobile: 'primary',
      },
      {key: 'purpose', label: t('settings.serviceAccount.purpose'), minWidth: 180},
      {
        key: 'ownerPrincipalId',
        label: t('settings.serviceAccount.ownerPrincipalId'),
        minWidth: 140,
        formatter: ownerNameFor,
      },
      {key: 'expireTime', label: t('settings.serviceAccount.expireTime'), width: 165, kind: 'time'},
      {key: 'lastUsedTime', label: t('settings.serviceAccount.lastUsedTime'), width: 165, kind: 'time'},
      {key: 'enableFlag', label: t('common.enable'), width: 90, kind: 'custom'},
      {key: 'createTime', label: t('common.createTime'), width: 165, kind: 'time', mobile: 'hidden'},
    ]);
    const togglingIds = ref(new Set<string>());
    const isToggling = (row: ServiceAccountRecord) => togglingIds.value.has(String(row.id));
    const deletingIds = ref(new Set<string>());
    const isDeleting = (row: ServiceAccountRecord) => deletingIds.value.has(String(row.id));
    const isRowBusy = (row: ServiceAccountRecord) => isToggling(row) || isDeleting(row);

    // Resolve the current user's principalId to use as the default owner of a new service account.
    getUserByName(String(authStore.getName || ''))
      .then((res) => {
        if (disposed) return;
        ownerPrincipalId.value = String(res?.principalId || '');
      })
      .catch(() => {
        // handled globally
      });

    const openAdd = () => editRef.value?.show(ownerPrincipalId.value);
    const openEdit = (row: ServiceAccountRecord) => editRef.value?.showEdit(row);

    const onAdd = (form: ServiceAccountForm, done: (successful?: boolean) => void) => {
      addServiceAccount(form)
        .then(() => {
          if (disposed) return;
          successMessage();
          void load();
          done(true);
        })
        .catch(() => {
          if (!disposed) done(false);
        });
    };

    const onUpdate = (form: ServiceAccountForm, done: (successful?: boolean) => void) => {
      updateServiceAccount(form)
        .then(() => {
          if (disposed) return;
          successMessage();
          void load();
          done(true);
        })
        .catch(() => {
          if (!disposed) done(false);
        });
    };

    const remove = async (id: string) => {
      const key = String(id);
      if (deletingIds.value.has(key)) return;
      deletingIds.value.add(key);
      try {
        await deleteServiceAccount(id);
        if (disposed) return;
        successMessage();
        await load();
      } catch {
        // handled globally
      } finally {
        deletingIds.value.delete(key);
      }
    };

    const toggleEnable = (row: ServiceAccountRecord) => {
      const id = String(row.id);
      if (togglingIds.value.has(id) || deletingIds.value.has(id)) return;
      togglingIds.value.add(id);
      const disable = isEnabledFlag(row.enableFlag);
      (disable ? disableServiceAccount : enableServiceAccount)(row.id)
        .then(() => {
          if (disposed) return;
          successMessage();
          void load();
        })
        .catch(() => {
          // handled globally
        })
        .finally(() => {
          togglingIds.value.delete(id);
        });
    };

    onBeforeUnmount(() => {
      disposed = true;
      ownerLookupRequestId += 1;
      togglingIds.value.clear();
      deletingIds.value.clear();
    });

    void load();

    return {
      t,
      editRef,
      reactiveData,
      columns,
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
      isToggling,
      isDeleting,
      isRowBusy,
      sizeChange,
      currentChange,
      isEnabledFlag,
      ownerNameFor,
    };
  },
});
