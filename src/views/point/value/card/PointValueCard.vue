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
          <div v-if="embedded != ''" :id="data.pointId" class="things-card-body-content-time"></div>
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
  import { onMounted, watch } from 'vue';
  import { CircleClose, Edit, Management, Sunrise, Sunset, Timer } from '@element-plus/icons-vue';
  import { useI18n } from 'vue-i18n';

  import { Chart } from '@antv/g2';

  import { copy } from '@/utils/CommonUtil';
  import { timestamp } from '@/utils/DateUtil';
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

  let tinyArea: Chart;
  const history = () => {
    getPointValueHistory(props.data.deviceId, props.data.pointId, 100)
      .then((res) => {
        let historyData: number[];
        const pointValueType = props.point.pointTypeFlag.toLowerCase();
        if (pointValueType === 'string') {
          historyData = [];
        } else if (pointValueType === 'boolean') {
          historyData = res.data.reverse().map((value: string) => (value === 'true' ? 1 : 0));
        } else {
          historyData = res.data.reverse().map((value: string) => +value);
        }

        tinyArea.changeData(historyData);
      })
      .catch(() => {
        // nothing to do
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
      tinyArea = new Chart({
        container: props.data.pointId,
        autoFit: true,
        height: 80,
      });

      tinyArea
        .area()
        .encode('x', (_: any, i: number) => i)
        .encode('y', (v: any) => v)
        .encode('shape', 'smooth')
        .scale('y', { zero: true })
        .style('fill', 'linear-gradient(-90deg, white 0%, darkgreen 100%)')
        .style('fillOpacity', 0.6)
        .animate('enter', { type: 'fadeIn' })
        .axis(false);

      tinyArea.interaction('tooltip', {
        render: (_e: any, { items }: any) => `${items[0].value} ${props.unit}`,
      });

      tinyArea.render();

      history();
    }
  });
</script>

<style lang="scss" scoped>
  @use '@/components/card/styles/things-card';
</style>
