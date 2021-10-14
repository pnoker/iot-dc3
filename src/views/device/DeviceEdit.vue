<template>
    <div class="edit-card">
        <el-card shadow="hover">
            <div class="edit-content">
                <el-steps :active="active" align-center>
                    <el-step title="设备信息编辑"></el-step>
                    <el-step title="设备驱动编辑"></el-step>
                    <el-step title="设备位号编辑"></el-step>
                </el-steps>
            </div>
        </el-card>

        <el-card shadow="hover" v-if="active===0">
            <div class="edit-content">
                <el-divider content-position="left">设备信息编辑</el-divider>
                <div class="content-edit-device">
                    <el-form :inline="true" :model="deviceFormData" :rules="deviceFormRule" ref="deviceFormData" size="small">
                        <div class="edit-device-form">
                            <el-form-item label="设备名称" prop="name">
                                <el-input clearable
                                          placeholder="请输入设备名称"
                                          @keyup.enter.native="deviceUpdate"
                                          v-model="deviceFormData.name">
                                </el-input>
                            </el-form-item>
                            <el-form-item label="所属驱动" prop="driverId">
                                <el-select clearable
                                           placeholder="请选择所属驱动"
                                           filterable
                                           v-model="deviceFormData.driverId">
                                    <el-option
                                            :label="dictionary.label"
                                            :value="dictionary.value"
                                            :key="dictionary.value"
                                            v-for="dictionary in driverDictionary"
                                    ></el-option>
                                </el-select>
                            </el-form-item>
                            <el-form-item label="存储类型" prop="multi">
                                <el-select clearable
                                           class="edit-form-medium"
                                           placeholder="请选择存储类型"
                                           v-model="deviceFormData.multi">
                                    <el-option label="单点数据" :value="false"></el-option>
                                    <el-option label="结构数据" :value="true"></el-option>
                                </el-select>
                            </el-form-item>
                            <el-form-item label="使能" prop="enable">
                                <el-select clearable
                                           class="edit-form-small"
                                           placeholder="请选择使能"
                                           v-model="deviceFormData.enable">
                                    <el-option label="启用" :value="true"></el-option>
                                    <el-option label="停用" :value="false"></el-option>
                                </el-select>
                            </el-form-item>
                        </div>
                        <div class="edit-device-form">
                            <el-form-item label="包含模板" prop="profileIds">
                                <el-select clearable
                                           class="edit-form-large"
                                           placeholder="请选择包含模板"
                                           :multiple="true"
                                           filterable
                                           v-model="deviceFormData.profileIds">
                                    <el-option
                                            :label="dictionary.label"
                                            :value="dictionary.value"
                                            :key="dictionary.value"
                                            v-for="dictionary in profileDictionary"
                                    ></el-option>
                                </el-select>
                            </el-form-item>
                        </div>
                        <div class="edit-device-form">
                            <el-form-item label="设备描述" prop="description">
                                <el-input clearable
                                          class="edit-form-large"
                                          type="textarea"
                                          maxlength="300"
                                          show-word-limit
                                          placeholder="请输入设备描述"
                                          v-model="deviceFormData.description">
                                </el-input>
                            </el-form-item>
                        </div>
                        <el-form-item class="edit-button">
                            <el-button type="primary" icon="el-icon-edit" @click="deviceUpdate">修改</el-button>
                            <el-button icon="el-icon-refresh-left" @click="deviceReset">恢复</el-button>
                            <el-button type="warning" icon="el-icon-right" @click="next" plain>下一步</el-button>
                        </el-form-item>
                    </el-form>
                </div>
            </div>
        </el-card>
        <el-card shadow="hover" v-if="active===1&&driverAttributes&&driverAttributes.length>0">
            <div class="edit-content">
                <el-divider content-position="left">设备驱动配置</el-divider>
                <el-alert :closable="false"
                          title="设备驱动配置说明"
                          type="success"
                          description="设备驱动配置用于配置连接到该设备所需的基本参数信息。">
                </el-alert>
                <div class="content-edit-driver">
                    <el-form ref="driverFormData" size="small"
                             :inline="true"
                             :model="driverFormData"
                             :v-if="driverFormData.length>0">
                        <el-row>
                            <el-form-item :key="attribute.id"
                                          :label="attribute.displayName"
                                          :prop="attribute.name"
                                          v-for="attribute in driverAttributes">
                                <el-input clearable
                                          v-if="driverFormData[attribute.name]"
                                          :key="driverFormData[attribute.name].id"
                                          :placeholder="'请输入'+attribute.displayName"
                                          @keyup.enter.native="driverUpdate"
                                          v-model="driverFormData[attribute.name].value">
                                </el-input>
                            </el-form-item>
                        </el-row>
                        <el-form-item class="edit-button">
                            <el-button type="success" icon="el-icon-back" @click="pre" plain>上一步</el-button>
                            <el-button type="primary" icon="el-icon-edit" @click="driverUpdate">修改</el-button>
                            <el-button icon="el-icon-refresh-left" @click="driverReset">恢复</el-button>
                            <el-button type="warning" icon="el-icon-right" @click="next" plain>下一步</el-button>
                        </el-form-item>
                    </el-form>
                </div>
            </div>
        </el-card>
        <el-card shadow="hover" v-if="active===2">
            <div class="edit-content">
                <el-divider content-position="left">设备位号配置</el-divider>
                <el-alert :closable="false"
                          title="设备位号配置说明"
                          type="success"
                          description="设备位号配置用于配置采集设备该位号的数据所需的基本参数信息。">
                </el-alert>
                <div class="content-edit-driver">
                    <el-form ref="pointFormData" size="small"
                             :inline="true"
                             :model="pointFormData"
                             :v-if="pointFormData.length>0">
                        <el-row>
                            <el-form-item label="位号名称" prop="name">
                                <el-input clearable v-model="pointFormData.name" disabled></el-input>
                            </el-form-item>
                            <el-form-item :key="attribute.id"
                                          :label="attribute.displayName"
                                          :prop="attribute.name"
                                          v-for="attribute in pointAttributes">
                                <el-input clearable
                                          v-if="pointFormData[attribute.name]"
                                          :key="pointFormData[attribute.name].id"
                                          :placeholder="'请输入'+attribute.displayName"
                                          @keyup.enter.native="pointUpdate"
                                          v-model="pointFormData[attribute.name].value">
                                </el-input>
                            </el-form-item>
                        </el-row>
                        <el-form-item class="edit-button">
                            <el-button type="success" icon="el-icon-back" @click="pre" plain>上一步</el-button>
                            <el-button type="primary" icon="el-icon-edit" @click="pointUpdate" :disabled="!Object.keys(pointFormData).length>0">修改</el-button>
                            <el-button icon="el-icon-refresh-left" @click="pointReset" :disabled="!Object.keys(pointFormData).length>0">恢复</el-button>
                            <el-button type="warning" icon="el-icon-right" @click="next" plain>完成</el-button>
                        </el-form-item>
                    </el-form>
                </div>
                <div class="content-edit-point">
                    <el-row>
                        <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-if="data" v-for="data in pointInfoData">
                            <point-info-card
                                    :data="data"
                                    :attributes="pointAttributes"
                                    @select-change="changePointInfo"
                            ></point-info-card>
                        </el-col>
                    </el-row>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>
    import {driverDictionary, profileDictionary} from "@/api/dictionary";
    import {deviceById, deviceUpdate} from "@/api/device";
    import {driverAttributeByDriverId, pointAttributeByDriverId} from "@/api/attribute";
    import {driverInfoAdd, driverInfoByDeviceId, driverInfoUpdate, pointInfoAdd, pointInfoByDeviceId, pointInfoUpdate} from "@/api/info";
    import {pointByDeviceId} from "@/api/point";
    import pointInfoCard from "../point/info/PointInfoCard"

    export default {
        components: {pointInfoCard},
        data() {
            return {
                id: this.$route.query.id,
                driverId: this.$route.query.driverId,
                active: +this.$route.query.active,
                driverDictionary: [],
                profileDictionary: [],
                driverTable: {},
                profileTable: {},
                pointTable: {},
                oldDeviceFormData: {},
                deviceFormData: {
                    profileIds: []
                },
                deviceFormRule: {
                    name: [
                        {
                            required: true,
                            message: '请输入设备名称',
                            trigger: 'blur'
                        }, {
                            min: 2,
                            max: 32,
                            message: '请输入 2~32 位字长的设备名称',
                            trigger: 'blur'
                        }, {
                            pattern: /^[A-Za-z0-9\u4e00-\u9fa5][A-Za-z0-9\u4e00-\u9fa5-_]*$/,
                            message: '请输入正确格式的设备名称'
                        }
                    ],
                    driverId: [
                        {
                            required: true,
                            message: '请选择所属驱动',
                            trigger: 'change'
                        }
                    ],
                    profileIds: [
                        {
                            required: true,
                            message: '请选择包含模板',
                            trigger: 'change'
                        }
                    ],
                    multi: [
                        {
                            required: true,
                            message: '请选择存储类型',
                            trigger: 'change'
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
                },
                driverAttributes: [],
                driverAttributeTable: {},
                oldDriverFormData: {},
                driverFormData: {},
                pointAttributes: [],
                pointAttributeTable: {},
                oldPointFormData: {},
                pointFormData: {},
                pointInfoData: []
            }
        },
        created() {
            this.driver();
            this.profile();
            this.device();
            this.point();
        },
        methods: {
            device() {
                let id = this.$route.query.id;
                deviceById(id).then(res => {
                    this.deviceFormData = res.data;
                    this.changeDriver(this.driverId);
                    this.changePoint();
                    this.oldDeviceFormData = {...res.data};
                });
            },
            driver() {
                driverDictionary().then(res => {
                    this.driverDictionary = res.data;
                    this.driverTable = this.driverDictionary.reduce((pre, cur) => {
                        pre[cur.value] = cur.label;
                        return pre;
                    }, {});
                }).catch(() => {
                });
            },
            profile() {
                profileDictionary().then(res => {
                    this.profileDictionary = res.data;
                    this.profileTable = this.profileDictionary.reduce((pre, cur) => {
                        pre[cur.value] = cur.label;
                        return pre;
                    }, {});
                }).catch(() => {
                });
            },
            point() {
                pointByDeviceId(this.id).then(res1 => {
                    this.pointTable = res1.data.reduce((pre, cur) => {
                        pre[cur.id] = cur.name;
                        return pre;
                    }, {});

                    pointAttributeByDriverId(this.driverId).then(res2 => {
                        this.pointAttributes = res2.data;
                        this.pointAttributeTable = this.pointAttributes.reduce((pre, cur) => {
                            pre[cur.id] = cur.name;
                            return pre;
                        }, {});

                        this.pointInfoData = res1.data.map(point => {
                            let pointInfo = {
                                id: point.id,
                                name: point.name,
                                shadow: 'hover'
                            };

                            res2.data.forEach(attribute => {
                                pointInfo[attribute.name] = {
                                    id: null,
                                    value: ''
                                }
                            });
                            return pointInfo;
                        });
                    });

                }).catch(() => {
                });
            },
            profileName(profileId) {
                return this.profileTable[profileId];
            },
            changeDriver(driverId) {
                driverAttributeByDriverId(driverId).then(res => {
                    this.driverAttributes = res.data;
                    this.driverAttributeTable = res.data.reduce((pre, cur) => {
                        pre[cur.id] = cur.name;
                        return pre;
                    }, {});

                    let formData = {};
                    res.data.forEach(attribute => {
                        formData[attribute.name] = {
                            id: null,
                            value: ''
                        };
                    });
                    this.driverFormData = JSON.parse(JSON.stringify(formData));
                    this.oldDriverFormData = JSON.parse(JSON.stringify(formData));

                    driverInfoByDeviceId(this.id).then(res => {
                        let formData = {};
                        res.data.forEach(info => {
                            formData[this.driverAttributeTable[info.driverAttributeId]] = {
                                id: info.id,
                                value: info.value
                            };
                        });

                        this.driverFormData = JSON.parse(JSON.stringify(formData));
                        this.oldDriverFormData = JSON.parse(JSON.stringify(formData));
                    }).catch(() => {
                    });
                });
            },
            changePoint() {
                pointAttributeByDriverId(this.driverId).then(res => {
                    this.pointAttributes = res.data;
                    this.pointAttributeTable = this.pointAttributes.reduce((pre, cur) => {
                        pre[cur.id] = cur.name;
                        return pre;
                    }, {});

                    pointInfoByDeviceId(this.id).then(res => {
                        res.data.forEach(info => {
                            this.pointInfoData = this.pointInfoData.map(pointInfo => {
                                if (pointInfo.id === info.pointId) {
                                    pointInfo[this.pointAttributeTable[info.pointAttributeId]] = {
                                        id: info.id,
                                        value: info.value
                                    }
                                }
                                return pointInfo;
                            });
                        });
                    });
                });
            },
            changePointInfo(row) {
                this.pointAttributes.forEach(attribute => {
                    if (!row[attribute.name]) {
                        row[attribute.name] = {
                            id: null,
                            value: ''
                        }
                    }
                });
                this.pointFormData = JSON.parse(JSON.stringify(row));

                this.pointInfoData.forEach(pointInfo => {
                    pointInfo.shadow = 'hover';
                    if (row.id === pointInfo.id) {
                        pointInfo.shadow = 'always';
                    }
                });
            },
            deviceUpdate() {
                this.$refs['deviceFormData'].validate((valid) => {
                    if (valid) {
                        deviceUpdate(this.deviceFormData).then(res => {
                            this.changeDriver(this.driverId);
                            this.changePoint(this.id);
                            this.oldDeviceFormData = {...res.data};
                        });
                    }
                });
            },
            driverUpdate() {
                this.$refs['driverFormData'].validate((valid) => {
                    if (valid) {
                        let formData = {};
                        this.driverAttributes.forEach(attribute => {
                            let driverInfo = {
                                id: this.driverFormData[attribute.name].id,
                                driverAttributeId: attribute.id,
                                deviceId: this.id,
                                value: this.driverFormData[attribute.name].value
                            };

                            driverInfo.id ? driverInfoUpdate(driverInfo).then(res => loadFormData(formData, res, this)) : driverInfoAdd(driverInfo).then(res => loadFormData(formData, res, this));

                            function loadFormData(formData, res, that) {
                                formData[attribute.name] = {
                                    id: res.data.id,
                                    value: res.data.value
                                };
                                that.oldDriverFormData = JSON.parse(JSON.stringify(formData));
                            }
                        });
                    }
                });
            },
            pointUpdate() {
                this.$refs['pointFormData'].validate((valid) => {
                    if (valid) {
                        this.pointAttributes.forEach(attribute => {
                            let pointInfo = {
                                id: this.pointFormData[attribute.name].id,
                                pointAttributeId: attribute.id,
                                deviceId: this.id,
                                pointId: this.pointFormData.id,
                                value: this.pointFormData[attribute.name].value
                            };

                            pointInfo.id ? pointInfoUpdate(pointInfo).then(res => loadTableData(res, attribute, this)) : pointInfoAdd(pointInfo).then(res => loadTableData(res, attribute, this));

                            function loadTableData(res, attribute, that) {
                                that.pointInfoData = that.pointInfoData.map(tableData => {
                                    if (tableData.id === that.pointFormData.id) {
                                        tableData[attribute.name] = {
                                            id: res.data.id,
                                            value: res.data.value
                                        };
                                    }
                                    return tableData;
                                });
                            }
                        });
                    }
                });
            },
            pre() {
                this.active--;
                this.changeActive(this.active);
            },
            next() {
                let step = 1;
                if (this.active === 0 && this.driverAttributes.length < 1) {
                    step = 2;
                }
                this.active += step;
                if (this.active > 2) {
                    this.$router.push({name: 'device'});
                } else {
                    this.changeActive(this.active);
                }
            },
            deviceReset() {
                this.deviceFormData = {...this.oldDeviceFormData};
            },
            driverReset() {
                this.driverFormData = JSON.parse(JSON.stringify(this.oldDriverFormData));
            },
            pointReset() {
                this.$refs['pointFormData'].resetFields();
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
