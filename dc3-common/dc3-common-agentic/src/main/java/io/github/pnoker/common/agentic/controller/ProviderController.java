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

import io.github.pnoker.common.agentic.entity.request.ModelProviderRequest;
import io.github.pnoker.common.agentic.entity.vo.ModelProviderVO;
import io.github.pnoker.common.agentic.service.ModelProviderService;
import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Agentic model provider endpoints.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@RestController
@RequestMapping(AgenticConstant.PROVIDER_URL_PREFIX)
public class ProviderController implements BaseController {

    private final ModelProviderService modelProviderService;

    public ProviderController(ModelProviderService modelProviderService) {
        this.modelProviderService = modelProviderService;
    }

    @GetMapping("/list")
    public Mono<R<List<ModelProviderVO>>> list() {
        return Mono.just(R.ok(modelProviderService.list()));
    }

    @PostMapping("/config/add")
    public Mono<R<ModelProviderVO>> add(@RequestBody ModelProviderRequest request) {
        return getUserHeader().flatMap(header -> async(() ->
                R.ok(modelProviderService.save(request, header))));
    }

    @PostMapping("/config/update")
    public Mono<R<ModelProviderVO>> update(@RequestBody ModelProviderRequest request) {
        return getUserHeader().flatMap(header -> async(() ->
                R.ok(modelProviderService.update(request, header))));
    }

    @PostMapping("/config/delete/{id}")
    public Mono<R<Boolean>> delete(@PathVariable Long id) {
        modelProviderService.remove(id);
        return Mono.just(R.ok(true));
    }

}
