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
package io.github.pnoker.common.auth.entity.oauth;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/** Persisted OAuth consent projection scoped to client, principal and tenant. */
@Getter
@Setter
@ToString
public class OAuthConsentRecord {
    private Long id;
    private Long registeredClientId;
    private String clientId;
    private Long principalId;
    private Long tenantId;
    private String scopes;
    private String consentExt;
}
