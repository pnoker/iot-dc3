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
package io.github.pnoker.common.agentic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.github.pnoker.common.agentic.dal.ActionManager;
import io.github.pnoker.common.agentic.entity.bo.ActionBO;
import io.github.pnoker.common.agentic.entity.builder.ActionBuilder;
import io.github.pnoker.common.agentic.entity.model.ActionDO;
import io.github.pnoker.common.agentic.service.ActionService;
import io.github.pnoker.common.constant.common.QueryWrapperConstant;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.enums.AgenticActionStatusEnum;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.facade.api.PointCommandFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Implements agentic action lifecycle: creation, confirmation, rejection, and write-point-value execution.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActionServiceImpl implements ActionService {

    private static final String ACTION_WRITE_POINT_VALUE = "writePointValue";

    private final ActionManager actionManager;
    private final ActionBuilder actionBuilder;

    private final PointCommandFacade pointCommandFacade;

    @Override
    public String createWritePointValueAction(String conversationId, Long deviceId, Long pointId, String value,
                                              RequestHeader.PrincipalHeader header) {
        ActionBO entityBO = new ActionBO();
        entityBO.setActionId(UUID.randomUUID().toString());
        entityBO.setConversationId(conversationId);
        entityBO.setActionType(ACTION_WRITE_POINT_VALUE);
        entityBO.setTitle("Write point value");
        entityBO.setDescription("Write value to device " + deviceId + ", point " + pointId);
        entityBO.setPayload(Map.of("deviceId", deviceId, "pointId", pointId, "value", value));
        entityBO.setStatus(AgenticActionStatusEnum.PENDING);
        entityBO.setExpireTime(LocalDateTime.now().plusMinutes(10));
        entityBO.setTenantId(header.getTenantId());
        entityBO.setUserId(header.getUserId());
        fillCreateAudit(entityBO, header);
        ActionDO entityDO = actionBuilder.buildDOByBO(entityBO);
        actionManager.save(entityDO);
        return entityDO.getActionId();
    }

    @Override
    public List<ActionBO> listPending(String conversationId, RequestHeader.PrincipalHeader header) {
        LambdaQueryWrapper<ActionDO> wrapper = scopedWrapper(header)
                .eq(ActionDO::getConversationId, conversationId)
                .eq(ActionDO::getStatus, AgenticActionStatusEnum.PENDING)
                .ge(ActionDO::getExpireTime, LocalDateTime.now())
                .orderByDesc(ActionDO::getCreateTime);
        return actionBuilder.buildBOListByDOList(actionManager.list(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionBO confirm(String actionId, RequestHeader.PrincipalHeader header) {
        ActionDO action = getPending(actionId, header);
        claimPending(action, header, AgenticActionStatusEnum.CONFIRMED, "Agentic action is no longer pending");

        try {
            if (ACTION_WRITE_POINT_VALUE.equals(action.getActionType())) {
                Map<String, Object> payload = action.getPayload();
                boolean success = pointCommandFacade.submitWrite(header.getTenantId(),
                        longValue(payload.get("deviceId")),
                        longValue(payload.get("pointId")), Objects.toString(payload.get("value"), ""));
                action.setStatus(success ? AgenticActionStatusEnum.EXECUTED.getIndex()
                        : AgenticActionStatusEnum.FAILED.getIndex());
                action.setRemark(success ? "Executed" : "Facade returned false");
            } else {
                action.setStatus(AgenticActionStatusEnum.FAILED.getIndex());
                action.setRemark("Unsupported action type");
            }
        } catch (Exception e) {
            log.warn("Action execution failed, actionId={}", action.getId(), e);
            action.setStatus(AgenticActionStatusEnum.FAILED.getIndex());
            action.setRemark(e.getMessage());
        }

        action.setOperateTime(LocalDateTime.now());
        actionManager.updateById(action);
        return actionBuilder.buildBOByDO(action);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ActionBO reject(String actionId, RequestHeader.PrincipalHeader header) {
        ActionDO action = getPending(actionId, header);
        claimPending(action, header, AgenticActionStatusEnum.REJECTED,
                "Agentic action is no longer pending");
        return actionBuilder.buildBOByDO(action);
    }

    private ActionDO getPending(String actionId, RequestHeader.PrincipalHeader header) {
        LambdaQueryWrapper<ActionDO> wrapper = scopedWrapper(header)
                .eq(ActionDO::getActionId, actionId)
                .last(QueryWrapperConstant.LIMIT_ONE);
        ActionDO action = actionManager.getOne(wrapper);
        if (Objects.isNull(action)) {
            throw new NotFoundException("Agentic action does not exist");
        }
        if (!Objects.equals(action.getStatus(), AgenticActionStatusEnum.PENDING.getIndex())) {
            throw new RequestException("Agentic action is no longer pending");
        }
        if (Objects.nonNull(action.getExpireTime()) && action.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new RequestException("Agentic action has expired");
        }
        return action;
    }

    private void claimPending(ActionDO action, RequestHeader.PrincipalHeader header, AgenticActionStatusEnum nextStatus,
                              String failureMessage) {
        LocalDateTime now = LocalDateTime.now();
        boolean updated = actionManager.update(Wrappers.<ActionDO>lambdaUpdate()
                .set(ActionDO::getStatus, nextStatus.getIndex())
                .set(ActionDO::getOperateTime, now)
                .eq(ActionDO::getId, action.getId())
                .eq(ActionDO::getUserId, header.getUserId())
                .eq(ActionDO::getStatus, AgenticActionStatusEnum.PENDING.getIndex())
                .and(wrapper -> wrapper.isNull(ActionDO::getExpireTime)
                        .or()
                        .ge(ActionDO::getExpireTime, now)));
        if (!updated) {
            throw new RequestException(failureMessage);
        }
        action.setStatus(nextStatus.getIndex());
        action.setOperateTime(now);
    }

    private void fillCreateAudit(ActionBO entityBO, RequestHeader.PrincipalHeader header) {
        LocalDateTime now = LocalDateTime.now();
        entityBO.setCreateTime(now);
        entityBO.setOperateTime(now);
        entityBO.setCreatorId(header.getUserId());
        entityBO.setCreatorName(header.getUserName());
        entityBO.setOperatorId(header.getUserId());
        entityBO.setOperatorName(header.getUserName());
    }

    private LambdaQueryWrapper<ActionDO> scopedWrapper(RequestHeader.PrincipalHeader header) {
        return Wrappers.<ActionDO>query()
                .lambda()
                .eq(ActionDO::getUserId, header.getUserId());
    }

    private Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (Objects.nonNull(value)) {
            return Long.valueOf(value.toString());
        }
        throw new RequestException("Agentic action payload is missing required ID");
    }

}
