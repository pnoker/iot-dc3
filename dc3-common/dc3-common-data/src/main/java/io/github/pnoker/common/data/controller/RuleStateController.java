package io.github.pnoker.common.data.controller;

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.entity.builder.RuleStateBuilder;
import io.github.pnoker.common.data.entity.query.RuleStateQuery;
import io.github.pnoker.common.data.entity.vo.RuleStateVO;
import io.github.pnoker.common.data.service.RuleStateService;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Rule runtime state controller. */
@Tag(name = "rule_state", description = "Rule execution states")
@RestController
@RequestMapping(DataConstant.RULE_STATE_URL_PREFIX)
@RequiredArgsConstructor
public class RuleStateController implements BaseController {

    private final RuleStateBuilder ruleStateBuilder;
    private final RuleStateService ruleStateService;

    @PreAuthorize("@perm.can('rule_state', 'get')")
    @Operation(summary = "Get Rule State by ID", description = "Return one tenant-owned rule runtime state for diagnostics and operational review.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @GetMapping("/get_by_id")
    public Mono<RuleStateVO> getById(@Parameter(description = "Primary key of the tenant-owned rule state record") @NotNull @RequestParam("id") Long id) {
        return getTenantId().flatMap(tenantId -> ruleStateService.getById(tenantId, id)
                .map(ruleStateBuilder::buildVOByBO));
    }

    @PreAuthorize("@perm.can('rule_state', 'list')")
    @Operation(summary = "List Rule States", description = "List tenant-owned rule runtime states with offset pagination and explicit filters.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @PostMapping("/list")
    public Mono<OffsetPage<RuleStateVO>> list(@RequestBody(required = false) RuleStateQuery query) {
        return getTenantId().flatMap(tenantId -> ruleStateService.list(tenantId, query)
                .map(page -> OffsetPage.of(page.items().stream().map(ruleStateBuilder::buildVOByBO).toList(),
                        page.offset(), page.limit(), page.total())));
    }

    @PreAuthorize("@perm.can('rule_state', 'delete')")
    @Operation(summary = "Delete Rule State", description = "Delete one tenant-owned rule runtime state record permanently.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "MEDIUM"), @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @DeleteMapping("/delete")
    public Mono<Boolean> delete(@Parameter(description = "Primary key of the tenant-owned rule state record to delete") @NotNull @RequestParam("id") Long id) {
        return getTenantId().flatMap(tenantId -> ruleStateService.delete(tenantId, id));
    }
}
