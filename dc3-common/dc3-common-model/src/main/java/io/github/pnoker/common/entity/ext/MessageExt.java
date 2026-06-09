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
 * JSON extension object for message metadata.
 * <p>
 * Extended information related to messages.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "JSON extension object for message metadata, carrying message-related extended information")
public class MessageExt extends BaseExt {

    /**
     * Extended content.
     * <p>
     * The content can be distinguished by Type and Version.
     */
    @Schema(description = "Extended content, distinguished by the inherited type and version fields")
    private Content content;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Extended content holding template variables and channel-specific templates")
    public static class Content {

        /**
         * Variables expected by the templates.
         */
        @Schema(description = "Variables expected by the templates")
        private List<String> variables;

        /**
         * Channel-specific templates. Destinations and credentials belong to notification channels.
         */
        @Schema(description = "Channel-specific templates; destinations and credentials belong to notification channels")
        private List<Template> templates;

    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Channel-specific message template definition")
    public static class Template {

        /**
         * Channel type such as FEISHU_BOT, WEBHOOK, or EMAIL.
         */
        @Schema(description = "Channel type such as FEISHU_BOT, WEBHOOK, or EMAIL", example = "FEISHU_BOT")
        private String channelType;

        /**
         * Payload type such as CARD, TEXT, JSON, or HTML.
         */
        @Schema(description = "Payload type such as CARD, TEXT, JSON, or HTML", example = "CARD")
        private String payloadType;

        /**
         * Structured template payload to render.
         */
        @Schema(description = "Structured template payload to render")
        private Map<String, Object> template;

    }

}
