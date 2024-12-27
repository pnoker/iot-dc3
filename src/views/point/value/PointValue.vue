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
        <point-value-tool
            :embedded="embedded"
            :page="reactiveData.page"
            @refresh="refresh"
            @reset="reset"
            @search="search"
            @size-change="sizeChange"
            @current-change="currentChange"
        ></point-value-tool>

        <blank-card>
            <el-row>
                <el-col v-for="data in 12" :key="data" :lg="6" :md="8" :sm="12" :xl="4" :xs="24">
                    <skeleton-card :loading="reactiveData.loading"></skeleton-card>
                </el-col>
                <el-col v-if="hasData">
                    <el-empty :description="embedded == 'device' ? '暂无设备数据' : '暂无数据, 请选择设备和位号!'"></el-empty>
                </el-col>
                <el-col v-for="data in reactiveData.listData" :key="data.id" :lg="6" :md="8" :sm="12" :xl="4" :xs="24">
                    <point-value-card
                        :data="data"
                        :device="reactiveData.deviceTable[data.deviceId]"
                        :embedded="embedded"
                        :point="reactiveData.pointTable[data.pointId]"
                        :unit="reactiveData.unitTable[data.pointId]"
                    ></point-value-card>
                </el-col>
            </el-row>
        </blank-card>
    </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive } from 'vue'

import { getPointByIds, getPointUnit, getPointValueLatest, getPointValueList } from '@/api/point'
import { getDeviceByIds } from '@/api/device'

import blankCard from '@/components/card/blank/BlankCard.vue'
import skeletonCard from '@/components/card/skeleton/SkeletonCard.vue'
import pointValueTool from './tool/PointValueTool.vue'
import pointValueCard from './card/PointValueCard.vue'

import { isNull } from '@/utils/utils'

const props = defineProps({
    embedded: {
        type: String,
        default: () => {
            return ''
        }
    },
    deviceId: {
        type: String,
        default: () => {
            return ''
        }
    }
})

const reactiveData = reactive({
    loading: true,
    deviceTable: {},
    pointTable: {},
    unitTable: {},
    listData: [] as any[],
    query: {},
    page: {
        total: 0,
        size: 12,
        current: 1
    }
})

const hasData = computed(() => {
    return !reactiveData.loading && reactiveData.listData?.length < 1
})

const list = () => {
    if (!isNull(props.deviceId)) {
        reactiveData.query = {
            ...reactiveData.query,
            deviceId: props.deviceId
        }
    }

    if (props.embedded == 'device') {
        getPointValueLatest({
            page: reactiveData.page,
            ...reactiveData.query
        })
            .then((res) => {
                loadPointValueList(res)
            })
            .catch(() => {
                // nothing to do
            })
            .finally(() => {
                reactiveData.loading = false
            })
    } else {
        getPointValueList({
            page: reactiveData.page,
            ...reactiveData.query
        })
            .then((res) => {
                loadPointValueList(res)
            })
            .catch(() => {
                // nothing to do
            })
            .finally(() => {
                reactiveData.loading = false
            })
    }
}

const loadPointValueList = (res) => {
    reactiveData.listData = res.data.records.map((record) => {
        const tempDate1 = new Date(record.createTime)
        const tempDate2 = new Date(record.originTime)
        record.interval = tempDate1.getTime() - tempDate2.getTime()
        return record
    })
    reactiveData.page.total = res.data.total

    // device
    const deviceIds = Array.from(new Set(reactiveData.listData.map((pointValue) => pointValue.deviceId)))
    if (deviceIds.length > 0) {
        getDeviceByIds(deviceIds)
            .then((res) => {
                reactiveData.deviceTable = res.data
            })
            .catch(() => {
                // nothing to do
            })
    }

    // point & unit
    const pointIds = Array.from(new Set(reactiveData.listData.map((pointValue) => pointValue.pointId)))
    if (pointIds.length > 0) {
        getPointByIds(pointIds)
            .then((res) => {
                reactiveData.pointTable = res.data
            })
            .catch(() => {
                // nothing to do
            })

        getPointUnit(pointIds)
            .then((res) => {
                reactiveData.unitTable = res.data
            })
            .catch(() => {
                // nothing to do
            })
    }
}

const search = (params) => {
    reactiveData.query = params
    list()
}

const reset = () => {
    reactiveData.query = {}
    list()
}

const refresh = () => {
    list()
}

const sizeChange = (size: number) => {
    reactiveData.page.size = size
    list()
}

const currentChange = (current: number) => {
    reactiveData.page.current = current
    list()
}

onMounted(() => {
    list()
})

defineExpose({
    refresh,
    list
})
</script>

<style lang="scss"></style>
