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

package io.github.pnoker.center.data.service.impl.repository;

import cn.hutool.core.util.StrUtil;
import io.github.pnoker.center.data.service.RepositoryService;
import io.github.pnoker.center.data.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.bean.point.PointValue;
import io.github.pnoker.common.bean.point.TsPointValue;
import io.github.pnoker.common.constant.CommonConstant;
import io.github.pnoker.common.utils.JsonUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author pnoker
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "data.point.sava.opentsdb.enable", havingValue = "true")
public class OpentsdbServiceImpl implements RepositoryService, InitializingBean {

    @Value("${data.point.sava.opentsdb.host}")
    private String host;
    @Value("${data.point.sava.opentsdb.port}")
    private Integer port;

    @Resource
    private OkHttpClient okHttpClient;

    @Override
    public void savePointValue(PointValue pointValue) {
        if (!StrUtil.isAllNotEmpty(pointValue.getDeviceId(), pointValue.getPointId())) {
            return;
        }

        savePointValues(pointValue.getDeviceId(), Collections.singletonList(pointValue));
    }

    @Override
    public void savePointValues(String deviceId, List<PointValue> pointValues) {
        if (StrUtil.isEmpty(deviceId)) {
            return;
        }

        List<TsPointValue> tsPointValues = pointValues.stream()
                .filter(pointValue -> StrUtil.isNotEmpty(pointValue.getPointId()))
                .map(pointValue -> convertPointValues(CommonConstant.Storage.POINT_VALUE_PREFIX + deviceId, pointValue))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

        List<List<TsPointValue>> partition = Lists.partition(tsPointValues, 100);
        partition.forEach(this::putPointValues);
    }

    @Override
    public void afterPropertiesSet() {
        RepositoryStrategyFactory.put(CommonConstant.RepositoryStrategy.REPOSITORY_STRATEGY_OPENTSDB, this);
    }

    private List<TsPointValue> convertPointValues(String metric, PointValue pointValue) {
        String point = pointValue.getPointId();
        String value = pointValue.getValue();
        long timestamp = pointValue.getOriginTime().getTime();

        List<TsPointValue> tsPointValues = new ArrayList<>(2);

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

    private void putPointValues(List<TsPointValue> tsPointValues) {
        String putUrl = String.format("http://%s:%s/api/put?details", host, port);
        RequestBody requestBody = RequestBody.create(JsonUtil.toJsonString(tsPointValues), MediaType.parse(ContentType.APPLICATION_JSON.toString()));
        Request request = new Request.Builder()
                .url(putUrl)
                .post(requestBody)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("Send pointValue to opentsdb error: {}", e.getMessage(), e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                if (null != response.body()) {
                    log.debug("Send pointValue to opentsdb, Response: {}", response.message());
                }
            }
        });
    }
}
