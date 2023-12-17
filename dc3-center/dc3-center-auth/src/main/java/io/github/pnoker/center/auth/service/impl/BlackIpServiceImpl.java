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
import io.github.pnoker.center.auth.entity.bo.BlackIpBO;
import io.github.pnoker.center.auth.entity.builder.BlackIpBuilder;
import io.github.pnoker.center.auth.entity.model.BlackIpDO;
import io.github.pnoker.center.auth.entity.query.BlackIpQuery;
import io.github.pnoker.center.auth.manager.BlackIpManager;
import io.github.pnoker.center.auth.service.BlackIpService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.constant.enums.EnableFlagEnum;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 用户服务接口实现类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class BlackIpServiceImpl implements BlackIpService {

    @Resource
    private BlackIpBuilder blackIpBuilder;

    @Resource
    private BlackIpManager blackIpManager;


    @Override
    public void save(BlackIpBO entityBO) {
        BlackIpBO select = selectByIp(entityBO.getIp());
        if (ObjectUtil.isNotNull(select)) {
            throw new DuplicateException("The ip already exists in the blacklist");
        }
        BlackIpDO entityDO = blackIpBuilder.buildDOByBO(entityBO);
        if (!blackIpManager.save(entityDO)) {
            throw new AddException("The ip {} add to the blacklist failed", entityBO.getIp());
        }
    }

    @Override
    public void remove(Long id) {
        BlackIpBO blackIpBO = selectById(id);
        if (ObjectUtil.isNull(blackIpBO)) {
            throw new NotFoundException("The ip does not exist in the blacklist");
        }

        if (!blackIpManager.removeById(id)) {
            throw new DeleteException("The ip delete failed");
        }
    }

    @Override
    public void update(BlackIpBO entityBO) {
        entityBO.setIp(null);
        entityBO.setOperateTime(null);
        BlackIpDO entityDO = blackIpBuilder.buildDOByBO(entityBO);
        if (blackIpManager.updateById(entityDO)) {
            throw new UpdateException("The ip update failed in the blacklist");
        }
    }

    @Override
    public BlackIpBO selectById(Long id) {
        return null;
    }

    @Override
    public BlackIpBO selectByIp(String ip) {
        LambdaQueryWrapper<BlackIpDO> wrapper = Wrappers.<BlackIpDO>query().lambda();
        wrapper.eq(BlackIpDO::getIp, ip);
        wrapper.eq(BlackIpDO::getEnableFlag, EnableFlagEnum.ENABLE);
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        BlackIpDO entityDO = blackIpManager.getOne(wrapper);
        return blackIpBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<BlackIpBO> selectByPage(BlackIpQuery entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<BlackIpDO> entityPageDO = blackIpManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return blackIpBuilder.buildBOPageByDOPage(entityPageDO);
    }

    @Override
    public Boolean checkBlackIpValid(String ip) {
        BlackIpBO blackIpBO = selectByIp(ip);
        return ObjectUtil.isNotNull(blackIpBO);
    }

    private LambdaQueryWrapper<BlackIpDO> fuzzyQuery(BlackIpQuery query) {
        LambdaQueryWrapper<BlackIpDO> wrapper = Wrappers.<BlackIpDO>query().lambda();
        if (ObjectUtil.isNotNull(query)) {
            wrapper.like(CharSequenceUtil.isNotEmpty(query.getIp()), BlackIpDO::getIp, query.getIp());
        }
        return wrapper;
    }

}
