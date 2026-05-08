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
            'header-enable': data.interval < 200,
            'header-disable': data.interval >= 200,
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
          <div class="things-card-header-status" :title="$t('pointValue.card.rwType')">
            <el-tag v-if="data.rwFlag === 'R'" effect="plain" type="warning">{{ $t('status.readOnly') }}</el-tag>
            <el-tag v-else-if="data.rwFlag === 'W'" effect="plain" type="info">{{ $t('status.writeOnly') }}</el-tag>
            <el-tag v-else-if="data.rwFlag === 'RW'" effect="plain" type="success">{{ $t('status.readWrite') }}</el-tag>
          </div>
        </div>
        <div class="things-card__body">
          <div class="things-card-body-content">
            <div class="things-card-body-content-column">
              <div class="things-card-body-content-value">
                <span class="nowrap-item value" :title="$t('pointValue.card.processedValue')" @click="copyValue(data)"
                  >{{ data.calValue }} {{ unit }}</span
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
                  {{ $t('pointValue.card.delay') }}: {{ data.interval }} ms
                </li>
                <li class="nowrap-item">
                  <el-icon>
                    <Edit />
                  </el-icon>
                  {{ $t('pointValue.card.collectTime') }}: {{ timestamp(data.createTime) }}
                </li>
                <li class="nowrap-item">
                  <el-icon>
                    <Sunset />
                  </el-icon>
                  {{ $t('pointValue.card.saveTime') }}: {{ timestamp(data.createTime) }}
                </li>
              </ul>
            </div>
          </div>
          <div v-if="embedded != ''" class="things-card-body-content-time">
            <mini-area-chart :data="historyData" :height="80" color="#409eff" :tooltip-unit="unit" animate />
          </div>
        </div>
        <div v-if="embedded == ''" class="things-card__footer">
          <div class="things-card-footer-operation">
            <el-popconfirm
              :icon="CircleClose"
              icon-color="#f56c6c"
              placement="top"
              :title="$t('pointValue.card.confirmDelete')"
            >
              <template #reference>
                <el-button link type="primary">{{ $t('common.delete') }}</el-button>
              </template>
            </el-popconfirm>
            <el-button link type="primary">{{ $t('common.edit') }}</el-button>
            <el-button link type="primary">{{ $t('common.detail') }}</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, ref, watch } from 'vue';
  import { CircleClose, Edit, Management, Sunrise, Sunset, Timer } from '@element-plus/icons-vue';
  import { useI18n } from 'vue-i18n';

  import MiniAreaChart from '@/components/chart/MiniAreaChart.vue';
  import { copy } from '@/utils/commonUtil';
  import { timestamp } from '@/utils/dateUtil';
  import { getPointValueHistory } from '@/api/point';

  const { t } = useI18n();

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

  const copyValue = (data: any) => {
    const content = {
      deviceId: data.deviceId,
      pointId: data.pointId,
      calValue: data.calValue,
      rawValue: data.rawValue,
    };
    copy(JSON.stringify(content, null, 2), t('pointValue.card.pointValueId'));
  };

  // Numeric series fed into MiniAreaChart. BOOL points are coerced to 0/1,
  // STRING points render as an empty chart (and the embedded timeline area
  // collapses to the fallback spacer).
  const historyData = ref<number[]>([]);

  const history = () => {
    getPointValueHistory(props.data.deviceId, props.data.pointId, 100)
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
  @use '@/styles/things-card.scss';

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
      color: rgba(0, 0, 0, 0.85);
      cursor: pointer;

      &:hover {
        color: #1890ff;
      }
    }

    .things-card-header-status {
      height: 48px;
      line-height: 48px;
      text-align: right;
      flex: 1;

      :deep(.el-tag) {
        vertical-align: middle;
      }
    }
  }

  .header-enable {
    border-bottom: 1px solid #c2e7b0;
  }

  .header-disable {
    border-bottom: 1px solid #fbc4c4;
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

      .value-point {
        height: 17px;
      }
    }

    .things-card-body-content-time {
      display: flex;
      justify-content: center;
    }
  }

  .things-card__footer {
    height: 35px;
    margin-top: 2px;
    display: flex;
    justify-content: flex-end;
    border-top: 1px solid #dcdfe6;

    .things-card-footer-operation {
      height: 35px;
      display: flex;
    }
  }

  @keyframes hue {
    0% {
      color: #409eff;
    }
    50% {
      color: #f3f4fe;
    }
    100% {
      color: #409eff;
    }
  }
</style>
