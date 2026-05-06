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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Represents a loaded skill definition parsed from a classpath YAML file.
 * <p>
 * Each skill provides a scoped set of tools, a system prompt addition,
 * and few-shot examples that guide the LLM behavior for a particular
 * domain (e.g. device queries, data monitoring, device control).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillDefinition {

    /**
     * Unique skill identifier (matches the YAML filename stem by convention).
     */
    private String name;

    /**
     * Human-readable description of what this skill enables.
     */
    private String description;

    /**
     * Whether the skill is active and should be loaded at startup.
     */
    private boolean enabled;

    /**
     * Additional system prompt text injected when this skill is activated.
     */
    private String systemPromptAddition;

    /**
     * Names of the Spring AI tools this skill is allowed to use.
     */
    private List<String> tools;

    /**
     * Few-shot examples illustrating expected user-assistant interactions.
     */
    private List<SkillExample> examples;

    /**
     * A single user-assistant exchange used as a few-shot example.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillExample {

        /**
         * Example user message.
         */
        private String user;

        /**
         * Example assistant response.
         */
        private String assistant;
    }
}
