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
 * Notify channel Ext.
 * <p>
 * Non-sensitive channel configuration. Secrets are referenced by credentialRef.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Schema(description = "Notify channel extension object. Holds non-sensitive channel configuration; secrets are referenced by credentialRef.")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotifyChannelExt extends BaseExt {

    /**
     * Extended content.
     */
    @Schema(description = "Extended content holding the channel configuration.")
    private Content content;

    @Schema(description = "Extended content for a notify channel.")
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {

        /**
         * Whether channel signing is enabled.
         */
        @Schema(description = "Whether channel signing is enabled.", example = "true")
        private Boolean signEnabled;

        /**
         * Message card version used by card-capable channels.
         */
        @Schema(description = "Message card version used by card-capable channels.")
        private String cardVersion;

        /**
         * Whether this channel allows at-all style mentions.
         */
        @Schema(description = "Whether this channel allows at-all style mentions.", example = "false")
        private Boolean atAllAllowed;

        /**
         * Whether test messages can be sent from the management UI.
         */
        @Schema(description = "Whether test messages can be sent from the management UI.", example = "true")
        private Boolean testMessageEnabled;

        /**
         * Provider-specific non-sensitive options.
         */
        @Schema(description = "Provider-specific non-sensitive options.")
        private Map<String, Object> options;

    }

}
