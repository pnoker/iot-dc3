/*
 * Copyright 2016-2021 Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.data.service.impl;

import com.dc3.center.data.service.DataCustomService;
import com.dc3.common.model.PointValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DataCustomServiceImpl implements DataCustomService {

    @Override
    public void preHandle(PointValue pointValue) {
        // TODO 接收数据之后，存储数据之前的操作
    }

    @Override
    public void postHandle(PointValue pointValue) {
        // TODO 接收数据之后，存储数据的时候的操作，此处可以自定义逻辑，将数据存放到别的数据库，或者发送到别的地方
    }

    @Override
    public void postHandle(List<PointValue> pointValues) {
        // TODO 接收数据之后，存储数据的时候的操作，此处可以自定义逻辑，将数据存放到别的数据库，或者发送到别的地方
    }

    @Override
    public void afterHandle(PointValue pointValue) {
        // TODO 接收数据之后，存储数据之后的操作
    }
}
