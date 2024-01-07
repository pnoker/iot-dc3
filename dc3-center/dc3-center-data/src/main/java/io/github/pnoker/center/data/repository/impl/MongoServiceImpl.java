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

package io.github.pnoker.center.data.repository.impl;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.center.data.entity.bo.PointValueBO;
import io.github.pnoker.center.data.entity.builder.PointValueBuilder;
import io.github.pnoker.center.data.entity.point.MgPointValueDO;
import io.github.pnoker.center.data.repository.RepositoryService;
import io.github.pnoker.center.data.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.constant.driver.StorageConstant;
import io.github.pnoker.common.constant.driver.StrategyConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class MongoServiceImpl implements RepositoryService, InitializingBean {

    @Resource
    private PointValueBuilder pointValueBuilder;

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String getRepositoryName() {
        return StrategyConstant.Storage.MONGO;
    }

    @Override
    public void savePointValue(PointValueBO entityBO) {
        if (!ObjectUtil.isAllNotEmpty(entityBO.getDeviceId(), entityBO.getPointId())) {
            return;
        }

        final String collection = StorageConstant.POINT_VALUE_PREFIX + entityBO.getDeviceId();
        ensurePointValueIndex(collection);
        MgPointValueDO entityDO = pointValueBuilder.buildMgDOByBO(entityBO);
        mongoTemplate.insert(entityDO, collection);
    }

    @Override
    public void savePointValues(Long deviceId, List<PointValueBO> pointValueBOS) {
        if (ObjectUtil.isEmpty(deviceId)) {
            return;
        }

        final String collection = StorageConstant.POINT_VALUE_PREFIX + deviceId;
        ensurePointValueIndex(collection);
        final List<MgPointValueDO> entityDOS = pointValueBOS.stream()
                .filter(entityBO -> ObjectUtil.isNotEmpty(entityBO.getPointId()))
                .map(entityBO -> {
                    MgPointValueDO entityDO = pointValueBuilder.buildMgDOByBO(entityBO);
                    entityDO.setDeviceId(deviceId);
                    return entityDO;
                })
                .collect(Collectors.toList());
        mongoTemplate.insert(entityDOS, collection);
    }

    @Override
    public void afterPropertiesSet() {
        RepositoryStrategyFactory.put(StrategyConstant.Storage.MONGO, this);
    }

    /**
     * Ensure device point and time index
     *
     * @param collection Collection Name
     */
    private void ensurePointValueIndex(String collection) {
        // ensure point index
        Index pointIndex = new Index();
        pointIndex.background()
                .on("pointId", Sort.Direction.DESC)
                .named("IX_point_id");
        mongoTemplate.indexOps(collection).ensureIndex(pointIndex);

        // ensure time index
        Index timeIndex = new Index();
        timeIndex.background()
                .on("createTime", Sort.Direction.DESC)
                .named("IX_create_time");
        mongoTemplate.indexOps(collection).ensureIndex(timeIndex);
    }

}