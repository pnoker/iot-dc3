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
import io.github.pnoker.center.manager.dal.ProduceManager;
import io.github.pnoker.center.manager.entity.model.ProduceDO;
import io.github.pnoker.center.manager.service.ProduceService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 * Produce Service Impl
 * </p>
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@Service
public class ProduceServiceImpl implements ProduceService {

    @Resource
    private ProduceManager produceManager;

    @Override
    public void save(ProduceDO entityDO) {
        checkDuplicate(entityDO, false, true);

        if (!produceManager.save(entityDO)) {
            throw new AddException("分组创建失败");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        if (!produceManager.removeById(id)) {
            throw new DeleteException("分组删除失败");
        }
    }

    @Override
    public void update(ProduceDO entityDO) {
        getDOById(Long.valueOf(entityDO.getId()), true);

        checkDuplicate(entityDO, true, true);

        entityDO.setUpdateTime(null);
        if (!produceManager.updateById(entityDO)) {
            throw new UpdateException("分组更新失败");
        }
    }

    @Override
    public ProduceDO selectById(Long id) {
        return getDOById(id, true);
    }

    @Override
    public Page<ProduceDO> selectByPage(ProduceDO entityQuery) {
        if (ObjectUtil.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        return produceManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link ProduceDO}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<ProduceDO> fuzzyQuery(ProduceDO entityQuery) {
        LambdaQueryWrapper<ProduceDO> wrapper = Wrappers.<ProduceDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getTopicName()), ProduceDO::getTopicName, entityQuery.getTopicName());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityDO       {@link ProduceDO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(ProduceDO entityDO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<ProduceDO> wrapper = Wrappers.<ProduceDO>query().lambda();
        wrapper.eq(ProduceDO::getTopicName, entityDO.getTopicName());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        ProduceDO one = produceManager.getOne(wrapper);
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
     * @return {@link ProduceDO}
     */
    private ProduceDO getDOById(Long id, boolean throwException) {
        ProduceDO entityDO = produceManager.getById(id);
        if (throwException && ObjectUtil.isNull(entityDO)) {
            throw new NotFoundException("分组不存在");
        }
        return entityDO;
    }

}
