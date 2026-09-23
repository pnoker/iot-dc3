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
            <el-icon v-if="!isImageUrl" :size="24">
              <component :is="icon"/>
            </el-icon>
            <img v-else :alt="data?.pointName || point?.pointName || ''" :src="icon"/>
          </div>
          <div
            class="things-card-header-name nowrap-name"
            @click="copy(data?.pointId, $t('pointValue.card.pointValueId'))"
          >
            {{ point?.pointName || data?.pointName || '-' }}
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
              <div
                :class="[
                  'things-card-body-content-value',
                  {
                    'value-panel-fresh': delayOk,
                    'value-panel-stale': delaySlow,
                    'value-panel-missing': !hasLatestValue,
                  },
                ]"
              >
                <span
                  :aria-label="$t('pointValue.card.processedValue')"
                  :title="$t('pointValue.card.processedValue')"
                  class="nowrap-item value"
                  role="button"
                  tabindex="0"
                  @click="copyValue(data)"
                  @keydown.enter="copyValue(data)"
                  @keydown.space.prevent="copyValue(data)"
                >
                  <span class="value-number">{{ data?.calValue ?? '--' }}</span>
                  <span v-if="hasLatestValue && unit" class="value-unit">{{ unit }}</span>
                </span>
                <span class="value-caption">{{ $t('pointValue.card.processedValue') }}</span>
              </div>
              <ul>
                <li class="nowrap-item">
                  <el-icon>
                    <Sunrise/>
                  </el-icon>
                  {{ $t('pointValue.card.rawValue') }}: {{ data?.rawValue ?? '--' }}
                </li>
                <li v-if="embedded == ''" class="nowrap-item value-point">
                  <el-icon>
                    <Management/>
                  </el-icon>
                  {{ $t('pointValue.card.device') }}: {{ device?.deviceName || '-' }}
                </li>
                <li class="nowrap-item">
                  <el-icon>
                    <Timer/>
                  </el-icon>
                  {{ $t('pointValue.card.delay') }}: {{ displayDelay }}
                </li>
                <li class="nowrap-item">
                  <el-icon>
                    <Edit/>
                  </el-icon>
                  {{ $t('pointValue.card.collectTime') }}: {{ displayTime(data?.createTime) }}
                </li>
                <li class="nowrap-item">
                  <el-icon>
                    <Sunset/>
                  </el-icon>
                  {{ $t('pointValue.card.saveTime') }}: {{ displayTime(data?.operateTime) }}
                </li>
              </ul>
            </div>
          </div>
          <div v-if="embedded != ''" class="things-card-body-content-time">
            <div
              v-if="historyLoading"
              aria-live="polite"
              class="point-value-history-loading"
            >
              {{ $t('common.loading') }}
            </div>
            <el-alert
              v-else-if="historyError"
              :closable="false"
              :title="$t('pointValue.card.historyLoadFailed')"
              class="point-value-history-error"
              show-icon
              type="error"
            >
              <el-button :loading="historyLoading" link type="danger" @click="history">
                {{ $t('common.retry') }}
              </el-button>
            </el-alert>
            <mini-area-chart
              v-else-if="hasLatestValue && historyData.length > 0"
              :data="historyData"
              :height="80"
              :tooltip-unit="unit"
              animate
              color="var(--el-color-success)"
            />
            <div v-else class="point-value-empty-chart">{{ $t('pointValue.card.noHistory') }}</div>
          </div>
        </div>
        <!-- Action order follows the system-wide card footer contract:
             detail (browse) → write (modify). -->
        <div v-if="embedded == ''" class="things-card__footer">
          <div class="things-card-footer-operation">
            <el-button link type="primary" @click="$emit('detail-thing', data)">
              {{ $t('common.detail') }}
            </el-button>
            <el-button :disabled="writeDisabled" link type="primary" @click="$emit('write-thing', data)">
              {{ $t('pointValue.card.write') }}
            </el-button>
          </div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script lang="ts" setup>
import {computed, onBeforeUnmount, onMounted, ref, watch} from 'vue';
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
    default: 'TrendCharts',
  },
});

defineEmits(['write-thing', 'detail-thing']);

const copyValue = (data: any) => {
  const content = {
    deviceId: data?.deviceId,
    pointId: data?.pointId,
    calValue: data?.calValue,
    rawValue: data?.rawValue,
    hasLatestValue: data?.hasLatestValue,
  };
  copy(JSON.stringify(content, null, 2), t('pointValue.card.pointValueId'));
};

const hasLatestValue = computed(() => props.data?.hasLatestValue !== false);
// Element Plus icon name (default) vs legacy image URL — see the header tile.
const isImageUrl = computed(() => props.icon.includes('/'));
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
// STRING points render the explicit no-history fallback instead of feeding
// non-numeric values into the chart.
const historyData = ref<number[]>([]);
const historyLoading = ref(false);
const historyError = ref(false);
let historyRequestId = 0;

const history = () => {
  const requestId = ++historyRequestId;
  const deviceId = String(props.data?.deviceId ?? '');
  const pointId = String(props.data?.pointId ?? '');
  if (!hasLatestValue.value || !deviceId || !pointId) {
    historyData.value = [];
    historyLoading.value = false;
    historyError.value = false;
    return;
  }
  historyLoading.value = true;
  historyError.value = false;
  listPointValueHistory(deviceId, pointId, undefined, 100)
    .then((res) => {
      if (requestId !== historyRequestId) return;
      const pointValueType = String(props.point?.pointTypeFlag || '').toLowerCase();
      const values = (res?.items || []).map((item) => String(item.value ?? ''));
      if (pointValueType === 'string') {
        historyData.value = [];
      } else if (pointValueType === 'boolean') {
        historyData.value = values.reverse().map((value) => (value === 'true' ? 1 : 0));
      } else {
        historyData.value = values
          .reverse()
          .map((value) => Number(value))
          .filter((value) => Number.isFinite(value));
      }
    })
    .catch(() => {
      if (requestId !== historyRequestId) return;
      historyData.value = [];
      historyError.value = true;
    })
    .finally(() => {
      if (requestId === historyRequestId) historyLoading.value = false;
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

onBeforeUnmount(() => {
  historyRequestId += 1;
});
</script>

<style lang="scss" scoped>
// PointValueCard owns its header, footer, and live-value area. Its header-enable and
// header-disable states indicate whether data.interval is within the expected delay.

.things-card__header {
  width: 100%;
  height: 55px;
  display: flex;
  align-items: center;

  // Same tone-tinted tile family as ThingsCardHeader / StatCard; this
  // card is the live-data tile, so it carries the success (data) accent.
  .things-card-header-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 44px;
    height: 44px;
    margin-right: 12px;
    border: 1px solid color-mix(in srgb, var(--el-color-success) 18%, transparent);
    border-radius: var(--dc3-radius-lg);
    background: var(--el-color-success-light-9);
    color: var(--el-color-success);
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
    gap: var(--dc3-space-2);

    // Soft tone chips: light-9 wash + hairline ring instead of the
    // default tag fill, so the status never shouts over the tile.
    :deep(.el-tag) {
      vertical-align: middle;
      height: 20px;
      padding: 0 8px;
      border-radius: var(--dc3-radius-full);
      font-size: 11px;
      font-weight: 600;
      line-height: 18px;

      &.el-tag--success {
        --el-tag-bg-color: var(--el-color-success-light-9);
        --el-tag-border-color: color-mix(in srgb, var(--el-color-success) 26%, transparent);
        --el-tag-text-color: var(--el-color-success);
      }

      &.el-tag--warning {
        --el-tag-bg-color: var(--el-color-warning-light-9);
        --el-tag-border-color: color-mix(in srgb, var(--el-color-warning) 26%, transparent);
        --el-tag-text-color: var(--el-color-warning);
      }

      &.el-tag--info {
        --el-tag-bg-color: var(--el-fill-color-light);
        --el-tag-border-color: var(--dc3-border-base);
        --el-tag-text-color: var(--dc3-text-secondary);
      }
    }
  }
}

.point-value-history-loading,
.point-value-empty-chart {
  display: grid;
  min-height: 80px;
  place-items: center;
  color: var(--el-text-color-secondary);
  font-size: 12px;
}

.point-value-history-error {
  min-height: 80px;

  :deep(.el-alert__content) {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: var(--dc3-space-2);
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

    // Info rows: icon + label + value with a quiet hover wash, so the
    // metadata list reads as a scannable stack instead of a bare dump.
    ul {
      display: flex;
      flex-direction: column;
      gap: var(--dc3-space-1);
      list-style: none;
      padding: var(--dc3-space-3) var(--dc3-space-1) 0;
      margin: 0;
      font-size: 12px;

      li {
        display: flex;
        align-items: center;
        gap: var(--dc3-space-2);
        padding: var(--dc3-space-1) var(--dc3-space-2);
        border-radius: var(--dc3-radius-md);
        color: var(--dc3-text-regular);
        transition: background-color 120ms ease;

        &:hover {
          background: var(--dc3-bg-interactive);
        }

        .el-icon {
          flex-shrink: 0;
          color: var(--dc3-text-secondary);
        }
      }
    }
  }

  // The processed-value block is the card's hero: a soft-tinted panel
  // (tone light-9 wash + hairline) holding the big number, a unit chip and
  // a muted caption — the same tone grammar as the header tile.
  .things-card-body-content-value {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--dc3-space-2);
    list-style: none;
    text-align: center;
    margin-top: var(--dc3-space-4);
    padding: var(--dc3-space-4) var(--dc3-space-3);
    border: 1px solid color-mix(in srgb, var(--value-tone, var(--el-border-color)) 16%, transparent);
    border-radius: var(--dc3-radius-lg);
    background: linear-gradient(
      180deg,
      color-mix(in srgb, var(--value-tone, var(--el-color-primary)) 8%, transparent),
      transparent 70%
    );

    .value {
      display: inline-flex;
      align-items: baseline;
      gap: var(--dc3-space-2);
      max-width: 100%;
      font-weight: 700;
      font-variant-numeric: tabular-nums;
      cursor: pointer;
    }

    .value-number {
      overflow: hidden;
      font-size: 30px;
      line-height: 1.1;
      text-overflow: ellipsis;
      white-space: nowrap;
      background: linear-gradient(
        180deg,
        var(--value-tone, var(--el-text-color-primary)),
        color-mix(in srgb, var(--value-tone, var(--el-text-color-primary)) 72%, var(--el-text-color-primary))
      );
      -webkit-background-clip: text;
      background-clip: text;
      color: transparent;
    }

    .value-unit {
      flex-shrink: 0;
      padding: 1px 6px;
      border: 1px solid color-mix(in srgb, var(--value-tone, var(--el-color-primary)) 26%, transparent);
      border-radius: var(--dc3-radius-full);
      background: color-mix(in srgb, var(--value-tone, var(--el-color-primary)) 10%, transparent);
      color: var(--value-tone, var(--el-color-primary));
      font-size: 11px;
      font-weight: 650;
      line-height: 16px;
    }

    .value-caption {
      color: var(--dc3-text-muted);
      font-size: 11px;
      letter-spacing: 0.02em;
    }

    // Tone per freshness state; the panel wash, hairline and number
    // gradient all resolve through this one variable.
    &.value-panel-fresh {
      --value-tone: var(--el-color-success);
    }

    &.value-panel-stale {
      --value-tone: var(--el-color-warning);
    }

    &.value-panel-missing {
      --value-tone: var(--el-text-color-secondary);
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
  box-sizing: border-box;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--el-border-color);

  .things-card-footer-operation {
    height: 35px;
    display: flex;
  }
}

</style>
