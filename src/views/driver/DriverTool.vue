<template>
    <div class="tool-card">
        <el-card shadow="hover">
            <div class="tool-content">
                <div class="content-search">
                    <el-form :inline="true" :model="formData" :rules="formRule" ref="formData" size="small">
                        <div class="search-form">
                            <el-form-item label="驱动名称" prop="name">
                                <el-input clearable
                                          placeholder="请输入驱动名称"
                                          @keyup.enter.native="search"
                                          v-model="formData.name">
                                </el-input>
                            </el-form-item>
                            <el-form-item label="服务名称" prop="serviceName">
                                <el-input clearable
                                          placeholder="请输入服务名称"
                                          @keyup.enter.native="search"
                                          v-model="formData.serviceName">
                                </el-input>
                            </el-form-item>
                            <el-form-item label="主机" prop="host">
                                <el-input clearable
                                          placeholder="请输入主机"
                                          @keyup.enter.native="search"
                                          v-model="formData.host">
                                </el-input>
                            </el-form-item>
                            <el-form-item label="端口" prop="port">
                                <el-input clearable
                                          class="search-form-small"
                                          autocomplete="off"
                                          placeholder="请输入端口"
                                          @keyup.enter.native="search"
                                          v-model.number="formData.port">
                                </el-input>
                            </el-form-item>
                            <el-form-item label="使能" prop="enable">
                                <el-select clearable
                                           class="search-form-small"
                                           placeholder="请选择使能"
                                           v-model="formData.enable">
                                    <el-option label="启用" :value="true"></el-option>
                                    <el-option label="停用" :value="false"></el-option>
                                </el-select>
                            </el-form-item>
                        </div>
                        <el-form-item class="search-button">
                            <el-button type="primary" icon="el-icon-search" @click="search">搜索</el-button>
                            <el-button icon="el-icon-refresh-left" @click="reset">重置</el-button>
                        </el-form-item>
                    </el-form>
                </div>
                <div class="tool-operation">
                    <el-button type="success" size="small" icon="el-icon-plus" disabled>新增</el-button>
                    <div class="tool-page">
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
                        <el-tooltip class="item" effect="dark" content="排序" placement="top">
                            <el-button size="small" icon="el-icon-sort" @click="sort" circle></el-button>
                        </el-tooltip>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
    export default {
        name: "driver-tool",
        props: {
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
                formRule: {
                    port: [
                        {type: 'number', message: '端口必须为数字值'}
                    ]
                }
            }
        },
        methods: {
            search() {
                this.$refs['formData'].validate((valid) => {
                    if (valid) {
                        this.$emit('search', this.formData);
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
            sort() {
                this.$emit('sort');
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
