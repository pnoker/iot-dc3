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

import {httpGet} from '@/api/common';
import {API_MANAGER_BASE} from '@/config/constant/api';
import type {TopologyMode} from '@/config/types/dashboard';

export const topology = (params: {mode?: TopologyMode; rangeKey?: string} = {}) =>
  httpGet(`${API_MANAGER_BASE}/dashboard/topology`, {
    params: {
      ...(params.mode ? {mode: params.mode} : {}),
      ...(params.rangeKey ? {range_key: params.rangeKey} : {}),
    },
  });
