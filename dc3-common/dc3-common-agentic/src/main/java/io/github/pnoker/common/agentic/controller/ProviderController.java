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
import io.github.pnoker.common.agentic.entity.request.ModelProviderRequest;
import io.github.pnoker.common.agentic.entity.vo.ModelProviderVO;
import io.github.pnoker.common.agentic.service.ModelProviderService;
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
 * REST controller exposing agentic model provider management endpoints.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@RestController
@RequestMapping(AgenticConstant.PROVIDER_URL_PREFIX)
@RequiredArgsConstructor
public class ProviderController implements BaseController {

    private final ModelProviderBuilder modelProviderBuilder;

    private final ModelProviderService modelProviderService;

    @GetMapping("/list")
    public Mono<R<List<ModelProviderVO>>> list() {
        return async(() -> R.ok(modelProviderBuilder.buildVOListByBOList(modelProviderService.list())));
    }

    @PostMapping("/config/add")
    public Mono<R<ModelProviderVO>> add(@Validated(Add.class) @RequestBody ModelProviderRequest request) {
        return getUserHeader().flatMap(header -> async(() -> {
            ModelProviderBO entityBO = modelProviderBuilder.buildBOByRequest(request);
            return R.ok(modelProviderBuilder.buildVOByBO(modelProviderService.add(entityBO, header)));
        }));
    }

    @PostMapping("/config/update")
    public Mono<R<ModelProviderVO>> update(@Validated(Update.class) @RequestBody ModelProviderRequest request) {
        return getUserHeader().flatMap(header -> async(() -> {
            ModelProviderBO entityBO = modelProviderBuilder.buildBOByRequest(request);
            return R.ok(modelProviderBuilder.buildVOByBO(modelProviderService.update(entityBO, header)));
        }));
    }

    @PostMapping("/config/delete")
    public Mono<R<Boolean>> delete(@NotNull @RequestParam(value = "id") Long id) {
        return async(() -> {
            modelProviderService.delete(id);
            return R.ok(true);
        });
    }

}
