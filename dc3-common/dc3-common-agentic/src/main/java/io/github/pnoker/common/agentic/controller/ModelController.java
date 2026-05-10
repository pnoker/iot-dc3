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

import io.github.pnoker.common.agentic.entity.request.ModelConfigRequest;
import io.github.pnoker.common.agentic.entity.vo.ModelConfigVO;
import io.github.pnoker.common.agentic.entity.vo.ModelVO;
import io.github.pnoker.common.agentic.service.ModelConfigService;
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
 * Agentic model option endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@RestController
@RequestMapping(AgenticConstant.MODEL_URL_PREFIX)
public class ModelController {

    private final ModelConfigService modelConfigService;

    public ModelController(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    @GetMapping("/list")
    public Mono<R<List<ModelVO>>> list() {
        return Mono.just(R.ok(modelConfigService.listOptions()));
    }

    @GetMapping("/config/list")
    public Mono<R<List<ModelConfigVO>>> listConfigs() {
        return Mono.just(R.ok(modelConfigService.listConfigs()));
    }

    @PostMapping("/config/add")
    public Mono<R<ModelConfigVO>> add(@RequestBody ModelConfigRequest request) {
        return Mono.just(R.ok(modelConfigService.save(request)));
    }

    @PostMapping("/config/update")
    public Mono<R<ModelConfigVO>> update(@RequestBody ModelConfigRequest request) {
        return Mono.just(R.ok(modelConfigService.update(request)));
    }

    @PostMapping("/config/delete/{id}")
    public Mono<R<Boolean>> delete(@PathVariable Long id) {
        modelConfigService.remove(id);
        return Mono.just(R.ok(true));
    }

}
