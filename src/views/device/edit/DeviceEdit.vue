<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <div>
    <base-card>
      <el-tabs v-model="reactiveData.active" @tab-click="changeActive">
        <!-- Device Config -->
        <el-tab-pane :label="$t('device.edit.deviceConfig')" name="deviceConfig">
          <info-card
            :form-model="reactiveData.deviceFormData"
            :rules="deviceFormRule"
            @reset="deviceReset"
            @save="deviceSave"
          >
            <template #fields>
              <el-form-item :label="$t('device.edit.deviceName')" prop="deviceName">
                <el-input
                  v-model="reactiveData.deviceFormData.deviceName"
                  :placeholder="$t('device.edit.deviceNamePlaceholder')"
                  clearable
                  maxlength="32"
                  show-word-limit
                />
              </el-form-item>
              <el-form-item :label="$t('device.edit.driver')" prop="driverId">
                <el-select
                  v-model="reactiveData.deviceFormData.driverId"
                  :loading="reactiveData.driverLoading"
                  :placeholder="$t('device.edit.driverPlaceholder')"
                  :remote-method="driverDictionary"
                  clearable
                  filterable
                  remote
                  reserve-keyword
                  @change="changeAttribute"
                  @visible-change="driverDictionaryVisible"
                >
                  <el-option
                    v-for="dictionary in reactiveData.driverDictionary"
                    :key="dictionary.value"
                    :label="dictionary.label"
                    :value="dictionary.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item :label="$t('device.edit.profile')" prop="profileId">
                <el-select
                  v-model="reactiveData.deviceFormData.profileId"
                  :loading="reactiveData.profileLoading"
                  :placeholder="$t('device.edit.profilePlaceholder')"
                  :remote-method="profileDictionary"
                  clearable
                  filterable
                  remote
                  reserve-keyword
                  @change="changeProfile"
                  @visible-change="profileDictionaryVisible"
                >
                  <el-option
                    v-for="dictionary in reactiveData.profileDictionary"
                    :key="dictionary.value"
                    :label="dictionary.label"
                    :value="dictionary.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
                <enable-flag-segmented v-model="reactiveData.deviceFormData.enableFlag" />
              </el-form-item>
              <el-form-item class="info-card-item-full" :label="$t('device.edit.description')" prop="remark">
                <el-input
                  v-model="reactiveData.deviceFormData.remark"
                  :placeholder="$t('device.edit.descriptionPlaceholder')"
                  clearable
                  maxlength="300"
                  show-word-limit
                  type="textarea"
                />
              </el-form-item>
            </template>
          </info-card>
        </el-tab-pane>

        <!-- Driver Config -->
        <el-tab-pane v-if="hasDriverAttributes" :label="$t('device.edit.driverConfig')" name="driverConfig">
          <matrix-toolbar
            :dirty-count="driverDirtyCount"
            :form-model="reactiveData"
            :saving="reactiveData.driverSaving"
            @discard="driverInfoReset"
            @save="saveDriverMatrix"
          />
          <el-empty v-if="!hasDriverAttributes" :description="$t('device.edit.driverAttributeEmpty')" />
          <el-table
            v-else
            v-loading="reactiveData.loading"
            :data="reactiveData.driverAttributes"
            border
            class="driver-matrix-table"
            row-key="id"
            stripe
          >
            <el-table-column :label="$t('device.edit.attributeName')" min-width="160" prop="attributeName" />
            <el-table-column :label="$t('device.edit.attributeType')" width="100">
              <template #default="{ row }">
                <el-tag effect="plain" size="small">{{ row.attributeTypeFlag }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column :label="$t('device.edit.defaultValue')" width="140">
              <template #default="{ row }">
                {{ row.defaultValue || '-' }}
              </template>
            </el-table-column>
            <el-table-column :label="$t('device.edit.configValue')" min-width="200">
              <template #default="{ row: attribute }">
                <div class="driver-matrix-cell">
                  <el-switch
                    v-if="isBooleanAttribute(attribute)"
                    :model-value="getDriverCellValue(attribute)"
                    :active-value="true"
                    :inactive-value="false"
                    size="small"
                    @change="(val: any) => setDriverCellValue(attribute, val)"
                  />
                  <el-input-number
                    v-else-if="isNumberAttribute(attribute)"
                    :model-value="getDriverCellValue(attribute)"
                    :placeholder="attributePlaceholder(attribute)"
                    :precision="attributePrecision(attribute)"
                    controls-position="right"
                    size="small"
                    @input="(val: any) => setDriverCellValue(attribute, val)"
                  />
                  <el-input
                    v-else
                    :model-value="getDriverCellValue(attribute)"
                    :placeholder="attributePlaceholder(attribute)"
                    clearable
                    maxlength="512"
                    show-word-limit
                    size="small"
                    @input="(val: string) => setDriverCellValue(attribute, val)"
                  />
                  <div v-if="driverCellDirty(attribute)" class="driver-matrix-cell__meta">
                    <span class="point-matrix-cell__dirty">
                      {{ $t('device.edit.modified') }}
                    </span>
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Point Config -->
        <el-tab-pane :label="$t('device.edit.pointConfig')" name="pointConfig">
          <matrix-toolbar
            :dirty-count="pointDirtyCount"
            :form-model="reactiveData"
            :saving="reactiveData.pointSaving"
            @discard="pointInfoReset"
            @save="savePointMatrix"
          >
            <template #filters>
              <el-form-item :label="$t('device.edit.pointName')" prop="pointMatrixKeyword">
                <el-input
                  v-model="reactiveData.pointMatrixKeyword"
                  :placeholder="$t('device.edit.pointSearchPlaceholder')"
                  :prefix-icon="Search"
                  clearable
                />
              </el-form-item>
              <el-form-item :label="$t('device.edit.configStatus')" prop="pointMatrixStatus">
                <matrix-status-segmented v-model="reactiveData.pointMatrixStatus" />
              </el-form-item>
            </template>
          </matrix-toolbar>

          <el-empty v-if="!hasPointAttributes" :description="$t('device.edit.pointAttributeEmpty')" />
          <el-table
            v-else
            v-loading="reactiveData.loading"
            :data="filteredPointInfoData"
            :row-class-name="pointMatrixRowClassName"
            border
            class="point-matrix-table"
            max-height="560"
            size="small"
            stripe
          >
            <el-table-column :label="$t('device.edit.pointName')" fixed min-width="220">
              <template #default="{ row }">
                <div class="point-matrix-point">
                  <div class="point-matrix-point__name">{{ row.pointName }}</div>
                  <div class="point-matrix-point__code">{{ row.pointCode || row.id }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="$t('device.edit.configStatus')" fixed min-width="120">
              <template #default="{ row }">
                <el-tag :type="pointRowStatusTag(row)" effect="plain" size="small">
                  {{ pointRowStatusLabel(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column v-for="attribute in reactiveData.pointAttributes" :key="attribute.id" min-width="220">
              <template #header>
                <div class="point-matrix-attribute-header">
                  <span>{{ attribute.attributeName }}</span>
                  <el-tag effect="plain" size="small">{{ attribute.attributeTypeFlag || 'STRING' }}</el-tag>
                </div>
              </template>
              <template #default="{ row }">
                <div
                  :class="[
                    'point-matrix-cell',
                    pointCellDirty(row, attribute) ? 'is-dirty' : '',
                    pointCellError(row, attribute) ? 'is-error' : '',
                  ]"
                >
                  <el-switch
                    v-if="isBooleanAttribute(attribute)"
                    v-model="row.attributes[attribute.attributeCode].configValue"
                    :active-value="true"
                    :inactive-value="false"
                    size="small"
                    @change="markPointCellDirty(row, attribute)"
                  />
                  <el-input
                    v-else-if="isNumberAttribute(attribute)"
                    v-model="row.attributes[attribute.attributeCode].configValue"
                    :placeholder="attributePlaceholder(attribute)"
                    class="point-matrix-input"
                    clearable
                    inputmode="decimal"
                    maxlength="512"
                    size="small"
                    @blur="validatePointCell(row, attribute)"
                    @input="markPointCellDirty(row, attribute)"
                  />
                  <el-input
                    v-else
                    v-model="row.attributes[attribute.attributeCode].configValue"
                    :placeholder="attributePlaceholder(attribute)"
                    clearable
                    maxlength="512"
                    size="small"
                    @input="markPointCellDirty(row, attribute)"
                  />
                  <div v-if="pointCellDirty(row, attribute)" class="point-matrix-cell__meta">
                    <span class="point-matrix-cell__dirty">
                      {{ $t('device.edit.modified') }}
                    </span>
                  </div>
                  <div v-if="pointCellError(row, attribute)" class="point-matrix-cell__error">
                    {{ pointCellError(row, attribute) }}
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Command Config -->
        <el-tab-pane :label="$t('device.edit.commandConfig')" name="commandConfig">
          <matrix-toolbar
            :dirty-count="commandDirtyCount"
            :form-model="reactiveData"
            :saving="reactiveData.commandSaving"
            @discard="commandInfoReset"
            @save="saveCommandMatrix"
          >
            <template #filters>
              <el-form-item :label="$t('device.edit.commandName')" prop="commandMatrixKeyword">
                <el-input
                  v-model="reactiveData.commandMatrixKeyword"
                  :placeholder="$t('device.edit.commandSearchPlaceholder')"
                  :prefix-icon="Search"
                  clearable
                />
              </el-form-item>
              <el-form-item :label="$t('device.edit.configStatus')" prop="commandMatrixStatus">
                <matrix-status-segmented v-model="reactiveData.commandMatrixStatus" />
              </el-form-item>
            </template>
          </matrix-toolbar>

          <el-empty v-if="!hasCommandAttributes" :description="$t('device.edit.commandAttributeEmpty')" />
          <el-table
            v-else
            v-loading="reactiveData.loading"
            :data="filteredCommandInfoData"
            :row-class-name="commandMatrixRowClassName"
            border
            class="point-matrix-table"
            max-height="560"
            size="small"
            stripe
          >
            <el-table-column :label="$t('device.edit.commandName')" fixed min-width="220">
              <template #default="{ row }">
                <div class="point-matrix-point">
                  <div class="point-matrix-point__name">{{ row.commandName }}</div>
                  <div class="point-matrix-point__code">{{ row.commandCode || row.id }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="$t('device.edit.configStatus')" fixed min-width="120">
              <template #default="{ row }">
                <el-tag :type="commandRowStatusTag(row)" effect="plain" size="small">
                  {{ commandRowStatusLabel(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column v-for="attribute in reactiveData.commandAttributes" :key="attribute.id" min-width="220">
              <template #header>
                <div class="point-matrix-attribute-header">
                  <span>{{ attribute.attributeName }}</span>
                  <el-tag effect="plain" size="small">{{ attribute.attributeTypeFlag || 'STRING' }}</el-tag>
                </div>
              </template>
              <template #default="{ row }">
                <div
                  :class="[
                    'point-matrix-cell',
                    commandCellDirty(row, attribute) ? 'is-dirty' : '',
                    commandCellError(row, attribute) ? 'is-error' : '',
                  ]"
                >
                  <el-switch
                    v-if="isBooleanAttribute(attribute)"
                    v-model="row.attributes[attribute.attributeCode].configValue"
                    :active-value="true"
                    :inactive-value="false"
                    size="small"
                    @change="markCommandCellDirty(row, attribute)"
                  />
                  <el-input
                    v-else-if="isNumberAttribute(attribute)"
                    v-model="row.attributes[attribute.attributeCode].configValue"
                    :placeholder="attributePlaceholder(attribute)"
                    class="point-matrix-input"
                    clearable
                    inputmode="decimal"
                    maxlength="512"
                    size="small"
                    @blur="validateCommandCell(row, attribute)"
                    @input="markCommandCellDirty(row, attribute)"
                  />
                  <el-input
                    v-else
                    v-model="row.attributes[attribute.attributeCode].configValue"
                    :placeholder="attributePlaceholder(attribute)"
                    clearable
                    maxlength="512"
                    size="small"
                    @input="markCommandCellDirty(row, attribute)"
                  />
                  <div v-if="commandCellDirty(row, attribute)" class="point-matrix-cell__meta">
                    <span class="point-matrix-cell__dirty">
                      {{ $t('device.edit.modified') }}
                    </span>
                  </div>
                  <div v-if="commandCellError(row, attribute)" class="point-matrix-cell__error">
                    {{ commandCellError(row, attribute) }}
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Event Config -->
        <el-tab-pane :label="$t('device.edit.eventConfig')" name="eventConfig">
          <matrix-toolbar
            :dirty-count="eventDirtyCount"
            :form-model="reactiveData"
            :saving="reactiveData.eventSaving"
            @discard="eventInfoReset"
            @save="saveEventMatrix"
          >
            <template #filters>
              <el-form-item :label="$t('device.edit.eventName')" prop="eventMatrixKeyword">
                <el-input
                  v-model="reactiveData.eventMatrixKeyword"
                  :placeholder="$t('device.edit.eventSearchPlaceholder')"
                  :prefix-icon="Search"
                  clearable
                />
              </el-form-item>
              <el-form-item :label="$t('device.edit.configStatus')" prop="eventMatrixStatus">
                <matrix-status-segmented v-model="reactiveData.eventMatrixStatus" />
              </el-form-item>
            </template>
          </matrix-toolbar>

          <el-empty v-if="!hasEventAttributes" :description="$t('device.edit.eventAttributeEmpty')" />
          <el-table
            v-else
            v-loading="reactiveData.loading"
            :data="filteredEventInfoData"
            :row-class-name="eventMatrixRowClassName"
            border
            class="point-matrix-table"
            max-height="560"
            size="small"
            stripe
          >
            <el-table-column :label="$t('device.edit.eventName')" fixed min-width="220">
              <template #default="{ row }">
                <div class="point-matrix-point">
                  <div class="point-matrix-point__name">{{ row.eventName }}</div>
                  <div class="point-matrix-point__code">{{ row.eventCode || row.id }}</div>
                </div>
              </template>
            </el-table-column>
            <el-table-column :label="$t('device.edit.configStatus')" fixed min-width="120">
              <template #default="{ row }">
                <el-tag :type="eventRowStatusTag(row)" effect="plain" size="small">
                  {{ eventRowStatusLabel(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column v-for="attribute in reactiveData.eventAttributes" :key="attribute.id" min-width="220">
              <template #header>
                <div class="point-matrix-attribute-header">
                  <span>{{ attribute.attributeName }}</span>
                  <el-tag effect="plain" size="small">{{ attribute.attributeTypeFlag || 'STRING' }}</el-tag>
                </div>
              </template>
              <template #default="{ row }">
                <div
                  :class="[
                    'point-matrix-cell',
                    eventCellDirty(row, attribute) ? 'is-dirty' : '',
                    eventCellError(row, attribute) ? 'is-error' : '',
                  ]"
                >
                  <el-switch
                    v-if="isBooleanAttribute(attribute)"
                    v-model="row.attributes[attribute.attributeCode].configValue"
                    :active-value="true"
                    :inactive-value="false"
                    size="small"
                    @change="markEventCellDirty(row, attribute)"
                  />
                  <el-input
                    v-else-if="isNumberAttribute(attribute)"
                    v-model="row.attributes[attribute.attributeCode].configValue"
                    :placeholder="attributePlaceholder(attribute)"
                    class="point-matrix-input"
                    clearable
                    inputmode="decimal"
                    maxlength="512"
                    size="small"
                    @blur="validateEventCell(row, attribute)"
                    @input="markEventCellDirty(row, attribute)"
                  />
                  <el-input
                    v-else
                    v-model="row.attributes[attribute.attributeCode].configValue"
                    :placeholder="attributePlaceholder(attribute)"
                    clearable
                    maxlength="512"
                    size="small"
                    @input="markEventCellDirty(row, attribute)"
                  />
                  <div v-if="eventCellDirty(row, attribute)" class="point-matrix-cell__meta">
                    <span class="point-matrix-cell__dirty">
                      {{ $t('device.edit.modified') }}
                    </span>
                  </div>
                  <div v-if="eventCellError(row, attribute)" class="point-matrix-cell__error">
                    {{ eventCellError(row, attribute) }}
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </base-card>
  </div>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped>
  @use '@/styles/edit-card.scss';

  .driver-info-card {
    margin-top: 12px;
  }

  .attribute-type {
    margin-left: 8px;
    vertical-align: middle;
  }

  .attribute-hint {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    margin-top: 6px;
    line-height: 18px;
    color: var(--el-text-color-secondary);
    font-size: 12px;
  }

  .point-matrix-table {
    width: 100%;
    font-size: 13px;

    :deep(.point-matrix-row-dirty) {
      --el-table-tr-bg-color: var(--el-color-warning-light-9);
    }

    :deep(.el-table__cell) {
      padding: 6px 0;
      vertical-align: top;
    }
  }

  .point-matrix-point__name {
    font-size: 13px;
    font-weight: 600;
    color: var(--el-text-color-primary);
  }

  .point-matrix-point__code {
    margin-top: 2px;
    font-size: 11px;
    color: var(--el-text-color-secondary);
  }

  .point-matrix-attribute-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
  }

  .point-matrix-cell {
    min-height: 42px;
    padding: 1px 0;

    &.is-dirty {
      :deep(.el-input__wrapper),
      :deep(.el-input-number .el-input__wrapper) {
        box-shadow: 0 0 0 1px var(--el-color-warning) inset;
      }
    }

    &.is-error {
      :deep(.el-input__wrapper),
      :deep(.el-input-number .el-input__wrapper) {
        box-shadow: 0 0 0 1px var(--el-color-danger) inset;
      }
    }
  }

  .point-matrix-input {
    width: 100%;
  }

  .point-matrix-cell__meta {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    margin-top: 3px;
    font-size: 11px;
    line-height: 14px;
  }

  .point-matrix-cell__dirty {
    color: var(--el-color-warning);
  }

  .point-matrix-cell__default {
    color: var(--el-text-color-secondary);
  }

  .point-matrix-cell__error {
    margin-top: 3px;
    font-size: 11px;
    color: var(--el-color-danger);
  }
</style>
