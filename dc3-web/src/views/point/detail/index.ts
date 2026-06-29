/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {defineComponent, reactive} from 'vue';

import {useRoute} from 'vue-router';
import router from '@/config/router';

import {listDeviceByPointId, listDeviceStatusByDriverId} from '@/api/device';
import {listDriverByIds} from '@/api/driver';
import {getPointById} from '@/api/point';

import baseCard from '@/components/card/base/BaseCard.vue';
import detailCard from '@/components/card/detail/DetailCard.vue';
import deviceCard from '@/views/device/card/DeviceCard.vue';
import pointCard from '@/views/point/card/PointCard.vue';

import {timestamp} from '@/utils/dateUtil';

export default defineComponent({
  components: {
    baseCard,
    detailCard,
    deviceCard,
    pointCard,
  },
  setup() {
    const route = useRoute();

    // 定义响应式数据
    const reactiveData = reactive({
      id: route.query.id as string,
      active: (route.query.active as string) || 'detail',
      driverTable: {} as Record<string, any>,
      statusTable: {} as Record<string, any>,
      data: {} as any,
      listDeviceData: [] as any[],
    });

    const point = () => {
      getPointById(reactiveData.id)
        .then((res) => {
          reactiveData.data = res.data;
        })
        .catch(() => {
          // nothing to do
        });
    };

    const device = () => {
      listDeviceByPointId(reactiveData.id)
        .then((res) => {
          reactiveData.listDeviceData = res.data?.devices || [];

          // driver
          const driverIds = Array.from(new Set(reactiveData.listDeviceData.map((device) => device.driverId))).filter(
            Boolean
          );
          if (driverIds.length > 0) {
            listDriverByIds(driverIds)
              .then((res) => {
                reactiveData.driverTable = res.data;
              })
              .catch(() => {
                // nothing to do
              });

            Promise.all(driverIds.map((driverId) => listDeviceStatusByDriverId(driverId)))
              .then((resList) => {
                reactiveData.statusTable = resList.reduce<Record<string, any>>((pre, cur) => {
                  return {...pre, ...(cur.data || {})};
                }, {});
              })
              .catch(() => {
                // nothing to do
              });
          } else {
            reactiveData.driverTable = {};
            reactiveData.statusTable = {};
          }
        })
        .catch(() => {
          // nothing to do
        });
    };

    const deviceName = () => {
      return reactiveData.listDeviceData.map((device) => device.deviceName).join(', ');
    };

    const changeActive = (tab: any) => {
      const query = route.query;
      router.push({query: {...query, active: tab.props.name}}).catch(() => {
        // nothing to do
      });
    };

    point();
    device();

    return {
      reactiveData,
      point,
      device,
      deviceName,
      changeActive,
      timestamp,
    };
  },
});
