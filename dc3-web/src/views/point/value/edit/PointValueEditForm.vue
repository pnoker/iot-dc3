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
  <el-dialog
    v-model="reactiveData.formVisible"
    :append-to-body="true"
    :before-close="requestClose"
    :close-on-click-modal="false"
    :close-on-press-escape="!reactiveData.submitting"
    :show-close="!reactiveData.submitting"
    :title="$t('pointValue.edit.title')"
    class="things-dialog"
    destroy-on-close
    draggable
    @closed="onClosed"
  >
    <el-alert
      v-if="reactiveData.saveError"
      :closable="false"
      :title="$t('common.saveFailed')"
      class="things-dialog-form-alert"
      show-icon
      type="error"
    />
    <el-form
      ref="formDataRef"
      v-loading="reactiveData.submitting"
      :aria-busy="reactiveData.submitting"
      :model="reactiveData.formData"
      :rules="formRule"
      label-position="top"
    >
      <el-form-item :label="$t('pointValue.edit.pointValue')" prop="value">
        <el-input
          v-model="reactiveData.formData.value"
          :disabled="reactiveData.submitting"
          :placeholder="$t('pointValue.edit.pointValuePlaceholder')"
          clearable
        ></el-input>
      </el-form-item>
      <el-form-item :label="$t('pointValue.edit.description')" prop="remark">
        <el-input
          v-model="reactiveData.formData.remark"
          :disabled="reactiveData.submitting"
          :placeholder="$t('pointValue.edit.descriptionPlaceholder')"
          clearable
          maxlength="300"
          show-word-limit
          type="textarea"
        ></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="things-dialog-footer">
        <slot name="footer">
          <el-button :disabled="reactiveData.submitting" @click="cancel">{{ $t('common.cancel') }}</el-button>
          <el-button :disabled="reactiveData.submitting" plain @click="reset">{{ $t('common.reset') }}</el-button>
          <el-button :loading="reactiveData.submitting" type="primary" @click="updateThing">
            {{ $t('common.confirm') }}
          </el-button>
        </slot>
      </div>
    </template>
  </el-dialog>
</template>

<script lang="ts" setup>
import type {PropType} from 'vue';
import {computed, onBeforeUnmount, reactive, ref, unref} from 'vue';
import type {FormInstance, FormRules} from 'element-plus';
import {ElMessageBox} from 'element-plus';
import {useI18n} from 'vue-i18n';


type PointValueFormData = Record<string, unknown> & { value?: string | number; remark?: string };

const props = defineProps({
  formData: {
    type: Object as PropType<PointValueFormData>,
    default: () => ({}),
  },
});

const emit = defineEmits<{
  (e: 'update-thing', formData: PointValueFormData, done: (successful?: boolean) => void): void;
}>();

const {t} = useI18n();
const formDataRef = ref<FormInstance>();

const reactiveData = reactive({
  formVisible: false,
  submitting: false,
  saveError: false,
  formData: {} as PointValueFormData,
});
const initialData = ref<PointValueFormData>({});
let formSession = 0;

const isDirty = computed(
  () => reactiveData.formVisible && JSON.stringify(reactiveData.formData) !== JSON.stringify(initialData.value)
);

const formRule = reactive<FormRules>({
  value: [
    {
      required: true,
      whitespace: true,
      message: t('pointValue.edit.valueRequired'),
      trigger: 'blur',
    },
  ],
  remark: [
    {
      max: 300,
      message: t('common.remarkLength'),
      trigger: 'blur',
    },
  ],
});

const syncFormData = (value = props.formData) => {
  reactiveData.formData = {...value};
  initialData.value = {...reactiveData.formData};
};

const show = (value?: PointValueFormData) => {
  formSession += 1;
  syncFormData(value);
  reactiveData.submitting = false;
  reactiveData.saveError = false;
  reactiveData.formVisible = true;
};

const cancel = () => {
  void requestClose();
};

const reset = () => {
  const form = unref(formDataRef);
  reactiveData.formData = {...initialData.value};
  reactiveData.saveError = false;
  form?.clearValidate();
};

const onClosed = () => {
  formSession += 1;
  reactiveData.submitting = false;
  formDataRef.value?.clearValidate();
};

onBeforeUnmount(() => {
  formSession += 1;
});

const requestClose = async (done?: () => void) => {
  if (reactiveData.submitting) return;
  const session = formSession;
  if (!isDirty.value) {
    if (done) done();
    else reactiveData.formVisible = false;
    return;
  }
  try {
    await ElMessageBox.confirm(t('common.discardConfirm'), t('common.confirm'), {
      type: 'warning',
      confirmButtonText: t('common.confirm'),
      cancelButtonText: t('common.cancel'),
    });
    if (session !== formSession || !reactiveData.formVisible) return;
    if (done) done();
    else reactiveData.formVisible = false;
  } catch {
    // Keep the draft open when the user cancels the confirmation.
  }
};

const updateThing = async () => {
  if (reactiveData.submitting) return;
  const form = unref(formDataRef);
  if (!form) {
    return;
  }

  const session = formSession;
  try {
    await form.validate();
    if (session !== formSession || !reactiveData.formVisible) return;
    reactiveData.submitting = true;
    reactiveData.saveError = false;
    try {
      emit('update-thing', {...reactiveData.formData}, (successful = true) => {
        if (session !== formSession) return;
        reactiveData.submitting = false;
        if (!successful) {
          reactiveData.saveError = true;
          return;
        }
        initialData.value = {...reactiveData.formData};
        reactiveData.formVisible = false;
      });
    } catch {
      reactiveData.submitting = false;
      reactiveData.saveError = true;
    }
  } catch {
    // validation errors are displayed by Element Plus
  }
};

defineExpose({
  show,
  cancel,
  reset,
  updateThing,
});
</script>
