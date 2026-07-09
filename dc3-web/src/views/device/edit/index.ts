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

import {computed, defineComponent, onBeforeUnmount, onMounted, reactive, watch} from 'vue';
import type {FormItemRule, FormRules} from 'element-plus';
import {Search} from '@element-plus/icons-vue';

import {onBeforeRouteLeave, useRoute} from 'vue-router';
import router from '@/config/router';

import {listDriverDictionary, listProfileDictionary} from '@/api/dictionary';
import {getDeviceById, updateDevice} from '@/api/device';
import {
  listCommandAttributeByDriverId,
  listDriverAttributeByDriverId,
  listEventAttributeByDriverId,
  listPointAttributeByDriverId,
} from '@/api/attribute';
import {
  addCommandInfo,
  addDriverInfo,
  addEventInfo,
  addPointInfo,
  getDriverInfoByDeviceIdAndAttributeId,
  listCommandInfoByDeviceId,
  listDriverInfoByDeviceId,
  listEventInfoByDeviceId,
  listPointInfoByDeviceId,
  updateCommandInfo,
  updateDriverInfo,
  updateEventInfo,
  updatePointInfo,
} from '@/api/info';

import type {
  Attribute,
  CommandInfoForm,
  CommandRecord,
  Dictionary,
  EventInfoForm,
  EventRecord,
  PointInfoForm,
  PointRecord,
} from '@/config/types';

import baseCard from '@/components/card/base/BaseCard.vue';
import InfoCard from '@/components/card/info/InfoCard.vue';
import MatrixToolbar from '@/components/card/matrix/MatrixToolbar.vue';
import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import MatrixStatusSegmented from '@/components/segmented/MatrixStatusSegmented.vue';
import {isNull} from '@/utils/validationUtil';
import {failMessage, successMessage} from '@/utils/notificationUtil';
import {getDriverById} from '@/api/driver';
import {getProfileById} from '@/api/profile';
import {listPointByProfileId} from '@/api/point';
import {listCommandByProfileId} from '@/api/command';
import {listEventByProfileId} from '@/api/event';
import {nameRules, remarkRules} from '@/utils/formRuleUtil';
import {logger} from '@/utils/log';
import {type ComposerTranslation, useI18n} from 'vue-i18n';

type AttributeConfigValue = string | number | boolean | null;

interface AttributeFormItem {
  id?: string;
  configValue: any;
}

type AttributeFormData = Record<string, AttributeFormItem>;
type PointMatrixStatus = '' | 'missing' | 'configured' | 'dirty' | 'error';

interface PointAttributeCell extends AttributeFormItem {
  attributeId: string;
  originalValue: AttributeConfigValue;
  dirty: boolean;
  saving: boolean;
  error: string;
}

export interface PointInfoMatrixRow {
  id: string;
  pointName: string;
  pointCode?: string;
  pointTypeFlag?: string;
  rwFlag?: string;
  enableFlag?: string;
  attributes: Record<string, PointAttributeCell>;
}

type CommandAttributeCell = PointAttributeCell;
type EventAttributeCell = PointAttributeCell;

export interface CommandInfoMatrixRow {
  id: string;
  commandName: string;
  commandCode?: string;
  commandTypeFlag?: string | number;
  callTypeFlag?: string | number;
  attributes: Record<string, CommandAttributeCell>;
}

export interface EventInfoMatrixRow {
  id: string;
  eventName: string;
  eventCode?: string;
  eventTypeFlag?: string | number;
  eventLevelFlag?: string | number;
  attributes: Record<string, EventAttributeCell>;
}

const INTEGER_ATTRIBUTE_TYPES = new Set(['BYTE', 'SHORT', 'INT', 'LONG']);
const DECIMAL_ATTRIBUTE_TYPES = new Set(['FLOAT', 'DOUBLE']);
const BOOLEAN_TRUE_VALUES = new Set(['true', '1', 'yes', 'y', 'on']);
const BOOLEAN_FALSE_VALUES = new Set(['false', '0', 'no', 'n', 'off']);
const DECIMAL_VALUE_PATTERN = /^[+-]?(?:(?:\d+\.?\d*)|(?:\.\d+))(?:[eE][+-]?\d+)?$/;
const INTEGER_VALUE_PATTERN = /^[+-]?\d+$/;
const ATTRIBUTE_CONFIG_MAX_LENGTH = 512;
const FLOAT_MAX = 3.4028234663852886e38;
const INTEGER_ATTRIBUTE_RANGES = {
  BYTE: {min: -128n, max: 127n},
  SHORT: {min: -32768n, max: 32767n},
  INT: {min: -2147483648n, max: 2147483647n},
  LONG: {min: -9223372036854775808n, max: 9223372036854775807n},
};

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

function attributePlaceholder(attribute: Attribute, t: ComposerTranslation): string {
  return attribute.defaultValue
    ? t('device.edit.defaultValue', {value: attribute.defaultValue})
    : t('device.edit.attributePlaceholder', {name: attribute.attributeName});
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

function hasConfigValue(value: unknown): boolean {
  return value !== '' && value !== null && value !== undefined;
}

function createPointAttributeCell(attribute: Attribute, id?: string, value?: unknown): PointAttributeCell {
  const configValue = hasConfigValue(value) ? coerceAttributeValue(attribute, value) : null;
  return {
    id: id || undefined,
    attributeId: attribute.id,
    configValue,
    originalValue: configValue,
    dirty: false,
    saving: false,
    error: '',
  };
}

function serializeAttributeValue(value: unknown): string {
  return value === null || value === undefined ? '' : String(value);
}

function validateAttributeConfigValue(
  attribute: Attribute,
  value: unknown,
  t: ComposerTranslation,
  required = false
): string {
  const rawValue = serializeAttributeValue(value).trim();
  if (required && !rawValue) {
    return t('device.edit.attributeConfigRequired');
  }
  if (rawValue.length > ATTRIBUTE_CONFIG_MAX_LENGTH) {
    return t('device.edit.attributeConfigLength', {max: ATTRIBUTE_CONFIG_MAX_LENGTH});
  }

  if (!isNumberAttribute(attribute)) {
    return '';
  }

  const type = attributeType(attribute);
  if (!rawValue) {
    return t('device.edit.attributeConfigRequired');
  }

  if (!DECIMAL_VALUE_PATTERN.test(rawValue)) {
    return t('device.edit.attributeNumberFormat');
  }

  if (INTEGER_ATTRIBUTE_TYPES.has(type)) {
    if (!INTEGER_VALUE_PATTERN.test(rawValue)) {
      return t('device.edit.attributeIntegerFormat');
    }

    const range = INTEGER_ATTRIBUTE_RANGES[type as keyof typeof INTEGER_ATTRIBUTE_RANGES];
    if (range) {
      const normalizedValue = rawValue.startsWith('+') ? rawValue.slice(1) : rawValue;
      const integerValue = BigInt(normalizedValue);
      if (integerValue < range.min || integerValue > range.max) {
        return t('device.edit.attributeRange', {
          min: range.min.toString(),
          max: range.max.toString(),
        });
      }
    }

    return '';
  }

  const numericValue = Number(rawValue);
  if (!Number.isFinite(numericValue)) {
    return t('device.edit.attributeNumberFormat');
  }

  const max = 'FLOAT' === type ? FLOAT_MAX : Number.MAX_VALUE;
  if (Math.abs(numericValue) > max) {
    return t('device.edit.attributeRange', {
      min: `-${max}`,
      max: String(max),
    });
  }

  return '';
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

const DEVICE_EDIT_TABS = ['deviceConfig', 'driverConfig', 'pointConfig', 'commandConfig', 'eventConfig'] as const;

function resolveDeviceTab(value: unknown): string {
  const str = String(value ?? '');
  if ((DEVICE_EDIT_TABS as readonly string[]).includes(str)) return str;
  return 'deviceConfig';
}

export default defineComponent({
  name: 'DeviceEdit',
  components: {
    baseCard,
    InfoCard,
    MatrixToolbar,
    EnableFlagSegmented,
    MatrixStatusSegmented,
  },
  setup() {
    const route = useRoute();
    const {t} = useI18n();

    // 定义响应式数据
    const reactiveData = reactive({
      id: route.query.id as string,
      active: resolveDeviceTab(route.query.active),
      loading: true,
      oldDeviceFormData: {} as Record<string, any>,
      deviceFormData: {} as any,
      driverAttributes: [] as Attribute[],
      driverAttributeTable: {} as Record<string, any>,
      oldDriverFormData: {} as AttributeFormData,
      driverFormData: {} as AttributeFormData,
      driverSaving: false,
      pointAttributes: [] as Attribute[],
      pointAttributeTable: {} as Record<string, any>,
      pointInfoData: [] as PointInfoMatrixRow[],
      oldPointInfoData: [] as PointInfoMatrixRow[],
      pointMatrixKeyword: '',
      pointMatrixStatus: '' as PointMatrixStatus,
      pointSaving: false,
      pointPageSize: 10,
      pointPageCurrent: 1,
      commandAttributes: [] as Attribute[],
      commandAttributeTable: {} as Record<string, any>,
      commandInfoData: [] as CommandInfoMatrixRow[],
      oldCommandInfoData: [] as CommandInfoMatrixRow[],
      commandMatrixKeyword: '',
      commandMatrixStatus: '' as PointMatrixStatus,
      commandSaving: false,
      commandPageSize: 10,
      commandPageCurrent: 1,
      eventAttributes: [] as Attribute[],
      eventAttributeTable: {} as Record<string, any>,
      eventInfoData: [] as EventInfoMatrixRow[],
      oldEventInfoData: [] as EventInfoMatrixRow[],
      eventMatrixKeyword: '',
      eventMatrixStatus: '' as PointMatrixStatus,
      eventSaving: false,
      eventPageSize: 10,
      eventPageCurrent: 1,
      driverDictionary: [] as Dictionary[],
      driverLoading: false,
      profileDictionary: [] as Dictionary[],
      profileLoading: false,
    });

    // 定义表单校验规则
    const deviceFormRule = reactive<FormRules>({
      deviceName: nameRules(t, t('common.entityDevice')),
      driverId: [
        {
          required: true,
          message: () => t('device.edit.driverRequired'),
          trigger: 'change',
        },
      ],
      profileId: [
        {
          required: true,
          message: () => t('device.edit.profileRequired'),
          trigger: 'change',
        },
      ],
      enableFlag: [
        {
          message: () => t('device.edit.enableFlagRequired'),
          trigger: 'change',
        },
      ],
      remark: remarkRules(t),
    });

    const hasPointAttributes = computed(() => reactiveData.pointAttributes.length > 0);
    const hasCommandAttributes = computed(() => reactiveData.commandAttributes.length > 0);
    const hasEventAttributes = computed(() => reactiveData.eventAttributes.length > 0);

    const pointDirtyCount = computed(() => {
      return reactiveData.pointInfoData.reduce((sum, row) => {
        return sum + Object.values(row.attributes).filter((cell) => cell.dirty).length;
      }, 0);
    });

    const commandDirtyCount = computed(() => {
      return reactiveData.commandInfoData.reduce((sum, row) => {
        return sum + Object.values(row.attributes).filter((cell) => cell.dirty).length;
      }, 0);
    });

    const eventDirtyCount = computed(() => {
      return reactiveData.eventInfoData.reduce((sum, row) => {
        return sum + Object.values(row.attributes).filter((cell) => cell.dirty).length;
      }, 0);
    });

    const filteredPointInfoData = computed(() => {
      const keyword = reactiveData.pointMatrixKeyword.trim().toLowerCase();
      return reactiveData.pointInfoData.filter((row) => {
        const matchesKeyword =
          !keyword ||
          row.pointName.toLowerCase().includes(keyword) ||
          String(row.pointCode || '')
            .toLowerCase()
            .includes(keyword);
        if (!matchesKeyword) return false;

        const status = reactiveData.pointMatrixStatus;
        if (!status) return true;
        if ('dirty' === status) return isPointRowDirty(row);
        if ('error' === status) return isPointRowError(row);
        if ('configured' === status) return isPointRowConfigured(row);
        if ('missing' === status) return !isPointRowConfigured(row);
        return true;
      });
    });

    const filteredCommandInfoData = computed(() => {
      const keyword = reactiveData.commandMatrixKeyword.trim().toLowerCase();
      return reactiveData.commandInfoData.filter((row) => {
        const matchesKeyword =
          !keyword ||
          row.commandName.toLowerCase().includes(keyword) ||
          String(row.commandCode || '')
            .toLowerCase()
            .includes(keyword);
        if (!matchesKeyword) return false;

        const status = reactiveData.commandMatrixStatus;
        if (!status) return true;
        if ('dirty' === status) return isCommandRowDirty(row);
        if ('error' === status) return isCommandRowError(row);
        if ('configured' === status) return isCommandRowConfigured(row);
        if ('missing' === status) return !isCommandRowConfigured(row);
        return true;
      });
    });

    const filteredEventInfoData = computed(() => {
      const keyword = reactiveData.eventMatrixKeyword.trim().toLowerCase();
      return reactiveData.eventInfoData.filter((row) => {
        const matchesKeyword =
          !keyword ||
          row.eventName.toLowerCase().includes(keyword) ||
          String(row.eventCode || '')
            .toLowerCase()
            .includes(keyword);
        if (!matchesKeyword) return false;

        const status = reactiveData.eventMatrixStatus;
        if (!status) return true;
        if ('dirty' === status) return isEventRowDirty(row);
        if ('error' === status) return isEventRowError(row);
        if ('configured' === status) return isEventRowConfigured(row);
        if ('missing' === status) return !isEventRowConfigured(row);
        return true;
      });
    });

    // Client-side pagination helpers — slice the filtered arrays so long
    // point / command / event lists don't force the user to scroll forever.
    const paginatedPointInfoData = computed(() => {
      const filtered = filteredPointInfoData.value;
      const start = (reactiveData.pointPageCurrent - 1) * reactiveData.pointPageSize;
      return filtered.slice(start, start + reactiveData.pointPageSize);
    });

    const paginatedCommandInfoData = computed(() => {
      const filtered = filteredCommandInfoData.value;
      const start = (reactiveData.commandPageCurrent - 1) * reactiveData.commandPageSize;
      return filtered.slice(start, start + reactiveData.commandPageSize);
    });

    const paginatedEventInfoData = computed(() => {
      const filtered = filteredEventInfoData.value;
      const start = (reactiveData.eventPageCurrent - 1) * reactiveData.eventPageSize;
      return filtered.slice(start, start + reactiveData.eventPageSize);
    });

    // Reset pagination to page 1 whenever the search keyword or status filter changes.
    watch(
      () => reactiveData.pointMatrixKeyword,
      () => {
        reactiveData.pointPageCurrent = 1;
      }
    );
    watch(
      () => reactiveData.pointMatrixStatus,
      () => {
        reactiveData.pointPageCurrent = 1;
      }
    );
    watch(
      () => reactiveData.commandMatrixKeyword,
      () => {
        reactiveData.commandPageCurrent = 1;
      }
    );
    watch(
      () => reactiveData.commandMatrixStatus,
      () => {
        reactiveData.commandPageCurrent = 1;
      }
    );
    watch(
      () => reactiveData.eventMatrixKeyword,
      () => {
        reactiveData.eventPageCurrent = 1;
      }
    );
    watch(
      () => reactiveData.eventMatrixStatus,
      () => {
        reactiveData.eventPageCurrent = 1;
      }
    );

    // Some drivers don't expose any configurable attributes. In that case we
    // still want to render step 2 (with prev / next buttons + an empty hint)
    // instead of blanking the whole card, which used to trap the user.
    const hasDriverAttributes = computed(() => {
      return Array.isArray(reactiveData.driverAttributes) && reactiveData.driverAttributes.length > 0;
    });

    const attributeFormItemRules = (attribute: Attribute): FormItemRule[] => [
      {
        validator: (_rule, value, callback) => {
          const message = validateAttributeConfigValue(attribute, value, t, true);
          if (message) {
            callback(new Error(message));
            return;
          }
          callback();
        },
        trigger: 'blur',
      },
    ];

    const attributeInputPlaceholder = (attribute: Attribute): string => attributePlaceholder(attribute, t);

    const driverDictionary = (query?: string) => {
      reactiveData.driverLoading = true;
      listDriverDictionary({
        page: {size: 50, current: 1},
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
      listProfileDictionary({
        page: {size: 50, current: 1},
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
          reactiveData.oldDeviceFormData = {...res.data};

          getDriverById(reactiveData.deviceFormData.driverId).then((res) => {
            const driver = res.data;
            reactiveData.driverDictionary.push({
              label: driver.driverName,
              value: driver.id,
            } as Dictionary);
          });

          if (reactiveData.deviceFormData.profileId) {
            getProfileById(String(reactiveData.deviceFormData.profileId)).then((res) => {
              const profile = res.data;
              if (!profile) return;
              reactiveData.profileDictionary.push({
                label: profile.profileName,
                value: profile.id,
              } as Dictionary);
            });
          }

          changeAttribute(reactiveData.deviceFormData.driverId);
        })
        .catch(() => {
          // nothing to do
        });
    };

    const changeAttribute = (driverId: string) => {
      if (isNull(driverId)) {
        reactiveData.loading = false;
        return;
      }
      reactiveData.loading = true;

      Promise.allSettled([
        listDriverAttributeByDriverId(driverId)
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
            reactiveData.driverAttributes = [];
            reactiveData.driverAttributeTable = {};
            reactiveData.driverFormData = {};
            reactiveData.oldDriverFormData = {};
          }),

        listPointAttributeByDriverId(driverId)
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
            reactiveData.pointAttributes = [];
            reactiveData.pointAttributeTable = {};
            reactiveData.pointInfoData = [];
            reactiveData.oldPointInfoData = [];
          }),

        listCommandAttributeByDriverId(driverId)
          .then((res) => {
            reactiveData.commandAttributes = res.data;
            reactiveData.commandAttributeTable = reactiveData.commandAttributes.reduce(
              (pre, cur) => {
                pre[cur.id] = cur.attributeCode;
                return pre;
              },
              {} as Record<string, any>
            );
            commandInfo();
          })
          .catch(() => {
            reactiveData.commandAttributes = [];
            reactiveData.commandAttributeTable = {};
            reactiveData.commandInfoData = [];
            reactiveData.oldCommandInfoData = [];
          }),

        listEventAttributeByDriverId(driverId)
          .then((res) => {
            reactiveData.eventAttributes = res.data;
            reactiveData.eventAttributeTable = reactiveData.eventAttributes.reduce(
              (pre, cur) => {
                pre[cur.id] = cur.attributeCode;
                return pre;
              },
              {} as Record<string, any>
            );
            eventInfo();
          })
          .catch(() => {
            reactiveData.eventAttributes = [];
            reactiveData.eventAttributeTable = {};
            reactiveData.eventInfoData = [];
            reactiveData.oldEventInfoData = [];
          }),
      ]).finally(() => {
        reactiveData.loading = false;
      });
    };

    const driverInfo = () => {
      listDriverInfoByDeviceId(reactiveData.id)
        .then((res) => {
          const formData: AttributeFormData = reactiveData.driverFormData;
          res.data.forEach((info: {attributeId: string | number; id: any; configValue: any}) => {
            const attributeCode = reactiveData.driverAttributeTable[info.attributeId];
            const attribute = reactiveData.driverAttributes.find((item) => item.attributeCode === attributeCode);
            if (attribute) {
              formData[attributeCode] = createAttributeFormItem(attribute, info.id, info.configValue);
            }
          });

          reactiveData.driverFormData = clone(formData);
          reactiveData.oldDriverFormData = clone(formData);
          driverDirtySet.clear();
        })
        .catch(() => {
          // nothing to do
        });
    };

    const pointInfo = () => {
      const profileId = String(reactiveData.deviceFormData.profileId || '');
      if (isNull(profileId)) {
        reactiveData.pointInfoData = [];
        reactiveData.oldPointInfoData = [];
        reactiveData.loading = false;
        return;
      }

      reactiveData.loading = true;
      listPointByProfileId(profileId)
        .then((res) => {
          const rows: PointInfoMatrixRow[] = (res.data || []).map((point: PointRecord) => {
            const attributes: Record<string, PointAttributeCell> = {};

            reactiveData.pointAttributes.forEach((attribute) => {
              attributes[attribute.attributeCode] = createPointAttributeCell(attribute);
            });

            return {
              id: point.id,
              pointName: point.pointName || '',
              pointCode: point.pointCode,
              pointTypeFlag: point.pointTypeFlag,
              rwFlag: point.rwFlag,
              enableFlag: point.enableFlag,
              attributes,
            };
          });

          const rowTable = rows.reduce(
            (table, row) => {
              table[row.id] = row;
              return table;
            },
            {} as Record<string, PointInfoMatrixRow>
          );

          return listPointInfoByDeviceId(reactiveData.id)
            .then((infoRes) => {
              (infoRes.data || []).forEach(
                (info: {pointId: string; attributeId: string | number; id: string; configValue: unknown}) => {
                  const attributeCode = reactiveData.pointAttributeTable[info.attributeId];
                  const attribute = reactiveData.pointAttributes.find((item) => item.attributeCode === attributeCode);
                  const row = rowTable[info.pointId];
                  if (row && attribute) {
                    row.attributes[attributeCode] = createPointAttributeCell(attribute, info.id, info.configValue);
                  }
                }
              );
              reactiveData.pointInfoData = rows;
              reactiveData.oldPointInfoData = clone(rows);
            })
            .catch(() => {
              reactiveData.pointInfoData = rows;
              reactiveData.oldPointInfoData = clone(rows);
            });
        })
        .catch(() => {
          reactiveData.pointInfoData = [];
          reactiveData.oldPointInfoData = [];
        })
        .finally(() => {
          reactiveData.loading = false;
        });
    };

    const commandInfo = () => {
      const profileId = String(reactiveData.deviceFormData.profileId || '');
      if (isNull(profileId)) {
        reactiveData.commandInfoData = [];
        reactiveData.oldCommandInfoData = [];
        return;
      }

      listCommandByProfileId(profileId)
        .then((res) => {
          const rows: CommandInfoMatrixRow[] = (res.data || []).map((command: CommandRecord) => {
            const attributes: Record<string, CommandAttributeCell> = {};

            reactiveData.commandAttributes.forEach((attribute) => {
              attributes[attribute.attributeCode] = createPointAttributeCell(attribute);
            });

            return {
              id: command.id,
              commandName: command.commandName || '',
              commandCode: command.commandCode,
              commandTypeFlag: command.commandTypeFlag,
              callTypeFlag: command.callTypeFlag,
              attributes,
            };
          });

          const rowTable = rows.reduce(
            (table, row) => {
              table[row.id] = row;
              return table;
            },
            {} as Record<string, CommandInfoMatrixRow>
          );

          return listCommandInfoByDeviceId(reactiveData.id)
            .then((infoRes) => {
              (infoRes.data || []).forEach(
                (info: {commandId: string; attributeId: string | number; id: string; configValue: unknown}) => {
                  const attributeCode = reactiveData.commandAttributeTable[info.attributeId];
                  const attribute = reactiveData.commandAttributes.find((item) => item.attributeCode === attributeCode);
                  const row = rowTable[String(info.commandId)];
                  if (row && attribute) {
                    row.attributes[attributeCode] = createPointAttributeCell(attribute, info.id, info.configValue);
                  }
                }
              );
              reactiveData.commandInfoData = rows;
              reactiveData.oldCommandInfoData = clone(rows);
            })
            .catch(() => {
              reactiveData.commandInfoData = rows;
              reactiveData.oldCommandInfoData = clone(rows);
            });
        })
        .catch(() => {
          reactiveData.commandInfoData = [];
          reactiveData.oldCommandInfoData = [];
        });
    };

    const eventInfo = () => {
      const profileId = String(reactiveData.deviceFormData.profileId || '');
      if (isNull(profileId)) {
        reactiveData.eventInfoData = [];
        reactiveData.oldEventInfoData = [];
        return;
      }

      listEventByProfileId(profileId)
        .then((res) => {
          const rows: EventInfoMatrixRow[] = (res.data || []).map((event: EventRecord) => {
            const attributes: Record<string, EventAttributeCell> = {};

            reactiveData.eventAttributes.forEach((attribute) => {
              attributes[attribute.attributeCode] = createPointAttributeCell(attribute);
            });

            return {
              id: event.id,
              eventName: event.eventName || '',
              eventCode: event.eventCode,
              eventTypeFlag: event.eventTypeFlag,
              eventLevelFlag: event.eventLevelFlag,
              attributes,
            };
          });

          const rowTable = rows.reduce(
            (table, row) => {
              table[row.id] = row;
              return table;
            },
            {} as Record<string, EventInfoMatrixRow>
          );

          return listEventInfoByDeviceId(reactiveData.id)
            .then((infoRes) => {
              (infoRes.data || []).forEach(
                (info: {eventId: string; attributeId: string | number; id: string; configValue: unknown}) => {
                  const attributeCode = reactiveData.eventAttributeTable[info.attributeId];
                  const attribute = reactiveData.eventAttributes.find((item) => item.attributeCode === attributeCode);
                  const row = rowTable[String(info.eventId)];
                  if (row && attribute) {
                    row.attributes[attributeCode] = createPointAttributeCell(attribute, info.id, info.configValue);
                  }
                }
              );
              reactiveData.eventInfoData = rows;
              reactiveData.oldEventInfoData = clone(rows);
            })
            .catch(() => {
              reactiveData.eventInfoData = rows;
              reactiveData.oldEventInfoData = clone(rows);
            });
        })
        .catch(() => {
          reactiveData.eventInfoData = [];
          reactiveData.oldEventInfoData = [];
        });
    };

    const deviceUpdate = async (): Promise<boolean> => {
      try {
        const res = await updateDevice(reactiveData.deviceFormData);
        reactiveData.oldDeviceFormData = {...res.data};
        return true;
      } catch {
        return false;
      }
    };

    const driverUpdate = async (): Promise<boolean> => {
      if (!hasDriverAttributes.value) {
        return true;
      }

      try {
        const dirtyAttributes = reactiveData.driverAttributes.filter((attribute) =>
          driverDirtySet.has(attribute.attributeCode)
        );
        if (dirtyAttributes.length < 1) {
          return true;
        }

        let failedCount = 0;
        await Promise.all(
          dirtyAttributes.map(async (attribute) => {
            const formItem = reactiveData.driverFormData[attribute.attributeCode];
            if (!formItem) {
              return;
            }
            const driverInfo = {
              id: formItem.id || undefined,
              attributeId: attribute.id,
              deviceId: reactiveData.id,
              configValue: serializeAttributeValue(formItem.configValue),
            };

            try {
              const res: any = driverInfo.id ? await updateDriverInfo(driverInfo) : await addDriverInfo(driverInfo);
              let savedId = String(res?.data?.id || formItem.id || '');
              if (!savedId) {
                const saved: any = await getDriverInfoByDeviceIdAndAttributeId(reactiveData.id, attribute.id);
                savedId = String(saved?.data?.id || '');
              }
              if (!savedId) {
                throw new Error(`Saved driver attribute config without id: ${attribute.attributeCode}`);
              }
              formItem.id = savedId;
              reactiveData.oldDriverFormData[attribute.attributeCode] = clone(formItem);
              driverDirtySet.delete(attribute.attributeCode);
            } catch {
              failedCount++;
            }
          })
        );
        if (failedCount > 0) {
          failMessage(t('device.edit.driverSaveFailed', {count: failedCount}));
          return false;
        }
        reactiveData.oldDriverFormData = clone(reactiveData.driverFormData);
        return true;
      } catch {
        return false;
      }
    };

    const validateAttributeCell = (attribute: Attribute, cell: PointAttributeCell): boolean => {
      cell.error = validateAttributeConfigValue(attribute, cell.configValue, t);
      return isNull(cell.error);
    };

    const validateDirtyCells = (dirtyCells: Array<{attribute: Attribute; cell: PointAttributeCell}>): boolean => {
      let valid = true;
      dirtyCells.forEach(({attribute, cell}) => {
        if (!validateAttributeCell(attribute, cell)) {
          valid = false;
        }
      });
      return valid;
    };

    const pointCell = (row: PointInfoMatrixRow, attribute: Attribute): PointAttributeCell => {
      if (!row.attributes[attribute.attributeCode]) {
        row.attributes[attribute.attributeCode] = createPointAttributeCell(attribute);
      }
      return row.attributes[attribute.attributeCode] as PointAttributeCell;
    };

    const markPointCellDirty = (row: PointInfoMatrixRow, attribute: Attribute) => {
      const cell = pointCell(row, attribute);
      cell.dirty = serializeAttributeValue(cell.configValue) !== serializeAttributeValue(cell.originalValue);
      cell.error = '';
    };

    const pointCellDirty = (row: PointInfoMatrixRow, attribute: Attribute): boolean => {
      return pointCell(row, attribute).dirty;
    };

    const pointCellError = (row: PointInfoMatrixRow, attribute: Attribute): string => {
      return pointCell(row, attribute).error;
    };

    const validatePointCell = (row: PointInfoMatrixRow, attribute: Attribute): boolean => {
      return validateAttributeCell(attribute, pointCell(row, attribute));
    };

    const isPointRowDirty = (row: PointInfoMatrixRow): boolean => {
      return Object.values(row.attributes).some((cell) => cell.dirty);
    };

    const isPointRowError = (row: PointInfoMatrixRow): boolean => {
      return Object.values(row.attributes).some((cell) => !isNull(cell.error));
    };

    const isPointRowConfigured = (row: PointInfoMatrixRow): boolean => {
      if (reactiveData.pointAttributes.length < 1) return false;
      return reactiveData.pointAttributes.every((attribute) => hasConfigValue(pointCell(row, attribute).configValue));
    };

    const pointRowStatus = (row: PointInfoMatrixRow): PointMatrixStatus => {
      if (isPointRowError(row)) return 'error';
      if (isPointRowDirty(row)) return 'dirty';
      if (isPointRowConfigured(row)) return 'configured';
      return 'missing';
    };

    const pointRowStatusLabel = (row: PointInfoMatrixRow): string => {
      return t(`common.configStatus.${pointRowStatus(row)}`);
    };

    const pointRowStatusTag = (row: PointInfoMatrixRow) => {
      const status = pointRowStatus(row);
      if ('configured' === status) return 'success';
      if ('dirty' === status) return 'warning';
      if ('error' === status) return 'danger';
      return 'info';
    };

    const pointMatrixRowClassName = ({row}: {row: PointInfoMatrixRow}) => {
      return isPointRowDirty(row) ? 'point-matrix-row-dirty' : '';
    };

    const savePointMatrix = async (): Promise<boolean> => {
      const dirtyCells = reactiveData.pointInfoData.flatMap((row) =>
        reactiveData.pointAttributes
          .map((attribute) => ({row, attribute, cell: pointCell(row, attribute)}))
          .filter(({cell}) => cell.dirty)
      );
      if (dirtyCells.length < 1) {
        return true;
      }
      if (!validateDirtyCells(dirtyCells)) {
        return false;
      }

      reactiveData.pointSaving = true;
      let failedCount = 0;
      await Promise.all(
        dirtyCells.map(async ({row, cell}) => {
          cell.saving = true;
          cell.error = '';

          const payload: PointInfoForm = {
            id: cell.id || undefined,
            attributeId: cell.attributeId,
            deviceId: reactiveData.id,
            pointId: row.id,
            configValue: serializeAttributeValue(cell.configValue),
          };

          try {
            const res = cell.id ? await updatePointInfo(payload) : await addPointInfo(payload);
            cell.id = String(res?.data?.id || cell.id || '');
            cell.originalValue = cell.configValue;
            cell.dirty = false;
          } catch (error) {
            failedCount++;
            cell.error = t('device.edit.pointSaveCellFailed');
            logger.debug(error);
          } finally {
            cell.saving = false;
          }
        })
      );
      reactiveData.pointSaving = false;

      if (failedCount > 0) {
        failMessage(t('device.edit.pointSaveFailed', {count: failedCount}));
        return false;
      }

      reactiveData.oldPointInfoData = clone(reactiveData.pointInfoData);
      successMessage(t('device.edit.pointSaveSuccess', {count: dirtyCells.length}));
      return true;
    };

    const commandCell = (row: CommandInfoMatrixRow, attribute: Attribute): CommandAttributeCell => {
      if (!row.attributes[attribute.attributeCode]) {
        row.attributes[attribute.attributeCode] = createPointAttributeCell(attribute);
      }
      return row.attributes[attribute.attributeCode] as CommandAttributeCell;
    };

    const markCommandCellDirty = (row: CommandInfoMatrixRow, attribute: Attribute) => {
      const cell = commandCell(row, attribute);
      cell.dirty = serializeAttributeValue(cell.configValue) !== serializeAttributeValue(cell.originalValue);
      cell.error = '';
    };

    const commandCellDirty = (row: CommandInfoMatrixRow, attribute: Attribute): boolean => {
      return commandCell(row, attribute).dirty;
    };

    const commandCellError = (row: CommandInfoMatrixRow, attribute: Attribute): string => {
      return commandCell(row, attribute).error;
    };

    const validateCommandCell = (row: CommandInfoMatrixRow, attribute: Attribute): boolean => {
      return validateAttributeCell(attribute, commandCell(row, attribute));
    };

    const isCommandRowDirty = (row: CommandInfoMatrixRow): boolean => {
      return Object.values(row.attributes).some((cell) => cell.dirty);
    };

    const isCommandRowError = (row: CommandInfoMatrixRow): boolean => {
      return Object.values(row.attributes).some((cell) => !isNull(cell.error));
    };

    const isCommandRowConfigured = (row: CommandInfoMatrixRow): boolean => {
      if (reactiveData.commandAttributes.length < 1) return false;
      return reactiveData.commandAttributes.every((attribute) =>
        hasConfigValue(commandCell(row, attribute).configValue)
      );
    };

    const commandRowStatus = (row: CommandInfoMatrixRow): PointMatrixStatus => {
      if (isCommandRowError(row)) return 'error';
      if (isCommandRowDirty(row)) return 'dirty';
      if (isCommandRowConfigured(row)) return 'configured';
      return 'missing';
    };

    const commandRowStatusLabel = (row: CommandInfoMatrixRow): string => {
      return t(`common.configStatus.${commandRowStatus(row)}`);
    };

    const commandRowStatusTag = (row: CommandInfoMatrixRow) => {
      const status = commandRowStatus(row);
      if ('configured' === status) return 'success';
      if ('dirty' === status) return 'warning';
      if ('error' === status) return 'danger';
      return 'info';
    };

    const commandMatrixRowClassName = ({row}: {row: CommandInfoMatrixRow}) => {
      return isCommandRowDirty(row) ? 'point-matrix-row-dirty' : '';
    };

    const saveCommandMatrix = async (): Promise<boolean> => {
      const dirtyCells = reactiveData.commandInfoData.flatMap((row) =>
        reactiveData.commandAttributes
          .map((attribute) => ({row, attribute, cell: commandCell(row, attribute)}))
          .filter(({cell}) => cell.dirty)
      );
      if (dirtyCells.length < 1) {
        return true;
      }
      if (!validateDirtyCells(dirtyCells)) {
        return false;
      }

      reactiveData.commandSaving = true;
      let failedCount = 0;
      await Promise.all(
        dirtyCells.map(async ({row, cell}) => {
          cell.saving = true;
          cell.error = '';

          const payload: CommandInfoForm = {
            id: cell.id || undefined,
            attributeId: cell.attributeId,
            deviceId: reactiveData.id,
            commandId: row.id,
            configValue: serializeAttributeValue(cell.configValue),
          };

          try {
            const res = cell.id ? await updateCommandInfo(payload) : await addCommandInfo(payload);
            cell.id = String(res?.data?.id || cell.id || '');
            cell.originalValue = cell.configValue;
            cell.dirty = false;
          } catch (error) {
            failedCount++;
            cell.error = t('device.edit.commandSaveCellFailed');
            logger.debug(error);
          } finally {
            cell.saving = false;
          }
        })
      );
      reactiveData.commandSaving = false;

      if (failedCount > 0) {
        failMessage(t('device.edit.commandSaveFailed', {count: failedCount}));
        return false;
      }

      reactiveData.oldCommandInfoData = clone(reactiveData.commandInfoData);
      successMessage(t('device.edit.commandSaveSuccess', {count: dirtyCells.length}));
      return true;
    };

    const eventCell = (row: EventInfoMatrixRow, attribute: Attribute): EventAttributeCell => {
      if (!row.attributes[attribute.attributeCode]) {
        row.attributes[attribute.attributeCode] = createPointAttributeCell(attribute);
      }
      return row.attributes[attribute.attributeCode] as EventAttributeCell;
    };

    const markEventCellDirty = (row: EventInfoMatrixRow, attribute: Attribute) => {
      const cell = eventCell(row, attribute);
      cell.dirty = serializeAttributeValue(cell.configValue) !== serializeAttributeValue(cell.originalValue);
      cell.error = '';
    };

    const eventCellDirty = (row: EventInfoMatrixRow, attribute: Attribute): boolean => {
      return eventCell(row, attribute).dirty;
    };

    const eventCellError = (row: EventInfoMatrixRow, attribute: Attribute): string => {
      return eventCell(row, attribute).error;
    };

    const validateEventCell = (row: EventInfoMatrixRow, attribute: Attribute): boolean => {
      return validateAttributeCell(attribute, eventCell(row, attribute));
    };

    const isEventRowDirty = (row: EventInfoMatrixRow): boolean => {
      return Object.values(row.attributes).some((cell) => cell.dirty);
    };

    const isEventRowError = (row: EventInfoMatrixRow): boolean => {
      return Object.values(row.attributes).some((cell) => !isNull(cell.error));
    };

    const isEventRowConfigured = (row: EventInfoMatrixRow): boolean => {
      if (reactiveData.eventAttributes.length < 1) return false;
      return reactiveData.eventAttributes.every((attribute) => hasConfigValue(eventCell(row, attribute).configValue));
    };

    const eventRowStatus = (row: EventInfoMatrixRow): PointMatrixStatus => {
      if (isEventRowError(row)) return 'error';
      if (isEventRowDirty(row)) return 'dirty';
      if (isEventRowConfigured(row)) return 'configured';
      return 'missing';
    };

    const eventRowStatusLabel = (row: EventInfoMatrixRow): string => {
      return t(`common.configStatus.${eventRowStatus(row)}`);
    };

    const eventRowStatusTag = (row: EventInfoMatrixRow) => {
      const status = eventRowStatus(row);
      if ('configured' === status) return 'success';
      if ('dirty' === status) return 'warning';
      if ('error' === status) return 'danger';
      return 'info';
    };

    const eventMatrixRowClassName = ({row}: {row: EventInfoMatrixRow}) => {
      return isEventRowDirty(row) ? 'point-matrix-row-dirty' : '';
    };

    const saveEventMatrix = async (): Promise<boolean> => {
      const dirtyCells = reactiveData.eventInfoData.flatMap((row) =>
        reactiveData.eventAttributes
          .map((attribute) => ({row, attribute, cell: eventCell(row, attribute)}))
          .filter(({cell}) => cell.dirty)
      );
      if (dirtyCells.length < 1) {
        return true;
      }
      if (!validateDirtyCells(dirtyCells)) {
        return false;
      }

      reactiveData.eventSaving = true;
      let failedCount = 0;
      await Promise.all(
        dirtyCells.map(async ({row, cell}) => {
          cell.saving = true;
          cell.error = '';

          const payload: EventInfoForm = {
            id: cell.id || undefined,
            attributeId: cell.attributeId,
            deviceId: reactiveData.id,
            eventId: row.id,
            configValue: serializeAttributeValue(cell.configValue),
          };

          try {
            const res = cell.id ? await updateEventInfo(payload) : await addEventInfo(payload);
            cell.id = String(res?.data?.id || cell.id || '');
            cell.originalValue = cell.configValue;
            cell.dirty = false;
          } catch (error) {
            failedCount++;
            cell.error = t('device.edit.eventSaveCellFailed');
            logger.debug(error);
          } finally {
            cell.saving = false;
          }
        })
      );
      reactiveData.eventSaving = false;

      if (failedCount > 0) {
        failMessage(t('device.edit.eventSaveFailed', {count: failedCount}));
        return false;
      }

      reactiveData.oldEventInfoData = clone(reactiveData.eventInfoData);
      successMessage(t('device.edit.eventSaveSuccess', {count: dirtyCells.length}));
      return true;
    };

    const deviceSave = async () => {
      const ok = await deviceUpdate();
      if (ok) {
        successMessage();
      }
    };

    const driverDirtySet = reactive(new Set<string>());

    const getDriverCellValue = (attribute: Attribute): any => {
      return reactiveData.driverFormData[attribute.attributeCode]?.configValue;
    };

    const setDriverCellValue = (attribute: Attribute, val: any) => {
      const item = reactiveData.driverFormData[attribute.attributeCode];
      if (!item) return;
      item.configValue = val;
      const oldVal = reactiveData.oldDriverFormData[attribute.attributeCode]?.configValue;
      if (serializeAttributeValue(val) !== serializeAttributeValue(oldVal)) {
        driverDirtySet.add(attribute.attributeCode);
      } else {
        driverDirtySet.delete(attribute.attributeCode);
      }
    };

    const driverCellDirty = (attribute: Attribute): boolean => {
      return driverDirtySet.has(attribute.attributeCode);
    };

    const driverDirtyCount = computed(() => driverDirtySet.size);

    const totalDirtyCount = computed(
      () => pointDirtyCount.value + commandDirtyCount.value + eventDirtyCount.value + driverDirtyCount.value
    );

    const saveDriverMatrix = async () => {
      reactiveData.driverSaving = true;
      const ok = await driverUpdate();
      reactiveData.driverSaving = false;
      if (ok) {
        driverDirtySet.clear();
        successMessage();
      }
    };

    const deviceReset = () => {
      reactiveData.deviceFormData = clone(reactiveData.oldDeviceFormData);
    };

    const driverInfoReset = () => {
      reactiveData.driverFormData = clone(reactiveData.oldDriverFormData);
      driverDirtySet.clear();
    };

    const pointInfoReset = () => {
      reactiveData.pointInfoData = clone(reactiveData.oldPointInfoData);
    };

    const commandInfoReset = () => {
      reactiveData.commandInfoData = clone(reactiveData.oldCommandInfoData);
    };

    const eventInfoReset = () => {
      reactiveData.eventInfoData = clone(reactiveData.oldEventInfoData);
    };

    const changeProfile = () => {
      pointInfo();
      commandInfo();
      eventInfo();
    };

    const changeActive = (tab: any) => {
      reactiveData.active = tab.props.name;
      const query = route.query;
      router.push({query: {...query, active: tab.props.name}});
    };

    watch(
      () => [route.query.id, route.query.active],
      ([id, active]) => {
        const nextId = id as string;
        const nextActive = resolveDeviceTab(active);

        if (reactiveData.active !== nextActive) {
          reactiveData.active = nextActive;
        }

        if (nextId && nextId !== reactiveData.id) {
          reactiveData.id = nextId;
          reactiveData.loading = true;
          reactiveData.deviceFormData = {};
          reactiveData.oldDeviceFormData = {};
          reactiveData.driverFormData = {};
          reactiveData.oldDriverFormData = {};
          reactiveData.pointInfoData = [];
          reactiveData.oldPointInfoData = [];
          reactiveData.commandInfoData = [];
          reactiveData.oldCommandInfoData = [];
          reactiveData.eventInfoData = [];
          reactiveData.oldEventInfoData = [];
          device();
        }
      }
    );

    const warnBeforeUnload = (e: BeforeUnloadEvent) => {
      if (totalDirtyCount.value > 0) {
        e.preventDefault();
        e.returnValue = '';
      }
    };

    onMounted(() => {
      window.addEventListener('beforeunload', warnBeforeUnload);
    });

    onBeforeUnmount(() => {
      window.removeEventListener('beforeunload', warnBeforeUnload);
    });

    onBeforeRouteLeave((_to, _from, next) => {
      if (totalDirtyCount.value > 0) {
        const leave = window.confirm('You have unsaved changes. Are you sure you want to leave?');
        if (!leave) {
          next(false);
          return;
        }
      }
      next();
    });

    device();

    return {
      deviceFormRule,
      reactiveData,
      hasPointAttributes,
      hasCommandAttributes,
      hasEventAttributes,
      hasDriverAttributes,
      filteredPointInfoData,
      filteredCommandInfoData,
      filteredEventInfoData,
      paginatedPointInfoData,
      paginatedCommandInfoData,
      paginatedEventInfoData,
      pointDirtyCount,
      commandDirtyCount,
      eventDirtyCount,
      totalDirtyCount,
      driverDictionary,
      driverDictionaryVisible,
      profileDictionary,
      profileDictionaryVisible,
      changeProfile,
      changeAttribute,
      deviceSave,
      driverDirtyCount,
      getDriverCellValue,
      setDriverCellValue,
      driverCellDirty,
      saveDriverMatrix,
      savePointMatrix,
      saveCommandMatrix,
      saveEventMatrix,
      pointCell,
      commandCell,
      eventCell,
      markPointCellDirty,
      markCommandCellDirty,
      markEventCellDirty,
      pointCellDirty,
      commandCellDirty,
      eventCellDirty,
      pointCellError,
      commandCellError,
      eventCellError,
      validatePointCell,
      validateCommandCell,
      validateEventCell,
      pointRowStatusLabel,
      commandRowStatusLabel,
      eventRowStatusLabel,
      pointRowStatusTag,
      commandRowStatusTag,
      eventRowStatusTag,
      pointMatrixRowClassName,
      commandMatrixRowClassName,
      eventMatrixRowClassName,
      deviceReset,
      driverInfoReset,
      pointInfoReset,
      commandInfoReset,
      eventInfoReset,
      changeActive,
      attributeFormItem,
      attributeFormItemRules,
      attributePlaceholder: attributeInputPlaceholder,
      attributePrecision,
      isBooleanAttribute,
      isNumberAttribute,
      Search,
    };
  },
});
