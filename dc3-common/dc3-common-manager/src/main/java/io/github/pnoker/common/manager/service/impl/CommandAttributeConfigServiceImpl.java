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
import io.github.pnoker.common.manager.dal.CommandAttributeConfigManager;
import io.github.pnoker.common.manager.dal.CommandAttributeManager;
import io.github.pnoker.common.manager.dal.CommandManager;
import io.github.pnoker.common.manager.dal.DeviceManager;
import io.github.pnoker.common.manager.entity.bo.CommandAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.builder.CommandAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.model.CommandAttributeConfigDO;
import io.github.pnoker.common.manager.entity.model.CommandAttributeDO;
import io.github.pnoker.common.manager.entity.model.CommandDO;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.query.CommandAttributeConfigQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.service.CommandAttributeConfigService;
import io.github.pnoker.common.manager.service.CommandService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business service implementation for command attribute configuration.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandAttributeConfigServiceImpl implements CommandAttributeConfigService {

    private final CommandAttributeConfigBuilder commandAttributeConfigBuilder;

    private final CommandAttributeConfigManager commandAttributeConfigManager;

    private final MetadataEventPublisher metadataEventPublisher;

    private final CommandService commandService;

    private final DeviceManager deviceManager;

    private final CommandManager commandManager;

    private final CommandAttributeManager commandAttributeManager;

    @Override
    public void add(CommandAttributeConfigBO entityBO) {
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException(
                    "Failed to create command attribute config: command attribute config has been duplicated");
        }

        CommandAttributeConfigDO entityDO = commandAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!commandAttributeConfigManager.save(entityDO)) {
            throw new AddException("Failed to create command attribute config");
        }

        //
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public CommandAttributeConfigBO innerSave(CommandAttributeConfigBO entityBO) {
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException(
                    "Failed to create command attribute config: command attribute config has been duplicated");
        }

        CommandAttributeConfigDO entityDO = commandAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!commandAttributeConfigManager.save(entityDO)) {
            throw new AddException("Failed to create command attribute config");
        }

        return commandAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public void delete(Long id) {
        CommandAttributeConfigDO entityDO = getDOById(id, true);

        if (!commandAttributeConfigManager.removeById(id)) {
            throw new DeleteException("Failed to remove command attribute config");
        }

        //
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public void update(CommandAttributeConfigBO entityBO) {
        CommandAttributeConfigDO current = getDOById(entityBO.getId(), true);
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        validateTenantRelations(entityBO);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException(
                    "Failed to update command attribute config: command attribute config has been duplicated");
        }

        CommandAttributeConfigDO entityDO = commandAttributeConfigBuilder.buildDOByBO(entityBO);
        entityBO.setOperateTime(null);
        if (!commandAttributeConfigManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update command attribute config");
        }

        //
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE,
                MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public CommandAttributeConfigBO getById(Long id) {
        CommandAttributeConfigDO entityDO = getDOById(id, true);
        return commandAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public CommandAttributeConfigBO getByAttributeIdAndDeviceIdAndCommandId(Long attributeId, Long deviceId,
                                                                            Long commandId) {
        LambdaQueryChainWrapper<CommandAttributeConfigDO> wrapper = commandAttributeConfigManager.lambdaQuery()
                .eq(CommandAttributeConfigDO::getAttributeId, attributeId)
                .eq(CommandAttributeConfigDO::getDeviceId, deviceId)
                .eq(CommandAttributeConfigDO::getCommandId, commandId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        CommandAttributeConfigDO entityDO = wrapper.one();
        return commandAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<CommandAttributeConfigBO> listByAttributeId(Long attributeId) {
        LambdaQueryChainWrapper<CommandAttributeConfigDO> wrapper = commandAttributeConfigManager.lambdaQuery()
                .eq(CommandAttributeConfigDO::getAttributeId, attributeId);
        List<CommandAttributeConfigDO> entityDO = wrapper.list();
        return commandAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public List<CommandAttributeConfigBO> listByDeviceId(Long deviceId) {
        List<CommandBO> commandBOList = commandService.listByDeviceId(deviceId);
        Set<Long> commandIds = commandBOList.stream().map(CommandBO::getId).collect(Collectors.toSet());
        if (CollectionUtils.isEmpty(commandIds)) {
            return Collections.emptyList();
        }

        LambdaQueryChainWrapper<CommandAttributeConfigDO> wrapper = commandAttributeConfigManager.lambdaQuery()
                .eq(CommandAttributeConfigDO::getDeviceId, deviceId)
                .in(CommandAttributeConfigDO::getCommandId, commandIds);
        List<CommandAttributeConfigDO> entityDO = wrapper.list();
        return commandAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public List<CommandAttributeConfigBO> listByDeviceIdAndCommandId(Long deviceId, Long commandId) {
        LambdaQueryChainWrapper<CommandAttributeConfigDO> wrapper = commandAttributeConfigManager.lambdaQuery()
                .eq(CommandAttributeConfigDO::getDeviceId, deviceId)
                .eq(CommandAttributeConfigDO::getCommandId, commandId);
        List<CommandAttributeConfigDO> entityDO = wrapper.list();
        return commandAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public Page<CommandAttributeConfigBO> list(CommandAttributeConfigQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<CommandAttributeConfigDO> entityPageDO = commandAttributeConfigManager
                .page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return commandAttributeConfigBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * @param entityQuery {@link CommandAttributeConfigQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<CommandAttributeConfigDO> fuzzyQuery(CommandAttributeConfigQuery entityQuery) {
        LambdaQueryWrapper<CommandAttributeConfigDO> wrapper = Wrappers.<CommandAttributeConfigDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getAttributeId()), CommandAttributeConfigDO::getAttributeId,
                entityQuery.getAttributeId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDeviceId()), CommandAttributeConfigDO::getDeviceId,
                entityQuery.getDeviceId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getCommandId()), CommandAttributeConfigDO::getCommandId,
                entityQuery.getCommandId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), CommandAttributeConfigDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), CommandAttributeConfigDO::getTenantId,
                entityQuery.getTenantId());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), CommandAttributeConfigDO::getVersion,
                entityQuery.getVersion());
        return wrapper;
    }

    /**
     * @param entityBO {@link CommandAttributeConfigBO}
     * @param isUpdate
     * @return
     */
    private boolean checkDuplicate(CommandAttributeConfigBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<CommandAttributeConfigDO> wrapper = Wrappers.<CommandAttributeConfigDO>query().lambda();
        wrapper.eq(CommandAttributeConfigDO::getAttributeId, entityBO.getAttributeId());
        wrapper.eq(CommandAttributeConfigDO::getDeviceId, entityBO.getDeviceId());
        wrapper.eq(CommandAttributeConfigDO::getCommandId, entityBO.getCommandId());
        wrapper.eq(CommandAttributeConfigDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        CommandAttributeConfigDO one = commandAttributeConfigManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
    }

    private void validateTenantRelations(CommandAttributeConfigBO entityBO) {
        DeviceDO deviceDO = deviceManager.getById(entityBO.getDeviceId());
        CommandDO commandDO = commandManager.getById(entityBO.getCommandId());
        CommandAttributeDO attributeDO = commandAttributeManager.getById(entityBO.getAttributeId());
        if (Objects.isNull(deviceDO) || Objects.isNull(commandDO) || Objects.isNull(attributeDO)
                || !Objects.equals(entityBO.getTenantId(), deviceDO.getTenantId())
                || !Objects.equals(entityBO.getTenantId(), commandDO.getTenantId())
                || !Objects.equals(entityBO.getTenantId(), attributeDO.getTenantId())
                || !Objects.equals(deviceDO.getDriverId(), attributeDO.getDriverId())
                || !Objects.equals(deviceDO.getProfileId(), commandDO.getProfileId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    /**
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link CommandAttributeConfigDO}
     */
    private CommandAttributeConfigDO getDOById(Long id, boolean throwException) {
        CommandAttributeConfigDO entityDO = commandAttributeConfigManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Command attribute config does not exist");
        }
        return entityDO;
    }

}
