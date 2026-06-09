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

import java.util.Map;

/**
 * Notification delivery request Ext.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification delivery request extension object, embedded inside the notify history VO extension field")
public class NotifyHistoryRequestExt extends BaseExt {

    /**
     * Extended content.
     */
    @Schema(description = "Rendered notification content carried by the delivery request")
    private Content content;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Rendered notification content, including title, body, template type and channel payload")
    public static class Content {

        /**
         * Rendered title.
         */
        @Schema(description = "Rendered notification title")
        private String title;

        /**
         * Rendered plain text or markdown summary.
         */
        @Schema(description = "Rendered notification body as plain text or markdown summary")
        private String text;

        /**
         * Message template type, for example TEXT, MARKDOWN or CARD.
         */
        @Schema(description = "Message template type, for example TEXT, MARKDOWN or CARD")
        private String templateType;

        /**
         * Template variables used during rendering.
         */
        @Schema(description = "Template variables used during rendering, keyed by variable name")
        private Map<String, Object> variables;

        /**
         * Channel request payload after rendering.
         */
        @Schema(description = "Channel-specific request payload produced after rendering")
        private Map<String, Object> payload;

    }

}
