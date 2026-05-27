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
  <div class="edit-card">
    <div class="edit-card-header">
      <el-card shadow="hover">
        <el-steps :active="reactiveData.active" align-center finish-status="success">
          <el-step :title="$t('device.edit.deviceConfig')"></el-step>
          <el-step :title="$t('device.edit.driverConfig')"></el-step>
          <el-step :title="$t('device.edit.pointConfig')"></el-step>
          <el-step :title="$t('device.edit.commandConfig')"></el-step>
          <el-step :title="$t('device.edit.eventConfig')"></el-step>
          <el-step :title="$t('device.edit.complete')"></el-step>
        </el-steps>
      </el-card>
    </div>

    <div class="edit-card-body">
      <el-card v-if="reactiveData.active === 0" shadow="hover">
        <el-divider content-position="left">{{ $t('device.edit.deviceConfig') }}</el-divider>
        <el-form ref="deviceFormRef" :model="reactiveData.deviceFormData" :rules="deviceFormRule" label-position="top">
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
          <el-form-item :label="$t('common.enableFlag')" prop="enableFlag">
            <enable-flag-segmented v-model="reactiveData.deviceFormData.enableFlag" />
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
          <el-form-item :label="$t('device.edit.description')" prop="remark">
            <el-input
              v-model="reactiveData.deviceFormData.remark"
              :placeholder="$t('device.edit.descriptionPlaceholder')"
              clearable
              maxlength="300"
              show-word-limit
              type="textarea"
            />
          </el-form-item>
          <el-form-item class="edit-form-button">
            <el-button :icon="Back" plain @click="done">{{ $t('common.return') }}</el-button>
            <el-button :icon="RefreshLeft" @click="deviceReset">{{ $t('common.reset') }}</el-button>
            <el-button :icon="Right" plain type="primary" @click="next">{{ $t('common.next') }}</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card v-if="reactiveData.active === 1" shadow="hover">
        <el-divider content-position="left">{{ $t('device.edit.driverConfig') }}</el-divider>
        <el-alert
          :closable="false"
          :description="$t('device.edit.driverConfigTip')"
          :title="$t('device.edit.driverConfig')"
          type="success"
        />
        <el-form
          v-if="hasDriverAttributes"
          ref="driverFormRef"
          :model="reactiveData.driverFormData"
          label-position="top"
        >
          <el-form-item
            v-for="attribute in reactiveData.driverAttributes"
            :key="attribute.id"
            :prop="`${attribute.attributeCode}.configValue`"
            :rules="attributeFormItemRules(attribute)"
          >
            <template #label>
              <span>{{ attribute.attributeName }}</span>
              <el-tag class="attribute-type" effect="plain" size="small">{{ attribute.attributeTypeFlag }}</el-tag>
            </template>
            <el-switch
              v-if="isBooleanAttribute(attribute)"
              v-model="attributeFormItem(reactiveData.driverFormData, attribute).configValue"
              :active-value="true"
              :inactive-value="false"
            />
            <el-input-number
              v-else-if="isNumberAttribute(attribute)"
              v-model="attributeFormItem(reactiveData.driverFormData, attribute).configValue"
              :placeholder="attributePlaceholder(attribute)"
              :precision="attributePrecision(attribute)"
              class="attribute-number-input"
              controls-position="right"
            />
            <el-input
              v-else
              v-model="attributeFormItem(reactiveData.driverFormData, attribute).configValue"
              :placeholder="attributePlaceholder(attribute)"
              clearable
              maxlength="512"
              show-word-limit
              @keyup.enter="driverUpdate"
            />
            <div v-if="attribute.remark || attribute.defaultValue" class="attribute-hint">
              <span v-if="attribute.remark">{{ attribute.remark }}</span>
              <span v-if="attribute.defaultValue">{{
                $t('device.edit.defaultValue', { value: attribute.defaultValue })
              }}</span>
            </div>
          </el-form-item>
        </el-form>
        <el-empty v-else :description="$t('device.edit.driverConfigEmpty')" />
        <el-form-item class="edit-form-button">
          <el-button :icon="Back" plain @click="pre">{{ $t('common.previous') }}</el-button>
          <el-button v-if="hasDriverAttributes" :icon="RefreshLeft" @click="driverInfoReset">
            {{ $t('common.reset') }}
          </el-button>
          <el-button :icon="Right" plain type="primary" @click="next">{{ $t('common.next') }}</el-button>
        </el-form-item>
      </el-card>

      <el-card v-if="reactiveData.active === 2" shadow="hover">
        <el-divider content-position="left">{{ $t('device.edit.pointConfig') }}</el-divider>
        <el-alert
          :closable="false"
          :description="$t('device.edit.pointConfigTip')"
          :title="$t('device.edit.pointConfig')"
          type="success"
        />
        <div class="point-matrix-toolbar">
          <div class="point-matrix-toolbar__filters">
            <el-input
              v-model="reactiveData.pointMatrixKeyword"
              :placeholder="$t('device.edit.pointSearchPlaceholder')"
              :prefix-icon="Search"
              clearable
              size="small"
            />
            <matrix-status-segmented v-model="reactiveData.pointMatrixStatus" size="small" />
          </div>
          <div class="point-matrix-toolbar__actions">
            <el-tag :type="pointDirtyCount > 0 ? 'warning' : 'info'" effect="plain" size="small">
              {{ $t('device.edit.changedCount', { count: pointDirtyCount }) }}
            </el-tag>
            <el-button :disabled="pointDirtyCount < 1" :icon="RefreshLeft" size="small" @click="pointInfoReset">
              {{ $t('device.edit.discardChanges') }}
            </el-button>
            <el-button
              :disabled="pointDirtyCount < 1"
              :icon="Check"
              :loading="reactiveData.pointSaving"
              size="small"
              type="primary"
              @click="savePointMatrix"
            >
              {{ $t('device.edit.saveAll') }}
            </el-button>
          </div>
        </div>

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
                <div v-if="pointCellDirty(row, attribute) || attribute.defaultValue" class="point-matrix-cell__meta">
                  <span v-if="pointCellDirty(row, attribute)" class="point-matrix-cell__dirty">
                    {{ $t('device.edit.modified') }}
                  </span>
                  <span v-if="attribute.defaultValue" class="point-matrix-cell__default">
                    {{ $t('device.edit.defaultValue', { value: attribute.defaultValue }) }}
                  </span>
                </div>
                <div v-if="pointCellError(row, attribute)" class="point-matrix-cell__error">
                  {{ pointCellError(row, attribute) }}
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <el-form-item class="edit-form-button">
          <el-button :icon="Back" plain @click="pre">{{ $t('common.previous') }}</el-button>
          <el-button :icon="Right" :loading="reactiveData.pointSaving" plain type="primary" @click="next">
            {{ $t('common.next') }}
          </el-button>
        </el-form-item>
      </el-card>
      <el-card v-if="reactiveData.active === 3" shadow="hover">
        <el-divider content-position="left">{{ $t('device.edit.commandConfig') }}</el-divider>
        <el-alert
          :closable="false"
          :description="$t('device.edit.commandConfigTip')"
          :title="$t('device.edit.commandConfig')"
          type="success"
        />
        <div class="point-matrix-toolbar">
          <div class="point-matrix-toolbar__filters">
            <el-input
              v-model="reactiveData.commandMatrixKeyword"
              :placeholder="$t('device.edit.commandSearchPlaceholder')"
              :prefix-icon="Search"
              clearable
              size="small"
            />
            <matrix-status-segmented v-model="reactiveData.commandMatrixStatus" size="small" />
          </div>
          <div class="point-matrix-toolbar__actions">
            <el-tag :type="commandDirtyCount > 0 ? 'warning' : 'info'" effect="plain" size="small">
              {{ $t('device.edit.changedCount', { count: commandDirtyCount }) }}
            </el-tag>
            <el-button :disabled="commandDirtyCount < 1" :icon="RefreshLeft" size="small" @click="commandInfoReset">
              {{ $t('device.edit.discardChanges') }}
            </el-button>
            <el-button
              :disabled="commandDirtyCount < 1"
              :icon="Check"
              :loading="reactiveData.commandSaving"
              size="small"
              type="primary"
              @click="saveCommandMatrix"
            >
              {{ $t('device.edit.saveAll') }}
            </el-button>
          </div>
        </div>

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
                <div v-if="commandCellDirty(row, attribute) || attribute.defaultValue" class="point-matrix-cell__meta">
                  <span v-if="commandCellDirty(row, attribute)" class="point-matrix-cell__dirty">
                    {{ $t('device.edit.modified') }}
                  </span>
                  <span v-if="attribute.defaultValue" class="point-matrix-cell__default">
                    {{ $t('device.edit.defaultValue', { value: attribute.defaultValue }) }}
                  </span>
                </div>
                <div v-if="commandCellError(row, attribute)" class="point-matrix-cell__error">
                  {{ commandCellError(row, attribute) }}
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <el-form-item class="edit-form-button">
          <el-button :icon="Back" plain @click="pre">{{ $t('common.previous') }}</el-button>
          <el-button :icon="Right" :loading="reactiveData.commandSaving" plain type="primary" @click="next">
            {{ $t('common.next') }}
          </el-button>
        </el-form-item>
      </el-card>
      <el-card v-if="reactiveData.active === 4" shadow="hover">
        <el-divider content-position="left">{{ $t('device.edit.eventConfig') }}</el-divider>
        <el-alert
          :closable="false"
          :description="$t('device.edit.eventConfigTip')"
          :title="$t('device.edit.eventConfig')"
          type="success"
        />
        <div class="point-matrix-toolbar">
          <div class="point-matrix-toolbar__filters">
            <el-input
              v-model="reactiveData.eventMatrixKeyword"
              :placeholder="$t('device.edit.eventSearchPlaceholder')"
              :prefix-icon="Search"
              clearable
              size="small"
            />
            <matrix-status-segmented v-model="reactiveData.eventMatrixStatus" size="small" />
          </div>
          <div class="point-matrix-toolbar__actions">
            <el-tag :type="eventDirtyCount > 0 ? 'warning' : 'info'" effect="plain" size="small">
              {{ $t('device.edit.changedCount', { count: eventDirtyCount }) }}
            </el-tag>
            <el-button :disabled="eventDirtyCount < 1" :icon="RefreshLeft" size="small" @click="eventInfoReset">
              {{ $t('device.edit.discardChanges') }}
            </el-button>
            <el-button
              :disabled="eventDirtyCount < 1"
              :icon="Check"
              :loading="reactiveData.eventSaving"
              size="small"
              type="primary"
              @click="saveEventMatrix"
            >
              {{ $t('device.edit.saveAll') }}
            </el-button>
          </div>
        </div>

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
                <div v-if="eventCellDirty(row, attribute) || attribute.defaultValue" class="point-matrix-cell__meta">
                  <span v-if="eventCellDirty(row, attribute)" class="point-matrix-cell__dirty">
                    {{ $t('device.edit.modified') }}
                  </span>
                  <span v-if="attribute.defaultValue" class="point-matrix-cell__default">
                    {{ $t('device.edit.defaultValue', { value: attribute.defaultValue }) }}
                  </span>
                </div>
                <div v-if="eventCellError(row, attribute)" class="point-matrix-cell__error">
                  {{ eventCellError(row, attribute) }}
                </div>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <el-form-item class="edit-form-button">
          <el-button :icon="Back" plain @click="pre">{{ $t('common.previous') }}</el-button>
          <el-button :icon="Right" :loading="reactiveData.eventSaving" plain type="primary" @click="next">
            {{ $t('common.next') }}
          </el-button>
        </el-form-item>
      </el-card>
      <el-card v-if="reactiveData.active === 5" shadow="hover">
        <el-divider content-position="left">{{ $t('device.edit.complete') }}</el-divider>
        <el-result
          :sub-title="$t('device.edit.completeSubTitle')"
          :title="$t('device.edit.completeTitle')"
          icon="success"
        >
          <template #extra>
            <el-button plain type="primary" @click="done">{{ $t('common.return') }}</el-button>
          </template>
        </el-result>
      </el-card>
    </div>
  </div>
</template>

<script lang="ts" src="./index.ts" />

<style lang="scss" scoped>
  @use '@/styles/edit-card.scss';

  .attribute-type {
    margin-left: 8px;
    vertical-align: middle;
  }

  .attribute-number-input {
    width: 100%;
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

  .point-matrix-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-wrap: wrap;
    gap: 10px;
    margin: 10px 0;
  }

  .point-matrix-toolbar__filters {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;

    .el-input {
      width: 260px;
    }
  }

  .point-matrix-toolbar__actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 8px;
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
