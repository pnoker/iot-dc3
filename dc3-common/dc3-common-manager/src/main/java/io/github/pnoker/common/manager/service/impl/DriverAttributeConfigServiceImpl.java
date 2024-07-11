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
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.manager.dal.DriverAttributeConfigManager;
import io.github.pnoker.common.manager.entity.bo.DriverAttributeConfigBO;
import io.github.pnoker.common.manager.entity.builder.DriverAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.model.DriverAttributeConfigDO;
import io.github.pnoker.common.manager.entity.query.DriverAttributeConfigQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.service.DriverAttributeConfigService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * DriverConfigService Impl
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
    private DriverAttributeConfigManager driverAttributeConfigManager;

    @Resource
    private MetadataEventPublisher metadataEventPublisher;

    @Override
    public void save(DriverAttributeConfigBO entityBO) {
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create driver attribute config: driver attribute config has been duplicated");
        }

        DriverAttributeConfigDO entityDO = driverAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!driverAttributeConfigManager.save(entityDO)) {
            throw new AddException("Failed to create driver attribute config");
        }

        // 通知驱动
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public DriverAttributeConfigBO innerSave(DriverAttributeConfigBO entityBO) {
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create driver attribute config: driver attribute config has been duplicated");
        }

        DriverAttributeConfigDO entityDO = driverAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!driverAttributeConfigManager.save(entityDO)) {
            throw new AddException("Failed to create driver attribute config");
        }

        return driverAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public void remove(Long id) {
        DriverAttributeConfigDO entityDO = getDOById(id, true);

        if (!driverAttributeConfigManager.removeById(id)) {
            throw new DeleteException("Failed to remove driver attribute config");
        }

        // 通知驱动
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public void update(DriverAttributeConfigBO entityBO) {
        getDOById(entityBO.getId(), true);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException("Failed to update driver attribute config: driver attribute config has been duplicated");
        }

        DriverAttributeConfigDO entityDO = driverAttributeConfigBuilder.buildDOByBO(entityBO);
        entityBO.setOperateTime(null);
        if (!driverAttributeConfigManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update driver attribute config");
        }

        // 通知驱动
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public DriverAttributeConfigBO selectById(Long id) {
        DriverAttributeConfigDO entityDO = getDOById(id, true);
        return driverAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public DriverAttributeConfigBO selectByAttributeIdAndDeviceId(Long deviceId, Long attributeId) {
        LambdaQueryChainWrapper<DriverAttributeConfigDO> wrapper = driverAttributeConfigManager.lambdaQuery()
                .eq(DriverAttributeConfigDO::getDriverAttributeId, attributeId)
                .eq(DriverAttributeConfigDO::getDeviceId, deviceId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        DriverAttributeConfigDO entityDO = wrapper.one();
        return driverAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<DriverAttributeConfigBO> selectByAttributeId(Long attributeId) {
        LambdaQueryChainWrapper<DriverAttributeConfigDO> wrapper = driverAttributeConfigManager.lambdaQuery()
                .eq(DriverAttributeConfigDO::getDriverAttributeId, attributeId);
        List<DriverAttributeConfigDO> entityDO = wrapper.list();
        return driverAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public List<DriverAttributeConfigBO> selectByDeviceId(Long deviceId) {
        LambdaQueryChainWrapper<DriverAttributeConfigDO> wrapper = driverAttributeConfigManager.lambdaQuery()
                .eq(DriverAttributeConfigDO::getDeviceId, deviceId);
        List<DriverAttributeConfigDO> entityDO = wrapper.list();
        return driverAttributeConfigBuilder.buildBOListByDOList(entityDO);
    }

    @Override
    public Page<DriverAttributeConfigBO> selectByPage(DriverAttributeConfigQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<DriverAttributeConfigDO> entityPageDO = driverAttributeConfigManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return driverAttributeConfigBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link DriverAttributeConfigQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<DriverAttributeConfigDO> fuzzyQuery(DriverAttributeConfigQuery entityQuery) {
        LambdaQueryWrapper<DriverAttributeConfigDO> wrapper = Wrappers.<DriverAttributeConfigDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDriverAttributeId()), DriverAttributeConfigDO::getDriverAttributeId, entityQuery.getDriverAttributeId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDeviceId()), DriverAttributeConfigDO::getDeviceId, entityQuery.getDeviceId());
        wrapper.eq(DriverAttributeConfigDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO {@link DriverAttributeConfigBO}
     * @param isUpdate 是否为更新操作
     * @return 是否重复
     */
    private boolean checkDuplicate(DriverAttributeConfigBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<DriverAttributeConfigDO> wrapper = Wrappers.<DriverAttributeConfigDO>query().lambda();
        wrapper.eq(DriverAttributeConfigDO::getDriverAttributeId, entityBO.getDriverAttributeId());
        wrapper.eq(DriverAttributeConfigDO::getDeviceId, entityBO.getDeviceId());
        wrapper.eq(DriverAttributeConfigDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        DriverAttributeConfigDO one = driverAttributeConfigManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        return !isUpdate || !one.getId().equals(entityBO.getId());
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
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Driver attribute does not exist");
        }
        return entityDO;
    }

}
