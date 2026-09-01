package io.github.pnoker.common.data.controller;

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.PointCommandHistoryService;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryQueryVO;
import io.github.pnoker.common.data.entity.vo.PointCommandHistoryVO;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import io.github.pnoker.common.exception.NotFoundException;

/** REST controller for reactive point command history queries. */
@Tag(name = "point_command_history", description = "Point command audit trail")
@Slf4j
@RestController
@RequestMapping(DataConstant.POINT_COMMAND_HISTORY_URL_PREFIX)
@RequiredArgsConstructor
public class PointCommandHistoryController implements BaseController {

    private final PointCommandHistoryService pointCommandHistoryService;

    @PreAuthorize("@perm.can('point_command_history', 'get')")
    @Operation(summary = "Get Point Command History by Command ID",
            description = "Return one tenant-scoped command history record for asynchronous status tracking.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"),
            @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @GetMapping("/get_by_command_id")
    public Mono<PointCommandHistoryVO> getByCommandId(
            @Parameter(description = "Tenant-owned point command identifier") @NotBlank @RequestParam String commandId) {
        return getTenantId().flatMap(tenantId -> pointCommandHistoryService.getByCommandId(tenantId, commandId)
                .switchIfEmpty(Mono.error(new NotFoundException("Point command history does not exist"))));
    }

    @PreAuthorize("@perm.can('point_command_history', 'list')")
    @Operation(summary = "List Point Command History",
            description = "List tenant-scoped asynchronous command records using offset pagination and stable sorting.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
            @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"),
            @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")
    }))
    @PostMapping("/list")
    public Mono<OffsetPage<PointCommandHistoryVO>> list(
            @RequestBody(required = false) PointCommandHistoryQueryVO queryVO) {
        return getTenantId().flatMap(tenantId -> pointCommandHistoryService.list(tenantId, queryVO));
    }
}
