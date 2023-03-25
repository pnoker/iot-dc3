/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.data.service;


import io.github.pnoker.center.data.entity.vo.query.DriverPageQuery;

import java.util.Map;

/**
 * Device Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface DriverStatusService {

    /**
     * 模糊分页查询 Driver 服务状态，同驱动模糊分页查询配套使用
     *
     * @param driverPageQuery 驱动和分页参数
     * @return Map String:String
     */
    Map<String, String> driver(DriverPageQuery driverPageQuery);
}
