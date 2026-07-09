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

import {computed, defineComponent, reactive, ref} from 'vue';

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

    const deviceViewRef: any = ref<InstanceType<typeof device>>();

    // 定义响应式数据
    const reactiveData = reactive({
      id: route.query.id as string,
      active: (route.query.active as string) || 'detail',
      data: {} as any,
    });

    const deviceLength = computed(() => {
      return deviceViewRef.value?.reactiveData?.page?.total || 0;
    });

    // 加载驱动数据
    const driver = () => {
      getDriverById(reactiveData.id).then((res) => {
        reactiveData.data = res.data;
      });
    };

    // 切换Tab
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
      router.push({query: {...query, active: tab.props.name}});
    };

    driver();

    return {
      deviceViewRef,
      reactiveData,
      deviceLength,
      changeActive,
      timestamp,
    };
  },
});
