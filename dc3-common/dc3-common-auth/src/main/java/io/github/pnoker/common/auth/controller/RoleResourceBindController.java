package io.github.pnoker.common.auth.controller;

import io.github.pnoker.common.auth.entity.bo.RoleResourceBindBO;
import io.github.pnoker.common.auth.entity.builder.ResourceBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleBuilder;
import io.github.pnoker.common.auth.entity.builder.RoleResourceBindBuilder;
import io.github.pnoker.common.auth.entity.query.RoleResourceBindOffsetRequest;
import io.github.pnoker.common.auth.entity.vo.ResourceVO;
import io.github.pnoker.common.auth.entity.vo.RoleResourceBindVO;
import io.github.pnoker.common.auth.entity.vo.RoleVO;
import io.github.pnoker.common.auth.repository.RoleResourceBindFilter;
import io.github.pnoker.common.auth.service.ReactiveRoleResourceBindService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AuthConstant;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.db.r2dbc.core.page.OffsetPage;
import io.github.pnoker.db.r2dbc.core.page.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/** REST endpoints for tenant-scoped role-resource assignments. */
@Tag(name = "role_resource_bind")
@RestController
@RequestMapping(AuthConstant.ROLE_RESOURCE_URL_PREFIX)
@RequiredArgsConstructor
public class RoleResourceBindController implements BaseController {
    private final RoleResourceBindBuilder bindingBuilder;
    private final ReactiveRoleResourceBindService bindingService;
    private final ResourceBuilder resourceBuilder;
    private final RoleBuilder roleBuilder;

    @PreAuthorize("@perm.can('role_resource_bind', 'add')")
    @Operation(summary = "Add Role-Resource Binding", description = "Grant an enabled resource permission to a role in the current tenant.", extensions = @Extension(name = "x-dc3-ai", properties = {@ExtensionProperty(name = "riskLevel", value = "MEDIUM"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "false"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @PostMapping("/add")
    public Mono<ResponseEntity<RoleResourceBindVO>> add(@Validated(Add.class) @RequestBody RoleResourceBindVO entityVO) {
        return getPrincipalHeader().flatMap(header -> {
            RoleResourceBindBO binding = bindingBuilder.buildBOByVO(entityVO);
            binding.setCreatorId(header.getUserId()); binding.setCreatorName(header.getNickName());
            binding.setOperatorId(header.getUserId()); binding.setOperatorName(header.getNickName());
            return bindingService.add(binding, header.getTenantId()).map(saved -> ResponseEntity.status(201).body(bindingBuilder.buildVOByBO(saved)));
        });
    }

    @PreAuthorize("@perm.can('role_resource_bind', 'delete')")
    @Operation(summary = "Delete Role-Resource Binding", description = "Revoke a role resource permission owned by the current tenant.", extensions = @Extension(name = "x-dc3-ai", properties = {@ExtensionProperty(name = "riskLevel", value = "MEDIUM"), @ExtensionProperty(name = "destructive", value = "true"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<Void>> delete(@Parameter(description = "Binding identifier owned by the current tenant") @NotNull @RequestParam("id") Long id) {
        return getPrincipalHeader().flatMap(header -> bindingService.delete(header.getTenantId(), id, header.getUserId(), header.getNickName()))
                .thenReturn(ResponseEntity.noContent().build());
    }

    @PreAuthorize("@perm.can('role_resource_bind', 'list')")
    @Operation(summary = "List Role-Resource Bindings", description = "List tenant role permissions using deterministic offset pagination and filters.", extensions = @Extension(name = "x-dc3-ai", properties = {@ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @PostMapping("/list")
    public Mono<ResponseEntity<OffsetPage<RoleResourceBindVO>>> list(@RequestBody(required = false) RoleResourceBindOffsetRequest request) {
        RoleResourceBindOffsetRequest query = request == null ? new RoleResourceBindOffsetRequest() : request;
        return getTenantId().flatMap(tenantId -> bindingService.list(new RoleResourceBindFilter(tenantId, query.roleId(), query.resourceId(), new PageRequest(query.offset(), query.limit(), query.sort())))
                .map(page -> ResponseEntity.ok(OffsetPage.of(page.items().stream().map(bindingBuilder::buildVOByBO).toList(), page.offset(), page.limit(), page.total()))));
    }

    @PreAuthorize("@perm.can('role_resource_bind', 'list')")
    @Operation(summary = "List Resources by Role", description = "List enabled resources granted to a role within the current tenant.", extensions = @Extension(name = "x-dc3-ai", properties = {@ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @GetMapping("/list_resource_by_role")
    public Mono<ResponseEntity<List<ResourceVO>>> listResourceByRole(@Parameter(description = "Role identifier within the current tenant") @NotNull @RequestParam("role_id") Long roleId) {
        return getTenantId().flatMap(tenantId -> bindingService.listResourcesByRole(tenantId, roleId)
                .map(resourceBuilder::buildVOByBO).collectList().map(ResponseEntity::ok));
    }

    @PreAuthorize("@perm.can('role_resource_bind', 'list')")
    @Operation(summary = "List Resources by Principal", description = "Resolve enabled resources granted through all roles of a tenant principal.", extensions = @Extension(name = "x-dc3-ai", properties = {@ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @GetMapping("/list_resource_by_principal")
    public Mono<ResponseEntity<List<ResourceVO>>> listResourceByPrincipal(@Parameter(description = "Principal identifier within the current tenant") @NotNull @RequestParam("principal_id") Long principalId) {
        return getTenantId().flatMap(tenantId -> bindingService.listResourcesByPrincipal(tenantId, principalId)
                .map(resourceBuilder::buildVOByBO).collectList().map(ResponseEntity::ok));
    }

    @PreAuthorize("@perm.can('role_resource_bind', 'list')")
    @Operation(summary = "List Roles by Resource", description = "List enabled tenant roles that currently grant a resource permission.", extensions = @Extension(name = "x-dc3-ai", properties = {@ExtensionProperty(name = "riskLevel", value = "LOW"), @ExtensionProperty(name = "destructive", value = "false"), @ExtensionProperty(name = "idempotent", value = "true"), @ExtensionProperty(name = "openWorld", value = "false")}))
    @GetMapping("/list_role_by_resource")
    public Mono<ResponseEntity<List<RoleVO>>> listRoleByResource(@Parameter(description = "Resource identifier to resolve granting roles") @NotNull @RequestParam("resource_id") Long resourceId) {
        return getTenantId().flatMap(tenantId -> bindingService.listRolesByResource(tenantId, resourceId)
                .map(roleBuilder::buildVOByBO).collectList().map(ResponseEntity::ok));
    }
}
