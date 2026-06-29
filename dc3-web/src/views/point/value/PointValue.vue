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
    <point-value-tool
      :embedded="embedded"
      :page="reactiveData.page"
      @refresh="refresh"
      @reset="reset"
      @search="search"
      @size-change="sizeChange"
      @current-change="currentChange"
    ></point-value-tool>

    <blank-card>
      <el-row>
        <el-col v-for="data in 12" :key="data" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
          <skeleton-card :footer="true" :loading="reactiveData.loading"></skeleton-card>
        </el-col>
        <el-col v-if="hasData">
          <el-empty
            :description="embedded == 'device' ? $t('pointValue.empty.noDevice') : $t('pointValue.empty.noData')"
          ></el-empty>
        </el-col>
        <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="12" :sm="12" :xl="6" :xs="24">
          <point-value-card
            :data="data"
            :device="reactiveData.deviceTable[data.deviceId]"
            :embedded="embedded"
            :point="reactiveData.pointTable[data.pointId]"
            :unit="reactiveData.unitTable[data.pointId]"
            @detail-thing="openDetail"
            @write-thing="openWrite"
          ></point-value-card>
        </el-col>
      </el-row>
    </blank-card>

    <point-value-edit-form ref="editRef" @update-thing="writeValue" />
    <point-value-detail ref="detailRef" :detail-data="reactiveData.detailData" />
  </div>
</template>

<script lang="ts" setup>
  import {computed, onMounted, reactive, ref} from 'vue';
  import {getPointValueLatest, listPointByIds, listPointUnit, listPointValue, writePointValue} from '@/api/point';
  import {listDeviceByIds} from '@/api/device';

  import blankCard from '@/components/card/blank/BlankCard.vue';
  import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
  import pointValueTool from './tool/PointValueTool.vue';
  import pointValueCard from './card/PointValueCard.vue';
  import pointValueEditForm from './edit/PointValueEditForm.vue';
  import pointValueDetail from './detail/PointValueDetail.vue';

  import {isNull} from '@/utils/validationUtil';

  const props = defineProps({
    embedded: {
      type: String,
      default: () => {
        return '';
      },
    },
    deviceId: {
      type: String,
      default: () => {
        return '';
      },
    },
  });

  const reactiveData = reactive({
    loading: true,
    deviceTable: {} as Record<string, any>,
    pointTable: {} as Record<string, any>,
    unitTable: {} as Record<string, any>,
    listData: [] as any[],
    detailData: {} as Record<string, unknown>,
    query: {},
    page: {
      total: 0,
      size: 12,
      current: 1,
    },
  });

  const editRef = ref<InstanceType<typeof pointValueEditForm>>();
  const detailRef = ref<InstanceType<typeof pointValueDetail>>();

  const hasData = computed(() => {
    return !reactiveData.loading && reactiveData.listData?.length < 1;
  });

  const list = () => {
    if (!isNull(props.deviceId)) {
      reactiveData.query = {
        ...reactiveData.query,
        deviceId: props.deviceId,
      };
    }

    if (props.embedded == 'device') {
      getPointValueLatest({
        page: reactiveData.page,
        ...reactiveData.query,
      })
        .then((res) => {
          loadPointValueList(res);
        })
        .catch(() => {
          // nothing to do
        })
        .finally(() => {
          reactiveData.loading = false;
        });
    } else {
      listPointValue({
        page: reactiveData.page,
        ...reactiveData.query,
      })
        .then((res) => {
          loadPointValueList(res);
        })
        .catch(() => {
          // nothing to do
        })
        .finally(() => {
          reactiveData.loading = false;
        });
    }
  };

  const loadPointValueList = (res: any) => {
    reactiveData.listData = res.data.records.map((record: any) => {
      record.hasLatestValue = record.hasLatestValue !== false;
      if (!record.hasLatestValue || !record.createTime || !record.operateTime) {
        record.interval = null;
        return record;
      }
      const tempDate1 = new Date(record.createTime);
      const tempDate2 = new Date(record.operateTime);
      record.interval = tempDate2.getTime() - tempDate1.getTime();
      return record;
    });
    reactiveData.page.total = res.data.total;

    // device
    const deviceIds = Array.from(new Set(reactiveData.listData.map((pointValue) => pointValue.deviceId)));
    if (deviceIds.length > 0) {
      listDeviceByIds(deviceIds)
        .then((res) => {
          reactiveData.deviceTable = res.data;
        })
        .catch(() => {
          // nothing to do
        });
    }

    // point & unit
    const pointIds = Array.from(new Set(reactiveData.listData.map((pointValue) => pointValue.pointId)));
    if (pointIds.length > 0) {
      listPointByIds(pointIds)
        .then((res) => {
          reactiveData.pointTable = res.data;
        })
        .catch(() => {
          // nothing to do
        });

      listPointUnit(pointIds)
        .then((res) => {
          reactiveData.unitTable = res.data;
        })
        .catch(() => {
          // nothing to do
        });
    }
  };

  const search = (params: any) => {
    const cleaned: Record<string, any> = {};
    for (const [k, v] of Object.entries(params)) {
      if (v !== '' && v != null) cleaned[k] = v;
    }
    reactiveData.query = cleaned;
    list();
  };

  const reset = () => {
    reactiveData.query = {};
    list();
  };

  const refresh = () => {
    list();
  };

  const openWrite = (row: Record<string, unknown>) => {
    editRef.value?.show({
      ...row,
      value: String(row.calValue ?? ''),
    });
  };

  const writeValue = (formData: Record<string, unknown>, done: () => void) => {
    writePointValue({
      deviceId: formData.deviceId,
      pointId: formData.pointId,
      value: String(formData.value ?? ''),
    })
      .then(() => {
        refresh();
        done();
      })
      .catch(() => {
        // handled globally
      });
  };

  const openDetail = (row: Record<string, unknown>) => {
    const deviceId = String(row.deviceId || '');
    const pointId = String(row.pointId || '');
    reactiveData.detailData = {
      ...row,
      device: reactiveData.deviceTable[deviceId],
      point: reactiveData.pointTable[pointId],
      unit: reactiveData.unitTable[pointId],
    };
    detailRef.value?.show();
  };

  const sizeChange = (size: number) => {
    reactiveData.page.size = size;
    list();
  };

  const currentChange = (current: number) => {
    reactiveData.page.current = current;
    list();
  };

  onMounted(() => {
    list();
  });

  defineExpose({
    refresh,
    list,
  });
</script>
