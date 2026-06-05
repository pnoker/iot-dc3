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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import io.github.pnoker.common.manager.dal.CommandParamManager;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.bo.CommandParamBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.builder.CommandParamBuilder;
import io.github.pnoker.common.manager.entity.model.CommandParamDO;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.query.CommandParamQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.service.CommandParamService;
import io.github.pnoker.common.manager.service.CommandService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business service implementation for command param operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandParamServiceImpl implements CommandParamService {

    private final CommandParamBuilder commandParamBuilder;

    private final CommandParamManager commandParamManager;

    private final CommandService commandService;

    private final MetadataEventPublisher metadataEventPublisher;

    private final DeviceMapper deviceMapper;

    private final DriverService driverService;

    @Override
    @Transactional
    public void add(CommandParamBO entityBO) {
        validateTenantRelations(entityBO);
        checkDuplicate(entityBO, false, true);

        CommandParamDO entityDO = commandParamBuilder.buildDOByBO(entityBO);
        if (!commandParamManager.save(entityDO)) {
            throw new AddException("Failed to create command param");
        }
        publishCommandUpdate(entityBO.getCommandId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        CommandParamDO entityDO = getDOById(id, true);
        if (!commandParamManager.removeById(id)) {
            throw new DeleteException("Failed to remove command param");
        }
        publishCommandUpdate(entityDO.getCommandId());
    }

    @Override
    @Transactional
    public void update(CommandParamBO entityBO) {
        CommandParamDO current = getDOById(entityBO.getId(), true);
        Long oldCommandId = current.getCommandId();
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        validateTenantRelations(entityBO);

        checkDuplicate(entityBO, true, true);

        CommandParamDO entityDO = commandParamBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!commandParamManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update command param");
        }
        publishCommandUpdate(oldCommandId);
        if (!Objects.equals(oldCommandId, entityBO.getCommandId())) {
            publishCommandUpdate(entityBO.getCommandId());
        }
    }

    @Override
    public CommandParamBO getById(Long id) {
        CommandParamDO entityDO = getDOById(id, true);
        return commandParamBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<CommandParamBO> listByIds(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<CommandParamDO> entityDOList = commandParamManager.listByIds(ids);
        return commandParamBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public List<CommandParamBO> listByCommandId(Long commandId) {
        LambdaQueryChainWrapper<CommandParamDO> wrapper = commandParamManager.lambdaQuery()
                .eq(CommandParamDO::getCommandId, commandId);
        List<CommandParamDO> entityDOList = wrapper.list();
        return commandParamBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public Page<CommandParamBO> list(CommandParamQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<CommandParamDO> entityPageDO = commandParamManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return commandParamBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private LambdaQueryWrapper<CommandParamDO> fuzzyQuery(CommandParamQuery entityQuery) {
        QueryWrapper<CommandParamDO> wrapper = Wrappers.query();
        wrapper.eq("deleted", 0);
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getParamName()), "param_name", entityQuery.getParamName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getParamCode()), "param_code", entityQuery.getParamCode());
        wrapper.eq(Objects.nonNull(entityQuery.getParamDirection()), "param_direction_flag",
                Objects.isNull(entityQuery.getParamDirection()) ? null : entityQuery.getParamDirection().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getParamTypeFlag()), "param_type_flag",
                Objects.isNull(entityQuery.getParamTypeFlag()) ? null : entityQuery.getParamTypeFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getCommandId()), "command_id", entityQuery.getCommandId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), "enable_flag",
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), "tenant_id", entityQuery.getTenantId());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), "version", entityQuery.getVersion());
        return wrapper.lambda();
    }

    private boolean checkDuplicate(CommandParamBO entityBO, boolean isUpdate, boolean throwException) {
        boolean hasName = StringUtils.isNotEmpty(entityBO.getParamName());
        boolean hasCode = StringUtils.isNotEmpty(entityBO.getParamCode());
        if (!hasName && !hasCode) {
            return false;
        }
        LambdaQueryWrapper<CommandParamDO> wrapper = Wrappers.<CommandParamDO>query().lambda();
        wrapper.eq(CommandParamDO::getCommandId, entityBO.getCommandId());
        wrapper.eq(CommandParamDO::getTenantId, entityBO.getTenantId());
        wrapper.ne(isUpdate && Objects.nonNull(entityBO.getId()), CommandParamDO::getId, entityBO.getId());
        wrapper.and(query -> {
            if (hasName) {
                query.eq(CommandParamDO::getParamName, entityBO.getParamName());
            }
            if (hasCode) {
                if (hasName) {
                    query.or();
                }
                query.eq(CommandParamDO::getParamCode, entityBO.getParamCode());
            }
        });
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        CommandParamDO one = commandParamManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Command param has been duplicated");
        }
        return duplicate;
    }

    private void validateTenantRelations(CommandParamBO entityBO) {
        CommandBO commandBO = commandService.getById(entityBO.getCommandId());
        if (Objects.isNull(commandBO) || !Objects.equals(entityBO.getTenantId(), commandBO.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    private void publishCommandUpdate(Long commandId) {
        if (Objects.isNull(commandId)) {
            return;
        }
        CommandBO commandBO;
        try {
            commandBO = commandService.getById(commandId);
        } catch (NotFoundException e) {
            return;
        }
        if (Objects.isNull(commandBO)) {
            return;
        }

        List<Long> deviceIds = listDeviceIdsByProfileId(commandBO.getProfileId());
        metadataEventPublisher.publishEvent(
                new MetadataEvent(this, commandId, MetadataTypeEnum.COMMAND, MetadataOperateTypeEnum.UPDATE,
                        driverServiceNamesByDeviceIds(deviceIds)));
        publishDeviceUpdateEvents(deviceIds);
    }

    private void publishDeviceUpdateEvents(Collection<Long> deviceIds) {
        if (CollectionUtils.isEmpty(deviceIds)) {
            return;
        }
        deviceIds.forEach(deviceId -> metadataEventPublisher.publishEvent(
                new MetadataEvent(this, deviceId, MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE,
                        driverServiceNamesByDeviceId(deviceId))));
    }

    private Set<String> driverServiceNamesByDeviceIds(Collection<Long> deviceIds) {
        if (CollectionUtils.isEmpty(deviceIds)) {
            return Collections.emptySet();
        }
        return deviceIds.stream()
                .map(this::driverServiceNamesByDeviceId)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    private Set<String> driverServiceNamesByDeviceId(Long deviceId) {
        if (Objects.isNull(deviceId)) {
            return Collections.emptySet();
        }
        DriverBO driverBO = driverService.listByDeviceId(deviceId, null);
        if (Objects.isNull(driverBO) || StringUtils.isBlank(driverBO.getServiceName())) {
            return Collections.emptySet();
        }
        return Set.of(driverBO.getServiceName());
    }

    private List<Long> listDeviceIdsByProfileId(Long profileId) {
        if (Objects.isNull(profileId)) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>lambdaQuery()
                .eq(DeviceDO::getProfileId, profileId);
        return deviceMapper.selectList(wrapper).stream().map(DeviceDO::getId).toList();
    }

    private CommandParamDO getDOById(Long id, boolean throwException) {
        CommandParamDO entityDO = commandParamManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Command param does not exist");
        }
        return entityDO;
    }

}
