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
import io.github.pnoker.common.enums.EntityTypeEnum;
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.manager.dal.DeviceManager;
import io.github.pnoker.common.manager.dal.DriverManager;
import io.github.pnoker.common.manager.dal.PointManager;
import io.github.pnoker.common.manager.entity.bo.DriverBO;
import io.github.pnoker.common.manager.entity.builder.DriverBuilder;
import io.github.pnoker.common.manager.entity.model.DeviceDO;
import io.github.pnoker.common.manager.entity.model.DriverDO;
import io.github.pnoker.common.manager.entity.model.PointDO;
import io.github.pnoker.common.manager.entity.query.DriverQuery;
import io.github.pnoker.common.manager.service.DriverService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business service implementation for driver operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

    private final DriverBuilder driverBuilder;

    private final DriverManager driverManager;

    private final DeviceManager deviceManager;

    private final PointManager pointManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(DriverBO entityBO) {
        checkDuplicate(entityBO, false, true);

        DriverDO entityDO = driverBuilder.buildDOByBO(entityBO);
        if (!driverManager.save(entityDO)) {
            throw new AddException("Failed to create driver");
        }
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        if (!driverManager.removeById(id)) {
            throw new DeleteException("Failed to remove driver");
        }
    }

    @Override
    public void update(DriverBO entityBO) {
        DriverDO current = getDOById(entityBO.getId(), true);
        if (!Objects.equals(entityBO.getTenantId(), current.getTenantId())) {
            throw new NotFoundException("Resource does not exist");
        }

        checkDuplicate(entityBO, true, true);

        DriverDO entityDO = driverBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!driverManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update point attribute config");
        }
    }

    @Override
    public DriverBO getById(Long id) {
        DriverDO entityDO = getDOById(id, true);
        return driverBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<DriverBO> list(DriverQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<DriverDO> entityPageDO = driverManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return driverBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public List<DriverBO> listByIds(Set<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<DriverDO> entityDOList = driverManager.listByIds(ids);
        return driverBuilder.buildBOListByDOList(entityDOList);
    }

    @Override
    public DriverBO getByServiceName(String serviceName, Long tenantId) {
        LambdaQueryChainWrapper<DriverDO> wrapper = driverManager.lambdaQuery()
                .eq(DriverDO::getServiceName, serviceName)
                .eq(DriverDO::getTenantId, tenantId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        DriverDO entityDO = wrapper.one();
        return driverBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<DriverBO> listByProfileId(Long profileId, Long tenantId) {
        LambdaQueryWrapper<DeviceDO> wrapper = Wrappers.<DeviceDO>query().lambda()
                .eq(DeviceDO::getProfileId, profileId);
        if (Objects.nonNull(tenantId)) {
            wrapper.eq(DeviceDO::getTenantId, tenantId);
        }
        List<DeviceDO> deviceDOList = deviceManager.list(wrapper);
        if (CollectionUtils.isEmpty(deviceDOList)) {
            return Collections.emptyList();
        }

        Set<Long> driverIds = deviceDOList.stream().map(DeviceDO::getDriverId).collect(Collectors.toSet());
        List<DriverBO> driverBOList = listByIds(driverIds);
        if (CollectionUtils.isEmpty(driverBOList)) {
            return Collections.emptyList();
        }

        return driverBOList;
    }

    @Override
    public List<DriverBO> listByPointId(Long pointId, Long tenantId) {
        PointDO entityDO = pointManager.getById(pointId);
        if (Objects.isNull(entityDO)) {
            throw new NotFoundException("Point does not exist");
        }
        return listByProfileId(entityDO.getProfileId(), tenantId);
    }

    @Override
    public DriverBO getByDeviceId(Long deviceId, Long tenantId) {
        DeviceDO entityDO = deviceManager.getById(deviceId);
        if (Objects.isNull(entityDO)) {
            throw new NotFoundException("Device does not exist");
        }
        if (Objects.nonNull(tenantId) && !Objects.equals(tenantId, entityDO.getTenantId())) {
            return null;
        }
        return getById(entityDO.getDriverId());
    }

    /**
     * @param entityQuery {@link DriverQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<DriverDO> fuzzyQuery(DriverQuery entityQuery) {
        LambdaQueryWrapper<DriverDO> wrapper = Wrappers.<DriverDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getDriverName()), DriverDO::getDriverName,
                entityQuery.getDriverName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getDriverCode()), DriverDO::getDriverCode,
                entityQuery.getDriverCode());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getServiceName()), DriverDO::getServiceName,
                entityQuery.getServiceName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getServiceHost()), DriverDO::getServiceHost,
                entityQuery.getServiceHost());
        wrapper.eq(Objects.nonNull(entityQuery.getDriverTypeFlag()), DriverDO::getDriverTypeFlag,
                Objects.isNull(entityQuery.getDriverTypeFlag()) ? null : entityQuery.getDriverTypeFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), DriverDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), DriverDO::getTenantId, entityQuery.getTenantId());
        wrapper.eq(Objects.nonNull(entityQuery.getVersion()), DriverDO::getVersion, entityQuery.getVersion());
        wrapper.exists(FieldUtil.isValidIdField(entityQuery.getGroupId()),
                "select 1 from dc3_group_bind dgb where dgb.deleted = 0 "
                        + "and dgb.tenant_id = dc3_driver.tenant_id "
                        + "and dgb.entity_type_flag = {0} "
                        + "and dgb.entity_id = dc3_driver.id "
                        + "and dgb.group_id = {1}",
                EntityTypeEnum.DRIVER.getIndex(), entityQuery.getGroupId());
        wrapper.exists(FieldUtil.isValidIdField(entityQuery.getLabelId()),
                "select 1 from dc3_label_bind dlb where dlb.deleted = 0 "
                        + "and dlb.tenant_id = dc3_driver.tenant_id "
                        + "and dlb.entity_type_flag = {0} "
                        + "and dlb.entity_id = dc3_driver.id "
                        + "and dlb.label_id = {1}",
                EntityTypeEnum.DRIVER.getIndex(), entityQuery.getLabelId());
        return wrapper;
    }

    /**
     * @param entityBO       {@link DriverBO}
     * @param isUpdate
     * @param throwException
     * @return
     */
    private boolean checkDuplicate(DriverBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<DriverDO> wrapper = Wrappers.<DriverDO>query().lambda();
        wrapper.eq(DriverDO::getDriverName, entityBO.getDriverName());
        wrapper.eq(DriverDO::getDriverCode, entityBO.getDriverCode());
        wrapper.eq(DriverDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DriverDO one = driverManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Driver has been duplicated");
        }
        return duplicate;
    }

    /**
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
     * @return {@link DriverDO}
     */
    private DriverDO getDOById(Long id, boolean throwException) {
        DriverDO entityDO = driverManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Driver does not exist");
        }
        return entityDO;
    }

}
