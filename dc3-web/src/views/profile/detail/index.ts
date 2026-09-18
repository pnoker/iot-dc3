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
import type {DeviceRecord, DriverRecord, PointRecord, ProfileRecord} from '@/config/types/manager';
import {useBreakpoint} from '@/composables/useBreakpoint';

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
    const {isMobile} = useBreakpoint();

    const pointViewRef: any = ref<InstanceType<typeof point>>();
    const deviceViewRef: any = ref<InstanceType<typeof device>>();
    const commandViewRef: any = ref<InstanceType<typeof CommandList>>();
    const eventViewRef: any = ref<InstanceType<typeof EventList>>();

    const reactiveData = reactive({
      id: String(route.query.id ?? ''),
      active: (route.query.active as string) || 'detail',
      loading: true,
      status: 'idle' as 'idle' | 'loading' | 'success' | 'error',
      driverTable: {} as DriverRecord,
      profileTable: {} as ProfileRecord,
      statusTable: {} as Record<string, unknown>,
      data: {} as ProfileRecord,
      listDeviceData: [] as DeviceRecord[],
      listPointData: [] as PointRecord[],
    });
    let requestId = 0;

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
      const currentRequestId = ++requestId;
      const profileId = String(reactiveData.id || '');
      reactiveData.loading = true;
      reactiveData.status = 'loading';
      reactiveData.data = {} as ProfileRecord;
      if (!profileId) {
        reactiveData.loading = false;
        reactiveData.status = 'error';
        return Promise.resolve();
      }
      return getProfileById(profileId)
        .then((res) => {
          if (currentRequestId !== requestId || profileId !== String(reactiveData.id || '')) return;
          reactiveData.data = res || ({} as ProfileRecord);
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
      router.push({query: {...query, active: tab.props.name}}).catch(() => undefined);
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
        const nextId = String(id ?? '');
        if (nextId !== reactiveData.id) {
          reactiveData.id = nextId;
          reactiveData.data = {} as ProfileRecord;
          profile();
        }
        reactiveData.active = (active as string) || 'detail';
      }
    );

    profile();

    onBeforeUnmount(() => {
      requestId += 1;
    });

    return {
      pointViewRef,
      deviceViewRef,
      commandViewRef,
      eventViewRef,
      reactiveData,
      profile,
      pointLength,
      deviceLength,
      commandLength,
      eventLength,
      changeActive,
      timestamp,
      isMobile,
    };
  },
});
