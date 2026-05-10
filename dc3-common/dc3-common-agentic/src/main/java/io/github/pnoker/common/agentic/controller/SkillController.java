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

import io.github.pnoker.common.agentic.entity.vo.SkillVO;
import io.github.pnoker.common.agentic.skill.SkillRegistry;
import io.github.pnoker.common.constant.service.AgenticConstant;
import io.github.pnoker.common.entity.R;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Agentic skill metadata endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2022.1.0
 */
@RestController
@RequestMapping(AgenticConstant.SKILL_URL_PREFIX)
public class SkillController {

    private final SkillRegistry skillRegistry;

    public SkillController(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
    }

    @GetMapping("/list")
    public Mono<R<List<SkillVO>>> list() {
        List<SkillVO> skills = skillRegistry.all()
                .stream()
                .map(skill -> new SkillVO(skill.getName(), skill.getDescription()))
                .toList();
        return Mono.just(R.ok(skills));
    }

}
