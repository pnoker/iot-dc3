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
package io.github.pnoker.common.manager.controller;

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.manager.entity.bo.EventParamBO;
import io.github.pnoker.common.manager.entity.builder.EventParamBuilder;
import io.github.pnoker.common.manager.entity.query.EventParamOffsetRequest;
import io.github.pnoker.common.manager.entity.vo.EventParamVO;
import io.github.pnoker.common.manager.repository.EventParamFilter;
import io.github.pnoker.common.manager.service.ReactiveEventParamService;
import io.github.pnoker.common.manager.service.ReactiveEventService;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Reactive HTTP API for tenant-scoped event parameters. */
@Tag(name = "event_param", description = "Event parameter definitions")
@RestController
@RequestMapping(ManagerConstant.EVENT_PARAM_URL_PREFIX)
@RequiredArgsConstructor
public class EventParamController implements BaseController {
    private final EventParamBuilder eventParamBuilder;
    private final ReactiveEventParamService eventParamService;
    private final ReactiveEventService eventService;

    @PreAuthorize("@perm.can('event_param', 'add')")
    @Operation(
            summary = "Add Event Parameter",
            description =
                    "Create a tenant-scoped field definition on a device event and return the persisted parameter record.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "false"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/add")
    public Mono<EventParamVO> add(@Validated(Add.class) @RequestBody EventParamVO entityVO) {
        return actor().flatMap(actor -> {
            EventParamBO value = eventParamBuilder.buildBOByVO(entityVO);
            value.setTenantId(actor.tenantId());
            value.setCreatorId(actor.userId());
            value.setCreatorName(actor.userName());
            value.setOperatorId(actor.userId());
            value.setOperatorName(actor.userName());
            return eventParamService.add(value).map(eventParamBuilder::buildVOByBO);
        });
    }

    @PreAuthorize("@perm.can('event_param', 'delete')")
    @Operation(
            summary = "Delete Event Parameter",
            description =
                    "Soft-delete one tenant-owned event parameter and notify affected drivers that event metadata changed.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                                @ExtensionProperty(name = "destructive", value = "true"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @DeleteMapping("/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @Parameter(description = "Tenant-scoped event parameter ID") @NotNull @RequestParam("id") Long id,
            @Parameter(
                            description = "Current optimistic-lock version required as a deletion precondition.",
                            example = "0")
                    @NotNull
                    @Min(0)
                    @RequestParam("version")
                    Integer version) {
        return actor().flatMap(actor -> eventParamService
                .delete(actor.tenantId(), id, version, actor.userId(), actor.userName())
                .then());
    }

    @PreAuthorize("@perm.can('event_param', 'update')")
    @Operation(
            summary = "Update Event Parameter",
            description =
                    "Update a tenant-owned event parameter using optimistic locking while preserving its parent event and stable code.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/update")
    public Mono<EventParamVO> update(@Validated(Update.class) @RequestBody EventParamVO entityVO) {
        return actor().flatMap(actor -> {
            EventParamBO value = eventParamBuilder.buildBOByVO(entityVO);
            value.setTenantId(actor.tenantId());
            value.setOperatorId(actor.userId());
            value.setOperatorName(actor.userName());
            return eventParamService.update(value).map(eventParamBuilder::buildVOByBO);
        });
    }

    @PreAuthorize("@perm.can('event_param', 'get')")
    @Operation(
            summary = "Get Event Parameter by ID",
            description =
                    "Fetch one event parameter by primary key with strict tenant isolation and soft-delete filtering.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/get_by_id")
    public Mono<EventParamVO> getById(
            @Parameter(description = "Primary key of the tenant-owned event parameter.") @NotNull @RequestParam("id")
                    Long id) {
        return getTenantId()
                .flatMap(tenantId -> eventParamService.getById(tenantId, id).map(eventParamBuilder::buildVOByBO));
    }

    @PreAuthorize("@perm.can('event_param', 'list')")
    @Operation(
            summary = "List Event Parameters by Event ID",
            description =
                    "List every active parameter declared by one tenant-owned event in deterministic identifier order.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/list_by_event_id")
    public Mono<List<EventParamVO>> listByEventId(
            @Parameter(description = "Parent event identifier that must belong to the current tenant.")
                    @NotNull
                    @RequestParam("event_id")
                    Long eventId) {
        return getTenantId()
                .flatMap(tenantId -> eventService
                        .getById(tenantId, eventId)
                        .thenMany(eventParamService.listByEventId(tenantId, eventId))
                        .map(eventParamBuilder::buildVOByBO)
                        .collectList());
    }

    @PreAuthorize("@perm.can('event_param', 'list')")
    @Operation(
            summary = "List Event Parameters",
            description =
                    "Return an offset page of tenant-scoped event parameters using validated filters and stable whitelisted sorting.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/list")
    public Mono<OffsetPage<EventParamVO>> list(@RequestBody(required = false) EventParamOffsetRequest request) {
        EventParamOffsetRequest query = request == null ? new EventParamOffsetRequest() : request;
        return getTenantId()
                .flatMap(tenantId -> eventParamService
                        .list(new EventParamFilter(
                                tenantId,
                                query.paramName(),
                                query.paramCode(),
                                query.paramTypeFlag(),
                                query.eventId(),
                                query.enableFlag(),
                                query.version(),
                                query.offset(),
                                query.limit(),
                                query.sort()))
                        .map(page -> OffsetPage.of(
                                page.items().stream()
                                        .map(eventParamBuilder::buildVOByBO)
                                        .toList(),
                                page.offset(),
                                page.limit(),
                                page.total())));
    }

    private Mono<Actor> actor() {
        return getTenantId()
                .zipWith(getUserId().defaultIfEmpty(0L))
                .zipWith(getUserName().defaultIfEmpty(""))
                .map(tuple -> new Actor(tuple.getT1().getT1(), tuple.getT1().getT2(), tuple.getT2()));
    }

    private record Actor(Long tenantId, Long userId, String userName) {}
}
