/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.data.biz.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.driver.StrategyConstant;
import io.github.pnoker.common.data.dal.PointValueManager;
import io.github.pnoker.common.data.entity.builder.PointValueBuilder;
import io.github.pnoker.common.data.entity.model.PointValueDO;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.repository.RepositoryService;
import io.github.pnoker.common.strategy.RepositoryStrategyFactory;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
@Slf4j
@Service("postgresRepositoryService")
public class PostgresRepositoryServiceImpl implements RepositoryService, InitializingBean {

    @Resource
    private PointValueBuilder pointValueBuilder;

    @Resource
    private PointValueManager pointValueManager;

    @Override
    public String getRepositoryName() {
        return StrategyConstant.Storage.POSTGRES;
    }

    @Override
    public void savePointValue(PointValueBO entityBO) {
        PointValueDO entityDO = pointValueBuilder.buildDOByBO(entityBO);
        if (!pointValueManager.save(entityDO)) {
            throw new AddException("Failed to create point value");
        }
    }

    @Override
    public void savePointValues(List<PointValueBO> entityBOList) {
        List<PointValueDO> entityDOList = pointValueBuilder.buildDOListByBOList(entityBOList);
        if (!pointValueManager.saveBatch(entityDOList)) {
            throw new AddException("Failed to create point value list");
        }
    }

    @Override
    public List<String> selectHistoryPointValue(Long deviceId, Long pointId, int count) {
        LambdaQueryWrapper<PointValueDO> wrapper = Wrappers.<PointValueDO>query().lambda();
        wrapper.eq(PointValueDO::getDeviceId, deviceId);
        wrapper.eq(PointValueDO::getPointId, pointId);
        wrapper.orderByDesc(PointValueDO::getCreateTime);
        wrapper.last("limit %s".formatted(count));

        List<PointValueDO> entityPageDO = pointValueManager.list(wrapper);
        return entityPageDO.stream().map(PointValueDO::getCalValue).toList();
    }

    @Override
    public PointValueBO selectLatestPointValue(Long deviceId, Long pointId) {
        LambdaQueryWrapper<PointValueDO> wrapper = Wrappers.<PointValueDO>query().lambda();
        wrapper.eq(PointValueDO::getDeviceId, deviceId);
        wrapper.eq(PointValueDO::getPointId, pointId);
        wrapper.orderByDesc(PointValueDO::getCreateTime);
        wrapper.last("limit 1");

        PointValueDO entityDO = pointValueManager.getOne(wrapper);
        return pointValueBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<PointValueBO> selectLatestPointValues(Long deviceId, List<Long> pointIds) {
        return null;
    }

    @Override
    public Page<PointValueBO> selectPagePointValue(PointValueQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<PointValueDO> entityPageDO = pointValueManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return pointValueBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link PointValueQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<PointValueDO> fuzzyQuery(PointValueQuery entityQuery) {
        LambdaQueryWrapper<PointValueDO> wrapper = Wrappers.<PointValueDO>query().lambda();
        wrapper.eq(PointValueDO::getDeviceId, entityQuery.getDeviceId());
        wrapper.eq(PointValueDO::getPointId, entityQuery.getPointId());
        return wrapper;
    }

    @Override
    public void afterPropertiesSet() {
        RepositoryStrategyFactory.put(StrategyConstant.Storage.POSTGRES, this);
    }

}