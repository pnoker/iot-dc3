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

import io.github.pnoker.common.agentic.skill.SkillDefinition;
import io.github.pnoker.common.agentic.skill.SkillRegistry;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import reactor.test.StepVerifier;

class SkillControllerTest {

    private static SkillDefinition skill(String name, String description) {
        SkillDefinition skill = new SkillDefinition();
        skill.setName(name);
        skill.setDescription(description);
        skill.setEnabled(true);
        return skill;
    }

    @Test
    void listExposesEverySkillAsVO() {
        SkillRegistry registry = new SkillRegistry();
        registry.register(skill("device-query", "Device read"));
        registry.register(skill("device-control", "Device write"));
        SkillController controller = new SkillController(registry);

        StepVerifier.create(controller.list())
                .assertNext(response -> {
                    assertThat(response.isOk()).isTrue();
                    assertThat(response.getData()).hasSize(2);
                    assertThat(response.getData()).extracting("name")
                            .containsExactly("device-query", "device-control");
                    assertThat(response.getData()).extracting("description")
                            .containsExactly("Device read", "Device write");
                })
                .verifyComplete();
    }

    @Test
    void listReturnsEmptyListWhenNothingRegistered() {
        SkillController controller = new SkillController(new SkillRegistry());
        StepVerifier.create(controller.list())
                .assertNext(response -> {
                    assertThat(response.isOk()).isTrue();
                    assertThat(response.getData()).isEmpty();
                })
                .verifyComplete();
    }
}
