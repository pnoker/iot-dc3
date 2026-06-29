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
    <entity-list-page :config="config" />
    <user-assign-roles ref="assignRef" @save="onAssignRoles" />
  </div>
</template>

<script lang="ts" setup>
  import {ref} from 'vue';
  import {useI18n} from 'vue-i18n';

  import {addRolePrincipalBind, deleteRolePrincipalBind} from '@/api/rolePrincipalBind';
  import EntityListPage from '@/components/entity/EntityListPage.vue';
  import {successMessage} from '@/utils/notificationUtil';

  import UserAssignRoles from './assign/UserAssignRoles.vue';
  import {createUserConfig} from './userConfig';

  const {t} = useI18n();

  const assignRef = ref<InstanceType<typeof UserAssignRoles>>();

  const openAssignRoles = (row: Record<string, any>) => assignRef.value?.show(row);

  const onAssignRoles = async (principalId: string, addIds: string[], removeBindIds: string[], done: () => void) => {
    try {
      await Promise.all([
        ...addIds.map((roleId) => addRolePrincipalBind({principalId, principalType: 'USER', roleId})),
        ...removeBindIds.map((id) => deleteRolePrincipalBind(id)),
      ]);
      successMessage(t('settings.user.assignSaved'));
      done();
    } catch {
      // handled globally
    }
  };

  const config = createUserConfig(t, {onAssignRoles: openAssignRoles});
</script>
