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
  <div>
    <entity-list-page ref="listRef" :config="config"/>
    <register-client-dialog ref="dialogRef" @saved="reload"/>
  </div>
</template>

<script lang="ts" setup>
import {ref} from 'vue';
import {useI18n} from 'vue-i18n';

import EntityListPage from '@/components/entity/EntityListPage.vue';

import RegisterClientDialog from './components/RegisterClientDialog.vue';
import {createMcpClientConfig} from './mcpClientConfig';

const {t} = useI18n();
const listRef = ref<InstanceType<typeof EntityListPage>>();
const dialogRef = ref<InstanceType<typeof RegisterClientDialog>>();

const reload = () => listRef.value?.reload();

const config = createMcpClientConfig(t, {onRegister: () => dialogRef.value?.open()});
</script>
