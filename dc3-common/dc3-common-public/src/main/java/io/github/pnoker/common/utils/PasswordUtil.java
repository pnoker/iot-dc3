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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Password hashing utility using bcrypt.
 * Frontend sends {@code MD5(rawPassword)}, server stores {@code bcrypt(MD5(rawPassword))}.
 *
 * @author pnoker
 * @version 2026.5.19
 * @since 2026.5.19
 */
public class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder(12);

    private PasswordUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Encode a pre-hashed password with bcrypt.
     *
     * @param prehashed the pre-hashed password (MD5 of plaintext)
     * @return bcrypt hash string
     */
    public static String encode(String prehashed) {
        return ENCODER.encode(prehashed);
    }

    /**
     * Verify a pre-hashed password against a stored bcrypt hash.
     *
     * @param prehashed  the pre-hashed password to verify (MD5 of plaintext)
     * @param storedHash the bcrypt hash stored in the database
     * @return {@code true} if the password matches
     */
    public static boolean verify(String prehashed, String storedHash) {
        return ENCODER.matches(prehashed, storedHash);
    }

}
