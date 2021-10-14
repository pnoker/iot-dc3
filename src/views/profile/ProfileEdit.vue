<template>
    <div class="edit-card">
        <el-card shadow="hover">
            <div class="edit-content">
                <el-steps :active="active" align-center>
                    <el-step title="模板信息编辑"></el-step>
                    <el-step title="模板位号编辑"></el-step>
                </el-steps>
            </div>
        </el-card>

        <el-card shadow="hover" v-if="active===0">
            <div class="edit-content">
                <el-divider content-position="left">模板信息编辑</el-divider>
                <div class="content-edit-profile">
                    <el-form :inline="true" :model="profileFormData" :rules="profileFormRule" ref="profileFormData" size="small">
                        <div class="edit-profile-form">
                            <el-form-item label="模板名称" prop="name">
                                <el-input clearable
                                          placeholder="请输入模板名称"
                                          @keyup.enter.native="profileUpdate"
                                          v-model="profileFormData.name">
                                </el-input>
                            </el-form-item>
                            <el-form-item label="使能" prop="enable">
                                <el-select clearable
                                           class="edit-form-small"
                                           placeholder="请选择使能"
                                           v-model="profileFormData.enable">
                                    <el-option label="启用" :value="true"></el-option>
                                    <el-option label="停用" :value="false"></el-option>
                                </el-select>
                            </el-form-item>
                        </div>
                        <div class="edit-profile-form">
                            <el-form-item label="模板描述" prop="description">
                                <el-input clearable
                                          class="edit-form-large"
                                          type="textarea"
                                          maxlength="300"
                                          show-word-limit
                                          placeholder="请输入模板描述"
                                          v-model="profileFormData.description">
                                </el-input>
                            </el-form-item>
                        </div>
                        <el-form-item class="edit-button">
                            <el-button type="primary" icon="el-icon-edit" @click="profileUpdate">修改</el-button>
                            <el-button icon="el-icon-refresh-left" @click="profileReset">恢复</el-button>
                            <el-button type="warning" icon="el-icon-right" @click="next" plain>下一步</el-button>
                        </el-form-item>
                    </el-form>
                </div>
            </div>
        </el-card>
        <el-card shadow="hover" v-if="active===1">
            <div class="edit-content">
                <el-divider content-position="left">模板位号配置</el-divider>
                <point-list
                        :pre="true"
                        :profileId="id"
                        @pre-handle="pre"
                        @next-handle="next"
                ></point-list>
            </div>
        </el-card>
    </div>
</template>

<script>
    import pointList from '../point/PointList'
    import {profileById, profileUpdate} from "@/api/profile";

    export default {
        components: {pointList},
        data() {
            return {
                id: this.$route.query.id,
                active: +this.$route.query.active,
                oldProfileFormData: {},
                profileFormData: {
                    pointIds: []
                },
                profileFormRule: {
                    name: [
                        {
                            required: true,
                            message: '请输入模板名称',
                            trigger: 'blur'
                        }, {
                            min: 2,
                            max: 32,
                            message: '请输入 2~32 位字长的模板名称',
                            trigger: 'blur'
                        }, {
                            pattern: /^[A-Za-z0-9\u4e00-\u9fa5][A-Za-z0-9\u4e00-\u9fa5-_]*$/,
                            message: '请输入正确格式的模板名称'
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
            this.profile();
        },
        methods: {
            profile() {
                let id = this.$route.query.id;
                profileById(id).then(res => {
                    this.profileFormData = res.data;
                    this.oldProfileFormData = {...res.data};
                });
            },
            profileUpdate() {
                this.$refs['profileFormData'].validate((valid) => {
                    if (valid) {
                        profileUpdate(this.profileFormData).then(res => {
                            this.oldProfileFormData = {...res.data};
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
                if (this.active > 1) {
                    this.$router.push({name: 'profile'});
                } else {
                    this.changeActive(this.active);
                }
            },
            profileReset() {
                this.profileFormData = {...this.oldProfileFormData};
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
