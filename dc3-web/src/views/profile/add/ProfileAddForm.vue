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
  <el-dialog
    v-model="reactiveData.formVisible"
    :append-to-body="true"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    :title="$t('profile.add.title')"
    class="things-dialog"
    draggable
  >
    <el-form ref="formDataRef" :model="reactiveData.formData" :rules="formRule" label-position="top">
      <el-form-item :label="$t('profile.add.profileName')" prop="profileName">
        <el-input
          v-model="reactiveData.formData.profileName"
          :placeholder="$t('profile.add.profileNamePlaceholder')"
          clearable
          maxlength="32"
          show-word-limit
        ></el-input>
      </el-form-item>
      <el-form-item :label="$t('profile.add.description')" prop="remark">
        <el-input
          v-model="reactiveData.formData.remark"
          :placeholder="$t('profile.add.descriptionPlaceholder')"
          clearable
          maxlength="300"
          show-word-limit
          type="textarea"
        ></el-input>
      </el-form-item>
    </el-form>
    <div class="things-dialog-footer">
      <slot name="footer">
        <el-button @click="cancel">{{ $t('common.cancel') }}</el-button>
        <el-button plain @click="reset">{{ $t('common.reset') }}</el-button>
        <el-button type="primary" @click="addThing">{{ $t('common.confirm') }}</el-button>
      </slot>
    </div>
  </el-dialog>
</template>

<script lang="ts" setup>
  import {reactive, ref, unref} from 'vue';
  import type {FormInstance, FormRules} from 'element-plus';
  import {useI18n} from 'vue-i18n';

  import {successMessage} from '@/utils/notificationUtil';
  import {nameRules, remarkRules} from '@/utils/formRuleUtil';

  interface ProfileAddFormData {
    profileName?: string;
    remark?: string;
  }

  const emit = defineEmits<{
    (e: 'add-thing', formData: ProfileAddFormData, done: () => void): void;
  }>();

  const {t} = useI18n();
  const formDataRef = ref<FormInstance>();

  const formRule = reactive<FormRules>({
    profileName: nameRules(t, t('common.entityProfile')),
    remark: remarkRules(t),
  });

  const reactiveData = reactive({
    formData: {} as ProfileAddFormData,
    formVisible: false,
  });

  const show = () => {
    reactiveData.formVisible = true;
  };

  const cancel = () => {
    reactiveData.formVisible = false;
  };

  const reset = () => {
    const form = unref(formDataRef);
    form?.resetFields();
  };

  const addThing = async () => {
    const form = unref(formDataRef);
    if (!form) {
      return;
    }

    try {
      await form.validate();
      emit('add-thing', {...reactiveData.formData}, () => {
        cancel();
        reset();
        successMessage();
      });
    } catch {
      // validation errors are displayed by Element Plus
    }
  };

  defineExpose({
    show,
    cancel,
    reset,
    addThing,
  });
</script>
