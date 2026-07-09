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
import io.github.pnoker.common.manager.dal.CommandManager;
import io.github.pnoker.common.manager.dal.CommandParamManager;
import io.github.pnoker.common.manager.entity.bo.CommandBO;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.bo.ProfileBO;
import io.github.pnoker.common.manager.entity.builder.CommandBuilder;
import io.github.pnoker.common.manager.entity.model.CommandDO;
import io.github.pnoker.common.manager.entity.model.CommandParamDO;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.query.CommandQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.mapper.CommandMapper;
import io.github.pnoker.common.manager.mapper.DeviceMapper;
import io.github.pnoker.common.manager.service.CommandService;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.manager.service.ProfileService;
import io.github.pnoker.common.utils.FieldUtil;
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
 * Business service implementation for command operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommandServiceImpl implements CommandService {

    private final CommandBuilder commandBuilder;

    private final CommandManager commandManager;

    private final CommandMapper commandMapper;

    private final CommandParamManager commandParamManager;

    private final MetadataEventPublisher metadataEventPublisher;

    private final ProfileService profileService;

    private final DeviceMapper deviceMapper;

    private final DriverService driverService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(CommandBO entityBO) {
        validateTenantRelations(entityBO);
        entityBO.setCommandCode(null);
        checkDuplicate(entityBO, false, true);

        CommandDO entityDO = commandBuilder.buildDOByBO(entityBO);
        if (!commandManager.save(entityDO)) {
            throw new AddException("Failed to create command");
        }
        entityBO.setId(entityDO.getId());

        List<Long> deviceIds = listDeviceIdsByProfileId(entityDO.getProfileId());
        metadataEventPublisher.publishEvent(
                new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.COMMAND, MetadataOperateTypeEnum.ADD,
                        driverServiceNamesByDeviceIds(deviceIds)));
        publishDeviceUpdateEvents(deviceIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        CommandDO entityDO = getDOById(id, true);
        List<Long> deviceIds = listDeviceIdsByProfileId(entityDO.getProfileId());
        Set<String> targetServices = driverServiceNamesByDeviceIds(deviceIds);

        cascadeDeleteParams(id);

        if (!commandManager.removeById(id)) {
            throw new DeleteException("Failed to remove command");
        }

        metadataEventPublisher.publishEvent(
                new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.COMMAND,
                        MetadataOperateTypeEnum.DELETE, targetServices));
        publishDeviceUpdateEvents(deviceIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(CommandBO entityBO) {
        CommandDO current = getDOById(entityBO.getId(), true);
        List<Long> oldDeviceIds = listDeviceIdsByProfileId(current.getProfileId());
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
        entityBO.setCommandCode(current.getCommandCode());
        validateTenantRelations(entityBO);

        checkDuplicate(entityBO, true, true);

        CommandDO entityDO = commandBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!commandManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update command");
        }

        if (Objects.equals(current.getProfileId(), entityDO.getProfileId())) {
            metadataEventPublisher.publishEvent(
                    new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.COMMAND,
                            MetadataOperateTypeEnum.UPDATE, driverServiceNamesByDeviceIds(oldDeviceIds)));
        } else {
            List<Long> newDeviceIds = listDeviceIdsByProfileId(entityDO.getProfileId());
            metadataEventPublisher.publishEvent(
                    new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.COMMAND,
                            MetadataOperateTypeEnum.DELETE, driverServiceNamesByDeviceIds(oldDeviceIds)));
            metadataEventPublisher.publishEvent(
                    new MetadataEvent(this, entityDO.getId(), MetadataTypeEnum.COMMAND,
                            MetadataOperateTypeEnum.ADD, driverServiceNamesByDeviceIds(newDeviceIds)));
            publishDeviceUpdateEvents(oldDeviceIds);
            publishDeviceUpdateEvents(newDeviceIds);
        }
    }

    @Override
    public CommandBO getById(Long id) {
        CommandDO entityDO = getDOById(id, true);
        return commandBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<CommandBO> listByIds(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<CommandDO> entityDOList = commandManager.listByIds(ids);
        return commandBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public List<CommandBO> listByDeviceId(Long deviceId, Long tenantId) {
        DeviceDO deviceDO = deviceMapper.selectById(deviceId);
        if (Objects.isNull(deviceDO) || Objects.isNull(deviceDO.getProfileId())) {
            return Collections.emptyList();
        }
        return listByProfileId(deviceDO.getProfileId(), deviceDO.getTenantId());
    }

    @Override
    public List<CommandBO> listByProfileId(Long profileId, Long tenantId) {
        LambdaQueryChainWrapper<CommandDO> wrapper = commandManager.lambdaQuery().eq(CommandDO::getProfileId, profileId);
        List<CommandDO> entityDOList = wrapper.list();
        return commandBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public List<CommandBO> listByProfileIds(List<Long> profileIds) {
        if (CollectionUtils.isEmpty(profileIds)) {
            return Collections.emptyList();
        }
        LambdaQueryChainWrapper<CommandDO> wrapper = commandManager.lambdaQuery().in(CommandDO::getProfileId, profileIds);
        List<CommandDO> entityDOList = wrapper.list();
        return commandBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public Page<CommandBO> list(CommandQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<CommandDO> entityPageDO = commandMapper.selectPageWithDevice(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery), entityQuery.getDeviceId());
        return commandBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for command search.
     *
     * @param entityQuery {@link CommandQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link CommandDO}
     */
    private LambdaQueryWrapper<CommandDO> fuzzyQuery(CommandQuery entityQuery) {
        QueryWrapper<CommandDO> wrapper = Wrappers.query();
        wrapper.eq("dc.deleted", 0);
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getCommandName()), "dc.command_name", entityQuery.getCommandName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getCommandCode()), "dc.command_code", entityQuery.getCommandCode());
        wrapper.eq(Objects.nonNull(entityQuery.getCommandType()), "dc.command_type_flag",
                Objects.isNull(entityQuery.getCommandType()) ? null : entityQuery.getCommandType().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getCallType()), "dc.call_type_flag",
                Objects.isNull(entityQuery.getCallType()) ? null : entityQuery.getCallType().getIndex());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getProfileId()), "dc.profile_id", entityQuery.getProfileId());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), "dc.enable_flag",
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), "dc.version", entityQuery.getVersion());
        return wrapper.lambda();
    }

    private boolean checkDuplicate(CommandBO entityBO, boolean isUpdate, boolean throwException) {
        boolean hasName = StringUtils.isNotEmpty(entityBO.getCommandName());
        boolean hasCode = StringUtils.isNotEmpty(entityBO.getCommandCode());
        if (!hasName && !hasCode) {
            return false;
        }
        LambdaQueryWrapper<CommandDO> wrapper = Wrappers.<CommandDO>query().lambda();
        wrapper.eq(CommandDO::getProfileId, entityBO.getProfileId());
        wrapper.ne(isUpdate && Objects.nonNull(entityBO.getId()), CommandDO::getId, entityBO.getId());
        wrapper.and(query -> {
            if (hasName) {
                query.eq(CommandDO::getCommandName, entityBO.getCommandName());
            }
            if (hasCode) {
                if (hasName) {
                    query.or();
                }
                query.eq(CommandDO::getCommandCode, entityBO.getCommandCode());
            }
        });
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        CommandDO one = commandManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Command has been duplicated");
        }
        return duplicate;
    }

    private void validateTenantRelations(CommandBO entityBO) {
        ProfileBO profileBO = profileService.getById(entityBO.getProfileId());
        if (Objects.isNull(profileBO) || !Objects.equals(entityBO.getTenantId(), profileBO.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }
    }

    private void cascadeDeleteParams(Long commandId) {
        LambdaQueryChainWrapper<CommandParamDO> wrapper = commandParamManager.lambdaQuery()
                .eq(CommandParamDO::getCommandId, commandId);
        List<CommandParamDO> params = wrapper.list();
        if (CollectionUtils.isNotEmpty(params)) {
            commandParamManager.removeByIds(params.stream().map(CommandParamDO::getId).toList());
        }
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
        DriverBO driverBO = driverService.getByDeviceId(deviceId, null);
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

    private CommandDO getDOById(Long id, boolean throwException) {
        CommandDO entityDO = commandManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Command does not exist");
        }
        return entityDO;
    }

}
