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

<!-- Compatibility shim over CardShell. Note: TitleCard currently has no
     consumers in src/ (verified via rg) — kept for parity / future reuse.
     Forwards title prop + optional #header slot to CardShell. The
     v-if="$slots.header" guard lets the title prop fall through when no
     header slot is supplied. -->

<template>
  <CardShell :title="props.title">
    <template v-if="$slots.header" #header>
      <slot name="header"/>
    </template>
    <slot/>
  </CardShell>
</template>

<script lang="ts" setup>
import CardShell from '@/components/card/base/CardShell.vue';

const props = defineProps({
  title: {
    type: String,
    default: '',
  },
});
</script>
