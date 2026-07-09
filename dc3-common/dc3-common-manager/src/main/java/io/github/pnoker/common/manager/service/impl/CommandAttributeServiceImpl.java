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
import io.github.pnoker.common.manager.dal.CommandAttributeManager;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.builder.CommandAttributeBuilder;
import io.github.pnoker.common.manager.entity.model.CommandAttributeDO;
import io.github.pnoker.common.manager.entity.query.CommandAttributeQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.service.CommandAttributeService;
import io.github.pnoker.common.manager.service.DriverService;
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
 * Business service implementation for command attribute operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandAttributeServiceImpl implements CommandAttributeService {

    private final CommandAttributeBuilder commandAttributeBuilder;

    private final CommandAttributeManager commandAttributeManager;

    private final DriverService driverService;

    private final MetadataEventPublisher metadataEventPublisher;

    @Override
    public void add(CommandAttributeBO entityBO) {
        validateTenantRelations(entityBO);
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create command attribute: command attribute has been duplicated");
        }

        CommandAttributeDO entityDO = commandAttributeBuilder.buildDOByBO(entityBO);
        if (!commandAttributeManager.save(entityDO)) {
            throw new AddException("Failed to create command attribute");
        }
        publishDriverMetadataEvent(entityDO.getDriverId());
    }

    @Override
    public void delete(Long id) {
        CommandAttributeDO entityDO = getDOById(id, true);

        if (!commandAttributeManager.removeById(id)) {
            throw new DeleteException("Failed to remove command attribute");
        }
        publishDriverMetadataEvent(entityDO.getDriverId());
    }

    @Override
    public void update(CommandAttributeBO entityBO) {
        CommandAttributeDO current = getDOById(entityBO.getId(), true);
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException("Failed to update command attribute: command attribute has been duplicated");
        }

        CommandAttributeDO entityDO = commandAttributeBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!commandAttributeManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update command attribute");
        }
        publishDriverMetadataEvent(entityDO.getDriverId());
    }

    @Override
    public CommandAttributeBO getById(Long id) {
        CommandAttributeDO entityDO = getDOById(id, true);
        return commandAttributeBuilder.buildBOByDO(entityDO);
    }

    @Override
    public CommandAttributeBO getByNameAndDriverId(String name, Long driverId) {
        LambdaQueryChainWrapper<CommandAttributeDO> wrapper = commandAttributeManager.lambdaQuery()
                .eq(CommandAttributeDO::getAttributeCode, name)
                .eq(CommandAttributeDO::getDriverId, driverId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        CommandAttributeDO entityDO = wrapper.one();
        return commandAttributeBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<CommandAttributeBO> listByDriverId(Long driverId) {
        LambdaQueryChainWrapper<CommandAttributeDO> wrapper = commandAttributeManager.lambdaQuery()
                .eq(CommandAttributeDO::getDriverId, driverId);
        List<CommandAttributeDO> entityDO = wrapper.list();
        return commandAttributeBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public void saveBatch(List<CommandAttributeBO> entityBOList) {
        if (Objects.isNull(entityBOList) || entityBOList.isEmpty()) {
            return;
        }
        entityBOList.forEach(this::validateTenantRelations);
        List<CommandAttributeDO> doList = entityBOList.stream().map(commandAttributeBuilder::buildDOByBO).toList();
        if (!commandAttributeManager.saveBatch(doList)) {
            throw new AddException("Failed to batch create command attributes");
        }
        entityBOList.stream().map(CommandAttributeBO::getDriverId).distinct().forEach(this::publishDriverMetadataEvent);
    }

    @Override
    public void updateBatch(List<CommandAttributeBO> entityBOList) {
        if (Objects.isNull(entityBOList) || entityBOList.isEmpty()) {
            return;
        }
        entityBOList.forEach(this::validateTenantRelations);
        List<CommandAttributeDO> doList = entityBOList.stream().map(bo -> {
            CommandAttributeDO entityDO = commandAttributeBuilder.buildDOByBO(bo);
            entityDO.setOperateTime(null);
            return entityDO;
        }).toList();
        if (!commandAttributeManager.updateBatchById(doList)) {
            throw new UpdateException("Failed to batch update command attributes");
        }
        entityBOList.stream().map(CommandAttributeBO::getDriverId).distinct().forEach(this::publishDriverMetadataEvent);
    }

    @Override
    public void removeByIds(Collection<Long> ids) {
        if (Objects.isNull(ids) || ids.isEmpty()) {
            return;
        }
        List<CommandAttributeDO> entityDOList = commandAttributeManager.listByIds(ids);
        if (!commandAttributeManager.removeByIds(ids)) {
            throw new DeleteException("Failed to batch remove command attributes");
        }
        entityDOList.stream().map(CommandAttributeDO::getDriverId).distinct().forEach(this::publishDriverMetadataEvent);
    }

    @Override
    public Page<CommandAttributeBO> list(CommandAttributeQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<CommandAttributeDO> entityPageDO = commandAttributeManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return commandAttributeBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for command attribute search.
     *
     * @param entityQuery {@link CommandAttributeQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link CommandAttributeDO}
     */
    private LambdaQueryWrapper<CommandAttributeDO> fuzzyQuery(CommandAttributeQuery entityQuery) {
        LambdaQueryWrapper<CommandAttributeDO> wrapper = Wrappers.<CommandAttributeDO>query().lambda();
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getAttributeCode()), CommandAttributeDO::getAttributeCode,
                entityQuery.getAttributeCode());
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getAttributeName()), CommandAttributeDO::getAttributeName,
                entityQuery.getAttributeName());
        wrapper.eq(Objects.nonNull(entityQuery.getAttributeTypeFlag()), CommandAttributeDO::getAttributeTypeFlag,
                Objects.isNull(entityQuery.getAttributeTypeFlag()) ? null
                        : entityQuery.getAttributeTypeFlag().getIndex());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDriverId()), CommandAttributeDO::getDriverId,
                entityQuery.getDriverId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), CommandAttributeDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), CommandAttributeDO::getVersion, entityQuery.getVersion());
        return wrapper;
    }

    /**
     * Check whether a command attribute is duplicated by attribute code and driver.
     * Unlike the throwing variant, this only reports the duplicate without raising
     * an exception.
     *
     * @param entityBO {@link CommandAttributeBO} to be validated
     * @param isUpdate whether the operation is an update (true) or create (false)
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(CommandAttributeBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<CommandAttributeDO> wrapper = Wrappers.<CommandAttributeDO>query().lambda();
        wrapper.eq(CommandAttributeDO::getAttributeCode, entityBO.getAttributeCode());
        wrapper.eq(CommandAttributeDO::getDriverId, entityBO.getDriverId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        CommandAttributeDO one = commandAttributeManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    private void validateTenantRelations(CommandAttributeBO entityBO) {
        DriverBO driverBO = driverService.getById(entityBO.getDriverId());
        if (Objects.isNull(driverBO) || !Objects.equals(entityBO.getTenantId(), driverBO.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    private void publishDriverMetadataEvent(Long driverId) {
        DriverBO driverBO = driverService.getById(driverId);
        if (Objects.isNull(driverBO) || StringUtils.isBlank(driverBO.getServiceName())) {
            return;
        }
        metadataEventPublisher.publishEvent(new MetadataEvent(this, driverId, MetadataTypeEnum.DRIVER,
                MetadataOperateTypeEnum.UPDATE, Set.of(driverBO.getServiceName())));
    }

    /**
     * Get command attribute data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link CommandAttributeDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private CommandAttributeDO getDOById(Long id, boolean throwException) {
        CommandAttributeDO entityDO = commandAttributeManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Command attribute does not exist");
        }
        return entityDO;
    }

}
