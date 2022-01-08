<template>
    <el-skeleton :loading="loading" animated>
        <template slot="template">
            <div class="things-card">
                <el-card shadow="hover">
                    <div class="things-card-content">
                        <div v-if="!header" class="things-card__header">
                            <el-skeleton-item class="things-card-header-icon skeleton" variant="image"/>
                            <el-skeleton-item class="things-card-header-name skeleton" variant="text"/>
                        </div>
                        <div class="things-card__body">
                            <div class="things-card-body-content" v-bind:class="{'left':header,'center':!header}">
                                <ul>
                                    <el-skeleton-item class="nowrap-item skeleton" variant="text"/>
                                    <el-skeleton-item class="nowrap-item skeleton" variant="text"/>
                                    <el-skeleton-item class="nowrap-item skeleton" variant="text"/>
                                    <el-skeleton-item class="nowrap-item skeleton" variant="text"/>
                                    <div v-if="header">
                                        <el-skeleton-item class="nowrap-item skeleton" variant="text"/>
                                        <el-skeleton-item class="nowrap-item skeleton" variant="text"/>
                                        <el-skeleton-item class="nowrap-item skeleton" variant="text"/>
                                        <el-skeleton-item class="nowrap-item skeleton" variant="text"/>
                                    </div>
                                </ul>
                            </div>
                            <div v-if="!header" class="things-card-body-content">
                                <el-skeleton-item class="nowrap-description skeleton" variant="text"/>
                            </div>
                        </div>
                        <div v-if="!footer" class="things-card__footer">
                            <div class="things-card-footer-operation">
                                <el-skeleton-item class="operation-tooltip skeleton" variant="button"/>
                                <el-skeleton-item class="operation-tooltip skeleton" variant="button"/>
                                <el-skeleton-item class="operation-tooltip skeleton" variant="button"/>
                                <el-skeleton-item class="operation-tooltip skeleton" variant="button"/>
                                <el-skeleton-item class="operation-tooltip skeleton" variant="button"/>
                            </div>
                        </div>
                    </div>
                </el-card>
            </div>
        </template>
    </el-skeleton>
</template>

<script>
    export default {
        name: "skeleton-card",
        props: {
            loading: {
                type: Boolean,
                default: () => {
                    return false;
                }
            },
            header: {
                type: Boolean,
                default: () => {
                    return false;
                }
            },
            footer: {
                type: Boolean,
                default: () => {
                    return false;
                }
            }
        }
    };
</script>

<style lang="scss">
    @import "~@/components/card/styles/things-card.scss";

    .things-card-header-icon.skeleton {
        width: 55px !important;
    }

    .things-card-header-name.skeleton {
        margin-top: 10px;
        height: 28px !important;
    }

    .nowrap-item.skeleton {
        margin-bottom: 12px;
    }

    .nowrap-description.skeleton {
        margin-top: 10px;
        margin-bottom: 17px;
    }

    .things-card-body-content.center {
        justify-content: center !important;
    }

    .things-card-body-content.left {
        margin-left: 30px;
        justify-content: left !important;
    }

    .operation-tooltip.skeleton {
        width: 28px;
        height: 20px;
        margin-top: 15px;
    }
</style>
