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

package io.github.pnoker.common.entity.ext;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * JSON extension object for rule runtime state metadata.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JSON extension object for rule runtime state metadata, embedded inside VO extension fields")
public class RuleStateExt extends BaseExt {

    /**
     * Extended content.
     */
    @Schema(description = "Extended content holding the rule runtime state snapshot")
    private Content content;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Rule runtime state snapshot captured at the time the state was updated")
    public static class Content {

        /**
         * Rule code at the time the state was updated.
         */
        @Schema(description = "Rule code at the time the state was updated")
        private String ruleCode;

        /**
         * Alarm severity at the time the state was updated.
         */
        @Schema(description = "Alarm severity at the time the state was updated")
        private String severity;

        /**
         * Alarm event type at the time the state was updated.
         */
        @Schema(description = "Alarm event type at the time the state was updated")
        private String eventType;

        /**
         * Rule labels at the time the state was updated.
         */
        @Schema(description = "Rule labels at the time the state was updated")
        private List<String> labels;

        /**
         * Latest normalized fact snapshot.
         */
        @Schema(description = "Latest normalized fact snapshot, keyed by fact name")
        private Map<String, Object> lastFact;

        /**
         * Rule match type at the time the state was updated.
         */
        @Schema(description = "Rule match type at the time the state was updated")
        private String matchType;

        /**
         * Runtime metadata that is not part of the matching key.
         */
        @Schema(description = "Runtime metadata that is not part of the matching key")
        private Map<String, Object> metadata;

    }

}
