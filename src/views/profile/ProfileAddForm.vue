<template>
    <el-dialog title="新增模板" :visible.sync="formVisible"
               class="things-dialog"
               :show-close="false"
               :append-to-body="true"
               :model-append-to-body="false"
               :close-on-click-modal="false"
               :close-on-press-escape="false">
        <el-form ref="formData" size="small" :model="formData" :rules="formRule">
            <el-form-item class="things-dialog-form-item" label="模板名称" prop="name">
                <el-input clearable
                          placeholder="请输入模板名称"
                          v-model="formData.name">
                </el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="描述" prop="description">
                <el-input clearable
                          type="textarea"
                          maxlength="300"
                          show-word-limit
                          placeholder="请输入模板描述"
                          v-model="formData.description">
                </el-input>
            </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
            <el-button @click="cancel" size="small">取消</el-button>
            <el-button @click="reset" type="success" size="small" plain>重置</el-button>
            <el-button type="primary" @click="addThing" size="small">确定</el-button>
        </div>
    </el-dialog>
</template>

<script>
    import {successMessage} from "@/util/util";

    export default {
        name: "profile-add-form",
        props: {},
        data() {
            return {
                formData: {},
                formRule: {
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
                    description: [
                        {
                            max: 300,
                            message: '最多输入300个字符',
                            trigger: 'blur'
                        }
                    ]
                },
                formVisible: false
            }
        },
        methods: {
            show() {
                this.formVisible = true;
            },
            cancel() {
                this.formVisible = false;
            },
            reset() {
                this.$refs['formData'].resetFields();
            },
            addThing() {
                this.$refs['formData'].validate((valid) => {
                    if (valid) {
                        this.$emit('add-thing', this.formData, () => {
                            this.cancel();
                            this.reset();
                            successMessage();
                        });
                    }
                });
            }

        }
    };
</script>

<style lang="scss">
    @import "~@/components/dialog/styles/things-dialog.scss";
</style>
