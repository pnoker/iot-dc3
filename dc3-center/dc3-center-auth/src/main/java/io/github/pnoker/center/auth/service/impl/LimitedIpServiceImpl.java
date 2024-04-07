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

package io.github.pnoker.center.auth.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.auth.dal.LimitedIpManager;
import io.github.pnoker.center.auth.entity.bo.LimitedIpBO;
import io.github.pnoker.center.auth.entity.builder.LimitedIpBuilder;
import io.github.pnoker.center.auth.entity.model.LimitedIpDO;
import io.github.pnoker.center.auth.entity.query.LimitedIpQuery;
import io.github.pnoker.center.auth.service.LimitedIpService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.EnableFlagEnum;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 限制IP服务接口实现类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class LimitedIpServiceImpl implements LimitedIpService {

    @Resource
    private LimitedIpBuilder limitedIpBuilder;

    @Resource
    private LimitedIpManager limitedIpManager;

    @Override
    public void save(LimitedIpBO entityBO) {
        checkDuplicate(entityBO, false, true);

        LimitedIpDO entityDO = limitedIpBuilder.buildDOByBO(entityBO);
        if (!limitedIpManager.save(entityDO)) {
            throw new AddException("The ip {} add to the blacklist failed", entityBO.getIp());
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!limitedIpManager.removeById(id)) {
            throw new DeleteException("The ip delete failed");
        }
    }

    @Override
    public void update(LimitedIpBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        LimitedIpDO entityDO = limitedIpBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (limitedIpManager.updateById(entityDO)) {
            throw new UpdateException("限制IP更新失败");
        }
    }

    @Override
    public LimitedIpBO selectById(Long id) {
        LimitedIpDO entityDO = getDOById(id, true);
        return limitedIpBuilder.buildBOByDO(entityDO);
    }

    @Override
    public LimitedIpBO selectByIp(String ip) {
        LambdaQueryWrapper<LimitedIpDO> wrapper = Wrappers.<LimitedIpDO>query().lambda();
        wrapper.eq(LimitedIpDO::getIp, ip);
        wrapper.eq(LimitedIpDO::getEnableFlag, EnableFlagEnum.ENABLE);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        LimitedIpDO entityDO = limitedIpManager.getOne(wrapper);
        return limitedIpBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<LimitedIpBO> selectByPage(LimitedIpQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<LimitedIpDO> entityPageDO = limitedIpManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return limitedIpBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public Boolean checkValid(String ip) {
        LimitedIpBO limitedIpBO = selectByIp(ip);
        return ObjectUtil.isNotNull(limitedIpBO);
    }

    private LambdaQueryWrapper<LimitedIpDO> fuzzyQuery(LimitedIpQuery entityQuery) {
        LambdaQueryWrapper<LimitedIpDO> wrapper = Wrappers.<LimitedIpDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getIp()), LimitedIpDO::getIp, entityQuery.getIp());
        wrapper.eq(LimitedIpDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link LimitedIpBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(LimitedIpBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<LimitedIpDO> wrapper = Wrappers.<LimitedIpDO>query().lambda();
        wrapper.eq(LimitedIpDO::getIp, entityBO.getIp());
        wrapper.eq(LimitedIpDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        LimitedIpDO one = limitedIpManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("限制IP重复");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link LimitedIpDO}
     */
    private LimitedIpDO getDOById(Long id, boolean throwException) {
        LimitedIpDO entityDO = limitedIpManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("受限IP不存在");
        }
        return entityDO;
    }

}
