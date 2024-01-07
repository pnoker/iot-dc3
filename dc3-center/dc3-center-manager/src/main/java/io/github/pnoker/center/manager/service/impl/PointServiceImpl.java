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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.biz.DriverNotifyService;
import io.github.pnoker.center.manager.dal.PointManager;
import io.github.pnoker.center.manager.dal.ProfileBindManager;
import io.github.pnoker.center.manager.entity.bo.PointBO;
import io.github.pnoker.center.manager.entity.builder.PointBuilder;
import io.github.pnoker.center.manager.entity.model.PointDO;
import io.github.pnoker.center.manager.entity.model.ProfileBindDO;
import io.github.pnoker.center.manager.entity.query.PointQuery;
import io.github.pnoker.center.manager.mapper.PointMapper;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.constant.enums.MetadataCommandTypeEnum;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PointService Impl
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class PointServiceImpl implements PointService {

    @Resource
    private PointBuilder pointBuilder;

    @Resource
    private PointManager pointManager;
    @Resource
    private ProfileBindManager profileBindManager;

    @Resource
    private PointMapper pointMapper;

    @Resource
    private DriverNotifyService driverNotifyService;

    @Override
    public void save(PointBO entityBO) {
        checkDuplicate(entityBO, false, true);

        PointDO entityDO = pointBuilder.buildDOByBO(entityBO);
        if (!pointManager.save(entityDO)) {
            throw new AddException("位号创建失败");
        }

        // 通知驱动新增
        entityDO = pointManager.getById(entityDO.getId());
        entityBO = pointBuilder.buildBOByDO(entityDO);
        driverNotifyService.notifyPoint(MetadataCommandTypeEnum.ADD, entityBO);
    }

    @Override
    public void remove(Long id) {
        PointDO entityDO = getDOById(id, true);

        if (!pointManager.removeById(id)) {
            throw new DeleteException("位号删除失败");
        }

        PointBO entityBO = pointBuilder.buildBOByDO(entityDO);
        driverNotifyService.notifyPoint(MetadataCommandTypeEnum.DELETE, entityBO);
    }

    @Override
    public void update(PointBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        PointDO entityDO = pointBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!pointManager.updateById(entityDO)) {
            throw new UpdateException("位号更新失败");
        }

        entityDO = pointManager.getById(entityDO.getId());
        entityBO = pointBuilder.buildBOByDO(entityDO);
        driverNotifyService.notifyPoint(MetadataCommandTypeEnum.UPDATE, entityBO);
    }

    @Override
    public PointBO selectById(Long id) {
        PointDO entityDO = getDOById(id, true);
        return pointBuilder.buildBOByDO(entityDO);
    }

    @Override
    public List<PointBO> selectByIds(Set<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<PointDO> entityDOS = pointManager.listByIds(ids);
        return pointBuilder.buildBOListByDOList(entityDOS);
    }

    @Override
    public List<PointBO> selectByDeviceId(Long deviceId) {
        LambdaQueryChainWrapper<ProfileBindDO> wrapper = profileBindManager.lambdaQuery().eq(ProfileBindDO::getDeviceId, deviceId);
        List<ProfileBindDO> entityDOS = wrapper.list();
        Set<Long> profileIds = entityDOS.stream().map(ProfileBindDO::getProfileId).collect(Collectors.toSet());
        return selectByProfileIds(profileIds);
    }

    @Override
    public List<PointBO> selectByProfileId(Long profileId) {
        LambdaQueryChainWrapper<PointDO> wrapper = pointManager.lambdaQuery().eq(PointDO::getProfileId, profileId);
        List<PointDO> entityDOS = wrapper.list();
        return pointBuilder.buildBOListByDOList(entityDOS);
    }

    @Override
    public List<PointBO> selectByProfileIds(Set<Long> profileIds) {
        LambdaQueryChainWrapper<PointDO> wrapper = pointManager.lambdaQuery().in(PointDO::getProfileId, profileIds);
        List<PointDO> entityDOS = wrapper.list();
        return pointBuilder.buildBOListByDOList(entityDOS);
    }

    @Override
    public Page<PointBO> selectByPage(PointQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<PointDO> entityPageDO = pointMapper.selectPageWithDevice(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery), entityQuery.getDeviceId());
        return pointBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public Map<Long, String> unit(Set<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyMap();
        }
        List<PointDO> pointDOS = pointManager.listByIds(ids);
        return pointDOS.stream().collect(Collectors.toMap(PointDO::getId, PointDO::getUnit));
    }

    private LambdaQueryWrapper<PointDO> fuzzyQuery(PointQuery entityQuery) {
        QueryWrapper<PointDO> wrapper = Wrappers.query();
        wrapper.eq("dp.deleted", 0);
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getPointName()), "dp.point_name", entityQuery.getPointName());
        wrapper.eq(ObjectUtil.isNotEmpty(entityQuery.getPointCode()), "dp.point_code", entityQuery.getPointTypeFlag());
        wrapper.eq(ObjectUtil.isNotEmpty(entityQuery.getPointTypeFlag()), "dp.point_type_flag", entityQuery.getPointTypeFlag());
        wrapper.eq(ObjectUtil.isNotNull(entityQuery.getRwFlag()), "dp.rw_flag", entityQuery.getRwFlag());
        wrapper.eq(ObjectUtil.isNotEmpty(entityQuery.getProfileId()), "dp.profile_id", entityQuery.getProfileId());
        wrapper.eq(ObjectUtil.isNotNull(entityQuery.getEnableFlag()), "dp.enable_flag", entityQuery.getEnableFlag());
        wrapper.eq("dp.tenant_id", entityQuery.getTenantId());
        return wrapper.lambda();
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link PointBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(PointBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<PointDO> wrapper = Wrappers.<PointDO>query().lambda();
        wrapper.eq(PointDO::getPointName, entityBO.getPointName());
        wrapper.eq(PointDO::getPointCode, entityBO.getPointCode());
        wrapper.eq(PointDO::getProfileId, entityBO.getProfileId());
        wrapper.eq(PointDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        PointDO one = pointManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("位号重复");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link PointDO}
     */
    private PointDO getDOById(Long id, boolean throwException) {
        PointDO entityDO = pointManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("位号不存在");
        }
        return entityDO;
    }

}
