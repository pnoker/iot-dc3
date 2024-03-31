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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.center.manager.dal.OutputProtocolManager;
import io.github.pnoker.center.manager.entity.model.OutputProtocolDO;
import io.github.pnoker.center.manager.service.OutputProtocolService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 * OutputProtocol Service Impl
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class OutputProtocolServiceImpl implements OutputProtocolService {

    @Resource
    private OutputProtocolManager outputProtocolManager;

    @Override
    public void save(OutputProtocolDO entityDO) {
        checkDuplicate(entityDO, false, true);

        if (!outputProtocolManager.save(entityDO)) {
            throw new AddException("分组创建失败");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!outputProtocolManager.removeById(id)) {
            throw new DeleteException("分组删除失败");
        }
    }

    @Override
    public void update(OutputProtocolDO entityDO) {
        getDOById(Long.valueOf(entityDO.getId()), true);

        checkDuplicate(entityDO, true, true);

        entityDO.setUpdateTime(null);
        if (!outputProtocolManager.updateById(entityDO)) {
            throw new UpdateException("分组更新失败");
        }
    }

    @Override
    public OutputProtocolDO selectById(Long id) {
        return getDOById(id, true);
    }

    @Override
    public Page<OutputProtocolDO> selectByPage(OutputProtocolDO entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return outputProtocolManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link OutputProtocolDO}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<OutputProtocolDO> fuzzyQuery(OutputProtocolDO entityQuery) {
        LambdaQueryWrapper<OutputProtocolDO> wrapper = Wrappers.<OutputProtocolDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getUrl()), OutputProtocolDO::getUrl, entityQuery.getUrl());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityDO       {@link OutputProtocolDO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(OutputProtocolDO entityDO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<OutputProtocolDO> wrapper = Wrappers.<OutputProtocolDO>query().lambda();
        wrapper.eq(OutputProtocolDO::getUrl, entityDO.getUrl());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        OutputProtocolDO one = outputProtocolManager.getOne(wrapper);
        if (ObjectUtil.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityDO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("分组重复");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link OutputProtocolDO}
     */
    private OutputProtocolDO getDOById(Long id, boolean throwException) {
        OutputProtocolDO entityDO = outputProtocolManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("分组不存在");
        }
        return entityDO;
    }

}
