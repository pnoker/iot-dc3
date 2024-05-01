/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { UploadFilled } from '@element-plus/icons-vue'
import type { UploadInstance, UploadProps, UploadRawFile } from 'element-plus'
import { FormInstance, FormRules, genFileId } from 'element-plus'
import { defineComponent, reactive, ref, unref } from 'vue'

import { Dictionary, Order } from '@/config/entity'

import { getDriverDictionary, getProfileDictionary } from '@/api/dictionary'
import { successMessage } from '@/utils/NotificationUtil'

export default defineComponent({
    name: 'DeviceImportForm',
    components: { UploadFilled },
    emits: ['import-template', 'import-thing'],
    setup(props, { emit }) {
        // 定义表单引用
        const formDataRef = ref<FormInstance>()

        // 定义文件上传引用
        const formUploadRef = ref<UploadInstance>()

        // 定义响应式数据
        const reactiveData = reactive({
            formData: {} as any,
            formVisible: false,
            formLoading: false,
            driverQuery: '',
            driverDictionary: [] as Dictionary[],
            driverPage: {
                total: 0,
                size: 5,
                current: 1,
                orders: [] as Order[],
            },
            profileQuery: '',
            profileDictionary: [] as Dictionary[],
            profilePage: {
                total: 0,
                size: 5,
                current: 1,
                orders: [] as Order[],
            },
        })

        // 定义表单校验规则
        const formRule = reactive<FormRules>({
            driverId: [
                {
                    required: true,
                    message: '请选择所属驱动',
                    trigger: 'change',
                },
            ],
            profileIds: [
                {
                    required: true,
                    message: '请选择关联模板',
                    trigger: 'change',
                },
            ],
        })

        // 图标
        const Icon = {
            UploadFilled,
        }

        const driverDictionary = () => {
            getDriverDictionary({
                page: reactiveData.driverPage,
                label: reactiveData.driverQuery,
            })
                .then((res) => {
                    const data = res.data
                    reactiveData.driverPage.total = data.total
                    reactiveData.driverDictionary = data.records
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const driverCurrentChange = (current: number) => {
            reactiveData.driverPage.current = current
            driverDictionary()
        }

        const driverDictionaryVisible = (visible: boolean) => {
            if (visible) {
                reactiveData.driverQuery = ''
                driverDictionary()
            }
        }

        const profileDictionary = () => {
            getProfileDictionary({
                page: reactiveData.profilePage,
                label: reactiveData.profileQuery,
            })
                .then((res) => {
                    const data = res.data
                    reactiveData.profilePage.total = data.total
                    reactiveData.profileDictionary = data.records
                })
                .catch(() => {
                    // nothing to do
                })
        }

        const profileCurrentChange = (current: number) => {
            reactiveData.driverPage.current = current
            profileDictionary()
        }

        const profileDictionaryVisible = (visible: boolean) => {
            if (visible) {
                reactiveData.profileQuery = ''
                profileDictionary()
            }
        }

        const show = () => {
            reactiveData.formVisible = true
            reactiveData.formLoading = false
        }
        const cancel = () => {
            reactiveData.formVisible = false
            reactiveData.formLoading = false
        }
        const reset = () => {
            const form = unref(formDataRef)
            form?.resetFields()

            formUploadRef.value!.clearFiles()
        }
        const importTemplate = () => {
            const form = unref(formDataRef)
            form?.validate((valid) => {
                if (valid) {
                    emit('import-template', reactiveData.formData, () => {
                        successMessage('模板生成成功，正在导出！')
                    })
                }
            })
        }
        const uploadRequest = (param) => {
            const formData = reactiveData.formData
            formData['file'] = param.file

            emit('import-thing', formData, () => {
                cancel()
                reset()
                successMessage('设备导入成功！')
            })
        }
        const importThing = () => {
            const form = unref(formDataRef)
            form?.validate((valid) => {
                if (valid) {
                    formUploadRef.value!.submit()
                    reactiveData.formLoading = true
                }
            })
        }

        const handleExceed: UploadProps['onExceed'] = (files) => {
            formUploadRef.value!.clearFiles()
            const file = files[0] as UploadRawFile
            file.uid = genFileId()
            formUploadRef.value!.handleStart(file)
        }

        return {
            formDataRef,
            formUploadRef,
            formRule,
            reactiveData,
            driverDictionary,
            driverCurrentChange,
            driverDictionaryVisible,
            profileDictionary,
            profileCurrentChange,
            profileDictionaryVisible,
            show,
            cancel,
            reset,
            importTemplate,
            uploadRequest,
            importThing,
            handleExceed,
            ...Icon,
        }
    },
})
