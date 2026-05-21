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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.entity.bo.NotifyChannelBindBO;
import io.github.pnoker.common.data.entity.builder.NotifyChannelBindBuilder;
import io.github.pnoker.common.data.entity.query.NotifyChannelBindQuery;
import io.github.pnoker.common.data.entity.vo.NotifyChannelBindVO;
import io.github.pnoker.common.data.service.NotifyChannelBindService;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.enums.ResponseEnum;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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
 * Notification channel binding controller.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.NOTIFY_CHANNEL_BIND_URL_PREFIX)
@RequiredArgsConstructor
public class NotifyChannelBindController implements BaseController {

    private final NotifyChannelBindBuilder notifyChannelBindBuilder;

    private final NotifyChannelBindService notifyChannelBindService;

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody NotifyChannelBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyChannelBindBO entityBO = notifyChannelBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            notifyChannelBindService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, notifyChannelBindService.getById(id));
            notifyChannelBindService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody NotifyChannelBindVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyChannelBindBO entityBO = notifyChannelBindBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, notifyChannelBindService.getById(entityBO.getId()));
            notifyChannelBindService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @GetMapping("/get_by_id")
    public Mono<R<NotifyChannelBindVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyChannelBindBO entityBO = requireTenant(tenantId, notifyChannelBindService.getById(id));
            return R.ok(notifyChannelBindBuilder.buildVOByBO(entityBO));
        }));
    }

    @PostMapping("/list")
    public Mono<R<Page<NotifyChannelBindVO>>> list(@RequestBody(required = false) NotifyChannelBindQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyChannelBindQuery query = Objects.isNull(entityQuery) ? new NotifyChannelBindQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<NotifyChannelBindBO> entityPageBO = notifyChannelBindService.list(query);
            return R.ok(notifyChannelBindBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

}
