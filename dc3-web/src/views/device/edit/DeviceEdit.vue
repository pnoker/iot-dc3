<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - This program is free software: you can redistribute it and/or modify
  - it under the terms of the GNU Affero General Public License as
  - published by the Free Software Foundation, either version 3 of the
  - License, or (at your option) any later version.
  -
  - This program is distributed in the hope that it will be useful,
  - but WITHOUT ANY WARRANTY; without even the implied warranty of
  - MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  - GNU Affero General Public License for more details.
  -
  - You should have received a copy of the GNU Affero General Public License
  - along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
                <enable-flag-segmented v-model="reactiveData.deviceFormData.enableFlag"/>
              </el-form-item>
              <el-form-item :label="$t('device.edit.description')" class="info-card-item-full" prop="remark">
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
        <el-tab-pane :label="$t('device.edit.driverConfig')" name="driverConfig">
          <matrix-toolbar
            :dirty-count="driverDirtyCount"
            :form-model="reactiveData"
            :saving="reactiveData.driverSaving"
            @discard="driverInfoReset"
            @save="saveDriverMatrix"
          />
          <el-empty v-if="!hasDriverAttributes" :description="$t('device.edit.driverAttributeEmpty')"/>
          <el-table
            v-else
            v-loading="reactiveData.loading"
            :data="reactiveData.driverAttributes"
            class="matrix-table"
            row-key="id"
            stripe
          >
            <el-table-column :label="$t('device.edit.attributeName')" min-width="140" prop="attributeName"/>
            <el-table-column :label="$t('device.edit.attributeType')" width="90">
              <template #default="{row}">
                <el-tag effect="plain" size="small">{{ row.attributeTypeFlag }}</el-tag>
              </template>
            </el-table-column>
            <!-- @vue-generic {import('@/config/types').Attribute} -->
            <el-table-column :label="$t('device.edit.configValue')" min-width="240">
              <template #default="{row: attribute}">
                <div class="matrix-cell">
                  <el-switch
                    v-if="isBooleanAttribute(attribute)"
                    :active-value="true"
                    :inactive-value="false"
                    :model-value="getDriverCellValue(attribute)"
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
                    style="width: 100%"
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
                    style="width: 100%"
                    @input="(val: string) => setDriverCellValue(attribute, val)"
                  />
                  <div v-if="driverCellDirty(attribute)" class="matrix-cell__meta">
                    <span class="matrix-cell__dirty">{{ $t('device.edit.modified') }}</span>
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
                <matrix-status-segmented v-model="reactiveData.pointMatrixStatus"/>
              </el-form-item>
            </template>
            <template #trailing>
              <el-pagination
                v-if="filteredPointInfoData.length > reactiveData.pointPageSize"
                v-model:current-page="reactiveData.pointPageCurrent"
                v-model:page-size="reactiveData.pointPageSize"
                :page-sizes="[10, 20, 50]"
                :total="filteredPointInfoData.length"
                layout="total, sizes, prev, pager, next"
                size="small"
              />
            </template>
          </matrix-toolbar>

          <el-empty v-if="!hasPointAttributes" :description="$t('device.edit.pointAttributeEmpty')"/>
          <el-table
            v-else
            v-loading="reactiveData.loading"
            :data="paginatedPointInfoData"
            :row-class-name="pointMatrixRowClassName"
            stripe
          >
            <el-table-column :label="$t('device.edit.pointName')" fixed min-width="160" show-overflow-tooltip>
              <template #default="{row}">
                <el-tooltip :content="row.pointCode || row.id" :disabled="!row.pointCode" placement="top">
                  <span class="point-matrix-name">{{ row.pointName }}</span>
                </el-tooltip>
              </template>
            </el-table-column>
            <!-- @vue-generic {import('./index').PointInfoMatrixRow} -->
            <el-table-column :label="$t('device.edit.configStatus')" fixed width="100">
              <template #default="{row}">
                <el-tag :type="pointRowStatusTag(row)" effect="plain" size="small">
                  {{ pointRowStatusLabel(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <!-- @vue-generic {import('./index').PointInfoMatrixRow} -->
            <el-table-column v-for="attribute in reactiveData.pointAttributes" :key="attribute.id" min-width="180">
              <template #header>
                <div class="point-matrix-attribute-header">
                  <span>{{ attribute.attributeName }}</span>
                  <el-tag effect="plain" size="small">{{ attribute.attributeTypeFlag || 'STRING' }}</el-tag>
                </div>
              </template>
              <template #default="{row}">
                <div
                  :class="[
                    'matrix-cell',
                    pointCellDirty(row, attribute) ? 'is-dirty' : '',
                    pointCellError(row, attribute) ? 'is-error' : '',
                  ]"
                >
                  <el-switch
                    v-if="isBooleanAttribute(attribute)"
                    v-model="pointCell(row, attribute).configValue"
                    :active-value="true"
                    :inactive-value="false"
                    size="small"
                    @change="markPointCellDirty(row, attribute)"
                  />
                  <el-input
                    v-else-if="isNumberAttribute(attribute)"
                    v-model="pointCell(row, attribute).configValue"
                    :placeholder="attributePlaceholder(attribute)"
                    clearable
                    inputmode="decimal"
                    maxlength="512"
                    size="small"
                    style="width: 100%"
                    @blur="validatePointCell(row, attribute)"
                    @input="markPointCellDirty(row, attribute)"
                  />
                  <el-input
                    v-else
                    v-model="pointCell(row, attribute).configValue"
                    :placeholder="attributePlaceholder(attribute)"
                    clearable
                    maxlength="512"
                    size="small"
                    @input="markPointCellDirty(row, attribute)"
                  />
                  <div v-if="pointCellDirty(row, attribute)" class="matrix-cell__meta">
                    <span class="matrix-cell__dirty">
                      {{ $t('device.edit.modified') }}
                    </span>
                  </div>
                  <div v-if="pointCellError(row, attribute)" class="matrix-cell__error">
                    {{ pointCellError(row, attribute) }}
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Command Config -->
        <el-tab-pane :label="$t('device.detail.relatedCommands')" name="commandConfig">
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
                <matrix-status-segmented v-model="reactiveData.commandMatrixStatus"/>
              </el-form-item>
            </template>
            <template #trailing>
              <el-pagination
                v-if="filteredCommandInfoData.length > reactiveData.commandPageSize"
                v-model:current-page="reactiveData.commandPageCurrent"
                v-model:page-size="reactiveData.commandPageSize"
                :page-sizes="[10, 20, 50]"
                :total="filteredCommandInfoData.length"
                layout="total, sizes, prev, pager, next"
                size="small"
              />
            </template>
          </matrix-toolbar>

          <el-empty v-if="!hasCommandAttributes" :description="$t('device.edit.commandAttributeEmpty')"/>
          <el-table
            v-else
            v-loading="reactiveData.loading"
            :data="paginatedCommandInfoData"
            :row-class-name="commandMatrixRowClassName"
            stripe
          >
            <el-table-column :label="$t('device.edit.commandName')" fixed min-width="160" show-overflow-tooltip>
              <template #default="{row}">
                <el-tooltip :content="row.commandCode || row.id" :disabled="!row.commandCode" placement="top">
                  <span class="point-matrix-name">{{ row.commandName }}</span>
                </el-tooltip>
              </template>
            </el-table-column>
            <!-- @vue-generic {import('./index').CommandInfoMatrixRow} -->
            <el-table-column :label="$t('device.edit.configStatus')" fixed width="100">
              <template #default="{row}">
                <el-tag :type="commandRowStatusTag(row)" effect="plain" size="small">
                  {{ commandRowStatusLabel(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <!-- @vue-generic {import('./index').CommandInfoMatrixRow} -->
            <el-table-column v-for="attribute in reactiveData.commandAttributes" :key="attribute.id" min-width="180">
              <template #header>
                <div class="point-matrix-attribute-header">
                  <span>{{ attribute.attributeName }}</span>
                  <el-tag effect="plain" size="small">{{ attribute.attributeTypeFlag || 'STRING' }}</el-tag>
                </div>
              </template>
              <template #default="{row}">
                <div
                  :class="[
                    'matrix-cell',
                    commandCellDirty(row, attribute) ? 'is-dirty' : '',
                    commandCellError(row, attribute) ? 'is-error' : '',
                  ]"
                >
                  <el-switch
                    v-if="isBooleanAttribute(attribute)"
                    v-model="commandCell(row, attribute).configValue"
                    :active-value="true"
                    :inactive-value="false"
                    size="small"
                    @change="markCommandCellDirty(row, attribute)"
                  />
                  <el-input
                    v-else-if="isNumberAttribute(attribute)"
                    v-model="commandCell(row, attribute).configValue"
                    :placeholder="attributePlaceholder(attribute)"
                    clearable
                    inputmode="decimal"
                    maxlength="512"
                    size="small"
                    style="width: 100%"
                    @blur="validateCommandCell(row, attribute)"
                    @input="markCommandCellDirty(row, attribute)"
                  />
                  <el-input
                    v-else
                    v-model="commandCell(row, attribute).configValue"
                    :placeholder="attributePlaceholder(attribute)"
                    clearable
                    maxlength="512"
                    size="small"
                    @input="markCommandCellDirty(row, attribute)"
                  />
                  <div v-if="commandCellDirty(row, attribute)" class="matrix-cell__meta">
                    <span class="matrix-cell__dirty">
                      {{ $t('device.edit.modified') }}
                    </span>
                  </div>
                  <div v-if="commandCellError(row, attribute)" class="matrix-cell__error">
                    {{ commandCellError(row, attribute) }}
                  </div>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- Event Config -->
        <el-tab-pane :label="$t('device.detail.relatedEvents')" name="eventConfig">
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
                <matrix-status-segmented v-model="reactiveData.eventMatrixStatus"/>
              </el-form-item>
            </template>
            <template #trailing>
              <el-pagination
                v-if="filteredEventInfoData.length > reactiveData.eventPageSize"
                v-model:current-page="reactiveData.eventPageCurrent"
                v-model:page-size="reactiveData.eventPageSize"
                :page-sizes="[10, 20, 50]"
                :total="filteredEventInfoData.length"
                layout="total, sizes, prev, pager, next"
                size="small"
              />
            </template>
          </matrix-toolbar>

          <el-empty v-if="!hasEventAttributes" :description="$t('device.edit.eventAttributeEmpty')"/>
          <el-table
            v-else
            v-loading="reactiveData.loading"
            :data="paginatedEventInfoData"
            :row-class-name="eventMatrixRowClassName"
            stripe
          >
            <el-table-column :label="$t('device.edit.eventName')" fixed min-width="160" show-overflow-tooltip>
              <template #default="{row}">
                <el-tooltip :content="row.eventCode || row.id" :disabled="!row.eventCode" placement="top">
                  <span class="point-matrix-name">{{ row.eventName }}</span>
                </el-tooltip>
              </template>
            </el-table-column>
            <!-- @vue-generic {import('./index').EventInfoMatrixRow} -->
            <el-table-column :label="$t('device.edit.configStatus')" fixed width="100">
              <template #default="{row}">
                <el-tag :type="eventRowStatusTag(row)" effect="plain" size="small">
                  {{ eventRowStatusLabel(row) }}
                </el-tag>
              </template>
            </el-table-column>
            <!-- @vue-generic {import('./index').EventInfoMatrixRow} -->
            <el-table-column v-for="attribute in reactiveData.eventAttributes" :key="attribute.id" min-width="180">
              <template #header>
                <div class="point-matrix-attribute-header">
                  <span>{{ attribute.attributeName }}</span>
                  <el-tag effect="plain" size="small">{{ attribute.attributeTypeFlag || 'STRING' }}</el-tag>
                </div>
              </template>
              <template #default="{row}">
                <div
                  :class="[
                    'matrix-cell',
                    eventCellDirty(row, attribute) ? 'is-dirty' : '',
                    eventCellError(row, attribute) ? 'is-error' : '',
                  ]"
                >
                  <el-switch
                    v-if="isBooleanAttribute(attribute)"
                    v-model="eventCell(row, attribute).configValue"
                    :active-value="true"
                    :inactive-value="false"
                    size="small"
                    @change="markEventCellDirty(row, attribute)"
                  />
                  <el-input
                    v-else-if="isNumberAttribute(attribute)"
                    v-model="eventCell(row, attribute).configValue"
                    :placeholder="attributePlaceholder(attribute)"
                    clearable
                    inputmode="decimal"
                    maxlength="512"
                    size="small"
                    style="width: 100%"
                    @blur="validateEventCell(row, attribute)"
                    @input="markEventCellDirty(row, attribute)"
                  />
                  <el-input
                    v-else
                    v-model="eventCell(row, attribute).configValue"
                    :placeholder="attributePlaceholder(attribute)"
                    clearable
                    maxlength="512"
                    size="small"
                    @input="markEventCellDirty(row, attribute)"
                  />
                  <div v-if="eventCellDirty(row, attribute)" class="matrix-cell__meta">
                    <span class="matrix-cell__dirty">
                      {{ $t('device.edit.modified') }}
                    </span>
                  </div>
                  <div v-if="eventCellError(row, attribute)" class="matrix-cell__error">
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

<script lang="ts" src="./index.ts"/>

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

.matrix-table {
  width: 100%;

  :deep(.point-matrix-row-dirty) {
    --el-table-tr-bg-color: var(--el-color-warning-light-9);
  }
}

.point-matrix-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.point-matrix-attribute-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
}

.matrix-cell {
  &.is-dirty :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 1px var(--el-color-warning) inset;
  }

  &.is-error :deep(.el-input__wrapper) {
    box-shadow: 0 0 0 1px var(--el-color-danger) inset;
  }
}

.matrix-cell__meta {
  display: flex;
  gap: 4px;
  margin-top: 2px;
  font-size: 10px;
  line-height: 1;
}

.matrix-cell__dirty {
  color: var(--el-color-warning);
}

.matrix-cell__error {
  margin-top: 2px;
  font-size: 10px;
  color: var(--el-color-danger);
}
</style>
