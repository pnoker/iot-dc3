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
  <div class="things-card">
    <el-card shadow="hover">
      <div class="things-card-content">
        <div
          :class="{
            'header-enable': delayOk,
            'header-disable': delaySlow,
            'header-missing': !hasLatestValue,
          }"
          class="things-card__header"
        >
          <div class="things-card-header-icon">
            <img :alt="data.pointName" :src="icon" />
          </div>
          <div
            class="things-card-header-name nowrap-name"
            @click="copy(data.pointId, $t('pointValue.card.pointValueId'))"
          >
            {{ point.pointName }}
          </div>
          <div :title="$t('pointValue.card.rwType')" class="things-card-header-status">
            <el-tag v-if="!hasLatestValue" effect="plain" type="info">{{ $t('pointValue.card.noLatestValue') }}</el-tag>
            <el-tag v-if="isReadOnly" effect="plain" type="warning">{{ $t('status.readOnly') }}</el-tag>
            <el-tag v-else-if="isWriteOnly" effect="plain" type="info">{{ $t('status.writeOnly') }}</el-tag>
            <el-tag v-else-if="isReadWrite" effect="plain" type="success">{{ $t('status.readWrite') }}</el-tag>
          </div>
        </div>
        <div class="things-card__body">
          <div class="things-card-body-content">
            <div class="things-card-body-content-column">
              <div class="things-card-body-content-value">
                <span
                  :class="{'value-missing': !hasLatestValue}"
                  :title="$t('pointValue.card.processedValue')"
                  class="nowrap-item value"
                  @click="copyValue(data)"
                  >{{ data.calValue }} {{ hasLatestValue ? unit : '' }}</span
                >
              </div>
              <ul>
                <li class="nowrap-item">
                  <el-icon>
                    <Sunrise />
                  </el-icon>
                  {{ $t('pointValue.card.rawValue') }}: {{ data.rawValue }}
                </li>
                <li v-if="embedded == ''" class="nowrap-item value-point">
                  <el-icon>
                    <Management />
                  </el-icon>
                  {{ $t('pointValue.card.device') }}: {{ device.deviceName }}
                </li>
                <li class="nowrap-item">
                  <el-icon>
                    <Timer />
                  </el-icon>
                  {{ $t('pointValue.card.delay') }}: {{ displayDelay }}
                </li>
                <li class="nowrap-item">
                  <el-icon>
                    <Edit />
                  </el-icon>
                  {{ $t('pointValue.card.collectTime') }}: {{ displayTime(data.createTime) }}
                </li>
                <li class="nowrap-item">
                  <el-icon>
                    <Sunset />
                  </el-icon>
                  {{ $t('pointValue.card.saveTime') }}: {{ displayTime(data.createTime) }}
                </li>
              </ul>
            </div>
          </div>
          <div v-if="embedded != ''" class="things-card-body-content-time">
            <mini-area-chart
              v-if="hasLatestValue"
              :data="historyData"
              :height="80"
              :tooltip-unit="unit"
              animate
              color="var(--el-color-primary)"
            />
            <div v-else class="point-value-empty-chart">{{ $t('pointValue.card.noHistory') }}</div>
          </div>
        </div>
        <div v-if="embedded == ''" class="things-card__footer">
          <div class="things-card-footer-operation">
            <el-button :disabled="writeDisabled" link type="primary" @click="$emit('write-thing', data)">
              {{ $t('pointValue.card.write') }}
            </el-button>
            <el-button link type="primary" @click="$emit('detail-thing', data)">
              {{ $t('common.detail') }}
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
  import {computed, onMounted, ref, watch} from 'vue';
  import {Edit, Management, Sunrise, Sunset, Timer} from '@element-plus/icons-vue';
  import {useI18n} from 'vue-i18n';

  import MiniAreaChart from '@/components/chart/MiniAreaChart.vue';
  import {copy} from '@/utils/commonUtil';
  import {timestamp} from '@/utils/dateUtil';
  import {listPointValueHistory} from '@/api/point';

  const {t} = useI18n();

  const props = defineProps({
    embedded: {
      type: String,
      default: () => {
        return '';
      },
    },
    data: {
      type: Object,
      default: () => {
        return {};
      },
    },
    device: {
      type: Object,
      default: () => {
        return {};
      },
    },
    point: {
      type: Object,
      default: () => {
        return {};
      },
    },
    unit: {
      type: String,
      default: '',
    },
    icon: {
      type: String,
      default: 'images/common/point.png',
    },
  });

  defineEmits(['write-thing', 'detail-thing']);

  const copyValue = (data: any) => {
    const content = {
      deviceId: data.deviceId,
      pointId: data.pointId,
      calValue: data.calValue,
      rawValue: data.rawValue,
      hasLatestValue: data.hasLatestValue,
    };
    copy(JSON.stringify(content, null, 2), t('pointValue.card.pointValueId'));
  };

  const hasLatestValue = computed(() => props.data?.hasLatestValue !== false);
  const delayOk = computed(() => {
    return hasLatestValue.value && typeof props.data?.interval === 'number' && props.data.interval < 200;
  });
  const delaySlow = computed(() => {
    return hasLatestValue.value && typeof props.data?.interval === 'number' && props.data.interval >= 200;
  });
  const displayDelay = computed(() => {
    return typeof props.data?.interval === 'number' ? `${props.data.interval} ms` : '--';
  });
  const rwFlag = computed(() => String(props.data?.rwFlag || '').toUpperCase());
  const isReadOnly = computed(() => ['R', 'READ_ONLY'].includes(rwFlag.value));
  const isWriteOnly = computed(() => ['W', 'WRITE_ONLY'].includes(rwFlag.value));
  const isReadWrite = computed(() => ['RW', 'READ_WRITE'].includes(rwFlag.value));
  const writeDisabled = computed(() => !isWriteOnly.value && !isReadWrite.value);

  const displayTime = (value: string | null | undefined) => {
    if (!hasLatestValue.value || !value) {
      return '--';
    }
    return timestamp(value);
  };

  // Numeric series fed into MiniAreaChart. BOOL points are coerced to 0/1,
  // STRING points render as an empty chart (and the embedded timeline area
  // collapses to the fallback spacer).
  const historyData = ref<number[]>([]);

  const history = () => {
    if (!hasLatestValue.value) {
      historyData.value = [];
      return;
    }
    listPointValueHistory(props.data.deviceId, props.data.pointId, 100)
      .then((res) => {
        const pointValueType = (props.point.pointTypeFlag || '').toLowerCase();
        if (pointValueType === 'string') {
          historyData.value = [];
        } else if (pointValueType === 'boolean') {
          historyData.value = res.data.reverse().map((value: string) => (value === 'true' ? 1 : 0));
        } else {
          historyData.value = res.data.reverse().map((value: string) => +value);
        }
      })
      .catch(() => {
        // handled globally
      });
  };

  watch(
    () => props.data,
    () => {
      if (props.embedded != '') {
        history();
      }
    }
  );

  onMounted(() => {
    window.dispatchEvent(new Event('resize'));
    if (props.embedded != '') {
      history();
    }
  });
</script>

<style lang="scss" scoped>
  // PointValueCard 内联了 header / footer / 实时数值展示区,不使用 ThingsCardHeader / ThingsCardActions,
  // 因此在此补齐对应样式。`header-enable` / `header-disable` 语义不同:基于 data.interval 表示延时是否正常。

  .things-card__header {
    width: 100%;
    height: 55px;
    display: flex;

    .things-card-header-icon {
      width: 48px;
      height: 48px;
      margin-right: 12px;
      border-radius: 4px;
      overflow: hidden;

      img {
        width: 100%;
        height: 100%;
      }
    }

    .things-card-header-name {
      height: 48px;
      line-height: 48px;
      font-size: 14px;
      font-weight: bold;
      color: var(--el-text-color-primary);
      cursor: pointer;

      &:hover {
        color: var(--el-color-primary);
      }
    }

    .things-card-header-status {
      height: 48px;
      line-height: 48px;
      text-align: right;
      flex: 1;
      display: flex;
      justify-content: flex-end;
      align-items: center;
      gap: 6px;

      :deep(.el-tag) {
        vertical-align: middle;
      }
    }
  }

  .header-enable {
    border-bottom: 1px solid var(--el-color-success-light-5);
  }

  .header-disable {
    border-bottom: 1px solid var(--el-color-danger-light-5);
  }

  .header-missing {
    border-bottom: 1px solid var(--el-border-color);
  }

  .things-card__body {
    .things-card-body-content-column {
      display: flex;
      flex-direction: column;
    }

    .things-card-body-content-value {
      display: flex;
      flex-direction: column;
      list-style: none;
      text-align: center;
      margin-top: 20px;

      .value {
        font-weight: bold;
        font-size: xx-large;
        animation: hue 1s ease-in;
        cursor: pointer;
      }

      .value-missing {
        color: var(--el-text-color-secondary);
        animation: none;
      }

      .value-point {
        height: 17px;
      }
    }

    .things-card-body-content-time {
      display: flex;
      justify-content: center;

      .point-value-empty-chart {
        height: 80px;
        line-height: 80px;
        color: var(--el-text-color-secondary);
        font-size: 12px;
      }
    }
  }

  .things-card__footer {
    height: 35px;
    margin-top: 2px;
    display: flex;
    justify-content: flex-end;
    border-top: 1px solid var(--el-border-color);

    .things-card-footer-operation {
      height: 35px;
      display: flex;
    }
  }

  @keyframes hue {
    0% {
      color: var(--el-color-primary);
    }
    50% {
      color: var(--el-color-primary-light-9);
    }
    100% {
      color: var(--el-color-primary);
    }
  }
</style>
