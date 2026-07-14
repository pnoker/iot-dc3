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

package io.github.pnoker.common.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.entity.event.MetadataEvent;
import io.github.pnoker.common.enums.MetadataOperateTypeEnum;
import io.github.pnoker.common.enums.MetadataTypeEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.manager.dal.PointAttributeManager;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.PointAttributeBO;
import io.github.pnoker.common.manager.entity.builder.PointAttributeBuilder;
import io.github.pnoker.common.manager.entity.model.PointAttributeDO;
import io.github.pnoker.common.manager.entity.query.PointAttributeQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.PointAttributeService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Business service implementation for point attribute operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointAttributeServiceImpl implements PointAttributeService {

    private final PointAttributeBuilder pointAttributeBuilder;

    private final PointAttributeManager pointAttributeManager;

    private final DriverService driverService;

    private final MetadataEventPublisher metadataEventPublisher;

    @Override
    public void add(PointAttributeBO entityBO) {
        validateTenantRelations(entityBO);
        checkDuplicate(entityBO, false, true);

        PointAttributeDO entityDO = pointAttributeBuilder.buildDOByBO(entityBO);
        if (!pointAttributeManager.save(entityDO)) {
            throw new AddException("Failed to create point attribute");
        }
        publishDriverMetadataEvent(entityDO.getDriverId());
    }

    @Override
    public void delete(Long id) {
        PointAttributeDO entityDO = getDOById(id, true);

        if (!pointAttributeManager.removeById(id)) {
            throw new DeleteException("Failed to remove point attribute");
        }
        publishDriverMetadataEvent(entityDO.getDriverId());
    }

    @Override
    public void update(PointAttributeBO entityBO) {
        PointAttributeDO current = getDOById(entityBO.getId(), true);
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        validateTenantRelations(entityBO);

        checkDuplicate(entityBO, true, true);

        PointAttributeDO entityDO = pointAttributeBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!pointAttributeManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update point attribute");
        }
        publishDriverMetadataEvent(entityDO.getDriverId());
    }

    @Override
    public PointAttributeBO getById(Long id) {
        PointAttributeDO entityDO = getDOById(id, true);
        return pointAttributeBuilder.buildBOByDO(entityDO);
    }

    @Override
    public PointAttributeBO getByNameAndDriverId(String name, Long driverId) {
        LambdaQueryChainWrapper<PointAttributeDO> wrapper = pointAttributeManager.lambdaQuery()
                .eq(PointAttributeDO::getAttributeCode, name)
                .eq(PointAttributeDO::getDriverId, driverId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        PointAttributeDO entityDO = wrapper.one();
        return pointAttributeBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<PointAttributeBO> listByDriverId(Long driverId) {
        LambdaQueryChainWrapper<PointAttributeDO> wrapper = pointAttributeManager.lambdaQuery()
                .eq(PointAttributeDO::getDriverId, driverId);
        List<PointAttributeDO> entityDO = wrapper.list();
        return pointAttributeBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public void saveBatch(List<PointAttributeBO> entityBOList) {
        if (Objects.isNull(entityBOList) || entityBOList.isEmpty()) {
            return;
        }
        entityBOList.forEach(this::validateTenantRelations);
        List<PointAttributeDO> doList = entityBOList.stream().map(pointAttributeBuilder::buildDOByBO).toList();
        if (!pointAttributeManager.saveBatch(doList)) {
            throw new AddException("Failed to batch create point attributes");
        }
        entityBOList.stream().map(PointAttributeBO::getDriverId).distinct().forEach(this::publishDriverMetadataEvent);
    }

    @Override
    public void updateBatch(List<PointAttributeBO> entityBOList) {
        if (Objects.isNull(entityBOList) || entityBOList.isEmpty()) {
            return;
        }
        entityBOList.forEach(this::validateTenantRelations);
        List<PointAttributeDO> doList = entityBOList.stream().map(bo -> {
            PointAttributeDO entityDO = pointAttributeBuilder.buildDOByBO(bo);
            entityDO.setOperateTime(null);
            return entityDO;
        }).toList();
        if (!pointAttributeManager.updateBatchById(doList)) {
            throw new UpdateException("Failed to batch update point attributes");
        }
        entityBOList.stream().map(PointAttributeBO::getDriverId).distinct().forEach(this::publishDriverMetadataEvent);
    }

    @Override
    public void removeByIds(Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return;
        }
        List<PointAttributeDO> entityDOList = pointAttributeManager.listByIds(ids);
        if (!pointAttributeManager.removeByIds(ids)) {
            throw new DeleteException("Failed to batch remove point attributes");
        }
        entityDOList.stream().map(PointAttributeDO::getDriverId).distinct().forEach(this::publishDriverMetadataEvent);
    }

    @Override
    public Page<PointAttributeBO> list(PointAttributeQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<PointAttributeDO> entityPageDO = pointAttributeManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return pointAttributeBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for point attribute search.
     *
     * @param entityQuery {@link PointAttributeQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link PointAttributeDO}
     */
    private LambdaQueryWrapper<PointAttributeDO> fuzzyQuery(PointAttributeQuery entityQuery) {
        LambdaQueryWrapper<PointAttributeDO> wrapper = Wrappers.<PointAttributeDO>query().lambda();
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getAttributeCode()), PointAttributeDO::getAttributeCode,
                entityQuery.getAttributeCode());
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getAttributeName()), PointAttributeDO::getAttributeName,
                entityQuery.getAttributeName());
        wrapper.eq(Objects.nonNull(entityQuery.getAttributeTypeFlag()), PointAttributeDO::getAttributeTypeFlag,
                Objects.isNull(entityQuery.getAttributeTypeFlag()) ? null
                        : entityQuery.getAttributeTypeFlag().getIndex());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDriverId()), PointAttributeDO::getDriverId,
                entityQuery.getDriverId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), PointAttributeDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), PointAttributeDO::getVersion, entityQuery.getVersion());
        return wrapper;
    }

    /**
     * Check whether a point attribute is duplicated by attribute code and driver.
     *
     * @param entityBO       {@link PointAttributeBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(PointAttributeBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<PointAttributeDO> wrapper = Wrappers.<PointAttributeDO>query().lambda();
        wrapper.eq(PointAttributeDO::getAttributeCode, entityBO.getAttributeCode());
        wrapper.eq(PointAttributeDO::getDriverId, entityBO.getDriverId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        PointAttributeDO one = pointAttributeManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Point attribute has been duplicated");
        }
        return duplicate;
    }

    /**
     * Validate that the attribute's driver belongs to the same tenant.
     *
     * @param entityBO the attribute to validate
     */
    private void validateTenantRelations(PointAttributeBO entityBO) {
        DriverBO driverBO = driverService.getById(entityBO.getDriverId());
        if (Objects.isNull(driverBO) || !Objects.equals(entityBO.getTenantId(), driverBO.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    /**
     * Publish a driver-update metadata event for the given driver.
     *
     * @param driverId the driver that changed
     */
    private void publishDriverMetadataEvent(Long driverId) {
        DriverBO driverBO = driverService.getById(driverId);
        if (Objects.isNull(driverBO) || StringUtils.isBlank(driverBO.getServiceName())) {
            return;
        }
        metadataEventPublisher.publishEvent(new MetadataEvent(this, driverId, MetadataTypeEnum.DRIVER,
                MetadataOperateTypeEnum.UPDATE, Set.of(driverBO.getServiceName())));
    }

    /**
     * Get point attribute data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link PointAttributeDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private PointAttributeDO getDOById(Long id, boolean throwException) {
        PointAttributeDO entityDO = pointAttributeManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Point attribute does not exist");
        }
        return entityDO;
    }

}
