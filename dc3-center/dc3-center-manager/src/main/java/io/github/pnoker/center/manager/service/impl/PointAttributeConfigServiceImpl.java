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

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.biz.DriverNotifyService;
import io.github.pnoker.center.manager.dal.PointAttributeConfigManager;
import io.github.pnoker.center.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.builder.PointAttributeConfigBuilder;
import io.github.pnoker.center.manager.entity.model.PointAttributeConfigDO;
import io.github.pnoker.center.manager.entity.query.PointAttributeConfigQuery;
import io.github.pnoker.center.manager.service.PointAttributeConfigService;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PointInfoService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class PointAttributeConfigServiceImpl implements PointAttributeConfigService {

    @Resource
    private PointAttributeConfigBuilder pointAttributeConfigBuilder;

    @Resource
    private PointAttributeConfigManager pointAttributeConfigManager;

    @Resource
    private PointService pointService;
    @Resource
    private DriverNotifyService driverNotifyService;

    @Override
    public void save(PointAttributeConfigBO entityBO) {
        checkDuplicate(entityBO, false, true);

        PointAttributeConfigDO entityDO = pointAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!pointAttributeConfigManager.save(entityDO)) {
            throw new AddException("位号属性配置创建失败");
        }

        // 通知驱动新增
        entityDO = pointAttributeConfigManager.getById(entityDO.getId());
        entityBO = pointAttributeConfigBuilder.buildBOByDO(entityDO);
        driverNotifyService.notifyPointAttributeConfig(MetadataCommandTypeEnum.ADD, entityBO);
    }

    @Override
    public void remove(Long id) {
        PointAttributeConfigDO entityDO = getDOById(id, true);

        if (!pointAttributeConfigManager.removeById(id)) {
            throw new DeleteException("位号属性配置删除失败");
        }

        PointAttributeConfigBO entityBO = pointAttributeConfigBuilder.buildBOByDO(entityDO);
        driverNotifyService.notifyPointAttributeConfig(MetadataCommandTypeEnum.DELETE, entityBO);
    }

    @Override
    public void update(PointAttributeConfigBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        PointAttributeConfigDO entityDO = pointAttributeConfigBuilder.buildDOByBO(entityBO);
        entityBO.setOperateTime(null);
        if (!pointAttributeConfigManager.updateById(entityDO)) {
            throw new UpdateException("位号属性配置更新失败");
        }

        entityDO = pointAttributeConfigManager.getById(entityDO.getId());
        entityBO = pointAttributeConfigBuilder.buildBOByDO(entityDO);
        driverNotifyService.notifyPointAttributeConfig(MetadataCommandTypeEnum.UPDATE, entityBO);
    }

    @Override
    public PointAttributeConfigBO selectById(Long id) {
        PointAttributeConfigDO entityDO = getDOById(id, true);
        return pointAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public PointAttributeConfigBO selectByAttributeIdAndDeviceIdAndPointId(Long attributeId, Long deviceId, Long pointId) {
        LambdaQueryChainWrapper<PointAttributeConfigDO> wrapper = pointAttributeConfigManager.lambdaQuery()
                .eq(PointAttributeConfigDO::getDeviceId, deviceId)
                .eq(PointAttributeConfigDO::getPointId, pointId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        PointAttributeConfigDO entityDO = wrapper.one();
        return pointAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<PointAttributeConfigBO> selectByAttributeId(Long attributeId) {
        LambdaQueryChainWrapper<PointAttributeConfigDO> wrapper = pointAttributeConfigManager.lambdaQuery()
                .eq(PointAttributeConfigDO::getPointAttributeId, attributeId);
        List<PointAttributeConfigDO> entityDO = wrapper.list();
        return pointAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public List<PointAttributeConfigBO> selectByDeviceId(Long deviceId) {
        List<PointBO> pointBOS = pointService.selectByDeviceId(deviceId);
        Set<Long> pointIds = pointBOS.stream().map(PointBO::getId).collect(Collectors.toSet());
        LambdaQueryChainWrapper<PointAttributeConfigDO> wrapper = pointAttributeConfigManager.lambdaQuery()
                .eq(PointAttributeConfigDO::getDeviceId, deviceId)
                .in(PointAttributeConfigDO::getPointId, pointIds);
        List<PointAttributeConfigDO> entityDO = wrapper.list();
        return pointAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public List<PointAttributeConfigBO> selectByDeviceIdAndPointId(Long deviceId, Long pointId) {
        LambdaQueryChainWrapper<PointAttributeConfigDO> wrapper = pointAttributeConfigManager.lambdaQuery()
                .eq(PointAttributeConfigDO::getDeviceId, deviceId)
                .eq(PointAttributeConfigDO::getPointId, pointId);
        List<PointAttributeConfigDO> entityDO = wrapper.list();
        return pointAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public Page<PointAttributeConfigBO> selectByPage(PointAttributeConfigQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<PointAttributeConfigDO> entityPageDO = pointAttributeConfigManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return pointAttributeConfigBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private LambdaQueryWrapper<PointAttributeConfigDO> fuzzyQuery(PointAttributeConfigQuery entityQuery) {
        LambdaQueryWrapper<PointAttributeConfigDO> wrapper = Wrappers.<PointAttributeConfigDO>query().lambda();
        wrapper.eq(ObjectUtil.isNotEmpty(entityQuery.getPointAttributeId()), PointAttributeConfigDO::getPointAttributeId, entityQuery.getPointAttributeId());
        wrapper.eq(ObjectUtil.isNotEmpty(entityQuery.getDeviceId()), PointAttributeConfigDO::getDeviceId, entityQuery.getDeviceId());
        wrapper.eq(ObjectUtil.isNotEmpty(entityQuery.getPointId()), PointAttributeConfigDO::getPointId, entityQuery.getPointId());
        wrapper.eq(PointAttributeConfigDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link PointAttributeConfigBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(PointAttributeConfigBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<PointAttributeConfigDO> wrapper = Wrappers.<PointAttributeConfigDO>query().lambda();
        wrapper.eq(PointAttributeConfigDO::getPointAttributeId, entityBO.getPointAttributeId());
        wrapper.eq(PointAttributeConfigDO::getDeviceId, entityBO.getDeviceId());
        wrapper.eq(PointAttributeConfigDO::getPointId, entityBO.getPointId());
        wrapper.eq(PointAttributeConfigDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        PointAttributeConfigDO one = pointAttributeConfigManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("位号属性配置重复");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link PointAttributeConfigDO}
     */
    private PointAttributeConfigDO getDOById(Long id, boolean throwException) {
        PointAttributeConfigDO entityDO = pointAttributeConfigManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("位号属性配置不存在");
        }
        return entityDO;
    }

}
