<template>
    <div class="things-card cursor-pointer" @click="select(data)">
        <el-card :shadow="data.shadow">
            <div class="things-card-content">
                <div class="things-card__header" v-bind:class="{'header-enable':isConfig,'header-disable':!isConfig}">
                    <div class="things-card-header-icon"><img :src="isSelect" :alt="data.name"></div>
                    <div class="things-card-header-name nowrap-name" @click="copyId(data.id)">{{data.name}}</div>
                </div>
                <div class="things-card__body">
                    <div class="things-card-body-content">
                        <ul>
                            <li class="nowrap-item" :key="attribute.id" v-if="attribute" v-for="attribute in attributes">
                                <span><i class="el-icon-goblet"/> {{attribute.displayName}}: </span>{{data[attribute.name].value}}
                            </li>
                        </ul>
                    </div>
                </div>
            </div>
        </el-card>
    </div>
</template>

<script>

    export default {
        name: "point-info-card",
        props: {
            data: {
                type: Object,
                default: () => {
                    return {
                        name: '',
                        description: '',
                        createTime: '',
                        updateTime: ''
                    };
                }
            },
            attributes: {
                type: Array,
                default: []
            },
            icon: {
                type: String,
                default: 'images/common/point-info-disable.png'
            }
        },
        data() {
            return {}
        },
        computed: {
            isConfig() {
                for (let attribute of this.attributes) {
                    if (this.data[attribute.name].value === '') {
                        return false;
                    }
                }
                return true;
            },
            isSelect() {
                if (this.data.shadow === 'always') {
                    return 'images/common/point-info.png';
                } else {
                    return 'images/common/point-info-disable.png';
                }
            }
        },
        methods: {
            select(data) {
                this.$emit('select-change', data);
            }
        }
    };
</script>

<style lang="scss">
    @import "~@/components/card/styles/things-card.scss";
</style>
