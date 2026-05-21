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

package io.github.pnoker.common.data.biz.alarm;

import lombok.RequiredArgsConstructor;
import io.github.pnoker.common.data.entity.property.NotifyCredentialProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves notification credentials from deployment properties.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Service
@RequiredArgsConstructor
public class PropertyNotifyCredentialResolver implements NotifyCredentialResolver {

    private final NotifyCredentialProperties properties;

    @Override
    public Optional<NotifyCredential> resolve(String credentialRef) {
        if (StringUtils.isBlank(credentialRef) || Objects.isNull(properties.getCredentials())) {
            return Optional.empty();
        }

        NotifyCredentialProperties.Credential credential = properties.getCredentials().get(credentialRef);
        if (Objects.isNull(credential)) {
            return Optional.empty();
        }
        return Optional.of(new NotifyCredential(
                credential.getWebhookUrl(),
                credential.getSecret(),
                Objects.requireNonNullElse(credential.getHeaders(), Map.of())));
    }

}
