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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.data.dal.NotifyChannelBindManager;
import io.github.pnoker.common.data.dal.NotifyChannelManager;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBuilder;
import io.github.pnoker.common.data.entity.model.NotifyChannelBindDO;
import io.github.pnoker.common.data.entity.model.NotifyChannelDO;
import io.github.pnoker.common.data.entity.query.NotifyChannelQuery;
import io.github.pnoker.common.data.service.NotifyChannelService;
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
 * Notification channel service implementation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyChannelServiceImpl implements NotifyChannelService {

    private final NotifyChannelBuilder notifyChannelBuilder;

    private final NotifyChannelManager notifyChannelManager;

    private final NotifyChannelBindManager notifyChannelBindManager;

    private final io.github.pnoker.common.data.biz.alarm.NotifyConfigCache notifyConfigCache;

    @Override
    public void add(NotifyChannelBO entityBO) {
        checkDuplicate(entityBO, false, true);

        NotifyChannelDO entityDO = notifyChannelBuilder.buildDOByBO(entityBO);
        if (!notifyChannelManager.save(entityDO)) {
            throw new AddException("Failed to create notify channel");
        }
        notifyConfigCache.invalidateChannel(entityDO.getId());
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        long count = notifyChannelBindManager.lambdaQuery().eq(NotifyChannelBindDO::getChannelId, id).count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove notify channel: channel bindings exist");
        }

        if (!notifyChannelManager.removeById(id)) {
            throw new DeleteException("Failed to remove notify channel");
        }
        notifyConfigCache.invalidateChannel(id);
    }

    @Override
    public void update(NotifyChannelBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        NotifyChannelDO entityDO = notifyChannelBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!notifyChannelManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update notify channel");
        }
        notifyConfigCache.invalidateChannel(entityBO.getId());
    }

    @Override
    public NotifyChannelBO getById(Long id) {
        NotifyChannelDO entityDO = getDOById(id, true);
        return notifyChannelBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<NotifyChannelBO> list(NotifyChannelQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<NotifyChannelDO> entityPageDO = notifyChannelManager.page(
                PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return notifyChannelBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for notify channel search.
     *
     * @param entityQuery {@link NotifyChannelQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link NotifyChannelDO}
     */
    private LambdaQueryWrapper<NotifyChannelDO> fuzzyQuery(NotifyChannelQuery entityQuery) {
        LambdaQueryWrapper<NotifyChannelDO> wrapper = Wrappers.<NotifyChannelDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getChannelName()), NotifyChannelDO::getChannelName,
                entityQuery.getChannelName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getChannelCode()), NotifyChannelDO::getChannelCode,
                entityQuery.getChannelCode());
        wrapper.eq(Objects.nonNull(entityQuery.getChannelTypeFlag()), NotifyChannelDO::getChannelTypeFlag,
                Objects.isNull(entityQuery.getChannelTypeFlag()) ? null
                        : entityQuery.getChannelTypeFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), NotifyChannelDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        return wrapper;
    }

    /**
     * Check whether a notify channel is duplicated by channel code.
     *
     * @param entityBO       {@link NotifyChannelBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(NotifyChannelBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<NotifyChannelDO> wrapper = Wrappers.<NotifyChannelDO>query().lambda();
        wrapper.eq(NotifyChannelDO::getChannelCode, entityBO.getChannelCode());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        NotifyChannelDO one = notifyChannelManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Notify channel has been duplicated");
        }
        return duplicate;
    }

    /**
     * Get notify channel data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link NotifyChannelDO} if found, otherwise {@code null} when
     * {@code throwException} is false
     */
    private NotifyChannelDO getDOById(Long id, boolean throwException) {
        NotifyChannelDO entityDO = notifyChannelManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Notify channel does not exist");
        }
        return entityDO;
    }

}
