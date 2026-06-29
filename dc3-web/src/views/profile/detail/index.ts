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
