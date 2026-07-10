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
  <el-tag :size="size" :type="enabled ? 'success' : 'info'">
    {{ enabled ? t('common.enable') : t('common.disable') }}
  </el-tag>
</template>

<script lang="ts" setup>
import {computed} from 'vue';
import {useI18n} from 'vue-i18n';
import {isEnabledFlag} from '@/utils/thingModelFormatUtil';

const props = withDefaults(
  defineProps<{
    value?: string | number | boolean | null;
    size?: '' | 'large' | 'default' | 'small';
  }>(),
  {
    value: undefined,
    size: '',
  }
);

const {t} = useI18n();
const enabled = computed(() => isEnabledFlag(props.value));
</script>
