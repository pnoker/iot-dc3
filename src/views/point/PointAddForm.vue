<template>
    <el-dialog title="新增位号" :visible.sync="formVisible"
               class="thing-dialog"
               :show-close="false"
               :append-to-body="true"
               :model-append-to-body="false"
               :close-on-click-modal="false"
               :close-on-press-escape="false">
        <el-form ref="formData" size="small" :model="formData" :rules="formRule">
            <el-form-item class="thing-dialog-form-item" label="位号名称" prop="name">
                <el-input clearable
                          placeholder="请输入位号名称"
                          v-model="formData.name">
                </el-input>
            </el-form-item>
            <el-form-item class="thing-dialog-form-item" label="数据类型" prop="type">
                <el-select clearable
                           placeholder="请选择数据类型"
                           v-model="formData.type">
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
            <el-form-item class="thing-dialog-form-item" label="读写类型" prop="rw">
                <el-select clearable
                           placeholder="请选择读写类型"
                           v-model="formData.rw">
                    <el-option label="只读" :value="0"></el-option>
                    <el-option label="只写" :value="1"></el-option>
                    <el-option label="读写" :value="2"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item class="thing-dialog-form-item" label="累计标识" prop="accrue">
                <el-select clearable
                           placeholder="请选择累计标识"
                           v-model="formData.accrue">
                    <el-option label="累计" :value="true"></el-option>
                    <el-option label="不累计" :value="false"></el-option>
                </el-select>
            </el-form-item>
            <el-form-item class="thing-dialog-form-item" label="数据格式" prop="format">
                <el-input clearable
                          placeholder="请输入数据格式"
                          v-model="formData.format">
                </el-input>
            </el-form-item>
            <el-form-item class="thing-dialog-form-item" label="单位" prop="unit">
                <el-input clearable
                          placeholder="请输入单位"
                          v-model="formData.unit">
                </el-input>
            </el-form-item>
            <el-form-item class="thing-dialog-form-item" label="基值" prop="base">
                <el-input clearable
                          placeholder="请输入基值"
                          v-model="formData.base">
                </el-input>
            </el-form-item>
            <el-form-item class="thing-dialog-form-item" label="倍数" prop="multiple">
                <el-input clearable
                          placeholder="请输入倍数"
                          v-model="formData.multiple">
                </el-input>
            </el-form-item>
            <el-form-item class="thing-dialog-form-item" label="最小值" prop="minimum">
                <el-input clearable
                          placeholder="请输入最小值"
                          v-model="formData.minimum">
                </el-input>
            </el-form-item>
            <el-form-item class="thing-dialog-form-item" label="最大值" prop="maximum">
                <el-input clearable
                          placeholder="请输入最大值"
                          v-model="formData.maximum">
                </el-input>
            </el-form-item>
            <el-form-item class="thing-dialog-form-item" label="描述" prop="description">
                <el-input clearable
                          type="textarea"
                          maxlength="300"
                          show-word-limit
                          placeholder="请输入位号描述"
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
        name: "point-add-form",
        props: {
            profileId: {
                type: String,
                default: () => {
                    return '';
                }
            }
        },
        data() {
            return {
                formData: {
                    profileId: this.profileId,
                    type: 'string',
                    rw: 0,
                    accrue: false,
                    base: 0,
                    multiple: 1,
                    format: '%.3f',
                    unit: '"',
                    minimum: -999999,
                    maximum: 999999
                },
                formRule: {
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
                    type: [
                        {
                            required: true,
                            message: '请选择位号数据类型',
                            trigger: 'change'
                        }
                    ],
                    rw: [
                        {
                            required: true,
                            message: '请选择位号读写类型',
                            trigger: 'change'
                        }
                    ],
                    accrue: [
                        {
                            required: true,
                            message: '请选择位号是否为累计数据',
                            trigger: 'change'
                        }
                    ],
                    base: [
                        {
                            pattern: /^-?(([0-9]*(\.[0-9]{1,3})$)|([0-9]+$))/,
                            message: '请输入 正确格式的基值'
                        }
                    ],
                    multiple: [
                        {
                            pattern: /^-?(([0-9]*(\.[0-9]{1,3})$)|([0-9]+$))/,
                            message: '请输入 正确格式的倍数'
                        }
                    ],
                    format: [
                        {
                            required: true,
                            message: '请输入 数据格式',
                            trigger: 'blur'
                        }
                    ],
                    minimum: [
                        {
                            pattern: /^-?(([0-9]*(\.[0-9]{1,3})$)|([0-9]+$))/,
                            message: '请输入 正确格式的最小值'
                        }
                    ],
                    maximum: [
                        {
                            pattern: /^-?(([0-9]*(\.[0-9]{1,3})$)|([0-9]+$))/,
                            message: '请输入 正确格式的最大值'
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
    @import "~@/components/dialog/styles/thing-dialog.scss";
</style>
