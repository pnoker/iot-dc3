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

package io.github.pnoker.common.data.biz.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.driver.StrategyConstant;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.repository.RepositoryService;
import io.github.pnoker.common.strategy.RepositoryStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author pnoker
 * @version 2025.6.1
 * @since 2022.1.0
 */
@Slf4j
@Service("postgresRepositoryService")
public class PostgresRepositoryServiceImpl implements RepositoryService, InitializingBean {

    @Override
    public String getRepositoryName() {
        return StrategyConstant.Storage.POSTGRES;
    }

    @Override
    public void savePointValue(PointValueBO entityBO) {
    }

    @Override
    public void savePointValue(Long deviceId, List<PointValueBO> entityBOList) {
    }

    @Override
    public List<String> selectHistoryPointValue(Long deviceId, Long pointId, int count) {
        return null;
    }

    @Override
    public PointValueBO selectLatestPointValue(Long deviceId, Long pointId) {
        return null;
    }

    @Override
    public List<PointValueBO> selectLatestPointValue(Long deviceId, List<Long> pointIds) {
        return null;
    }

    @Override
    public Page<PointValueBO> selectPagePointValue(PointValueQuery entityQuery) {
        return null;
    }

    @Override
    public void afterPropertiesSet() {
        RepositoryStrategyFactory.put(StrategyConstant.Storage.POSTGRES, this);
    }

}