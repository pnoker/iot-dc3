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

import io.github.pnoker.common.agentic.entity.bo.ModelConfigBO;
import io.github.pnoker.common.agentic.entity.builder.ModelConfigBuilder;
import io.github.pnoker.common.agentic.entity.request.ModelConfigRequest;
import io.github.pnoker.common.agentic.entity.vo.ModelConfigVO;
import io.github.pnoker.common.agentic.entity.vo.ModelVO;
import io.github.pnoker.common.agentic.service.ModelConfigService;
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
 * REST controller exposing agentic model configuration endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "model", description = "AI model registry: manage model metadata including name, version, capabilities, context limits, and provider associations")
@RestController
@RequestMapping(AgenticConstant.MODEL_URL_PREFIX)
@RequiredArgsConstructor
public class ModelController implements BaseController {

    private final ModelConfigBuilder modelConfigBuilder;

    private final ModelConfigService modelConfigService;

    @PreAuthorize("@perm.can('model', 'list')")
    @Operation(summary = "List AI Models", description = "List the AI model options available to the current tenant for selection in a chat session." +
            " Returns model ids with display names; use to pick a model before chatting.")
    @GetMapping("/list")
    public Mono<R<List<ModelVO>>> list() {
        return async(() -> R.ok(modelConfigService.listOptions()));
    }

    @PreAuthorize("@perm.can('model', 'list')")
    @Operation(summary = "List AI Model Configurations", description = "List the stored AI model configurations for the current tenant, each binding a provider, model id and parameters." +
            " Returns full configuration records for management, unlike the lightweight model options used for chat selection.")
    @GetMapping("/config/list")
    public Mono<R<List<ModelConfigVO>>> listConfigs() {
        return async(() -> R.ok(modelConfigBuilder.buildVOListByBOList(modelConfigService.listConfigs())));
    }

    @PreAuthorize("@perm.can('model', 'add')")
    @Operation(summary = "Add AI Model Configuration", description = "Create an AI model configuration for the current tenant binding a provider, model id and parameters." +
            " Returns the saved configuration; the assistant can then use it as a selectable model in a session.")
    @PostMapping("/config/add")
    public Mono<R<ModelConfigVO>> add(@Validated(Add.class) @RequestBody ModelConfigRequest request) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            ModelConfigBO entityBO = modelConfigBuilder.buildBOByRequest(request);
            return R.ok(modelConfigBuilder.buildVOByBO(modelConfigService.add(entityBO, header)));
        }));
    }

    @PreAuthorize("@perm.can('model', 'update')")
    @Operation(summary = "Update AI Model Configuration", description = "Update an existing AI model configuration's provider, model id or parameters for the current tenant." +
            " Returns the updated configuration; changes apply to future sessions that select this model.")
    @PostMapping("/config/update")
    public Mono<R<ModelConfigVO>> update(@Validated(Update.class) @RequestBody ModelConfigRequest request) {
        return getPrincipalHeader().flatMap(header -> async(() -> {
            ModelConfigBO entityBO = modelConfigBuilder.buildBOByRequest(request);
            return R.ok(modelConfigBuilder.buildVOByBO(modelConfigService.update(entityBO, header)));
        }));
    }

    @PreAuthorize("@perm.can('model', 'delete')")
    @Operation(summary = "Delete AI Model Configuration", description = "Permanently delete the AI model configuration identified by id within the current tenant." +
            " Returns true on success; the model is no longer selectable in new sessions.")
    @PostMapping("/config/delete")
    public Mono<R<Boolean>> delete(@Parameter(description = "Primary key of the entity to delete. Must belong to the current tenant.", example = "1024") @NotNull @RequestParam(value = "id") Long id) {
        return async(() -> {
            modelConfigService.delete(id);
            return R.ok(true);
        });
    }

}
