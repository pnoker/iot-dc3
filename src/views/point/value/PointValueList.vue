<template>
    <div>
        <el-row>
            <point-value-tool
                    :page="page"
                    :deviceDictionary="deviceDictionary"
                    :pointDictionary="pointDictionary"
                    @search="search"
                    @reset="reset"
                    @refresh="refresh"
                    @size-change="sizeChange"
                    @current-change="currentChange"
            ></point-value-tool>
        </el-row>

        <blank-card>
            <el-row>
                <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in 12">
                    <skeleton-card :loading="loading"></skeleton-card>
                </el-col>
                <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in listData">
                    <point-value-card
                            :data="data"
                            :unit="unitTable[data.pointId]||'”'"
                            :deviceTable="deviceTable"
                            :pointTable="pointTable"
                    ></point-value-card>
                </el-col>
            </el-row>
        </blank-card>
    </div>
</template>

<script>
    import blankCard from '@/components/card/blank-card';
    import skeletonCard from '@/components/card/skeleton-card';
    import pointValueTool from './PointValueTool';
    import pointValueCard from './PointValueCard';
    import {pointUnit, pointValueList} from "@/api/point";
    import {deviceDictionary, pointDictionary} from "@/api/dictionary";

    export default {
        components: {blankCard, skeletonCard, pointValueTool, pointValueCard},
        data() {
            return {
                loading: true,
                deviceDictionary: [],
                pointDictionary: [],
                deviceTable: {},
                pointTable: {},
                unitTable: {},
                listData: [],
                query: {},
                page: {
                    total: 0,
                    size: 12,
                    current: 1
                }
            }
        },
        created() {
            this.device();
            this.point();
            this.list();
        },
        methods: {
            list() {
                pointValueList({
                    page: this.page,
                    ...this.query
                }).then(res => {
                    this.listData = res.data.records.map(record => {
                        let tempDate1 = new Date(record.createTime);
                        let tempDate2 = new Date(record.originTime);
                        record.interval = tempDate1.getTime() - tempDate2.getTime();
                        return record;
                    });
                    this.page.total = res.data.total;

                    // point unit
                    let pointIds = Array.from(new Set(res.data.records.map(pointValue => pointValue.pointId)));
                    if (pointIds.length > 0) {
                        pointUnit(pointIds).then(res => {
                            this.unitTable = res.data;
                        });
                    }
                }).catch(() => {
                }).finally(() => {
                    this.loading = false;
                });
            },
            device() {
                deviceDictionary().then(res => {
                    this.deviceDictionary = res.data;
                    this.deviceTable = this.deviceDictionary.reduce((pre, cur) => {
                        if (cur.children) {
                            cur.children.forEach(dictionary => {
                                pre[dictionary.value] = dictionary.label;
                            });
                        }
                        return pre;
                    }, {});
                }).catch(() => {
                });
            },
            point() {
                pointDictionary('profile').then(res => {
                    this.pointDictionary = res.data;
                    this.pointTable = this.pointDictionary.reduce((pre, cur) => {
                        if (cur.children) {
                            cur.children.forEach(dictionary => {
                                pre[dictionary.value] = dictionary.label;
                            });
                        }
                        return pre;
                    }, {});
                }).catch(() => {
                });
            },
            search(params) {
                this.query = params;
                this.list();
            },
            reset() {
                this.query = {};
                this.list();
            },
            refresh() {
                this.list();
            },
            sizeChange(size) {
                this.page.size = size;
                this.list();
            },
            currentChange(current) {
                this.page.current = current;
                this.list();
            }
        }
    }
</script>

<style lang="scss">
</style>
