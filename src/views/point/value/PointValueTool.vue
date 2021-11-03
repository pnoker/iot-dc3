<template>
    <div class="tool-card">
        <el-card shadow="hover">
            <div class="tool-card-content">
                <div class="tool-card-content-search">
                    <el-form :inline="true" :model="formData" :rules="formRule" ref="formData" size="small">
                        <div class="tool-card-content-search-form">
                            <el-form-item label="设备" prop="deviceId">
                                <el-cascader clearable
                                             class="search-form-large"
                                             :show-all-levels="false"
                                             placeholder="请选择设备"
                                             v-model="formData.deviceId"
                                             :options="deviceDictionary">
                                </el-cascader>
                            </el-form-item>
                            <el-form-item label="位号" prop="pointId">
                                <el-cascader clearable
                                             class="search-form-large"
                                             :show-all-levels="false"
                                             placeholder="请选择位号"
                                             v-model="formData.pointId"
                                             :options="pointDictionary">
                                </el-cascader>
                            </el-form-item>
                        </div>
                        <el-form-item class="tool-card-content-search-button">
                            <el-button type="primary" icon="el-icon-search" @click="search">搜索</el-button>
                            <el-button icon="el-icon-refresh-left" @click="reset">重置</el-button>
                        </el-form-item>
                    </el-form>
                </div>
                <div class="tool-card-content-operation">
                    <el-button type="success" size="small" icon="el-icon-plus" disabled>新增</el-button>
                    <div class="tool-card-content-operation-page">
                        <el-pagination background layout="total, prev, pager, next, sizes"
                                       :page-sizes="[12, 24, 36, 48]"
                                       :page-size="+page.size"
                                       :current-page="+page.current"
                                       :total="+page.total"
                                       @size-change="sizeChange"
                                       @current-change="currentChange"></el-pagination>
                        <el-tooltip class="item" effect="dark" content="刷新" placement="top">
                            <el-button size="small" icon="el-icon-refresh" @click="refresh" circle></el-button>
                        </el-tooltip>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
    export default {
        name: "point-value-tool",
        props: {
            deviceDictionary: {
                type: Array,
                default: () => {
                    return [];
                }
            },
            pointDictionary: {
                type: Array,
                default: () => {
                    return [];
                }
            },
            page: {
                type: Object,
                default: () => {
                    return {}
                }
            }
        },
        data() {
            return {
                formData: {},
                formRule: {}
            }
        },
        methods: {
            search() {
                this.$refs['formData'].validate((valid) => {
                    if (valid) {
                        let formData = {};
                        let data = this.formData;
                        if (data.deviceId && data.deviceId[1]) {
                            formData.deviceId = data.deviceId[1];
                        }
                        if (data.pointId && data.pointId[1]) {
                            formData.pointId = data.pointId[1];
                        }
                        this.$emit('search', formData);
                    }
                });
            },
            reset() {
                this.$refs['formData'].resetFields();
                this.$emit('reset');
            },
            refresh() {
                this.$emit('refresh');
            },
            sizeChange(size) {
                this.$emit('size-change', size);
            },
            currentChange(current) {
                this.$emit('current-change', current);
            }
        }
    };
</script>

<style lang="scss">
    @import "~@/components/card/styles/tool-card.scss";
</style>
