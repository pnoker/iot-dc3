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

package io.github.pnoker.common.auth.entity.query;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Query parameters for token validation and generation.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@Schema(description = "Query parameters for token validation, login, and password-change operations.")
public class TokenQuery {

    /**
     * Tenant
     */
    @Schema(description = "Tenant code that scopes the authentication request; must match an existing tenant. Format: 2–32 alphanumeric or -_#@/.|characters.", example = "dc3-tenant-01")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "Invalid tenant code")
    private String tenant;

    /**
     *
     */
    @Schema(description = "Username of the account being authenticated; must exist within the specified tenant. Format: 2–32 alphanumeric or -_#@/.|characters.", example = "alice")
    @Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9-_#@/.|]{1,31}$", message = "Invalid username")
    private String name;

    /**
     *
     */
    @Schema(description = "Random salt string issued by the server during the login handshake; must be echoed back when submitting credentials.", example = "a3f9c2d1")
    private String salt;

    /**
     *
     */
    @Schema(description = "Credential submitted for authentication; the plaintext password, protected in transit by HTTPS. The salt is NOT mixed into the password — it is concatenated with the server-side key to sign the JWT.", example = "your-plaintext-password")
    private String password;

    /**
     *
     */
    @Schema(description = "JWT or session token to be validated or invalidated; used by token-check and logout endpoints.", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbGljZSJ9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
    private String token;

    /**
     * New password, used by the self-service password change flow; {@code password} carries
     * the current password in that flow.
     */
    @Schema(description = "Replacement password for the self-service password change flow; {@code password} must carry the current credential in the same request.", example = "N3wP@ssw0rd!")
    private String newPassword;

}
