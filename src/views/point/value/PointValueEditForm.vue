<template>
    <el-dialog title="编辑位号值" :visible.sync="formVisible"
               class="things-dialog"
               :show-close="false"
               :append-to-body="true"
               :model-append-to-body="false"
               :close-on-click-modal="false"
               :close-on-press-escape="false">
        <el-form ref="formData" size="small" :model="formData" :rules="formRule">
            <el-form-item class="things-dialog-form-item" label="位号值" prop="value">
                <el-input clearable
                          placeholder="请输入位号值"
                          v-model="formData.value">
                </el-input>
            </el-form-item>
            <el-form-item class="things-dialog-form-item" label="操作描述" prop="description">
                <el-input clearable
                          type="textarea"
                          maxlength="300"
                          show-word-limit
                          placeholder="请输入本次操作描述"
                          v-model="formData.description">
                </el-input>
            </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
            <el-button @click="cancel" size="small">取消</el-button>
            <el-button @click="reset" type="success" size="small" plain>重置</el-button>
            <el-button type="primary" @click="updateThing" size="small">确定</el-button>
        </div>
    </el-dialog>
</template>

<script>
    import {successMessage} from "@/util/util";

    export default {
        name: "point-value-edit-form",
        props: {
            formData: {
                type: Object,
                default: () => {
                    return {}
                }
            }
        },
        data() {
            return {
                formRule: {
                    value: [
                        {
                            required: true,
                            message: '请输入位号值',
                            trigger: 'blur'
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
            updateThing() {
                this.$refs['formData'].validate((valid) => {
                    if (valid) {
                        this.$emit('update-thing', this.formData, () => {
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
