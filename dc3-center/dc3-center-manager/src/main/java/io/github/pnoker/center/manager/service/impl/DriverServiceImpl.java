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

package io.github.pnoker.center.manager.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.entity.bo.DriverBO;
import io.github.pnoker.center.manager.entity.builder.DriverBuilder;
import io.github.pnoker.center.manager.entity.model.DeviceDO;
import io.github.pnoker.center.manager.entity.model.DriverDO;
import io.github.pnoker.center.manager.entity.query.DriverQuery;
import io.github.pnoker.center.manager.manager.DeviceManager;
import io.github.pnoker.center.manager.manager.DriverManager;
import io.github.pnoker.center.manager.service.DriverService;
import io.github.pnoker.center.manager.service.ProfileBindService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * DriverService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverServiceImpl implements DriverService {

    @Resource
    private DriverBuilder driverBuilder;

    @Resource
    private DriverManager driverManager;
    @Resource
    private DeviceManager deviceManager;

    @Resource
    private ProfileBindService profileBindService;

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(DriverBO entityBO) {
        checkDuplicate(entityBO, false, true);

        DriverDO entityDO = driverBuilder.buildDOByBO(entityBO);
        if (!driverManager.save(entityDO)) {
            throw new AddException("驱动创建失败");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!driverManager.removeById(id)) {
            throw new DeleteException("驱动删除失败");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(DriverBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        DriverDO entityDO = driverBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!driverManager.updateById(entityDO)) {
            throw new UpdateException("驱动更新失败");
        }
    }

    @Override
    public DriverBO selectById(Long id) {
        DriverDO entityDO = getDOById(id, true);
        return driverBuilder.buildBOByDO(entityDO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<DriverBO> selectByPage(DriverQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<DriverDO> entityPageDO = driverManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return driverBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DriverBO> selectByIds(Set<Long> ids) {
        List<DriverDO> entityDOS = driverManager.listByIds(ids);
        return driverBuilder.buildBOListByDOList(entityDOS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverBO selectByServiceName(String serviceName, Long tenantId, boolean throwException) {
        LambdaQueryChainWrapper<DriverDO> wrapper = driverManager.lambdaQuery().eq(DriverDO::getServiceName, serviceName).eq(DriverDO::getTenantId, tenantId).last(QueryWrapperConstant.LIMIT_ONE);
        DriverDO entityDO = driverManager.getOne(wrapper);
        return driverBuilder.buildBOByDO(entityDO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DriverBO> selectByProfileId(Long profileId) {
        Set<Long> deviceIds = profileBindService.selectDeviceIdsByProfileId(profileId);
        List<DeviceDO> deviceDOS = deviceManager.listByIds(deviceIds);
        Set<Long> driverIds = deviceDOS.stream().map(DeviceDO::getDriverId).collect(Collectors.toSet());
        List<DriverBO> entityDOList = selectByIds(driverIds);
        if (CollUtil.isEmpty(entityDOList)) {
            return Collections.emptyList();
        }
        return entityDOList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverBO selectByDeviceId(Long deviceId) {
        DeviceDO entityDO = deviceManager.getById(deviceId);
        return selectById(entityDO.getDriverId());
    }

    private LambdaQueryWrapper<DriverDO> fuzzyQuery(DriverQuery query) {
        LambdaQueryWrapper<DriverDO> wrapper = Wrappers.<DriverDO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getDriverName()), DriverDO::getDriverName, query.getDriverName());
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getServiceName()), DriverDO::getServiceName, query.getServiceName());
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getServiceHost()), DriverDO::getServiceHost, query.getServiceHost());
            wrapper.eq(ObjectUtil.isNotNull(query.getDriverTypeFlag()), DriverDO::getDriverTypeFlag, query.getDriverTypeFlag());
            wrapper.eq(ObjectUtil.isNotNull(query.getEnableFlag()), DriverDO::getEnableFlag, query.getEnableFlag());
            wrapper.eq(ObjectUtil.isNotEmpty(query.getTenantId()), DriverDO::getTenantId, query.getTenantId());
        }
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link DriverBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(DriverBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<DriverDO> wrapper = Wrappers.<DriverDO>query().lambda();
        wrapper.eq(DriverDO::getDriverName, entityBO.getDriverName());
        wrapper.eq(DriverDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DriverDO one = driverManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("The driver is duplicates");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link DriverDO}
     */
    private DriverDO getDOById(Long id, boolean throwException) {
        DriverDO entityDO = driverManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("The driver not exist");
        }
        return entityDO;
    }
}
