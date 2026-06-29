<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <div>
    <entity-list-page ref="listRef" :config="config" />
    <register-client-dialog ref="dialogRef" @saved="reload" />
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
