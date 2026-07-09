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

import {computed, defineComponent, reactive, ref, watch} from 'vue';

import router from '@/config/router';
import {useRoute} from 'vue-router';

import {getProfileById} from '@/api/profile';

import baseCard from '@/components/card/base/BaseCard.vue';
import detailCard from '@/components/card/detail/DetailCard.vue';
import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
import deviceCard from '@/views/device/card/DeviceCard.vue';
import pointCard from '@/views/point/card/PointCard.vue';
import device from '@/views/device/Device.vue';
import point from '@/views/point/Point.vue';
import CommandList from '@/views/settings/command/CommandList.vue';
import EventList from '@/views/settings/event/definition/EventList.vue';

import {timestamp} from '@/utils/dateUtil';

export default defineComponent({
  components: {
    baseCard,
    detailCard,
    skeletonCard,
    deviceCard,
    device,
    pointCard,
    point,
    CommandList,
    EventList,
  },
  setup() {
    const route = useRoute();

    const pointViewRef: any = ref<InstanceType<typeof point>>();
    const deviceViewRef: any = ref<InstanceType<typeof device>>();
    const commandViewRef: any = ref<InstanceType<typeof CommandList>>();
    const eventViewRef: any = ref<InstanceType<typeof EventList>>();

    // 定义响应式数据
    const reactiveData = reactive({
      id: route.query.id as string,
      active: (route.query.active as string) || 'detail',
      deviceLoading: true,
      pointLoading: true,
      driverTable: {} as any,
      profileTable: {} as any,
      statusTable: {} as any,
      data: {} as any,
      listDeviceData: [] as any[],
      listPointData: [] as any[],
    });

    const pointLength = computed(() => {
      return pointViewRef.value?.reactiveData?.page?.total || 0;
    });

    const deviceLength = computed(() => {
      return deviceViewRef.value?.reactiveData?.page?.total || 0;
    });

    const commandLength = computed(() => {
      return commandViewRef.value?.reactiveData?.page?.total || 0;
    });

    const eventLength = computed(() => {
      return eventViewRef.value?.reactiveData?.page?.total || 0;
    });

    const profile = () => {
      getProfileById(reactiveData.id).then((res) => {
        reactiveData.data = res.data;
      });
    };

    const changeActive = (tab: any) => {
      reactiveData.active = tab.props.name;
      const query = route.query;
      router.push({query: {...query, active: tab.props.name}});
      switch (tab.props.name) {
        case 'device':
          deviceViewRef.value?.refresh();
          break;
        case 'point':
          pointViewRef.value?.refresh();
          break;
        case 'command':
          commandViewRef.value?.refresh();
          break;
        case 'event':
          eventViewRef.value?.refresh();
          break;
        default:
          break;
      }
    };

    watch(
      () => [route.query.id, route.query.active],
      ([id, active]) => {
        const nextId = id as string;
        if (nextId && nextId !== reactiveData.id) {
          reactiveData.id = nextId;
          profile();
        }
        reactiveData.active = (active as string) || 'detail';
      }
    );

    profile();

    return {
      pointViewRef,
      deviceViewRef,
      commandViewRef,
      eventViewRef,
      reactiveData,
      pointLength,
      deviceLength,
      commandLength,
      eventLength,
      changeActive,
      timestamp,
    };
  },
});
