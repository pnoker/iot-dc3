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
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
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
@RestController
@RequestMapping(AgenticConstant.MODEL_URL_PREFIX)
@RequiredArgsConstructor
public class ModelController implements BaseController {

    private final ModelConfigBuilder modelConfigBuilder;

    private final ModelConfigService modelConfigService;

    @GetMapping("/list")
    public Mono<R<List<ModelVO>>> list() {
        return async(() -> R.ok(modelConfigService.listOptions()));
    }

    @GetMapping("/config/list")
    public Mono<R<List<ModelConfigVO>>> listConfigs() {
        return async(() -> R.ok(modelConfigBuilder.buildVOListByBOList(modelConfigService.listConfigs())));
    }

    @PostMapping("/config/add")
    public Mono<R<ModelConfigVO>> add(@Validated(Add.class) @RequestBody ModelConfigRequest request) {
        return getUserHeader().flatMap(header -> async(() -> {
            ModelConfigBO entityBO = modelConfigBuilder.buildBOByRequest(request);
            return R.ok(modelConfigBuilder.buildVOByBO(modelConfigService.add(entityBO, header)));
        }));
    }

    @PostMapping("/config/update")
    public Mono<R<ModelConfigVO>> update(@Validated(Update.class) @RequestBody ModelConfigRequest request) {
        return getUserHeader().flatMap(header -> async(() -> {
            ModelConfigBO entityBO = modelConfigBuilder.buildBOByRequest(request);
            return R.ok(modelConfigBuilder.buildVOByBO(modelConfigService.update(entityBO, header)));
        }));
    }

    @PostMapping("/config/delete")
    public Mono<R<Boolean>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return async(() -> {
            modelConfigService.delete(id);
            return R.ok(true);
        });
    }

}
