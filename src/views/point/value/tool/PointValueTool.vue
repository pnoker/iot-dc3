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
  <div class="tool-card">
    <el-card shadow="never">
      <el-form
        ref="formDataRef"
        :inline="true"
        :model="reactiveData.formData"
        :rules="formRule"
        class="tool-card__body"
      >
        <div class="tool-card-body-form">
          <el-form-item v-if="embedded == ''" :label="$t('pointValue.tool.device')" prop="deviceId">
            <el-select
              v-model="reactiveData.formData.deviceId"
              :remote-method="deviceDictionary"
              class="edit-form-special"
              clearable
              filterable
              :placeholder="$t('pointValue.tool.devicePlaceholder')"
              remote
              @change="deviceCurrentChange"
              @visible-change="deviceDictionaryVisible"
            >
              <div class="tool-select">
                <el-pagination
                  :current-page="+reactiveData.devicePage.current"
                  :hide-on-single-page="true"
                  :page-size="+reactiveData.devicePage.size"
                  :pager-count="5"
                  :total="+reactiveData.devicePage.total"
                  background
                  class="tool-select-pagination"
                  layout="prev, pager, next"
                  @current-change="deviceCurrentChange"
                ></el-pagination>
              </div>
              <el-option
                v-for="dictionary in reactiveData.deviceDictionary"
                :key="dictionary.value"
                :label="dictionary.label"
                :value="dictionary.value"
              ></el-option>
            </el-select>
          </el-form-item>
          <el-form-item v-if="embedded == ''" :label="$t('pointValue.tool.point')" prop="pointId">
            <el-select
              v-model="reactiveData.formData.pointId"
              :remote-method="pointDictionary"
              class="edit-form-special"
              clearable
              filterable
              :placeholder="$t('pointValue.tool.pointPlaceholder')"
              remote
              @change="pointCurrentChange"
              @visible-change="pointDictionaryVisible"
            >
              <div class="tool-select">
                <el-pagination
                  :current-page="+reactiveData.pointPage.current"
                  :hide-on-single-page="true"
                  :page-size="+reactiveData.pointPage.size"
                  :pager-count="5"
                  :total="+reactiveData.pointPage.total"
                  background
                  class="tool-select-pagination"
                  layout="prev, pager, next"
                  @current-change="pointCurrentChange"
                ></el-pagination>
              </div>
              <el-option
                v-for="dictionary in reactiveData.pointDictionary"
                :key="dictionary.value"
                :label="dictionary.label"
                :value="dictionary.value"
              ></el-option>
            </el-select>
          </el-form-item>
          <el-form-item v-if="embedded == 'device'" :label="$t('pointValue.tool.pointName')" prop="pointName">
            <el-input
              v-model="reactiveData.formData.pointName"
              class="edit-form-default"
              clearable
              :placeholder="$t('pointValue.tool.pointNamePlaceholder')"
              @keyup.enter="search"
            ></el-input>
          </el-form-item>
          <el-form-item v-if="embedded == 'device'" :label="$t('common.enableFlag')" prop="enableFlag">
            <el-segmented
              v-model="reactiveData.formData.enableFlag"
              :options="[
                { label: 'All', value: '' },
                { label: 'Enable', value: 'ENABLE' },
                { label: 'Disable', value: 'DISABLE' },
              ]"
            />
          </el-form-item>
        </div>
        <el-form-item class="tool-card-body-button">
          <el-button :icon="Search" type="primary" @click="search">{{ $t('common.search') }}</el-button>
          <el-button :icon="RefreshRight" @click="reset">{{ $t('common.reset') }}</el-button>
        </el-form-item>
      </el-form>
      <div class="tool-card__footer">
        <div class="tool-card-footer-button">
          <el-button v-if="embedded == ''" :icon="Plus" disabled type="success">{{ $t('common.add') }}</el-button>
        </div>
        <div class="tool-card-footer-page">
          <el-pagination
            :current-page="+page.current"
            :page-size="+page.size"
            :page-sizes="[6, 12, 24, 36, 48, 96]"
            :total="+page.total"
            background
            layout="total, prev, pager, next, sizes"
            @size-change="sizeChange"
            @current-change="currentChange"
          >
          </el-pagination>
          <el-tooltip class="item" :content="$t('common.refresh')" effect="dark" placement="top">
            <el-button :icon="Refresh" circle @click="refresh"></el-button>
          </el-tooltip>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
  import { reactive, ref, unref } from 'vue';
  import type { FormInstance, FormRules } from 'element-plus';
  import { Plus, Refresh, RefreshRight, Search } from '@element-plus/icons-vue';
  import type { Dictionary, Order } from '@/config/entity';
  import { getDeviceDictionary, getPointDictionary } from '@/api/dictionary';

  defineProps({
    embedded: {
      type: String,
      default: () => {
        return '';
      },
    },
    page: {
      type: Object,
      default: () => {
        return {};
      },
    },
  });

  const emit = defineEmits(['search', 'reset', 'refresh', 'size-change', 'current-change']);

  // 定义表单引用
  const formDataRef = ref<FormInstance>();

  // 定义响应式数据
  const reactiveData = reactive({
    formData: {} as any,
    deviceQuery: '',
    deviceDictionary: [] as Dictionary[],
    devicePage: {
      total: 0,
      size: 5,
      current: 1,
      orders: [] as Order[],
    },
    pointQuery: '',
    pointDictionary: [] as Dictionary[],
    pointPage: {
      total: 0,
      size: 5,
      current: 1,
      orders: [] as Order[],
    },
  });

  // 定义表单校验规则
  const formRule = reactive<FormRules>({});

  const deviceDictionary = (query?: string) => {
    getDeviceDictionary({
      page: reactiveData.devicePage,
      label: query ? query : reactiveData.deviceQuery,
    })
      .then((res) => {
        const data = res.data;
        reactiveData.devicePage.total = data.total;
        reactiveData.deviceDictionary = data.records;
      })
      .catch(() => {
        // nothing to do
      });
  };

  const deviceCurrentChange = (current: number) => {
    reactiveData.devicePage.current = current;
    deviceDictionary();
  };

  const pointDictionary = (query?: string) => {
    getPointDictionary({
      page: reactiveData.pointPage,
      label: query ? query : reactiveData.pointQuery,
      parentId: reactiveData.formData.deviceId,
    })
      .then((res) => {
        const data = res.data;
        reactiveData.pointPage.total = data.total;
        reactiveData.pointDictionary = data.records;
      })
      .catch(() => {
        // nothing to do
      });
  };

  const pointCurrentChange = (current: number) => {
    reactiveData.pointPage.current = current;
    pointDictionary();
  };

  const deviceDictionaryVisible = (visible: boolean) => {
    if (visible) {
      reactiveData.deviceQuery = '';
      deviceDictionary();
    }
  };

  const pointDictionaryVisible = (visible: boolean) => {
    if (visible) {
      reactiveData.pointQuery = '';
      pointDictionary();
    }
  };

  const search = () => {
    const form = unref(formDataRef);
    form?.validate((valid) => {
      if (valid) {
        emit('search', reactiveData.formData);
      }
    });
  };

  const reset = () => {
    const form = unref(formDataRef);
    form?.resetFields();
    emit('reset');
  };

  const refresh = () => {
    emit('refresh');
  };

  const sizeChange = (size: number) => {
    emit('size-change', size);
  };

  const currentChange = (current: number) => {
    emit('current-change', current);
  };
</script>

<style lang="scss" scoped>
  @use '@/components/card/styles/tool-card.scss';
</style>
