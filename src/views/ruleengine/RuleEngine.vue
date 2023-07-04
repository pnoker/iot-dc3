<template>
    <div>
        <ruleengine-tool
            :embedded="embedded"
        />
        <blank-card  :embedded="embedded != ''">
            <el-row>
                <el-col v-for="data in 12" :key="data" :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
                    <skeleton-card :loading="reactiveData.loading" />
                </el-col>
                <el-col v-if="hasData">
                    <el-empty description="暂无规则数据！" />
                </el-col>
                <el-col v-for="data in reactiveData.listData" :key="data.id" :xs="24" :sm="12" :md="8" :lg="6" :xl="4">
                    <ruleengine-card
                        :embedded="embedded != ''"
                        :data="data"
                    />
                </el-col>
            </el-row>
        </blank-card>
    </div>
</template>

<script src="./index.ts" lang="ts" />

<style lang="scss"></style>