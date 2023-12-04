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

package io.github.pnoker.center.data.service.impl.repository;

import io.github.pnoker.center.data.mapper.TaosPointValueMapper;
import io.github.pnoker.center.data.service.RepositoryService;
import io.github.pnoker.center.data.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.constant.driver.StrategyConstant;
import io.github.pnoker.common.entity.point.PointValue;
import io.github.pnoker.common.entity.point.TaosPointValue;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@ConditionalOnProperty(name = "data.point.sava.tdengine.enable", havingValue = "true")
public class TDengineServiceImpl implements RepositoryService, InitializingBean {

    @Resource
    TaosPointValueMapper taosPointValueMapper;

    @EventListener
    public void initDatabase(ContextRefreshedEvent event) {
        taosPointValueMapper.createSuperTable();
    }

    @Override
    public String getRepositoryName() {
        return StrategyConstant.Storage.TDENGINE;
    }

    @Override
    public void savePointValue(PointValue pointValue) throws IOException {
        taosPointValueMapper.createDeviceTable(pointValue.getDeviceId(), pointValue.getPointId());
        taosPointValueMapper.insertOne(new TaosPointValue(pointValue));
    }

    @Override
    public void savePointValues(Long deviceId, List<PointValue> pointValues) throws IOException {
        taosPointValueMapper.createDeviceTable(deviceId, pointValues.get(0).getPointId());
        taosPointValueMapper.batchInsert(pointValues.stream().map(TaosPointValue::new).collect(Collectors.toList()));

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        RepositoryStrategyFactory.put(StrategyConstant.Storage.TDENGINE, this);
    }
}
