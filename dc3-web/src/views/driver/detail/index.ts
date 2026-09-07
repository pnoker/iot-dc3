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

import {computed, defineComponent, onBeforeUnmount, reactive, ref, watch} from 'vue';

import {useRoute} from 'vue-router';
import router from '@/config/router';

import {getDriverById} from '@/api/driver';

import blankCard from '@/components/card/blank/BlankCard.vue';
import baseCard from '@/components/card/base/BaseCard.vue';
import detailCard from '@/components/card/detail/DetailCard.vue';
import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
import driverTool from '@/views/driver/tool/DriverTool.vue';
import deviceList from '@/views/device/Device.vue';
import device from '@/views/device/Device.vue';
import driverCard from '@/views/driver/card/DriverCard.vue';
import deviceCard from '@/views/device/card/DeviceCard.vue';
import pointCard from '@/views/point/card/PointCard.vue';

import {timestamp} from '@/utils/dateUtil';
import {useBreakpoint} from '@/composables/useBreakpoint';

export default defineComponent({
  name: 'DriverDetail',
  components: {
    blankCard,
    baseCard,
    detailCard,
    skeletonCard,
    driverTool,
    deviceList,
    driverCard,
    deviceCard,
    device,
    pointCard,
  },
  setup() {
    const route = useRoute();
    const {isMobile} = useBreakpoint();

    const deviceViewRef: any = ref<InstanceType<typeof device>>();

    const reactiveData = reactive({
      id: String(route.query.id ?? ''),
      active: (route.query.active as string) || 'detail',
      loading: true,
      status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
      data: {} as any,
    });
    let requestId = 0;

    const deviceLength = computed(() => {
      return deviceViewRef.value?.reactiveData?.page?.total || 0;
    });

    const driver = () => {
      const currentRequestId = ++requestId;
      const driverId = String(reactiveData.id || '');
      reactiveData.loading = true;
      reactiveData.status = 'loading';
      reactiveData.data = {};
      if (!driverId) {
        reactiveData.loading = false;
        reactiveData.status = 'error';
        return Promise.resolve();
      }
      return getDriverById(driverId)
        .then((res) => {
          if (currentRequestId !== requestId || driverId !== String(reactiveData.id || '')) return;
          reactiveData.data = res || {};
          reactiveData.status = reactiveData.data.id ? 'success' : 'error';
        })
        .catch(() => {
          if (currentRequestId === requestId) reactiveData.status = 'error';
        })
        .finally(() => {
          if (currentRequestId === requestId) reactiveData.loading = false;
        });
    };

    const changeActive = (tab: any) => {
      reactiveData.active = tab.props.name;
      const query = route.query;
      switch (reactiveData.active) {
        case 'detail':
          driver();
          break;
        case 'device':
          deviceViewRef.value?.refresh();
          break;
        case 'model':
          // to do something
          break;
        case 'event':
          // to do something
          break;
        default:
          break;
      }
      router.push({query: {...query, active: tab.props.name}}).catch(() => undefined);
    };

    watch(
      () => [route.query.id, route.query.active],
      ([id, active]) => {
        const nextId = String(id ?? '');
        if (nextId !== String(reactiveData.id || '')) {
          reactiveData.id = nextId;
          reactiveData.data = {};
          driver();
        }
        reactiveData.active = String(active || 'detail');
      },
    );

    onBeforeUnmount(() => {
      requestId += 1;
    });

    driver();

    return {
      deviceViewRef,
      reactiveData,
      deviceLength,
      driver,
      changeActive,
      timestamp,
      isMobile,
    };
  },
});
