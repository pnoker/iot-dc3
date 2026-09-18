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
package io.github.pnoker.common.agentic.controller;

import io.github.pnoker.common.agentic.entity.bo.ModelProviderBO;
import io.github.pnoker.common.agentic.entity.builder.ModelProviderBuilder;
import io.github.pnoker.common.agentic.entity.vo.ModelProviderVO;
import io.github.pnoker.common.agentic.service.ModelProviderService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * REST controller exposing agentic model provider management endpoints.
 *
 * @author pnoker
 * @since 2026.5.10
 */
@Tag(
        name = "provider",
        description =
                "AI model provider configuration: manage provider endpoints, authentication credentials, and capability specifications for connecting to LLM services")
@RestController
@RequestMapping(AgenticConstant.PROVIDER_URL_PREFIX)
@RequiredArgsConstructor
public class ProviderController implements BaseController {

    private final ModelProviderBuilder modelProviderBuilder;

    private final ModelProviderService modelProviderService;

    /**
     * List the upstream LLM providers configured for the current tenant.
     *
     * @return a list of ModelProviderVO entries with id, name, base URL and capability spec
     */
    @PreAuthorize("@perm.can('provider', 'list')")
    @Operation(
            summary = "List Model Providers",
            description =
                    "List the upstream LLM providers configured for the current tenant. Returns each provider's id, name, base URL and capability spec; use to choose a provider when creating a model configuration.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "LOW"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @GetMapping("/list")
    public Mono<List<ModelProviderVO>> list() {
        return getPrincipalHeader()
                .flatMap(header -> modelProviderService.list(header).map(modelProviderBuilder::buildVOListByBOList));
    }

    /**
     * Register a new upstream LLM provider for the current tenant.
     *
     * @param request provider payload carrying base URL, credentials and capability spec
     * @return the created ModelProviderVO, referenceable when defining model configurations
     */
    @PreAuthorize("@perm.can('provider', 'add')")
    @Operation(
            summary = "Add Model Provider",
            description =
                    "Register a new upstream LLM provider for the current tenant with its base URL, credentials and capability spec. "
                            + "Returns the created provider; reference it afterwards when defining model configurations.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "false"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/config/add")
    public Mono<ModelProviderVO> add(@Validated(Add.class) @RequestBody ModelProviderVO request) {
        return getPrincipalHeader().flatMap(header -> {
            ModelProviderBO entityBO = modelProviderBuilder.buildBOByVO(request);
            return modelProviderService.add(entityBO, header).map(modelProviderBuilder::buildVOByBO);
        });
    }

    /**
     * Update an existing LLM provider for the current tenant.
     *
     * @param request provider payload carrying the new base URL, credentials or capability spec
     * @return the updated ModelProviderVO; the target must belong to the current tenant
     */
    @PreAuthorize("@perm.can('provider', 'update')")
    @Operation(
            summary = "Update Model Provider",
            description =
                    "Update an existing LLM provider for the current tenant, changing its base URL, credentials or capability spec. "
                            + "Returns the updated provider; the target must belong to the current tenant.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "MEDIUM"),
                                @ExtensionProperty(name = "destructive", value = "false"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @PostMapping("/config/update")
    public Mono<ModelProviderVO> update(@Validated(Update.class) @RequestBody ModelProviderVO request) {
        return getPrincipalHeader().flatMap(header -> {
            ModelProviderBO entityBO = modelProviderBuilder.buildBOByVO(request);
            return modelProviderService.update(entityBO, header).map(modelProviderBuilder::buildVOByBO);
        });
    }

    /**
     * Permanently remove an LLM provider from the current tenant by id.
     *
     * @param id primary key of the provider to delete; must belong to the current tenant
     * @return delete-success status (true on success)
     */
    @PreAuthorize("@perm.can('provider', 'delete')")
    @Operation(
            summary = "Delete Model Provider",
            description =
                    "Permanently remove an LLM provider from the current tenant by id. "
                            + "Returns true on success; model configurations bound to this provider will no longer resolve, so call only when the provider is unused.",
            extensions =
                    @Extension(
                            name = "x-dc3-ai",
                            properties = {
                                @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                                @ExtensionProperty(name = "destructive", value = "true"),
                                @ExtensionProperty(name = "idempotent", value = "true"),
                                @ExtensionProperty(name = "openWorld", value = "false")
                            }))
    @DeleteMapping("/config/delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> delete(
            @Parameter(
                            description = "Primary key of the entity to delete. Must belong to the current tenant.",
                            example = "1024")
                    @NotNull
                    @RequestParam(value = "id")
                    Long id) {
        return getPrincipalHeader().flatMap(header -> modelProviderService.delete(id, header));
    }
}
