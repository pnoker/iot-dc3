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

import com.alibaba.fastjson.JSON;
import com.dc3.center.data.service.DataCustomService;
import com.dc3.common.bean.point.PointValue;
import com.dc3.common.bean.point.TsPointValue;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author pnoker
 */
@Slf4j
@Service
public class DataCustomServiceImpl implements DataCustomService {

    private static final String putUrl = "";

    @Resource
    private OkHttpClient okHttpClient;

    @Override
    public void preHandle(PointValue pointValue) {
        // TODO 接收数据之后，存储数据之前的操作
    }

    @Override
    public void postHandle(PointValue pointValue) {
        String metric = pointValue.getDeviceId().toString();
        List<TsPointValue> tsPointValues = convertToTsPointValues(metric, pointValue);
        savePointValueToOpenTsdb(tsPointValues);
    }

    @Override
    public void postHandle(Long deviceId, List<PointValue> pointValues) {
        String metric = deviceId.toString();

        List<TsPointValue> tsPointValues = pointValues.stream()
                .map(pointValue -> convertToTsPointValues(metric, pointValue))
                .reduce(new ArrayList<>(), (first, second) -> {
                    first.addAll(second);
                    return first;
                });
        List<List<TsPointValue>> partition = Lists.partition(tsPointValues, 100);
        partition.forEach(this::savePointValueToOpenTsdb);
    }

    @Override
    public void afterHandle(PointValue pointValue) {
        // TODO 接收数据之后，存储数据的时候的操作，此处可以自定义逻辑，将数据存放到别的数据库，或者发送到别的地方
    }

    /**
     * convert point value to opentsdb point value
     *
     * @param metric     Metric Name
     * @param pointValue Point Value
     * @return TsPointValue Array
     */
    private List<TsPointValue> convertToTsPointValues(String metric, PointValue pointValue) {
        String point = pointValue.getPointId().toString();
        String value = pointValue.getValue();
        Long timestamp = pointValue.getOriginTime().getTime();

        List<TsPointValue> tsPointValues = new ArrayList<>(4);

        TsPointValue tsValue = new TsPointValue(metric, value);
        tsValue.setTimestamp(timestamp)
                .addTag("point", point)
                .addTag("valueType", "value");
        tsPointValues.add(tsValue);

        TsPointValue tsRawValue = new TsPointValue(metric, value);
        tsRawValue.setTimestamp(timestamp)
                .addTag("point", point)
                .addTag("valueType", "rawValue");
        tsPointValues.add(tsRawValue);

        return tsPointValues;
    }

    /**
     * send point value to opentsdb
     *
     * @param tsPointValues TsPointValue Array
     */
    private void savePointValueToOpenTsdb(List<TsPointValue> tsPointValues) {
        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json;charset=utf-8"), JSON.toJSONString(tsPointValues));
        Request request = new Request.Builder()
                .url(putUrl)
                .post(requestBody)
                .build();

        try {
            Response response = okHttpClient.newCall(request).execute();
            ResponseBody body = response.body();
            if (null != body) {
                log.debug("send pointValue to opentsdb {}: {}", response.message(), body.string());
                body.close();
            }
            response.close();
        } catch (IOException e) {
            log.error("send pointValue to opentsdb error: {}", e.getMessage());
        }
    }
}
