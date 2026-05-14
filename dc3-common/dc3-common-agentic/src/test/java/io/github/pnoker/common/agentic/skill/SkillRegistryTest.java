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

package io.github.pnoker.common.agentic.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillRegistryTest {

    private static SkillDefinition skill(String name, List<String> tools) {
        SkillDefinition skill = new SkillDefinition();
        skill.setName(name);
        skill.setEnabled(true);
        skill.setDescription("d");
        skill.setSystemPromptAddition("");
        skill.setTools(tools);
        return skill;
    }

    @Test
    void registerAndLookupBySkillName() {
        SkillRegistry registry = new SkillRegistry();
        SkillDefinition skill = skill("device-query", List.of("listDevices"));
        registry.register(skill);
        assertThat(registry.get("device-query")).isSameAs(skill);
    }

    @Test
    void getReturnsNullWhenNameUnknown() {
        assertThat(new SkillRegistry().get("missing")).isNull();
    }

    @Test
    void registerOverwritesPreviousSkillWithSameName() {
        SkillRegistry registry = new SkillRegistry();
        SkillDefinition first = skill("dq", List.of("a"));
        SkillDefinition second = skill("dq", List.of("b"));
        registry.register(first);
        registry.register(second);
        assertThat(registry.get("dq")).isSameAs(second);
    }

    @Test
    void allReturnsRegisteredSkillsInInsertionOrder() {
        SkillRegistry registry = new SkillRegistry();
        registry.register(skill("a", List.of()));
        registry.register(skill("b", List.of()));
        registry.register(skill("c", List.of()));
        assertThat(registry.all()).extracting(SkillDefinition::getName).containsExactly("a", "b", "c");
    }

    @Test
    void allCollectionIsUnmodifiable() {
        SkillRegistry registry = new SkillRegistry();
        registry.register(skill("a", List.of()));
        assertThatThrownBy(() -> registry.all().add(skill("b", List.of())))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getEnabledToolNamesReturnsToolList() {
        SkillRegistry registry = new SkillRegistry();
        registry.register(skill("monitoring", List.of("readPoint", "summarize")));
        assertThat(registry.getEnabledToolNames("monitoring")).containsExactly("readPoint", "summarize");
    }

    @Test
    void getEnabledToolNamesReturnsNullWhenSkillUnknown() {
        assertThat(new SkillRegistry().getEnabledToolNames("missing")).isNull();
    }

    @Test
    void getEnabledToolNamesReturnsNullWhenSkillToolListEmpty() {
        SkillRegistry registry = new SkillRegistry();
        registry.register(skill("free-form", List.of()));
        assertThat(registry.getEnabledToolNames("free-form")).isNull();
    }

    @Test
    void getEnabledToolNamesReturnsNullWhenSkillToolListNull() {
        SkillRegistry registry = new SkillRegistry();
        SkillDefinition skill = skill("free-form", null);
        registry.register(skill);
        assertThat(registry.getEnabledToolNames("free-form")).isNull();
    }
}
