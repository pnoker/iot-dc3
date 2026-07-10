<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
  -->

<template>
  <entity-list-page ref="listRef" :config="config"/>
</template>

<script lang="ts" setup>
import {ref} from 'vue';
import {useI18n} from 'vue-i18n';

import {refreshMcpToolCatalog} from '@/api/mcp';
import EntityListPage from '@/components/entity/EntityListPage.vue';
import {successMessage} from '@/utils/notificationUtil';

import {createMcpToolConfig} from './mcpToolConfig';

const {t} = useI18n();
const listRef = ref<InstanceType<typeof EntityListPage>>();
const refreshing = ref(false);

const onRefresh = async () => {
  refreshing.value = true;
  try {
    const res = await refreshMcpToolCatalog();
    successMessage(t('settings.mcp.refreshed', {count: res.data || 0}));
    listRef.value?.reload();
  } finally {
    refreshing.value = false;
  }
};

const config = createMcpToolConfig(t, {onRefresh, refreshing: () => refreshing.value});
</script>
