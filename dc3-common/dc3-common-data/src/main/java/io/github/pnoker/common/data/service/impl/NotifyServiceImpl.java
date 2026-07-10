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
import io.github.pnoker.common.data.biz.alarm.NotifyConfigCache;
import io.github.pnoker.common.data.dal.NotifyChannelBindManager;
import io.github.pnoker.common.data.dal.NotifyManager;
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.builder.NotifyBuilder;
import io.github.pnoker.common.data.entity.model.NotifyChannelBindDO;
import io.github.pnoker.common.data.entity.model.NotifyDO;
import io.github.pnoker.common.data.entity.query.NotifyQuery;
import io.github.pnoker.common.data.service.NotifyService;
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
 * Business service implementation for alarm notification template operations.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyServiceImpl implements NotifyService {

    private final NotifyBuilder notifyBuilder;

    private final NotifyManager notifyManager;

    private final NotifyChannelBindManager notifyChannelBindManager;

    private final NotifyConfigCache notifyConfigCache;

    @Override
    public void add(NotifyBO entityBO) {
        checkDuplicate(entityBO, false, true);

        NotifyDO entityDO = notifyBuilder.buildDOByBO(entityBO);
        if (!notifyManager.save(entityDO)) {
            throw new AddException("Failed to create alarm notify profile");
        }
        notifyConfigCache.invalidateNotify(entityDO.getId());
    }

    @Override
    public void delete(Long id) {
        getDOById(id, true);

        long count = notifyChannelBindManager.lambdaQuery().eq(NotifyChannelBindDO::getNotifyId, id).count();
        if (count > 0) {
            throw new AssociatedException("Failed to remove alarm notify profile: notify channel bindings exist");
        }

        if (!notifyManager.removeById(id)) {
            throw new DeleteException("Failed to remove alarm notify profile");
        }
        notifyConfigCache.invalidateNotify(id);
    }

    @Override
    public void update(NotifyBO entityBO) {
        getDOById(entityBO.getId(), true);

        checkDuplicate(entityBO, true, true);

        NotifyDO entityDO = notifyBuilder.buildDOByBO(entityBO);
        entityDO.setOperateTime(null);
        if (!notifyManager.updateById(entityDO)) {
            throw new UpdateException("Failed to update alarm notify profile");
        }
        notifyConfigCache.invalidateNotify(entityBO.getId());
    }

    @Override
    public NotifyBO getById(Long id) {
        NotifyDO entityDO = getDOById(id, true);
        return notifyBuilder.buildBOByDO(entityDO);
    }

    @Override
    public Page<NotifyBO> list(NotifyQuery entityQuery) {
        if (Objects.isNull(entityQuery.getPage())) {
            entityQuery.setPage(new Pages());
        }
        Page<NotifyDO> entityPageDO = notifyManager.page(PageUtil.page(entityQuery.getPage()), fuzzyQuery(entityQuery));
        return notifyBuilder.buildBOPageByDOPage(entityPageDO);
    }

    /**
     * Build fuzzy query wrapper for alarm notify search.
     *
     * @param entityQuery {@link NotifyQuery} query parameters
     * @return {@link LambdaQueryWrapper} for {@link NotifyDO}
     */
    private LambdaQueryWrapper<NotifyDO> fuzzyQuery(NotifyQuery entityQuery) {
        LambdaQueryWrapper<NotifyDO> wrapper = Wrappers.<NotifyDO>query().lambda();
        wrapper.like(StringUtils.isNotEmpty(entityQuery.getNotifyName()), NotifyDO::getNotifyName,
                entityQuery.getNotifyName());
        wrapper.eq(StringUtils.isNotEmpty(entityQuery.getNotifyCode()), NotifyDO::getNotifyCode,
                entityQuery.getNotifyCode());
        wrapper.eq(Objects.nonNull(entityQuery.getAutoConfirmFlag()), NotifyDO::getAutoConfirmFlag,
                Objects.isNull(entityQuery.getAutoConfirmFlag()) ? null
                        : entityQuery.getAutoConfirmFlag().getIndex());
        wrapper.eq(Objects.nonNull(entityQuery.getNotifyInterval()), NotifyDO::getNotifyInterval,
                entityQuery.getNotifyInterval());
        wrapper.eq(Objects.nonNull(entityQuery.getEnableFlag()), NotifyDO::getEnableFlag,
                Objects.isNull(entityQuery.getEnableFlag()) ? null : entityQuery.getEnableFlag().getIndex());
        return wrapper;
    }

    /**
     * Check whether an alarm notify is duplicated by notify name and code.
     *
     * @param entityBO       {@link NotifyBO} to be validated
     * @param isUpdate       whether the operation is an update (true) or create (false)
     * @param throwException whether to throw {@link DuplicateException} when duplicated
     * @return {@code true} if duplicated, otherwise {@code false}
     */
    private boolean checkDuplicate(NotifyBO entityBO, boolean isUpdate, boolean throwException) {
        LambdaQueryWrapper<NotifyDO> wrapper = Wrappers.<NotifyDO>query().lambda();
        wrapper.eq(NotifyDO::getNotifyName, entityBO.getNotifyName());
        wrapper.eq(NotifyDO::getNotifyCode, entityBO.getNotifyCode());
        wrapper.last(QueryWrapperConstant.LIMIT_ONE);
        NotifyDO one = notifyManager.getOne(wrapper);
        if (Objects.isNull(one)) {
            return false;
        }
        boolean duplicate = !isUpdate || !one.getId().equals(entityBO.getId());
        if (throwException && duplicate) {
            throw new DuplicateException("Alarm notify profile has been duplicated");
        }
        return duplicate;
    }

    /**
     * Get alarm notify data object by primary key ID.
     *
     * @param id             primary key ID
     * @param throwException whether to throw {@link NotFoundException} when not found
     * @return {@link NotifyDO} if found, otherwise {@code null} when {@code throwException}
     * is false
     */
    private NotifyDO getDOById(Long id, boolean throwException) {
        NotifyDO entityDO = notifyManager.getById(id);
        if (throwException && Objects.isNull(entityDO)) {
            throw new NotFoundException("Alarm notify profile does not exist");
        }
        return entityDO;
    }

}
