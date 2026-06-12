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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.enums.PasswordAlgorithmEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Password hashing utility for server-side raw password handling.
 *
 * @author pnoker
 * @version 2026.6.12
 * @since 2026.5.19
 */
@Slf4j
public class PasswordUtil {

    private static final BCryptPasswordEncoder BCRYPT_ENCODER = new BCryptPasswordEncoder(12);

    private PasswordUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Encode a raw password with Argon2id. If the runtime cannot provide Argon2
     * support, fall back to bcrypt and let callers persist the resulting algorithm
     * through {@link #algorithmOfHash(String)}.
     *
     * @param rawPassword raw password received over HTTPS
     * @return password hash string
     */
    public static String encode(String rawPassword) {
        try {
            return argon2().encode(rawPassword);
        } catch (RuntimeException | LinkageError e) {
            log.warn("Argon2id password encoding is unavailable, falling back to bcrypt", e);
            return BCRYPT_ENCODER.encode(rawPassword);
        }
    }

    /**
     * Verify a raw password against a stored Argon2id or bcrypt hash.
     *
     * @param rawPassword raw password to verify
     * @param storedHash  password hash stored in the database
     * @return {@code true} if the password matches
     */
    public static boolean verify(String rawPassword, String storedHash) {
        if (StringUtils.isAnyBlank(rawPassword, storedHash)) {
            return false;
        }
        PasswordAlgorithmEnum algorithm = algorithmOfHash(storedHash);
        return switch (algorithm) {
            case ARGON2ID -> argon2().matches(rawPassword, storedHash);
            case BCRYPT -> BCRYPT_ENCODER.matches(rawPassword, storedHash);
        };
    }

    /**
     * Resolve the persisted algorithm from a generated password hash.
     *
     * @param hash password hash
     * @return password algorithm
     */
    public static PasswordAlgorithmEnum algorithmOfHash(String hash) {
        if (StringUtils.startsWith(hash, "$argon2")) {
            return PasswordAlgorithmEnum.ARGON2ID;
        }
        return PasswordAlgorithmEnum.BCRYPT;
    }

    private static Argon2PasswordEncoder argon2() {
        return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

}
