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
import io.github.pnoker.common.data.entity.bo.NotifyChannelBO;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBuilder;
import io.github.pnoker.common.data.entity.query.NotifyChannelQuery;
import io.github.pnoker.common.data.entity.vo.NotifyChannelVO;
import io.github.pnoker.common.data.service.NotifyChannelService;
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
 * Notification channel controller.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.NOTIFY_CHANNEL_URL_PREFIX)
@RequiredArgsConstructor
public class NotifyChannelController implements BaseController {

    private final NotifyChannelBuilder notifyChannelBuilder;

    private final NotifyChannelService notifyChannelService;

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody NotifyChannelVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyChannelBO entityBO = notifyChannelBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            notifyChannelService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, notifyChannelService.getById(id));
            notifyChannelService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody NotifyChannelVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyChannelBO entityBO = notifyChannelBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, notifyChannelService.getById(entityBO.getId()));
            notifyChannelService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @GetMapping("/get_by_id")
    public Mono<R<NotifyChannelVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyChannelBO entityBO = requireTenant(tenantId, notifyChannelService.getById(id));
            return R.ok(notifyChannelBuilder.buildVOByBO(entityBO));
        }));
    }

    @PostMapping("/list")
    public Mono<R<Page<NotifyChannelVO>>> list(@RequestBody(required = false) NotifyChannelQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyChannelQuery query = Objects.isNull(entityQuery) ? new NotifyChannelQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<NotifyChannelBO> entityPageBO = notifyChannelService.list(query);
            return R.ok(notifyChannelBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

}
