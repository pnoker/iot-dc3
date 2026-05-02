<!--
  - Copyright 2016-present the IoT DC3 original author or authors.
  -
  - Licensed under the Apache License, Version 2.0 (the "License");
  - you may not use this file except in compliance with the License.
  - You may obtain a copy of the License at
  -
  -      https://www.apache.org/licenses/LICENSE-2.0
  -
  - Unless required by applicable law or agreed to in writing, software
  - distributed under the License is distributed on an "AS IS" BASIS,
  - WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  - See the License for the specific language governing permissions and
  - limitations under the License.
  -->

<template>
  <div class="home">
    <!-- Row 0: greeting + clock + service status -->
    <el-row :gutter="12" class="home__row">
      <el-col :span="24" class="home__col">
        <home-banner :status="serviceStatus" />
      </el-col>
    </el-row>

    <!-- Row 1: indicator strip (5 cards) -->
    <el-row :gutter="12" class="home__row">
      <el-col v-for="c in cards" :key="c.key" :xl="5" :lg="8" :md="12" :sm="12" :xs="24" class="home__col">
        <stat-card
          :title="c.title"
          :value="c.value"
          :subtitle="c.subtitle"
          :icon="c.icon"
          :tone="c.tone"
          :trend="c.trend"
          :sparkline="c.sparkline"
          @click="c.onClick"
        />
      </el-col>
    </el-row>

    <!-- Row 2: 6-tab analytics (distribution + top-N) + live data feed -->
    <el-row :gutter="12" class="home__row">
      <el-col :xl="16" :lg="16" :md="24" :sm="24" :xs="24" class="home__col">
        <analytics-tabs />
      </el-col>
      <el-col :xl="8" :lg="8" :md="24" :sm="24" :xs="24" class="home__col">
        <live-data-feed :size="20" />
      </el-col>
    </el-row>

    <!-- Row 3: quick actions + recent alerts -->
    <el-row :gutter="12" class="home__row">
      <el-col :xl="8" :lg="10" :md="24" :sm="24" :xs="24" class="home__col">
        <quick-actions />
      </el-col>
      <el-col :xl="16" :lg="14" :md="24" :sm="24" :xs="24" class="home__col">
        <alert-list :size="10" />
      </el-col>
    </el-row>

    <!-- Row 4: data ingestion trend chart -->
    <el-row :gutter="12" class="home__row">
      <el-col :span="24" class="home__col">
        <trend-chart />
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" src="./index.ts"></script>

<style lang="scss" scoped>
  .home {
    padding: 0 4px;

    .home__row {
      margin-bottom: 12px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .home__col {
      margin-bottom: 12px;
    }
  }
</style>
