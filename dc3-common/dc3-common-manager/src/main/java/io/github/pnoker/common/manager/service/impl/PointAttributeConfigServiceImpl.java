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

import cn.hutool.core.collection.CollUtil;
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
import io.github.pnoker.common.manager.dal.PointAttributeConfigManager;
import io.github.pnoker.common.manager.entity.bo.PointAttributeConfigBO;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.builder.PointAttributeConfigBuilder;
import io.github.pnoker.common.manager.entity.model.PointAttributeConfigDO;
import io.github.pnoker.common.manager.entity.query.PointAttributeConfigQuery;
import io.github.pnoker.common.manager.event.metadata.MetadataEventPublisher;
import io.github.pnoker.common.manager.service.PointAttributeConfigService;
import io.github.pnoker.common.manager.service.PointService;
import io.github.pnoker.common.utils.FieldUtil;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PointConfigService Impl
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
    private MetadataEventPublisher metadataEventPublisher;

    @Resource
    private PointService pointService;

    @Override
    public void save(PointAttributeConfigBO entityBO) {
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create point attribute config: point attribute config has been duplicated");
        }

        PointAttributeConfigDO entityDO = pointAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!pointAttributeConfigManager.save(entityDO)) {
            throw new AddException("Failed to create point attribute config");
        }

        // 通知驱动
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public PointAttributeConfigBO innerSave(PointAttributeConfigBO entityBO) {
        if (checkDuplicate(entityBO, false)) {
            throw new DuplicateException("Failed to create point attribute config: point attribute config has been duplicated");
        }

        PointAttributeConfigDO entityDO = pointAttributeConfigBuilder.buildDOByBO(entityBO);
        if (!pointAttributeConfigManager.save(entityDO)) {
            throw new AddException("Failed to create point attribute config");
        }

        return pointAttributeConfigBuilder.buildBOByDO(entityDO);
    }

    @Override
    public void remove(Long id) {
        PointAttributeConfigDO entityDO = getDOById(id, true);

        if (!pointAttributeConfigManager.removeById(id)) {
            throw new DeleteException("Failed to remove point attribute config");
        }

        // 通知驱动
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
    }

    @Override
    public void update(PointAttributeConfigBO entityBO) {
        getDOById(entityBO.getId(), true);

        if (checkDuplicate(entityBO, true)) {
            throw new DuplicateException("Failed to update point attribute config: point attribute config has been duplicated");
        }

        PointAttributeConfigDO entityDO = pointAttributeConfigBuilder.buildDOByBO(entityBO);
        entityBO.setOperateTime(null);
        if (!pointAttributeConfigManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update point attribute config");
        }

        // 通知驱动
        MetadataEvent metadataEvent = new MetadataEvent(this, entityDO.getDeviceId(), MetadataTypeEnum.DEVICE, MetadataOperateTypeEnum.UPDATE);
        metadataEventPublisher.publishEvent(metadataEvent);
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
        List<PointBO> pointBOList = pointService.selectByDeviceId(deviceId);
        Set<Long> pointIds = pointBOList.stream().map(PointBO::getId).collect(Collectors.toSet());
        if (CollUtil.isEmpty(pointIds)) {
            return Collections.emptyList();
        }

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
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<PointAttributeConfigDO> entityPageDO = pointAttributeConfigManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return pointAttributeConfigBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link PointAttributeConfigQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<PointAttributeConfigDO> fuzzyQuery(PointAttributeConfigQuery entityQuery) {
        LambdaQueryWrapper<PointAttributeConfigDO> wrapper = Wrappers.<PointAttributeConfigDO>query().lambda();
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getPointAttributeId()), PointAttributeConfigDO::getPointAttributeId, entityQuery.getPointAttributeId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getDeviceId()), PointAttributeConfigDO::getDeviceId, entityQuery.getDeviceId());
        wrapper.eq(FieldUtil.isValidIdField(entityQuery.getPointId()), PointAttributeConfigDO::getPointId, entityQuery.getPointId());
        wrapper.eq(PointAttributeConfigDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO {@link PointAttributeConfigBO}
     * @param isUpdate 是否为更新操作
     * @return 是否重复
     */
    private boolean checkDuplicate(PointAttributeConfigBO entityBO, boolean isUpdate) {
        LambdaQueryWrapper<PointAttributeConfigDO> wrapper = Wrappers.<PointAttributeConfigDO>query().lambda();
        wrapper.eq(PointAttributeConfigDO::getPointAttributeId, entityBO.getPointAttributeId());
        wrapper.eq(PointAttributeConfigDO::getDeviceId, entityBO.getDeviceId());
        wrapper.eq(PointAttributeConfigDO::getPointId, entityBO.getPointId());
        wrapper.eq(PointAttributeConfigDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        PointAttributeConfigDO one = pointAttributeConfigManager.getOne(wrapper);
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
     * @return {@link PointAttributeConfigDO}
     */
    private PointAttributeConfigDO getDOById(Long id, boolean throwException) {
        PointAttributeConfigDO entityDO = pointAttributeConfigManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Point attribute config does not exist");
        }
        return entityDO;
    }

}
