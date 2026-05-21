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

package io.github.pnoker.common.data.controller;

import lombok.RequiredArgsConstructor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.entity.bo.MessageBO;
import io.github.pnoker.common.data.entity.builder.MessageBuilder;
import io.github.pnoker.common.data.entity.query.MessageQuery;
import io.github.pnoker.common.data.entity.vo.MessageVO;
import io.github.pnoker.common.data.service.MessageService;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Alarm message template controller.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.MESSAGE_URL_PREFIX)
@RequiredArgsConstructor
public class MessageController implements BaseController {

    private final MessageBuilder messageBuilder;

    private final MessageService messageService;

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody MessageVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            MessageBO entityBO = messageBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            messageService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, messageService.getById(id));
            messageService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody MessageVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            MessageBO entityBO = messageBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, messageService.getById(entityBO.getId()));
            messageService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @GetMapping("/get_by_id")
    public Mono<R<MessageVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            MessageBO entityBO = requireTenant(tenantId, messageService.getById(id));
            return R.ok(messageBuilder.buildVOByBO(entityBO));
        }));
    }

    @PostMapping("/list")
    public Mono<R<Page<MessageVO>>> list(@RequestBody(required = false) MessageQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            MessageQuery query = Objects.isNull(entityQuery) ? new MessageQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<MessageBO> entityPageBO = messageService.list(query);
            return R.ok(messageBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

}
