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
import io.github.pnoker.common.data.entity.bo.NotifyBO;
import io.github.pnoker.common.data.entity.builder.NotifyBuilder;
import io.github.pnoker.common.data.entity.query.NotifyQuery;
import io.github.pnoker.common.data.entity.vo.NotifyVO;
import io.github.pnoker.common.data.service.NotifyService;
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
 * Alarm notification policy controller.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@RestController
@RequestMapping(DataConstant.NOTIFY_URL_PREFIX)
@RequiredArgsConstructor
public class NotifyController implements BaseController {

    private final NotifyBuilder notifyBuilder;

    private final NotifyService notifyService;

    @PostMapping("/add")
    public Mono<R<String>> add(@Validated(Add.class) @RequestBody NotifyVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyBO entityBO = notifyBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            notifyService.add(entityBO);
            return R.ok(ResponseEnum.ADD_SUCCESS);
        }));
    }

    @PostMapping("/delete")
    public Mono<R<String>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            requireTenant(tenantId, notifyService.getById(id));
            notifyService.delete(id);
            return R.ok(ResponseEnum.DELETE_SUCCESS);
        }));
    }

    @PostMapping("/update")
    public Mono<R<String>> update(@Validated(Update.class) @RequestBody NotifyVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyBO entityBO = notifyBuilder.buildBOByVO(entityVO);
            entityBO.setTenantId(tenantId);
            requireTenant(tenantId, notifyService.getById(entityBO.getId()));
            notifyService.update(entityBO);
            return R.ok(ResponseEnum.UPDATE_SUCCESS);
        }));
    }

    @GetMapping("/get_by_id")
    public Mono<R<NotifyVO>> getById(@NotNull @RequestParam(value = "id") Long id) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyBO entityBO = requireTenant(tenantId, notifyService.getById(id));
            return R.ok(notifyBuilder.buildVOByBO(entityBO));
        }));
    }

    @PostMapping("/list")
    public Mono<R<Page<NotifyVO>>> list(@RequestBody(required = false) NotifyQuery entityQuery) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            NotifyQuery query = Objects.isNull(entityQuery) ? new NotifyQuery() : entityQuery;
            query.setTenantId(tenantId);
            Page<NotifyBO> entityPageBO = notifyService.list(query);
            return R.ok(notifyBuilder.buildVOPageByBOPage(entityPageBO));
        }));
    }

}
