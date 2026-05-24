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

import { computed, defineComponent, reactive, ref, unref, watch } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { Back, Check, RefreshLeft, Right, Search } from '@element-plus/icons-vue';

import { useRoute } from 'vue-router';
import router from '@/config/router';

import { getDriverDictionary, getProfileDictionary } from '@/api/dictionary';
import { getDeviceById, updateDevice } from '@/api/device';
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

import EnableFlagSegmented from '@/components/segmented/EnableFlagSegmented.vue';
import { isNull } from '@/utils/validationUtil';
import { failMessage, successMessage } from '@/utils/notificationUtil';
import { getDriverById } from '@/api/driver';
import { getProfileById } from '@/api/profile';
import { listPointByDeviceId } from '@/api/point';
import { listCommandByProfileId } from '@/api/command';
import { listEventByProfileId } from '@/api/event';
import { nameRules, remarkRules } from '@/utils/formRuleUtil';
import { useI18n } from 'vue-i18n';

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

interface PointInfoMatrixRow {
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

interface CommandInfoMatrixRow {
  id: string;
  commandName: string;
  commandCode?: string;
  commandTypeFlag?: string | number;
  callTypeFlag?: string | number;
  attributes: Record<string, CommandAttributeCell>;
}

interface EventInfoMatrixRow {
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

function activeStep(value: unknown): number {
  const step = Number(value ?? 0);
  if (!Number.isFinite(step)) {
    return 0;
  }
  return Math.min(Math.max(Math.trunc(step), 0), 5);
}

export default defineComponent({
  name: 'DeviceEdit',
  components: {
    EnableFlagSegmented,
  },
  setup() {
    const route = useRoute();
    const { t } = useI18n();

    // 定义表单引用
    const deviceFormRef = ref<FormInstance>();
    const driverFormRef = ref<FormInstance>();

    // 图标
    const Icon = {
      RefreshLeft,
      Right,
      Back,
      Check,
      Search,
    };

    // 定义响应式数据
    const reactiveData = reactive({
      id: route.query.id as string,
      active: activeStep(route.query.active),
      loading: true,
      oldDeviceFormData: {} as Record<string, any>,
      deviceFormData: {} as any,
      driverAttributes: [] as Attribute[],
      driverAttributeTable: {} as Record<string, any>,
      oldDriverFormData: {} as AttributeFormData,
      driverFormData: {} as AttributeFormData,
      pointAttributes: [] as Attribute[],
      pointAttributeTable: {} as Record<string, any>,
      pointInfoData: [] as PointInfoMatrixRow[],
      oldPointInfoData: [] as PointInfoMatrixRow[],
      pointMatrixKeyword: '',
      pointMatrixStatus: '' as PointMatrixStatus,
      pointSaving: false,
      commandAttributes: [] as Attribute[],
      commandAttributeTable: {} as Record<string, any>,
      commandInfoData: [] as CommandInfoMatrixRow[],
      oldCommandInfoData: [] as CommandInfoMatrixRow[],
      commandMatrixKeyword: '',
      commandMatrixStatus: '' as PointMatrixStatus,
      commandSaving: false,
      eventAttributes: [] as Attribute[],
      eventAttributeTable: {} as Record<string, any>,
      eventInfoData: [] as EventInfoMatrixRow[],
      oldEventInfoData: [] as EventInfoMatrixRow[],
      eventMatrixKeyword: '',
      eventMatrixStatus: '' as PointMatrixStatus,
      eventSaving: false,
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
      profileId: [
        {
          required: true,
          message: () => t('device.edit.profileRequired'),
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
          // nothing to do
        });

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
          reactiveData.loading = false;
        });

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
        });

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
        });
    };

    const driverInfo = () => {
      listDriverInfoByDeviceId(reactiveData.id)
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
      reactiveData.loading = true;
      listPointByDeviceId(reactiveData.id)
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
                (info: { pointId: string; attributeId: string | number; id: string; configValue: unknown }) => {
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
                (info: { commandId: string; attributeId: string | number; id: string; configValue: unknown }) => {
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
                (info: { eventId: string; attributeId: string | number; id: string; configValue: unknown }) => {
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
      return t(`device.edit.pointStatus.${pointRowStatus(row)}`);
    };

    const pointRowStatusTag = (row: PointInfoMatrixRow) => {
      const status = pointRowStatus(row);
      if ('configured' === status) return 'success';
      if ('dirty' === status) return 'warning';
      if ('error' === status) return 'danger';
      return 'info';
    };

    const pointMatrixRowClassName = ({ row }: { row: PointInfoMatrixRow }) => {
      return isPointRowDirty(row) ? 'point-matrix-row-dirty' : '';
    };

    const savePointMatrix = async (): Promise<boolean> => {
      const dirtyCells = reactiveData.pointInfoData.flatMap((row) =>
        reactiveData.pointAttributes
          .map((attribute) => ({ row, cell: pointCell(row, attribute) }))
          .filter(({ cell }) => cell.dirty)
      );
      if (dirtyCells.length < 1) {
        return true;
      }

      reactiveData.pointSaving = true;
      let failedCount = 0;
      await Promise.all(
        dirtyCells.map(async ({ row, cell }) => {
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
            if ('dev' === import.meta.env.MODE) {
              console.error(error);
            }
          } finally {
            cell.saving = false;
          }
        })
      );
      reactiveData.pointSaving = false;

      if (failedCount > 0) {
        failMessage(t('device.edit.pointSaveFailed', { count: failedCount }));
        return false;
      }

      reactiveData.oldPointInfoData = clone(reactiveData.pointInfoData);
      successMessage(t('device.edit.pointSaveSuccess', { count: dirtyCells.length }));
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
      return t(`device.edit.pointStatus.${commandRowStatus(row)}`);
    };

    const commandRowStatusTag = (row: CommandInfoMatrixRow) => {
      const status = commandRowStatus(row);
      if ('configured' === status) return 'success';
      if ('dirty' === status) return 'warning';
      if ('error' === status) return 'danger';
      return 'info';
    };

    const commandMatrixRowClassName = ({ row }: { row: CommandInfoMatrixRow }) => {
      return isCommandRowDirty(row) ? 'point-matrix-row-dirty' : '';
    };

    const saveCommandMatrix = async (): Promise<boolean> => {
      const dirtyCells = reactiveData.commandInfoData.flatMap((row) =>
        reactiveData.commandAttributes
          .map((attribute) => ({ row, cell: commandCell(row, attribute) }))
          .filter(({ cell }) => cell.dirty)
      );
      if (dirtyCells.length < 1) {
        return true;
      }

      reactiveData.commandSaving = true;
      let failedCount = 0;
      await Promise.all(
        dirtyCells.map(async ({ row, cell }) => {
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
            if ('dev' === import.meta.env.MODE) {
              console.error(error);
            }
          } finally {
            cell.saving = false;
          }
        })
      );
      reactiveData.commandSaving = false;

      if (failedCount > 0) {
        failMessage(t('device.edit.commandSaveFailed', { count: failedCount }));
        return false;
      }

      reactiveData.oldCommandInfoData = clone(reactiveData.commandInfoData);
      successMessage(t('device.edit.commandSaveSuccess', { count: dirtyCells.length }));
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
      return t(`device.edit.pointStatus.${eventRowStatus(row)}`);
    };

    const eventRowStatusTag = (row: EventInfoMatrixRow) => {
      const status = eventRowStatus(row);
      if ('configured' === status) return 'success';
      if ('dirty' === status) return 'warning';
      if ('error' === status) return 'danger';
      return 'info';
    };

    const eventMatrixRowClassName = ({ row }: { row: EventInfoMatrixRow }) => {
      return isEventRowDirty(row) ? 'point-matrix-row-dirty' : '';
    };

    const saveEventMatrix = async (): Promise<boolean> => {
      const dirtyCells = reactiveData.eventInfoData.flatMap((row) =>
        reactiveData.eventAttributes
          .map((attribute) => ({ row, cell: eventCell(row, attribute) }))
          .filter(({ cell }) => cell.dirty)
      );
      if (dirtyCells.length < 1) {
        return true;
      }

      reactiveData.eventSaving = true;
      let failedCount = 0;
      await Promise.all(
        dirtyCells.map(async ({ row, cell }) => {
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
            if ('dev' === import.meta.env.MODE) {
              console.error(error);
            }
          } finally {
            cell.saving = false;
          }
        })
      );
      reactiveData.eventSaving = false;

      if (failedCount > 0) {
        failMessage(t('device.edit.eventSaveFailed', { count: failedCount }));
        return false;
      }

      reactiveData.oldEventInfoData = clone(reactiveData.eventInfoData);
      successMessage(t('device.edit.eventSaveSuccess', { count: dirtyCells.length }));
      return true;
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
      if (reactiveData.active === 2) {
        const ok = await savePointMatrix();
        if (!ok) {
          return;
        }
      }
      if (reactiveData.active === 3) {
        const ok = await saveCommandMatrix();
        if (!ok) {
          return;
        }
      }
      if (reactiveData.active === 4) {
        const ok = await saveEventMatrix();
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
      reactiveData.pointInfoData = clone(reactiveData.oldPointInfoData);
    };

    const commandInfoReset = () => {
      reactiveData.commandInfoData = clone(reactiveData.oldCommandInfoData);
    };

    const eventInfoReset = () => {
      reactiveData.eventInfoData = clone(reactiveData.oldEventInfoData);
    };

    const changeProfile = () => {
      commandInfo();
      eventInfo();
    };

    const changeActive = (step: number) => {
      const query = route.query;
      router.push({ query: { ...query, active: step } }).catch(() => {
        // nothing to do
      });
    };

    watch(
      () => [route.query.id, route.query.active],
      ([id, active]) => {
        const nextId = id as string;
        const nextActive = activeStep(active);

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

    device();

    return {
      deviceFormRef,
      driverFormRef,
      deviceFormRule,
      reactiveData,
      hasPointAttributes,
      hasCommandAttributes,
      hasEventAttributes,
      hasDriverAttributes,
      filteredPointInfoData,
      filteredCommandInfoData,
      filteredEventInfoData,
      pointDirtyCount,
      commandDirtyCount,
      eventDirtyCount,
      driverDictionary,
      driverDictionaryVisible,
      profileDictionary,
      profileDictionaryVisible,
      changeProfile,
      changeAttribute,
      driverUpdate,
      savePointMatrix,
      saveCommandMatrix,
      saveEventMatrix,
      markPointCellDirty,
      markCommandCellDirty,
      markEventCellDirty,
      pointCellDirty,
      commandCellDirty,
      eventCellDirty,
      pointCellError,
      commandCellError,
      eventCellError,
      pointRowStatusLabel,
      commandRowStatusLabel,
      eventRowStatusLabel,
      pointRowStatusTag,
      commandRowStatusTag,
      eventRowStatusTag,
      pointMatrixRowClassName,
      commandMatrixRowClassName,
      eventMatrixRowClassName,
      pre,
      next,
      done,
      deviceReset,
      driverInfoReset,
      pointInfoReset,
      commandInfoReset,
      eventInfoReset,
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
