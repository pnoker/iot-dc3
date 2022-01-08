<template>
    <div>
        <el-row>
            <profile-tool
                    :page="page"
                    @search="search"
                    @reset="reset"
                    @showAdd="showAdd"
                    @refresh="refresh"
                    @sort="sort"
                    @size-change="sizeChange"
                    @current-change="currentChange"
            ></profile-tool>
        </el-row>

        <blank-card>
            <el-row>
                <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in 12">
                    <skeleton-card :loading="loading"></skeleton-card>
                </el-col>
                <el-col :xs="24" :sm="12" :md="8" :lg="6" :xl="4" :key="data.id" v-for="data in listData">
                    <profile-card
                            :data="data"
                            :pointTable="pointTable"
                            @delete-thing="deleteThing"
                    ></profile-card>
                </el-col>
            </el-row>
        </blank-card>

        <profile-add-form
                ref="add-form"
                @add-thing="addThing"
        ></profile-add-form>
    </div>
</template>
<script>
    import blankCard from '@/components/card/blank-card';
    import skeletonCard from '@/components/card/skeleton-card';
    import profileTool from './ProfileTool'
    import profileAddForm from './ProfileAddForm'
    import profileCard from './ProfileCard'
    import {profileAdd, profileDelete, profileList} from "@/api/profile";
    import {pointDictionary} from "@/api/dictionary";

    export default {
        components: {blankCard, skeletonCard, profileTool, profileAddForm, profileCard},
        data() {
            return {
                loading: true,
                pointDictionary: [],
                pointTable: {},
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
            this.point();
            this.list();
        },
        methods: {
            list() {
                profileList({
                    page: this.page,
                    ...this.query
                }).then(res => {
                    const data = res.data;
                    this.page.total = data.total;
                    this.listData = data.records;
                }).catch(() => {
                }).finally(() => {
                    this.loading = false;
                });
            },
            point() {
                pointDictionary('point').then(res => {
                    this.pointDictionary = res.data;
                    this.pointTable = this.pointDictionary.reduce((pre, cur) => {
                        pre[cur.value] = cur.label;
                        return pre;
                    }, {});
                }).catch(() => {
                });
            },
            search(params) {
                this.query = params;
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
                profileAdd(form).then(() => {
                    this.list();
                    done();
                }).catch(() => {
                });
            },
            deleteThing(id, done) {
                profileDelete(id).then(() => {
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
            }
        }
    }

</script>

<style lang="scss">
</style>
