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

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Central registry that holds all {@link SkillDefinition} instances loaded
 * at startup. Supports lookup by name and enumeration of all registered skills.
 */
@Component
public class SkillRegistry {

    private final Map<String, SkillDefinition> skills = new LinkedHashMap<>();

    /**
     * Register a skill definition. Overwrites any existing skill with the same name.
     *
     * @param skill the skill to register
     */
    public void register(SkillDefinition skill) {
        skills.put(skill.getName(), skill);
    }

    /**
     * Look up a skill by name.
     *
     * @param name the skill name
     * @return the matching skill, or {@code null} if not found
     */
    public SkillDefinition get(String name) {
        return skills.get(name);
    }

    /**
     * Return all registered skills in insertion order.
     *
     * @return unmodifiable collection of all skills
     */
    public Collection<SkillDefinition> all() {
        return Collections.unmodifiableCollection(skills.values());
    }

    /**
     * Return the tool names associated with the given skill.
     * <p>
     * Returns {@code null} (meaning "all tools available") when the skill
     * is not found or the skill's tool list is empty.
     *
     * @param skillName the skill to look up
     * @return list of allowed tool names, or {@code null} for unrestricted
     */
    public List<String> getEnabledToolNames(String skillName) {
        SkillDefinition skill = skills.get(skillName);
        if (skill == null) {
            return null;
        }
        List<String> tools = skill.getTools();
        return (tools == null || tools.isEmpty()) ? null : tools;
    }
}
