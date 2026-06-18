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
import io.github.pnoker.common.agentic.entity.vo.ModelProviderVO;
import io.github.pnoker.common.agentic.service.ModelProviderService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.R;
import io.github.pnoker.common.valid.Add;
import io.github.pnoker.common.valid.Update;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST controller exposing agentic model provider management endpoints.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Tag(name = "provider", description = "AI model provider configuration: manage provider endpoints, authentication credentials, and capability specifications for connecting to LLM services")
@RestController
@RequestMapping(AgenticConstant.PROVIDER_URL_PREFIX)
@RequiredArgsConstructor
public class ProviderController implements BaseController {

    private final ModelProviderBuilder modelProviderBuilder;

    private final ModelProviderService modelProviderService;

    @PreAuthorize("@perm.can('provider', 'list')")
    @Operation(summary = "List Model Providers", description = "List the upstream LLM providers configured for the current tenant. Returns each provider's id, name, base URL and capability spec; use to choose a provider when creating a model configuration.")
    @GetMapping("/list")
    public Mono<R<List<ModelProviderVO>>> list() {
        return async(() -> R.ok(modelProviderBuilder.buildVOListByBOList(modelProviderService.list())));
    }

    @PreAuthorize("@perm.can('provider', 'add')")
    @Operation(summary = "Add Model Provider", description = "Register a new upstream LLM provider for the current tenant with its base URL, credentials and capability spec. "
            + "Returns the created provider; reference it afterwards when defining model configurations.")
    @PostMapping("/config/add")
    public Mono<R<ModelProviderVO>> add(@Validated(Add.class) @RequestBody ModelProviderVO request) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            ModelProviderBO entityBO = modelProviderBuilder.buildBOByVO(request);
            return R.ok(modelProviderBuilder.buildVOByBO(modelProviderService.add(entityBO, header)));
        }));
    }

    @PreAuthorize("@perm.can('provider', 'update')")
    @Operation(summary = "Update Model Provider", description = "Update an existing LLM provider for the current tenant, changing its base URL, credentials or capability spec. "
            + "Returns the updated provider; the target must belong to the current tenant.")
    @PostMapping("/config/update")
    public Mono<R<ModelProviderVO>> update(@Validated(Update.class) @RequestBody ModelProviderVO request) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            ModelProviderBO entityBO = modelProviderBuilder.buildBOByVO(request);
            return R.ok(modelProviderBuilder.buildVOByBO(modelProviderService.update(entityBO, header)));
        }));
    }

    @PreAuthorize("@perm.can('provider', 'delete')")
    @Operation(summary = "Delete Model Provider", description = "Permanently remove an LLM provider from the current tenant by id. "
            + "Returns true on success; model configurations bound to this provider will no longer resolve, so call only when the provider is unused.")
    @PostMapping("/config/delete")
    public Mono<R<Boolean>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return async(() -> {
            modelProviderService.delete(id);
            return R.ok(true);
        });
    }

}
