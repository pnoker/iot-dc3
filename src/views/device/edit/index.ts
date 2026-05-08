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

import { computed, defineComponent, reactive, ref, unref } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { Back, Check, Edit, RefreshLeft, Right } from '@element-plus/icons-vue';

import { useRoute } from 'vue-router';
import router from '@/config/router';

import { getDriverDictionary, getProfileDictionary } from '@/api/dictionary';
import { getDeviceById, updateDevice } from '@/api/device';
import { getDriverAttributeByDriverId, getPointAttributeByDriverId } from '@/api/attribute';
import {
  addDriverInfo,
  addPointInfo,
  getDriverInfoByDeviceId,
  getPointInfoByDeviceId,
  updateDriverInfo,
  updatePointInfo,
} from '@/api/info';

import type { Dictionary } from '@/config/entity';

import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
import pointInfoCard from '@/views/point/info/PointInfoCard.vue';
import { isNull } from '@/utils/ValidationUtil';
import { getDriverById } from '@/api/driver';
import { getProfileByIds } from '@/api/profile';
import { getPointByDeviceId } from '@/api/point';
import { nameRules, remarkRules } from '@/utils/FormRuleUtil';
import { useI18n } from 'vue-i18n';

export default defineComponent({
  name: 'DeviceEdit',
  components: {
    skeletonCard,
    pointInfoCard,
  },
  setup() {
    const route = useRoute();
    const { t } = useI18n();

    // 定义表单引用
    const deviceFormRef = ref<FormInstance>();
    const driverFormRef = ref<FormInstance>();
    const pointFormRef = ref<FormInstance>();

    // 图标
    const Icon = {
      Edit,
      RefreshLeft,
      Right,
      Back,
      Check,
    };

    // 定义响应式数据
    const reactiveData = reactive({
      id: route.query.id as string,
      active: +(route.query.active || 0),
      loading: true,
      oldDeviceFormData: {} as Record<string, any>,
      deviceFormData: {} as any,
      driverAttributes: [] as any[],
      driverAttributeTable: {} as Record<string, any>,
      oldDriverFormData: {} as Record<string, any>,
      driverFormData: {} as any,
      pointAttributes: [] as any[],
      pointAttributeTable: {} as Record<string, any>,
      oldPointFormData: {} as Record<string, any>,
      pointFormData: {} as any,
      pointInfoData: [] as any[],
      driverDictionary: [] as Dictionary[],
      driverLoading: false,
      profileDictionary: [] as Dictionary[],
      profileLoading: false,
    });

    // 定义表单校验规则
    const deviceFormRule = reactive<FormRules>({
      deviceName: nameRules(t, '设备'),
      driverId: [
        {
          required: true,
          message: () => t('device.edit.driverRequired'),
          trigger: 'change',
        },
      ],
      profileIds: [
        {
          required: true,
          message: () => t('device.add.profileRequired'),
          trigger: 'change',
        },
      ],
      enableFlag: [
        {
          required: true,
          message: 'Select enable status',
          trigger: 'change',
        },
      ],
      remark: remarkRules(t),
    });

    const hasPointFormData = computed(() => {
      return !isNull(reactiveData.pointFormData);
    });

    // Some drivers don't expose any configurable attributes. In that case we
    // still want to render step 2 (with prev / next buttons + an empty hint)
    // instead of blanking the whole card, which used to trap the user.
    const hasDriverAttributes = computed(() => {
      return Array.isArray(reactiveData.driverAttributes) && reactiveData.driverAttributes.length > 0;
    });

    const driverDictionary = (query?: string) => {
      reactiveData.driverLoading = true;
      getDriverDictionary({
        page: { size: 50, current: 1 },
        label: query || '',
      })
        .then((res) => {
          reactiveData.driverDictionary = res.data.records;
        })
        .catch(() => {
          // nothing to do
        })
        .finally(() => {
          reactiveData.driverLoading = false;
        });
    };

    const driverDictionaryVisible = (visible: boolean) => {
      if (visible) driverDictionary('');
    };

    const profileDictionary = (query?: string) => {
      reactiveData.profileLoading = true;
      getProfileDictionary({
        page: { size: 50, current: 1 },
        label: query || '',
      })
        .then((res) => {
          reactiveData.profileDictionary = res.data.records;
        })
        .catch(() => {
          // nothing to do
        })
        .finally(() => {
          reactiveData.profileLoading = false;
        });
    };

    const profileDictionaryVisible = (visible: boolean) => {
      if (visible) profileDictionary('');
    };

    const device = () => {
      getDeviceById(reactiveData.id)
        .then((res) => {
          reactiveData.deviceFormData = res.data;
          reactiveData.oldDeviceFormData = { ...res.data };

          getDriverById(reactiveData.deviceFormData.driverId).then((res) => {
            const driver = res.data;
            reactiveData.driverDictionary.push({
              label: driver.driverName,
              value: driver.id,
            } as Dictionary);
          });

          getProfileByIds(reactiveData.deviceFormData.profileIds).then((res) => {
            const profiles = res.data;
            for (const key in profiles) {
              const profile = profiles[key];
              if (!profile) continue;
              reactiveData.profileDictionary.push({
                label: profile.profileName,
                value: profile.id,
              } as Dictionary);
            }
          });

          changeAttribute(reactiveData.deviceFormData.driverId);
        })
        .catch(() => {
          // nothing to do
        });
    };

    const changeAttribute = (driverId: string) => {
      if (isNull(driverId)) {
        return;
      }

      getDriverAttributeByDriverId(driverId)
        .then((res) => {
          reactiveData.driverAttributes = res.data;
          reactiveData.driverAttributeTable = reactiveData.driverAttributes.reduce(
            (pre, cur) => {
              pre[cur.id] = cur.attributeCode;
              return pre;
            },
            {} as Record<string, any>
          );

          const driverFormData: Record<string, any> = {};
          reactiveData.driverAttributes.forEach((attribute) => {
            driverFormData[attribute.attributeCode] = {
              id: null,
              configValue: '',
            };
          });
          reactiveData.driverFormData = JSON.parse(JSON.stringify(driverFormData));
          reactiveData.oldDriverFormData = JSON.parse(JSON.stringify(driverFormData));

          driverInfo();
        })
        .catch(() => {
          // nothing to do
        });

      getPointAttributeByDriverId(driverId)
        .then((res) => {
          reactiveData.pointAttributes = res.data;
          reactiveData.pointAttributeTable = reactiveData.pointAttributes.reduce(
            (pre, cur) => {
              pre[cur.id] = cur.attributeCode;
              return pre;
            },
            {} as Record<string, any>
          );

          pointInfo();
        })
        .catch(() => {
          // nothing to do
        })
        .finally(() => {
          reactiveData.loading = false;
        });
    };

    const driverInfo = () => {
      getDriverInfoByDeviceId(reactiveData.id)
        .then((res) => {
          const formData: Record<string, any> = reactiveData.driverFormData;
          res.data.forEach((info: { attributeId: string | number; id: any; configValue: any }) => {
            formData[reactiveData.driverAttributeTable[info.attributeId]] = {
              id: info.id,
              configValue: info.configValue,
            };
          });

          reactiveData.driverFormData = JSON.parse(JSON.stringify(formData));
          reactiveData.oldDriverFormData = JSON.parse(JSON.stringify(formData));
        })
        .catch(() => {
          // nothing to do
        });
    };

    const pointInfo = () => {
      getPointByDeviceId(reactiveData.id)
        .then((res) => {
          reactiveData.pointInfoData = res.data.map((point: { id: any; pointName: any }) => {
            const pointInfo: Record<string, any> = {
              id: point.id,
              pointName: point.pointName,
              shadow: 'hover',
            };

            reactiveData.pointAttributes.forEach((attribute) => {
              pointInfo[attribute.attributeCode] = {
                id: null,
                configValue: '',
              };
            });
            return pointInfo;
          });

          getPointInfoByDeviceId(reactiveData.id)
            .then((res) => {
              res.data.forEach((info: { pointId: any; attributeId: string | number; id: any; configValue: any }) => {
                reactiveData.pointInfoData.forEach((pointInfo) => {
                  if (pointInfo.id === info.pointId) {
                    pointInfo[reactiveData.pointAttributeTable[info.attributeId]] = {
                      id: info.id,
                      configValue: info.configValue,
                    };
                  }
                  return pointInfo;
                });
              });
            })
            .catch(() => {
              // nothing to do
            });
        })
        .catch(() => {
          // nothing to do
        });
    };

    const deviceUpdate = async (): Promise<boolean> => {
      const form = unref(deviceFormRef);
      if (!form) {
        return false;
      }

      try {
        await form.validate();
        const res = await updateDevice(reactiveData.deviceFormData);
        reactiveData.oldDeviceFormData = { ...res.data };
        return true;
      } catch {
        return false;
      }
    };

    const driverUpdate = async (): Promise<boolean> => {
      const form = unref(driverFormRef);
      if (!form) {
        return false;
      }

      try {
        await form.validate();
        const driverFormData: Record<string, unknown> = {};
        await Promise.all(
          reactiveData.driverAttributes.map((attribute) => {
            const driverInfo = {
              id: reactiveData.driverFormData[attribute.attributeCode].id,
              attributeId: attribute.id,
              deviceId: reactiveData.id,
              configValue: reactiveData.driverFormData[attribute.attributeCode].configValue,
            };

            const persist = driverInfo.id ? updateDriverInfo(driverInfo) : addDriverInfo(driverInfo);
            return persist
              .then(() => {
                driverFormData[attribute.attributeCode] = {
                  id: driverInfo.id,
                  configValue: driverInfo.configValue,
                };
                reactiveData.oldDriverFormData = JSON.parse(JSON.stringify(driverFormData));
              })
              .catch(() => {
                // nothing to do
              });
          })
        );
        return true;
      } catch {
        return false;
      }
    };

    const pointUpdate = async (): Promise<boolean> => {
      const form = unref(pointFormRef);
      if (!form) {
        return false;
      }

      try {
        await form.validate();
        await Promise.all(
          reactiveData.pointAttributes.map((attribute) => {
            const pointInfo = {
              id: reactiveData.pointFormData[attribute.attributeCode].id,
              attributeId: attribute.id,
              deviceId: reactiveData.id,
              pointId: reactiveData.pointFormData.id,
              configValue: reactiveData.pointFormData[attribute.attributeCode].configValue,
            };

            const persist = pointInfo.id ? updatePointInfo(pointInfo) : addPointInfo(pointInfo);
            return persist
              .then(() => {
                reactiveData.pointInfoData.forEach((row) => {
                  if (row.id === reactiveData.pointFormData.id) {
                    row[attribute.attributeCode] = {
                      id: pointInfo.id,
                      configValue: pointInfo.configValue,
                    };
                    reactiveData.oldPointFormData = JSON.parse(JSON.stringify(row));
                  }
                  return row;
                });
              })
              .catch(() => {
                // nothing to do
              });
          })
        );
        return true;
      } catch {
        return false;
      }
    };

    const selectPoint = (row: { [x: string]: { id: null; configValue: string }; id: any }) => {
      reactiveData.pointAttributes.forEach((attribute) => {
        if (!row[attribute.attributeCode]) {
          row[attribute.attributeCode] = {
            id: null,
            configValue: '',
          };
        }
      });
      reactiveData.pointFormData = JSON.parse(JSON.stringify(row));
      reactiveData.oldPointFormData = JSON.parse(JSON.stringify(row));

      reactiveData.pointInfoData.forEach((pointInfo) => {
        pointInfo.shadow = 'hover';
        if (row.id === pointInfo.id) {
          pointInfo.shadow = 'always';
        }
      });
    };

    const pre = () => {
      let step = 1;
      if (reactiveData.active === 2 && reactiveData.driverAttributes?.length < 1) {
        step = 2;
      }
      reactiveData.active -= step;
      changeActive(reactiveData.active);
    };

    const next = async () => {
      if (reactiveData.active === 0) {
        const ok = await deviceUpdate();
        if (!ok) {
          return;
        }
      }
      if (reactiveData.active === 1) {
        const ok = await driverUpdate();
        if (!ok) {
          return;
        }
      }

      let step = 1;
      if (reactiveData.active === 0 && reactiveData.driverAttributes?.length < 1) {
        step = 2;
      }
      reactiveData.active += step;
      changeActive(reactiveData.active);
    };

    const done = () => {
      router.push({ name: 'device' }).catch(() => {
        // nothing to do
      });
    };

    const deviceReset = () => {
      reactiveData.deviceFormData = JSON.parse(JSON.stringify(reactiveData.oldDeviceFormData));
    };

    const driverInfoReset = () => {
      reactiveData.driverFormData = JSON.parse(JSON.stringify(reactiveData.oldDriverFormData));
    };

    const pointInfoReset = () => {
      reactiveData.pointFormData = JSON.parse(JSON.stringify(reactiveData.oldPointFormData));
    };

    const changeActive = (step: number) => {
      const query = route.query;
      router.push({ query: { ...query, active: step } }).catch(() => {
        // nothing to do
      });
    };

    device();

    return {
      deviceFormRef,
      driverFormRef,
      pointFormRef,
      deviceFormRule,
      reactiveData,
      hasPointFormData,
      hasDriverAttributes,
      driverDictionary,
      driverDictionaryVisible,
      profileDictionary,
      profileDictionaryVisible,
      changeAttribute,
      driverUpdate,
      pointUpdate,
      selectPoint,
      pre,
      next,
      done,
      deviceReset,
      driverInfoReset,
      pointInfoReset,
      changeActive,
      ...Icon,
    };
  },
});
