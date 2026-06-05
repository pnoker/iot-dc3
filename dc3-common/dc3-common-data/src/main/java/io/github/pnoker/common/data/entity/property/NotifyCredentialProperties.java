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

package io.github.pnoker.common.data.entity.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Notification credential references.
 * <p>
 * This is a deployment-level resolver for {@code credentialRef}. The database stores
 * only the reference; secrets stay outside message templates and notification channel rows.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "dc3.data.notify")
public class NotifyCredentialProperties {

    /**
     * Credential map keyed by credentialRef.
     */
    private Map<String, Credential> credentials = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class Credential {

        /**
         * Target webhook URL.
         */
        private String webhookUrl;

        /**
         * Signing secret for providers that require one.
         */
        private String secret;

        /**
         * Additional non-secret request headers.
         */
        private Map<String, String> headers = new LinkedHashMap<>();

    }

}
