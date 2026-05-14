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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillLoaderTest {

    @Test
    void loaderRegistersAllEnabledProductionSkills() throws Exception {
        SkillRegistry registry = new SkillRegistry();
        SkillLoader loader = new SkillLoader(registry);
        loader.run(null);

        // Prod ships device-query, device-control, data-monitor — all enabled by default.
        assertThat(registry.all()).extracting(SkillDefinition::getName)
                .contains("device-query", "device-control", "data-monitor");
    }

    @Test
    void mapToSkillParsesAllExpectedFields() throws Exception {
        SkillLoader loader = new SkillLoader(new SkillRegistry());
        Map<String, Object> map = new HashMap<>();
        map.put("name", "demo");
        map.put("description", "A demo skill");
        map.put("enabled", true);
        map.put("system-prompt-addition", "Always be polite.");
        map.put("tools", List.of("toolA", "toolB"));
        map.put("examples", List.of(Map.of("user", "hi", "assistant", "hello")));

        SkillDefinition skill = invokeMapToSkill(loader, map);

        assertThat(skill.getName()).isEqualTo("demo");
        assertThat(skill.getDescription()).isEqualTo("A demo skill");
        assertThat(skill.isEnabled()).isTrue();
        assertThat(skill.getSystemPromptAddition()).isEqualTo("Always be polite.");
        assertThat(skill.getTools()).containsExactly("toolA", "toolB");
        assertThat(skill.getExamples()).hasSize(1);
        assertThat(skill.getExamples().get(0).getUser()).isEqualTo("hi");
        assertThat(skill.getExamples().get(0).getAssistant()).isEqualTo("hello");
    }

    @Test
    void mapToSkillTreatsAbsentEnabledAsFalse() throws Exception {
        SkillLoader loader = new SkillLoader(new SkillRegistry());
        Map<String, Object> map = new HashMap<>();
        map.put("name", "demo");
        // enabled key missing -> Boolean.TRUE.equals(null) == false
        SkillDefinition skill = invokeMapToSkill(loader, map);
        assertThat(skill.isEnabled()).isFalse();
    }

    @Test
    void mapToSkillToleratesMissingExamples() throws Exception {
        SkillLoader loader = new SkillLoader(new SkillRegistry());
        Map<String, Object> map = new HashMap<>();
        map.put("name", "demo");
        map.put("enabled", true);
        SkillDefinition skill = invokeMapToSkill(loader, map);
        assertThat(skill.getExamples()).isNull();
    }

    private static SkillDefinition invokeMapToSkill(SkillLoader loader, Map<String, Object> map) throws Exception {
        Method method = SkillLoader.class.getDeclaredMethod("mapToSkill", Map.class);
        method.setAccessible(true);
        return (SkillDefinition) method.invoke(loader, map);
    }
}
