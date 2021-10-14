<template>
    <div class="edit-card">
        <el-card shadow="hover">
            <div class="edit-content">
                <el-steps :active="active" align-center>
                    <el-step title="位号信息编辑"></el-step>
                </el-steps>
            </div>
        </el-card>

        <el-card shadow="hover" v-if="active===0">
            <div class="edit-content">
                <el-divider content-position="left">位号信息编辑</el-divider>
                <div class="content-edit-point">
                    <el-form :inline="true" :model="pointFormData" :rules="pointFormRule" ref="pointFormData" size="small">
                        <div class="edit-point-form">
                            <el-form-item label="位号名称" prop="name">
                                <el-input clearable
                                          placeholder="请输入位号名称"
                                          @keyup.enter.native="pointUpdate"
                                          v-model="pointFormData.name">
                                </el-input>
                            </el-form-item>
                            <el-form-item label="使能" prop="enable">
                                <el-select clearable
                                           class="edit-form-small"
                                           placeholder="请选择使能"
                                           v-model="pointFormData.enable">
                                    <el-option label="启用" :value="true"></el-option>
                                    <el-option label="停用" :value="false"></el-option>
                                </el-select>
                            </el-form-item>
                        </div>
                        <div class="edit-point-form">
                            <el-form-item label="数据类型" prop="type">
                                <el-select clearable
                                           class="edit-form-medium"
                                           placeholder="请选择数据类型"
                                           v-model="pointFormData.type">
                                    <el-option label="字符串(string)" value="string"></el-option>
                                    <el-option label="字节(byte)" value="byte"></el-option>
                                    <el-option label="短整数(short)" value="short"></el-option>
                                    <el-option label="整数(int)" value="int"></el-option>
                                    <el-option label="长整数(long)" value="long"></el-option>
                                    <el-option label="浮点数(float)" value="float"></el-option>
                                    <el-option label="双精度浮点数(double)" value="double"></el-option>
                                    <el-option label="布尔量(boolean)" value="boolean"></el-option>
                                </el-select>
                            </el-form-item>
                            <el-form-item label="读写类型" prop="rw">
                                <el-select clearable
                                           class="edit-form-medium"
                                           placeholder="请选择读写类型"
                                           v-model="pointFormData.rw">
                                    <el-option label="只读" :value="0"></el-option>
                                    <el-option label="只写" :value="1"></el-option>
                                    <el-option label="读写" :value="2"></el-option>
                                </el-select>
                            </el-form-item>
                            <el-form-item label="累计标识" prop="accrue">
                                <el-select clearable
                                           class="edit-form-medium"
                                           placeholder="请选择累计标识"
                                           v-model="pointFormData.accrue">
                                    <el-option label="累计" :value="true"></el-option>
                                    <el-option label="不累计" :value="false"></el-option>
                                </el-select>
                            </el-form-item>
                        </div>
                        <div class="edit-point-form">
                            <el-form-item label="数据格式" prop="format">
                                <el-input clearable
                                          placeholder="请输入数据格式"
                                          @keyup.enter.native="pointUpdate"
                                          v-model="pointFormData.format">
                                </el-input>
                            </el-form-item>
                            <el-form-item label="单位" prop="unit">
                                <el-input clearable
                                          placeholder="请输入单位"
                                          @keyup.enter.native="pointUpdate"
                                          v-model="pointFormData.unit">
                                </el-input>
                            </el-form-item>
                        </div>
                        <div class="edit-point-form">
                            <el-form-item label="基值" prop="base">
                                <el-input clearable
                                          placeholder="请输入基值"
                                          @keyup.enter.native="pointUpdate"
                                          v-model="pointFormData.base">
                                </el-input>
                            </el-form-item>
                            <el-form-item label="倍数" prop="multiple">
                                <el-input clearable
                                          placeholder="请输入倍数"
                                          @keyup.enter.native="pointUpdate"
                                          v-model="pointFormData.multiple">
                                </el-input>
                            </el-form-item>
                        </div>
                        <div class="edit-point-form">
                            <el-form-item label="最小值" prop="minimum">
                                <el-input clearable
                                          placeholder="请输入最小值"
                                          @keyup.enter.native="pointUpdate"
                                          v-model="pointFormData.minimum">
                                </el-input>
                            </el-form-item>
                            <el-form-item label="最大值" prop="maximum">
                                <el-input clearable
                                          placeholder="请输入最大值"
                                          @keyup.enter.native="pointUpdate"
                                          v-model="pointFormData.maximum">
                                </el-input>
                            </el-form-item>
                        </div>
                        <div class="edit-point-form">
                            <el-form-item label="位号描述" prop="description">
                                <el-input clearable
                                          class="edit-form-large"
                                          type="textarea"
                                          maxlength="300"
                                          show-word-limit
                                          placeholder="请输入位号描述"
                                          v-model="pointFormData.description">
                                </el-input>
                            </el-form-item>
                        </div>
                        <el-form-item class="edit-button">
                            <el-button type="primary" icon="el-icon-edit" @click="pointUpdate">修改</el-button>
                            <el-button icon="el-icon-refresh-left" @click="pointReset">恢复</el-button>
                            <el-button type="warning" icon="el-icon-right" @click="next" plain>完成</el-button>
                        </el-form-item>
                    </el-form>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
    import pointList from '../point/PointList'
    import {pointById, pointUpdate} from "@/api/point";

    export default {
        components: {pointList},
        data() {
            return {
                id: this.$route.query.id,
                active: +this.$route.query.active,
                oldPointFormData: {},
                pointFormData: {
                    pointIds: []
                },
                pointFormRule: {
                    name: [
                        {
                            required: true,
                            message: '请输入位号名称',
                            trigger: 'blur'
                        }, {
                            min: 2,
                            max: 32,
                            message: '请输入 2~32 位字长的位号名称',
                            trigger: 'blur'
                        }, {
                            pattern: /^[A-Za-z0-9\u4e00-\u9fa5][A-Za-z0-9\u4e00-\u9fa5-_]*$/,
                            message: '请输入正确格式的位号名称'
                        }
                    ],
                    enable: [
                        {
                            required: true,
                            message: '请选择使能',
                            trigger: 'change'
                        }
                    ],
                    description: [
                        {
                            max: 300,
                            message: '最多输入300个字符',
                            trigger: 'blur'
                        }
                    ]
                }
            }
        },
        created() {
            this.point();
        },
        methods: {
            point() {
                let id = this.$route.query.id;
                pointById(id).then(res => {
                    this.pointFormData = res.data;
                    this.oldPointFormData = {...res.data};
                });
            },
            pointUpdate() {
                this.$refs['pointFormData'].validate((valid) => {
                    if (valid) {
                        pointUpdate(this.pointFormData).then(res => {
                            this.oldPointFormData = {...res.data};
                        });
                    }
                });
            },
            pre() {
                this.active--;
                this.changeActive(this.active);
            },
            next() {
                this.active++;
                if (this.active > 0) {
                    this.$router.push({name: 'profileEdit', query: {id: this.$route.query.profileId, active: '1'}});
                } else {
                    this.changeActive(this.active);
                }
            },
            pointReset() {
                this.pointFormData = {...this.oldPointFormData};
            },
            changeActive(step) {
                let query = this.$route.query;
                this.$router.push({query: {...query, active: step}});
            }
        }
    };
</script>

<style lang="scss">
    @import "~@/components/card/styles/edit-card.scss";
</style>
