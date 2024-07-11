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

package io.github.pnoker.common.manager.biz;

import java.time.LocalDateTime;

/**
 * 积分统计服务
 *
 * @Author fukq
 * create by 2024/3/5 14:40
 * @Version 1.0
 * @date 2024/03/05
 */
public interface PointStatisticsService {
    /**
     * 统计点历史
     */
    void statisticsPointHistory(LocalDateTime datetime);
}
