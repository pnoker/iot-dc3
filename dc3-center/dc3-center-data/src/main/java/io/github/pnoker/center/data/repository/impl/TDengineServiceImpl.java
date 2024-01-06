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

import io.github.pnoker.center.data.entity.bo.PointValueBO;
import io.github.pnoker.center.data.entity.point.TaosPointValue;
import io.github.pnoker.center.data.mapper.TaosPointValueMapper;
import io.github.pnoker.center.data.repository.RepositoryService;
import io.github.pnoker.center.data.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.constant.driver.StrategyConstant;
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
    public void savePointValue(PointValueBO pointValueBO) throws IOException {
        taosPointValueMapper.createDeviceTable(pointValueBO.getDeviceId(), pointValueBO.getPointId());
        taosPointValueMapper.insertOne(new TaosPointValue(pointValueBO));
    }

    @Override
    public void savePointValues(Long deviceId, List<PointValueBO> pointValueBOS) throws IOException {
        taosPointValueMapper.createDeviceTable(deviceId, pointValueBOS.get(0).getPointId());
        taosPointValueMapper.batchInsert(pointValueBOS.stream().map(TaosPointValue::new).collect(Collectors.toList()));

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        RepositoryStrategyFactory.put(StrategyConstant.Storage.TDENGINE, this);
    }
}
