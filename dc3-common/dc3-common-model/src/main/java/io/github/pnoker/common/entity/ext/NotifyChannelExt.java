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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotifyChannelExt extends BaseExt {

    /**
     * Extended content.
     */
    private Content content;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {

        /**
         * Whether channel signing is enabled.
         */
        private Boolean signEnabled;

        /**
         * Message card version used by card-capable channels.
         */
        private String cardVersion;

        /**
         * Whether this channel allows at-all style mentions.
         */
        private Boolean atAllAllowed;

        /**
         * Whether test messages can be sent from the management UI.
         */
        private Boolean testMessageEnabled;

        /**
         * Provider-specific non-sensitive options.
         */
        private Map<String, Object> options;

    }

}
