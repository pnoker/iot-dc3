/*
 * Copyright 2022 Pnoker All Rights Reserved
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

import io.github.pnoker.common.bean.point.PointValue;

import java.util.List;

/**
 * 用户自定义数据处理服务接口
 *
 * @author pnoker
 */
public interface RepositoryHandleService {

    /**
     * 自定义数据处理，此处可以自定义逻辑，将数据存放到别的数据库，或者发送到别的地方
     *
     * @param pointValue PointValue
     */
    void save(PointValue pointValue);

    /**
     * 自定义数据处理，此处可以自定义逻辑，将数据存放到别的数据库，或者发送到别的地方
     *
     * @param pointValues PointValue Array
     */
    void save(List<PointValue> pointValues);

}
