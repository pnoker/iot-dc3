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
import io.github.pnoker.center.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.center.manager.entity.builder.DriverAttributeConfigBuilder;
import io.github.pnoker.center.manager.entity.model.DriverAttributeConfigDO;
import io.github.pnoker.center.manager.entity.query.DriverAttributeConfigQuery;
import io.github.pnoker.center.manager.manager.DriverAttributeConfigManager;
import io.github.pnoker.center.manager.manager.DriverAttributeManager;
import io.github.pnoker.center.manager.service.DriverAttributeConfigService;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.constant.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * DriverInfoService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class DriverAttributeConfigServiceImpl implements DriverAttributeConfigService {

    @Resource
    private DriverAttributeConfigBuilder driverAttributeConfigBuilder;

    @Resource
    private DriverAttributeManager driverAttributeManager;
    @Resource
    private DriverAttributeConfigManager driverAttributeConfigManager;

    @Resource
    private NotifyService notifyService;

    @Override
    public void save(DriverAttributeConfigBO entityBO) {
        checkDuplicate(entityBO, false, true);

        DriverAttributeConfigDO entityDO = driverAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!driverAttributeConfigManager.save(entityDO)) {
            throw new AddException("驱动属性配置创建失败");
        }

        // 通知驱动新增
        entityDO = driverAttributeConfigManager.getById(entityDO.getId());
        entityBO = driverAttributeConfigBuilder.buildBOByDO(entityDO);
        notifyService.notifyDriverDriverAttributeConfig(MetadataCommandTypeEnum.ADD, entityBO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void remove(Long id) {
        DriverAttributeConfigDO entityDO = getDOById(id, true);

        if (!driverAttributeConfigManager.removeById(id)) {
            throw new DeleteException("驱动属性配置删除失败");
        }

        DriverAttributeConfigBO entityBO = driverAttributeConfigBuilder.buildBOByDO(entityDO);
        notifyService.notifyDriverDriverAttributeConfig(MetadataCommandTypeEnum.DELETE, entityBO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(DriverAttributeConfigBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        DriverAttributeConfigDO entityDO = driverAttributeConfigBuilder.buildDOByBO(entityBO);
        entityBO.setOperateTime(null);
        if (!driverAttributeConfigManager.updateById(entityDO)) {
            throw new UpdateException("驱动属性配置更新失败");
        }

        entityDO = driverAttributeConfigManager.getById(entityDO.getId());
        entityBO = driverAttributeConfigBuilder.buildBOByDO(entityDO);
        notifyService.notifyDriverDriverAttributeConfig(MetadataCommandTypeEnum.UPDATE, entityBO);
    }

    @Override
    public DriverAttributeConfigBO selectById(Long id) {
        DriverAttributeConfigDO entityDO = getDOById(id, true);
        return driverAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DriverAttributeConfigBO selectByAttributeIdAndDeviceId(Long deviceId, Long driverAttributeId) {
        LambdaQueryChainWrapper<DriverAttributeConfigDO> wrapper = driverAttributeConfigManager.lambdaQuery().eq(DriverAttributeConfigDO::getDriverAttributeId, driverAttributeId).eq(DriverAttributeConfigDO::getDeviceId, deviceId).last(QueryWrapperConstant.LIMIT_ONE);
        DriverAttributeConfigDO entityDO = wrapper.one();
        return driverAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DriverAttributeConfigBO> selectByAttributeId(Long driverAttributeId) {
        LambdaQueryChainWrapper<DriverAttributeConfigDO> wrapper = driverAttributeConfigManager.lambdaQuery().eq(DriverAttributeConfigDO::getDriverAttributeId, driverAttributeId);
        List<DriverAttributeConfigDO> entityDO = wrapper.list();
        return driverAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DriverAttributeConfigBO> selectByDeviceId(Long deviceId) {
        LambdaQueryChainWrapper<DriverAttributeConfigDO> wrapper = driverAttributeConfigManager.lambdaQuery().eq(DriverAttributeConfigDO::getDeviceId, deviceId);
        List<DriverAttributeConfigDO> entityDO = wrapper.list();
        return driverAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Page<DriverAttributeConfigBO> selectByPage(DriverAttributeConfigQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<DriverAttributeConfigDO> entityPageDO = driverAttributeConfigManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return driverAttributeConfigBuilder.buildBOPageByDOPage(entityPageDO);
    }

    private LambdaQueryWrapper<DriverAttributeConfigDO> fuzzyQuery(DriverAttributeConfigQuery query) {
        LambdaQueryWrapper<DriverAttributeConfigDO> wrapper = Wrappers.<DriverAttributeConfigDO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            wrapper.eq(ObjectUtil.isNotEmpty(query.getDriverAttributeId()), DriverAttributeConfigDO::getDriverAttributeId, query.getDriverAttributeId());
            wrapper.eq(ObjectUtil.isNotEmpty(query.getDeviceId()), DriverAttributeConfigDO::getDeviceId, query.getDeviceId());
        }
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link DriverAttributeConfigBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(DriverAttributeConfigBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<DriverAttributeConfigDO> wrapper = Wrappers.<DriverAttributeConfigDO>query().lambda();
        wrapper.eq(DriverAttributeConfigDO::getDriverAttributeId, entityBO.getDriverAttributeId());
        wrapper.eq(DriverAttributeConfigDO::getDeviceId, entityBO.getDeviceId());
        wrapper.eq(DriverAttributeConfigDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DriverAttributeConfigDO one = driverAttributeConfigManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("驱动属性配置重复");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link DriverAttributeConfigDO}
     */
    private DriverAttributeConfigDO getDOById(Long id, boolean throwException) {
        DriverAttributeConfigDO entityDO = driverAttributeConfigManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("驱动属性配置不存在");
        }
        return entityDO;
    }

}
