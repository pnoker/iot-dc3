/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.data.service.impl;

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
import io.github.pnoker.common.exception.AddException;
import io.github.pnoker.common.exception.AssociatedException;
import io.github.pnoker.common.exception.DeleteException;
import io.github.pnoker.common.exception.DuplicateException;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.UpdateException;
import io.github.pnoker.common.utils.PageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**

 * Business service implementation for alarm message template operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageBuilder messageBuilder;

    private final MessageManager messageManager;

    private final io.github.pnoker.common.data.biz.alarm.NotifyConfigCache notifyConfigCache;

    @Override
    public void add(MessageBO entityBO) {
        checkDuplicate(entityBO, false, true);

        MessageDO entityDO = messageBuilder.buildDOByBO(entityBO);
        if (!messageManager.save(entityDO)) {
            throw new AddException("Failed to create group");
        }
        notifyConfigCache.invalidateMessage(entityDO.getId());
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        //
        LambdaQueryChainWrapper<MessageDO> wrapper = messageManager.lambdaQuery().eq(MessageDO::getTenantId, id);
        long count = wrapper.count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove group: there are subgroups under the group");
        }

        if (!messageManager.removeById(id)) {
            throw new DeleteException("Failed to remove group");
        }
        notifyConfigCache.invalidateMessage(id);
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
        notifyConfigCache.invalidateMessage(entityBO.getId());
    }

    @Override
    public MessageBO getById(Long id) {
        MessageDO entityDO = getDOById(id, true);
        return messageBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<MessageBO> list(MessageQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<MessageDO> entityPageDO = messageManager.page(PageUtil.page(entityQuery.getPage()),
                fuzzyQuery(entityQuery));
        return messageBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * @param entityQuery {@link MessageQuery}
     * @return {@link LambdaQueryWrapper}
     */
    private LambdaQueryWrapper<MessageDO> fuzzyQuery(MessageQuery entityQuery) {
        LambdaQueryWrapper<MessageDO> wrapper = Wrappers.<MessageDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getMessageName()), MessageDO::getMessageName,
                entityQuery.getMessageName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getMessageCode()), MessageDO::getMessageCode,
                entityQuery.getMessageCode());
        wrapper.eq(Objects.nonNull(entityQuery.getMessageLevel()), MessageDO::getMessageLevel,
                Objects.isNull(entityQuery.getMessageLevel()) ? null : entityQuery.getMessageLevel().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), MessageDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getTenantId()), MessageDO::getTenantId, entityQuery.getTenantId());
        return wrapper;
    }

    /**
     * @param entityBO       {@link MessageBO}
     * @param isUpdate
     * @param throwException
     * @return
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
     * Primary key ID
     *
     * @param id             ID
     * @param throwException
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
