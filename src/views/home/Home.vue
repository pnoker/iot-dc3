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
    <el-row :gutter="8" class="home__row">
      <el-col :span="24" class="home__col">
        <home-banner />
      </el-col>
    </el-row>

    <!-- Row 1: 6 indicator cards, always 6-wide via CSS grid -->
    <div class="home__stats">
      <stat-card
        v-for="c in cards"
        :key="c.key"
        :icon="c.icon"
        :on-refresh="c.onRefresh"
        :sparkline="c.sparkline"
        :subtitle="c.subtitle"
        :title="c.title"
        :tone="c.tone"
        :trend="c.trend"
        :value="c.value"
        @click="c.onClick"
      />
    </div>

    <!-- Row 2: analytics split in half — structural breakdowns left,
         top-N activity right, mirroring the two intent groups operators
         use to explore the fleet. -->
    <el-row :gutter="8" class="home__row">
      <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="home__col">
        <analytics-tabs group="structural" />
      </el-col>
      <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="home__col">
        <analytics-tabs group="top" />
      </el-col>
    </el-row>

    <!-- Row 3: live streaming data + recent alarms side-by-side — the two
         "what's happening right now" widgets pair naturally. -->
    <el-row :gutter="8" class="home__row">
      <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="home__col">
        <live-data-feed :size="20" />
      </el-col>
      <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="home__col">
        <alert-list :size="10" />
      </el-col>
    </el-row>

    <!-- Row 4: trend chart on its own row — time series benefits from
         full width so the x-axis has room to breathe. -->
    <el-row :gutter="8" class="home__row">
      <el-col :span="24" class="home__col">
        <trend-chart />
      </el-col>
    </el-row>

    <!-- Row 5: latency histogram + hourly activity heatmap -->
    <el-row :gutter="8" class="home__row">
      <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="home__col">
        <latency-chart />
      </el-col>
      <el-col :lg="12" :md="24" :sm="24" :xl="12" :xs="24" class="home__col">
        <activity-heatmap />
      </el-col>
    </el-row>
  </div>
</template>

<script lang="ts" src="./index.ts"></script>

<style lang="scss" scoped>
  .home {
    padding: 0 4px;

    .home__row {
      margin-bottom: 8px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    // el-row already carries the 8px vertical rhythm between rows. On wide
    // screens the cols sit side-by-side so they don't need a bottom margin
    // of their own — adding one stacked with .home__row margin-bottom,
    // blowing the gap out to 16px. Only the narrow-screen breakpoint where
    // cols collapse into a single column actually needs the extra spacer.
    .home__col {
      margin-bottom: 0;

      @media (max-width: 1024px) {
        margin-bottom: 8px;

        &:last-child {
          margin-bottom: 0;
        }
      }
    }

    // Stat indicators: always fit the strip on one line, regardless of how
    // many cards the cards computed property ends up with. Below 1280px
    // (tablet / mobile) fall back to 2 cols so cards don't squeeze below
    // their minimum usable width.
    .home__stats {
      display: grid;
      grid-template-columns: repeat(6, minmax(0, 1fr));
      gap: 8px;
      margin-bottom: 8px;

      @media (max-width: 1280px) {
        grid-template-columns: repeat(3, minmax(0, 1fr));
      }

      @media (max-width: 640px) {
        grid-template-columns: 1fr;
      }
    }
  }
</style>
