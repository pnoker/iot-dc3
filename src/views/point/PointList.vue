<template>
    <div>
        <el-row>
            <point-tool
                    :pre="pre"
                    :next="next"
                    @pre-handle="preHandle"
                    @next-handle="nextHandle"
                    :page="page"
                    @search="search"
                    @reset="reset"
                    @showAdd="showAdd"
                    @refresh="refresh"
                    @sort="sort"
                    @size-change="sizeChange"
                    @current-change="currentChange"
            ></point-tool>
        </el-row>

        <el-row>
            <el-col :xs="24" :sm="12" :md="12" :lg="8" :xl="6" :key="data.id" v-for="data in listData">
                <point-card
                        :data="data"
                        :profileTable="profileTable"
                        @delete-thing="deleteThing"
                ></point-card>
            </el-col>
        </el-row>

        <point-add-form
                ref="add-form"
                :profileId="profileId"
                @add-thing="addThing"
        ></point-add-form>
    </div>
</template>
<script>
    import pointTool from './PointTool'
    import pointAddForm from './PointAddForm'
    import pointCard from './PointCard'
    import {pointAdd, pointDelete, pointList} from "@/api/point";
    import {profileDictionary} from "@/api/dictionary";

    export default {
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
            profileId: {
                type: String,
                default: () => {
                    return '';
                }
            }
        },
        components: {pointTool, pointAddForm, pointCard},
        data() {
            return {
                profileDictionary: [],
                profileTable: {},
                listData: [],
                query: {},
                order: false,
                page: {
                    total: 0,
                    size: 12,
                    current: 1
                }
            }
        },
        created() {
            this.profile();
            this.list();
        },
        beforeRouteLeave(to, from, next) {
            if (this.interval) {
                clearInterval(this.interval);
            }
            next();
        },
        methods: {
            list() {
                pointList({
                    page: this.page,
                    ...this.query,
                    profileId: this.profileId
                }).then(res => {
                    const data = res.data;
                    this.page.total = data.total;
                    this.listData = data.records;
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
            search(params) {
                this.query = {
                    ...params,
                    profileId: this.profileId
                };
                this.list();
            },
            reset() {
                this.query = {};
                this.list();
            },
            showAdd() {
                this.$refs['add-form'].show();
            },
            addThing(form, done) {
                pointAdd(form).then(() => {
                    this.list();
                    done();
                }).catch(() => {
                });
            },
            deleteThing(id, done) {
                pointDelete(id).then(() => {
                    this.list();
                    done();
                }).catch(() => {
                });
            },
            refresh() {
                this.list();
            },
            sort() {
                this.order = !this.order;
                if (this.order) {
                    this.page.orders = [{column: 'create_time', asc: true}];
                } else {
                    this.page.orders = [{column: 'create_time', asc: false}];
                }
                this.list();
            },
            sizeChange(size) {
                this.page.size = size;
                this.list();
            },
            currentChange(current) {
                this.page.current = current;
                this.list();
            },
            preHandle() {
                this.$emit('pre-handle');
            },
            nextHandle() {
                this.$emit('next-handle');
            }
        }
    }

</script>
