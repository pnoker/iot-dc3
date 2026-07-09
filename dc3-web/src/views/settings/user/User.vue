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
    <entity-list-page :config="config"/>
    <user-assign-roles ref="assignRef" @save="onAssignRoles"/>
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
