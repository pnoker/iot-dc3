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
  <div>
    <blank-card>
      <el-tabs v-model="reactiveData.active">
        <el-tab-pane :label="$t('settings.role.detailTitle')" name="detail">
          <detail-card>
            <el-descriptions :column="2" border>
              <el-descriptions-item :label="$t('settings.role.roleName')">
                {{ reactiveData.data.roleName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.role.roleCode')">
                {{ reactiveData.data.roleCode || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('settings.role.parentRoleId')">
                {{ reactiveData.data.parentRoleId || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.enable')">
                <el-tag
                  :type="
                    String(reactiveData.data.enableFlag) === 'ENABLE' || Number(reactiveData.data.enableFlag) === 0
                      ? 'success'
                      : 'info'
                  "
                >
                  {{
                    String(reactiveData.data.enableFlag) === 'ENABLE' || Number(reactiveData.data.enableFlag) === 0
                      ? $t('common.enable')
                      : $t('common.disable')
                  }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.remark')" :span="2">
                {{ reactiveData.data.remark || '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.createTime')">
                {{ reactiveData.data.createTime ? timestamp(reactiveData.data.createTime) : '-' }}
              </el-descriptions-item>
              <el-descriptions-item :label="$t('common.operationTime')">
                {{ reactiveData.data.operateTime ? timestamp(reactiveData.data.operateTime) : '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </detail-card>
        </el-tab-pane>
      </el-tabs>
    </blank-card>
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, reactive } from 'vue';
  import { useRoute } from 'vue-router';

  import { getRoleById } from '@/api/role';
  import { timestamp } from '@/utils/DateUtil';

  import blankCard from '@/components/card/blank/BlankCard.vue';
  import detailCard from '@/components/card/detail/DetailCard.vue';

  const route = useRoute();

  const reactiveData = reactive({
    id: route.query.id as string,
    active: (route.query.active as string) || 'detail',
    data: {} as Record<string, any>,
  });

  const load = () => {
    if (!reactiveData.id) return;
    getRoleById(reactiveData.id)
      .then((res: any) => {
        reactiveData.data = res.data || {};
      })
      .catch(() => {
        // handled globally
      });
  };

  onMounted(() => {
    load();
  });
</script>

<style lang="scss" scoped>
  @use '@/styles/things-card.scss';
</style>
