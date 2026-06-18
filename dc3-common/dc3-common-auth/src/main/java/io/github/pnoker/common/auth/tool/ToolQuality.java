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
package io.github.pnoker.common.auth.tool;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

/**
 * Quality facts parsed from one OpenAPI operation: the AI-facing text and the six
 * {@code x-dc3-ai} flags plus the merged input schema. {@code Boolean}/{@code String}
 * fields are {@code null} when the operation did not declare them, so the catalog
 * refresh can apply conservative defaults.
 *
 * @author pnoker
 */
@Getter
@Builder
@ToString
public class ToolQuality {

    /** Operation summary → MCP tool title. */
    private final String summary;

    /** Operation description (or x-dc3-ai.description override when present) → MCP tool description. */
    private final String description;

    /** Declared MCP risk level (HIGH/MEDIUM/LOW); null when undeclared. */
    private final String riskLevel;

    private final Boolean destructive;

    private final Boolean idempotent;

    private final Boolean openWorld;

    /** Hidden from tools/list; null treated as false. */
    private final Boolean hidden;

    /** AI-facing description override from x-dc3-ai.description; null when absent. */
    private final String aiDescription;

    /** Merged input JSON Schema string, or null when the operation has no body/params. */
    private final String inputSchema;
}
