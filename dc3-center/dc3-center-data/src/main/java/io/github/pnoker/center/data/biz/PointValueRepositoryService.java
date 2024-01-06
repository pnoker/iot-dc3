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

package io.github.pnoker.center.data.biz;

import io.github.pnoker.center.data.entity.bo.PointValueBO;

import java.util.List;

/**
 * 用户自定义数据处理服务接口
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface PointValueRepositoryService {

    /**
     * 自定义数据处理，此处可以自定义逻辑，将数据存放到别的数据库，或者发送到别的地方
     *
     * @param pointValueBO PointValue
     */
    void save(PointValueBO pointValueBO);

    /**
     * 自定义数据处理，此处可以自定义逻辑，将数据存放到别的数据库，或者发送到别的地方
     *
     * @param pointValueBOS PointValue Array
     */
    void save(List<PointValueBO> pointValueBOS);

}
