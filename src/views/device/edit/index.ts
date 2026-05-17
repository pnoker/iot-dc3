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

import type { Attribute, Dictionary } from '@/config/types';

import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue';
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import pointInfoCard from '@/views/point/info/PointInfoCard.vue';
import { isNull } from '@/utils/validationUtil';
import { getDriverById } from '@/api/driver';
import { getProfileByIds } from '@/api/profile';
import { getPointByDeviceId } from '@/api/point';
import { nameRules, remarkRules } from '@/utils/formRuleUtil';
import { useI18n } from 'vue-i18n';

type AttributeConfigValue = string | number | boolean | null;

interface AttributeFormItem {
  id?: string;
  configValue: any;
}

type AttributeFormData = Record<string, AttributeFormItem>;

const INTEGER_ATTRIBUTE_TYPES = new Set(['BYTE', 'SHORT', 'INT', 'LONG']);
const DECIMAL_ATTRIBUTE_TYPES = new Set(['FLOAT', 'DOUBLE']);
const BOOLEAN_TRUE_VALUES = new Set(['true', '1', 'yes', 'y', 'on']);
const BOOLEAN_FALSE_VALUES = new Set(['false', '0', 'no', 'n', 'off']);

function attributeType(attribute: Attribute): string {
  return String(attribute.attributeTypeFlag || 'STRING').toUpperCase();
}

function isBooleanAttribute(attribute: Attribute): boolean {
  return attributeType(attribute) === 'BOOLEAN';
}

function isNumberAttribute(attribute: Attribute): boolean {
  const type = attributeType(attribute);
  return INTEGER_ATTRIBUTE_TYPES.has(type) || DECIMAL_ATTRIBUTE_TYPES.has(type);
}

function attributePrecision(attribute: Attribute): number | undefined {
  return DECIMAL_ATTRIBUTE_TYPES.has(attributeType(attribute)) ? 3 : 0;
}

function attributePlaceholder(attribute: Attribute): string {
  return attribute.defaultValue ? `Default: ${attribute.defaultValue}` : `Enter ${attribute.attributeName}`;
}

function coerceAttributeValue(attribute: Attribute, value?: unknown): AttributeConfigValue {
  if (value === undefined || value === null || value === '') {
    return isBooleanAttribute(attribute) ? false : null;
  }

  if (isBooleanAttribute(attribute)) {
    if (typeof value === 'boolean') {
      return value;
    }
    const normalized = String(value).trim().toLowerCase();
    if (BOOLEAN_TRUE_VALUES.has(normalized)) {
      return true;
    }
    if (BOOLEAN_FALSE_VALUES.has(normalized)) {
      return false;
    }
    return false;
  }

  if (isNumberAttribute(attribute)) {
    const numericValue = Number(value);
    return Number.isFinite(numericValue) ? numericValue : null;
  }

  return String(value);
}

function createAttributeFormItem(attribute: Attribute, id?: string, value?: unknown): AttributeFormItem {
  return {
    id: id || undefined,
    configValue: coerceAttributeValue(attribute, value ?? attribute.defaultValue),
  };
}

function serializeAttributeValue(value: AttributeConfigValue): string {
  return value === null || value === undefined ? '' : String(value);
}

function attributeFormItem(formData: Record<string, AttributeFormItem>, attribute: Attribute): AttributeFormItem {
  if (!formData[attribute.attributeCode]) {
    formData[attribute.attributeCode] = createAttributeFormItem(attribute);
  }
  return formData[attribute.attributeCode] as AttributeFormItem;
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value));
}

export default defineComponent({
  name: 'DeviceEdit',
  components: {
    EnableFlagSegmented,
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
      driverAttributes: [] as Attribute[],
      driverAttributeTable: {} as Record<string, any>,
      oldDriverFormData: {} as AttributeFormData,
      driverFormData: {} as AttributeFormData,
      pointAttributes: [] as Attribute[],
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

          const driverFormData: AttributeFormData = {};
          reactiveData.driverAttributes.forEach((attribute) => {
            driverFormData[attribute.attributeCode] = createAttributeFormItem(attribute);
          });
          reactiveData.driverFormData = clone(driverFormData);
          reactiveData.oldDriverFormData = clone(driverFormData);

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
          const formData: AttributeFormData = reactiveData.driverFormData;
          res.data.forEach((info: { attributeId: string | number; id: any; configValue: any }) => {
            const attributeCode = reactiveData.driverAttributeTable[info.attributeId];
            const attribute = reactiveData.driverAttributes.find((item) => item.attributeCode === attributeCode);
            if (attribute) {
              formData[attributeCode] = createAttributeFormItem(attribute, info.id, info.configValue);
            }
          });

          reactiveData.driverFormData = clone(formData);
          reactiveData.oldDriverFormData = clone(formData);
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
              pointInfo[attribute.attributeCode] = createAttributeFormItem(attribute);
            });
            return pointInfo;
          });

          getPointInfoByDeviceId(reactiveData.id)
            .then((res) => {
              res.data.forEach((info: { pointId: any; attributeId: string | number; id: any; configValue: any }) => {
                reactiveData.pointInfoData.forEach((pointInfo) => {
                  if (pointInfo.id === info.pointId) {
                    const attributeCode = reactiveData.pointAttributeTable[info.attributeId];
                    const attribute = reactiveData.pointAttributes.find((item) => item.attributeCode === attributeCode);
                    if (attribute) {
                      pointInfo[attributeCode] = createAttributeFormItem(attribute, info.id, info.configValue);
                    }
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
        await Promise.all(
          reactiveData.driverAttributes.map((attribute) => {
            const formItem = reactiveData.driverFormData[attribute.attributeCode];
            if (!formItem) {
              return Promise.resolve();
            }
            const driverInfo = {
              id: formItem.id || undefined,
              attributeId: attribute.id,
              deviceId: reactiveData.id,
              configValue: serializeAttributeValue(formItem.configValue),
            };

            const persist = driverInfo.id ? updateDriverInfo(driverInfo) : addDriverInfo(driverInfo);
            return persist.catch(() => {
              // nothing to do
            });
          })
        );
        reactiveData.oldDriverFormData = clone(reactiveData.driverFormData);
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
            const formItem = reactiveData.pointFormData[attribute.attributeCode];
            if (!formItem) {
              return Promise.resolve();
            }
            const pointInfo = {
              id: formItem.id || undefined,
              attributeId: attribute.id,
              deviceId: reactiveData.id,
              pointId: reactiveData.pointFormData.id,
              configValue: serializeAttributeValue(formItem.configValue),
            };

            const persist = pointInfo.id ? updatePointInfo(pointInfo) : addPointInfo(pointInfo);
            return persist
              .then(() => {
                reactiveData.pointInfoData.forEach((row) => {
                  if (row.id === reactiveData.pointFormData.id) {
                    row[attribute.attributeCode] = {
                      id: pointInfo.id,
                      configValue: reactiveData.pointFormData[attribute.attributeCode].configValue,
                    };
                    reactiveData.oldPointFormData = clone(row);
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

    const selectPoint = (row: { [x: string]: AttributeFormItem | any; id: any }) => {
      reactiveData.pointAttributes.forEach((attribute) => {
        if (!row[attribute.attributeCode]) {
          row[attribute.attributeCode] = createAttributeFormItem(attribute);
        }
      });
      reactiveData.pointFormData = clone(row);
      reactiveData.oldPointFormData = clone(row);

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
      reactiveData.deviceFormData = clone(reactiveData.oldDeviceFormData);
    };

    const driverInfoReset = () => {
      reactiveData.driverFormData = clone(reactiveData.oldDriverFormData);
    };

    const pointInfoReset = () => {
      reactiveData.pointFormData = clone(reactiveData.oldPointFormData);
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
      attributeFormItem,
      attributePlaceholder,
      attributePrecision,
      isBooleanAttribute,
      isNumberAttribute,
      ...Icon,
    };
  },
});
