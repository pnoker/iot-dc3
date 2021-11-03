<template>
    <div class="tool-card">
        <el-card shadow="hover">
            <div class="tool-card-content">
                <div class="tool-card-content-search">
                    <el-form :inline="true" :model="formData" :rules="formRule" ref="formData" size="small">
                        <div class="tool-card-content-search-form">
                            <el-form-item label="位号名称" prop="name">
                                <el-input clearable
                                          placeholder="请输入位号名称"
                                          @keyup.enter.native="search"
                                          v-model="formData.name">
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
                        <el-form-item class="tool-card-content-search-button">
                            <el-button v-if="pre" type="success" icon="el-icon-back" @click="preHandle" plain>上一步</el-button>
                            <el-button type="primary" icon="el-icon-search" @click="search">搜索</el-button>
                            <el-button icon="el-icon-refresh-left" @click="reset">重置</el-button>
                            <el-button v-if="pre" type="warning" icon="el-icon-right" @click="nextHandle" plain>完成</el-button>
                        </el-form-item>
                    </el-form>
                </div>
                <div class="tool-card-content-operation">
                    <el-button type="success" size="small" icon="el-icon-plus" @click="showAdd">新增</el-button>
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
        name: "point-tool",
        props: {
            pre: {
                type: Boolean,
                default: () => {
                    return false;
                }
            },
            next: {
                type: Boolean,
                default: () => {
                    return false;
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
            showAdd() {
                this.$emit('showAdd');
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
            },
            preHandle() {
                this.$emit('pre-handle');
            },
            nextHandle() {
                this.$emit('next-handle');
            }
        }
    };
</script>

<style lang="scss">
    @import "~@/components/card/styles/tool-card.scss";
</style>
