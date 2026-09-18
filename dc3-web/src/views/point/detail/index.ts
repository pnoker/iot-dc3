/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import {defineComponent, onBeforeUnmount, reactive, watch} from 'vue';

import {useRoute} from 'vue-router';
import router from '@/config/router';

import {getDeviceStatisticsByPointId, listDeviceStatusByDriverId} from '@/api/device';
import {listDriverByIds} from '@/api/driver';
import {getPointById} from '@/api/point';

import baseCard from '@/components/card/base/BaseCard.vue';
import detailCard from '@/components/card/detail/DetailCard.vue';
import deviceCard from '@/views/device/card/DeviceCard.vue';
import pointCard from '@/views/point/card/PointCard.vue';

import {timestamp} from '@/utils/dateUtil';
import {useBreakpoint} from '@/composables/useBreakpoint';

export default defineComponent({
  components: {
    baseCard,
    detailCard,
    deviceCard,
    pointCard,
  },
  setup() {
    const route = useRoute();
    const {isMobile} = useBreakpoint();

    const reactiveData = reactive({
      id: String(route.query.id ?? ''),
      active: (route.query.active as string) || 'detail',
      loading: true,
      status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
      relationLoading: true,
      relationStatus: 'idle' as 'idle' | 'loading' | 'success' | 'error',
      driverTable: {} as Record<string, any>,
      statusTable: {} as Record<string, any>,
      data: {} as any,
      listDeviceData: [] as any[],
    });
    let pointRequestId = 0;
    let relationRequestId = 0;

    const point = () => {
      const currentRequestId = ++pointRequestId;
      const pointId = String(reactiveData.id || '');
      reactiveData.loading = true;
      reactiveData.status = 'loading';
      reactiveData.data = {};
      if (!pointId) {
        reactiveData.loading = false;
        reactiveData.status = 'error';
        return Promise.resolve();
      }
      return getPointById(pointId)
        .then((res) => {
          if (currentRequestId !== pointRequestId || pointId !== String(reactiveData.id || '')) return;
          reactiveData.data = res || {};
          reactiveData.status = reactiveData.data.id ? 'success' : 'error';
        })
        .catch(() => {
          if (currentRequestId === pointRequestId) reactiveData.status = 'error';
        })
        .finally(() => {
          if (currentRequestId === pointRequestId) reactiveData.loading = false;
        });
    };

    const device = () => {
      const currentRequestId = ++relationRequestId;
      const pointId = String(reactiveData.id || '');
      reactiveData.relationLoading = true;
      reactiveData.relationStatus = 'loading';
      reactiveData.listDeviceData = [];
      reactiveData.driverTable = {};
      reactiveData.statusTable = {};
      if (!pointId) {
        reactiveData.relationLoading = false;
        reactiveData.relationStatus = 'error';
        return Promise.resolve();
      }
      return getDeviceStatisticsByPointId(pointId)
        .then((res) => {
          if (currentRequestId !== relationRequestId || pointId !== String(reactiveData.id || '')) return;
          reactiveData.listDeviceData = res?.devices || [];

          // driver
          const driverIds = Array.from(new Set(reactiveData.listDeviceData.map((device) => device.driverId))).filter(
            Boolean
          );
          if (driverIds.length === 0) return;
          return Promise.allSettled([
            listDriverByIds(driverIds),
            Promise.all(driverIds.map((driverId) => listDeviceStatusByDriverId(driverId))),
          ]).then(([drivers, statuses]) => {
            if (currentRequestId !== relationRequestId) return;
            let partialFailure = false;
            if (drivers.status === 'fulfilled') reactiveData.driverTable = drivers.value || {};
            else partialFailure = true;
            if (statuses.status === 'fulfilled') {
              reactiveData.statusTable = statuses.value.reduce<Record<string, any>>((pre, cur) => ({
                ...pre,
                ...(cur || {}),
              }), {});
            } else partialFailure = true;
            reactiveData.relationStatus = partialFailure ? 'error' : 'success';
          });
        })
        .then(() => {
          if (currentRequestId === relationRequestId && reactiveData.relationStatus === 'loading') {
            reactiveData.relationStatus = 'success';
          }
        })
        .catch(() => {
          if (currentRequestId === relationRequestId) reactiveData.relationStatus = 'error';
        })
        .finally(() => {
          if (currentRequestId === relationRequestId) reactiveData.relationLoading = false;
        });
    };

    const reload = () => {
      load();
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

    const load = () => {
      void point();
      void device();
    };

    watch(
      () => [route.query.id, route.query.active],
      ([id, active]) => {
        const nextId = String(id ?? '');
        if (nextId !== String(reactiveData.id || '')) {
          reactiveData.id = nextId;
          reactiveData.data = {};
          reactiveData.listDeviceData = [];
          reactiveData.driverTable = {};
          reactiveData.statusTable = {};
          load();
        }
        reactiveData.active = String(active || 'detail');
      },
    );

    onBeforeUnmount(() => {
      pointRequestId += 1;
      relationRequestId += 1;
    });

    load();

    return {
      reactiveData,
      point,
      device,
      reload,
      deviceName,
      changeActive,
      timestamp,
      isMobile,
    };
  },
});
