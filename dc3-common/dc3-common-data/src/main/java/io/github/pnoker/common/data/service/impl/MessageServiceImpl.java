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

package io.github.pnoker.common.data.service.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.data.dal.MessageManager;
import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.builder.MessageBuilder;
import io.github.pnoker.common.data.entity.model.MessageDO;
import io.github.pnoker.common.data.entity.query.MessageQuery;
import io.github.pnoker.common.data.service.MessageService;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.exception.*;
import io.github.pnoker.common.utils.PageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * <p>
 * AlarmMessageProfile Service Impl
 * </p>
 *
 * @author pnoker
 * @version 2025.6.1
 * @since 2022.1.0
 */
@Slf4j
@Service
public class MessageServiceImpl implements MessageService {

    @Resource
    private MessageBuilder messageBuilder;

    @Resource
    private MessageManager messageManager;

    @Override
    public void save(MessageBO entityBO) {
        checkDuplicate(entityBO, false, true);

        MessageDO entityDO = messageBuilder.buildDOByBO(entityBO);
        if (!messageManager.save(entityDO)) {
            throw new AddException("Failed to create group");
        }
    }

    @Override
    public void remove(Long id) {
        getDOById(id, true);

        // 删除分组之前需要检查该分组是否存在关联
        LambdaQueryChainWrapper<MessageDO> wrapper = messageManager.lambdaQuery()
                .eq(MessageDO::getTenantId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove group: there are subgroups under the group");
        }

        if (!messageManager.removeById(id)) {
            throw new DeleteException("Failed to remove group");
        }
    }

    @Override
    public void update(MessageBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        MessageDO entityDO = messageBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!messageManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update group");
        }
    }

    @Override
    public MessageBO selectById(Long id) {
        MessageDO entityDO = getDOById(id, true);
        return messageBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<MessageBO> selectByPage(MessageQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<MessageDO> entityPageDO = messageManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return messageBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * 构造模糊查询
     *
     * @param entityQuery {@link MessageQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<MessageDO> fuzzyQuery(MessageQuery entityQuery) {
        LambdaQueryWrapper<MessageDO> wrapper = Wrappers.<MessageDO>query().lambda();
        wrapper.like(CharSequenceUtil.isNotEmpty(entityQuery.getAlarmMessageTitle()), MessageDO::getMessageName, entityQuery.getAlarmMessageTitle());
        wrapper.eq(MessageDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * 重复性校验
     *
     * @param entityBO       {@link MessageBO}
     * @param isUpdate       是否为更新操作
     * @param throwException 如果重复是否抛异常
     * @return 是否重复
     */
    private boolean checkDuplicate(MessageBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<MessageDO> wrapper = Wrappers.<MessageDO>query().lambda();
        wrapper.eq(MessageDO::getMessageName, entityBO.getMessageName());
        wrapper.eq(MessageDO::getMessageCode, entityBO.getMessageCode());
        wrapper.eq(MessageDO::getTenantId, entityBO.getTenantId());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        MessageDO one = messageManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Alarm message profile has been duplicated");
        }
        return duplicate;
    }

    /**
     * 根据 主键ID 获取
     *
     * @param id             ID
     * @param throwException 是否抛异常
     * @return {@link MessageDO}
     */
    private MessageDO getDOById(Long id, boolean throwException) {
        MessageDO entityDO = messageManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Alarm message profile does not exist");
        }
        return entityDO;
    }

}
