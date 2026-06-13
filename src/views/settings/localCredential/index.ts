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

import { defineComponent, reactive } from 'vue';
import { useI18n } from 'vue-i18n';
import { Plus } from '@element-plus/icons-vue';

import {
  addLocalCredential,
  deleteLocalCredential,
  listLocalCredential,
  resetLocalCredentialPassword,
} from '@/api/localCredential';
import { usePagedList } from '@/composables/usePagedList';
import { timestampColumn } from '@/utils/dateUtil';
import { successMessage } from '@/utils/notificationUtil';
import { cleanSearchParams } from '@/utils/searchParamUtil';

import type { LocalCredentialForm, LocalCredentialRecord } from '@/config/types';

import BlankCard from '@/components/card/blank/BlankCard.vue';
import ToolCard from '@/components/card/tool/ToolCard.vue';
import EnableTag from '@/components/tag/EnableTag.vue';

export default defineComponent({
  name: 'SettingsLocalCredential',
  components: {
    BlankCard,
    ToolCard,
    EnableTag,
  },
  setup() {
    const { t } = useI18n();
    const {
      state: reactiveData,
      load,
      search,
      reset,
      sort,
      sizeChange,
      currentChange,
    } = usePagedList<LocalCredentialRecord, Record<string, unknown>>({
      request: (query) => listLocalCredential(query),
    });

    const refresh = () => load();

    const addDialog = reactive({
      visible: false,
      submitting: false,
      form: { loginName: '', principalId: '', password: '' } as LocalCredentialForm,
    });

    const resetDialog = reactive({ visible: false, submitting: false, id: '', loginName: '', password: '' });

    const filterForm = reactive<Record<string, any>>({ loginName: '' });

    const openAdd = () => {
      addDialog.form = { loginName: '', principalId: '', password: '' };
      addDialog.visible = true;
    };

    const submitAdd = () => {
      addDialog.submitting = true;
      addLocalCredential(addDialog.form)
        .then(() => {
          successMessage();
          addDialog.visible = false;
          load();
        })
        .catch(() => {
          // handled globally
        })
        .finally(() => {
          addDialog.submitting = false;
        });
    };

    const openReset = (row: LocalCredentialRecord) => {
      resetDialog.id = row.id;
      resetDialog.loginName = row.loginName || '';
      resetDialog.password = '';
      resetDialog.visible = true;
    };

    const submitReset = () => {
      resetDialog.submitting = true;
      resetLocalCredentialPassword(resetDialog.id, resetDialog.password)
        .then(() => {
          successMessage();
          resetDialog.visible = false;
        })
        .catch(() => {
          // handled globally
        })
        .finally(() => {
          resetDialog.submitting = false;
        });
    };

    const remove = (id: string) => {
      deleteLocalCredential(id)
        .then(() => {
          successMessage();
          load();
        })
        .catch(() => {
          // handled globally
        });
    };

    const onSearch = (data: Record<string, any>) => search(cleanSearchParams(data));
    const onReset = () => {
      filterForm.loginName = '';
      reset();
    };

    load();

    return {
      t,
      reactiveData,
      refresh,
      sort,
      sizeChange,
      currentChange,
      addDialog,
      resetDialog,
      filterForm,
      openAdd,
      submitAdd,
      openReset,
      submitReset,
      remove,
      onSearch,
      onReset,
      timestampColumn,
      Plus,
    };
  },
});
