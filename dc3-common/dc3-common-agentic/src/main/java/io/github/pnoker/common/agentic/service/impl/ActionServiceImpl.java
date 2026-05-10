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
import io.github.pnoker.common.agentic.entity.model.ActionDO;
import io.github.pnoker.common.agentic.entity.vo.ActionVO;
import io.github.pnoker.common.agentic.service.ActionService;
import io.github.pnoker.common.entity.common.RequestHeader;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.exception.RequestException;
import io.github.pnoker.common.facade.api.PointValueCommandFacade;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class ActionServiceImpl implements ActionService {

    public static final byte STATUS_PENDING = 0;

    public static final byte STATUS_CONFIRMED = 1;

    public static final byte STATUS_REJECTED = 2;

    public static final byte STATUS_EXECUTED = 3;

    public static final byte STATUS_FAILED = 4;

    private static final String ACTION_WRITE_POINT_VALUE = "writePointValue";

    private final ActionManager actionManager;

    private final PointValueCommandFacade pointValueCommandFacade;

    public ActionServiceImpl(ActionManager actionManager, PointValueCommandFacade pointValueCommandFacade) {
        this.actionManager = actionManager;
        this.pointValueCommandFacade = pointValueCommandFacade;
    }

    @Override
    public String createWritePointValueAction(String conversationId, Long deviceId, Long pointId, String value,
                                              RequestHeader.UserHeader header) {
        ActionDO entity = new ActionDO();
        entity.setActionId(UUID.randomUUID().toString());
        entity.setConversationId(conversationId);
        entity.setActionType(ACTION_WRITE_POINT_VALUE);
        entity.setTitle("Write point value");
        entity.setDescription("Write value to device " + deviceId + ", point " + pointId);
        entity.setPayload(Map.of("deviceId", deviceId, "pointId", pointId, "value", value));
        entity.setStatus(STATUS_PENDING);
        entity.setExpireTime(LocalDateTime.now().plusMinutes(10));
        entity.setTenantId(header.getTenantId());
        entity.setUserId(header.getUserId());
        entity.setCreateTime(LocalDateTime.now());
        entity.setOperateTime(entity.getCreateTime());
        entity.setCreatorId(header.getUserId());
        entity.setOperatorId(header.getUserId());
        entity.setCreatorName(header.getUserName());
        entity.setOperatorName(header.getUserName());
        actionManager.save(entity);
        return entity.getActionId();
    }

    @Override
    public List<ActionVO> listPending(String conversationId, RequestHeader.UserHeader header) {
        LambdaQueryWrapper<ActionDO> wrapper = scopedWrapper(header)
                .eq(ActionDO::getConversationId, conversationId)
                .eq(ActionDO::getStatus, STATUS_PENDING)
                .ge(ActionDO::getExpireTime, LocalDateTime.now())
                .orderByDesc(ActionDO::getCreateTime);
        return actionManager.list(wrapper).stream().map(this::toVO).toList();
    }

    @Override
    public ActionVO confirm(String actionId, RequestHeader.UserHeader header) {
        ActionDO action = getPending(actionId, header);
        action.setStatus(STATUS_CONFIRMED);
        action.setOperateTime(LocalDateTime.now());
        actionManager.updateById(action);

        try {
            if (ACTION_WRITE_POINT_VALUE.equals(action.getActionType())) {
                Map<String, Object> payload = action.getPayload();
                boolean success = pointValueCommandFacade.write(header.getTenantId(), longValue(payload.get("deviceId")),
                        longValue(payload.get("pointId")), Objects.toString(payload.get("value"), ""));
                action.setStatus(success ? STATUS_EXECUTED : STATUS_FAILED);
                action.setRemark(success ? "Executed" : "Facade returned false");
            } else {
                action.setStatus(STATUS_FAILED);
                action.setRemark("Unsupported action type");
            }
        } catch (Exception e) {
            action.setStatus(STATUS_FAILED);
            action.setRemark(e.getMessage());
        }

        action.setOperateTime(LocalDateTime.now());
        actionManager.updateById(action);
        return toVO(action);
    }

    @Override
    public ActionVO reject(String actionId, RequestHeader.UserHeader header) {
        ActionDO action = getPending(actionId, header);
        action.setStatus(STATUS_REJECTED);
        action.setOperateTime(LocalDateTime.now());
        actionManager.updateById(action);
        return toVO(action);
    }

    private ActionDO getPending(String actionId, RequestHeader.UserHeader header) {
        LambdaQueryWrapper<ActionDO> wrapper = scopedWrapper(header)
                .eq(ActionDO::getActionId, actionId)
                .last("LIMIT 1");
        ActionDO action = actionManager.getOne(wrapper);
        if (Objects.isNull(action)) {
            throw new NotFoundException("Agentic action does not exist");
        }
        if (!Objects.equals(action.getStatus(), STATUS_PENDING)) {
            throw new RequestException("Agentic action is no longer pending");
        }
        if (Objects.nonNull(action.getExpireTime()) && action.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new RequestException("Agentic action has expired");
        }
        return action;
    }

    private LambdaQueryWrapper<ActionDO> scopedWrapper(RequestHeader.UserHeader header) {
        return Wrappers.<ActionDO>query()
                .lambda()
                .eq(ActionDO::getTenantId, header.getTenantId())
                .eq(ActionDO::getUserId, header.getUserId());
    }

    private ActionVO toVO(ActionDO entity) {
        ActionVO vo = new ActionVO();
        vo.setId(entity.getId());
        vo.setActionId(entity.getActionId());
        vo.setConversationId(entity.getConversationId());
        vo.setActionType(entity.getActionType());
        vo.setTitle(entity.getTitle());
        vo.setDescription(entity.getDescription());
        vo.setPayload(entity.getPayload());
        vo.setStatus(entity.getStatus());
        vo.setExpireTime(entity.getExpireTime());
        vo.setRemark(entity.getRemark());
        vo.setCreateTime(entity.getCreateTime());
        vo.setOperateTime(entity.getOperateTime());
        return vo;
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
