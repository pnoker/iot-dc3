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

package io.github.pnoker.common.constant.common;

/**
 * Algorithm related constants
 * <p>
 * Provides constants for encryption algorithms, certificate types, and default
 * keys/passwords.
 * </p>
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public class AlgorithmConstant {

    /**
     * Default encryption key (fallback only).
     * <p>
     * Production deployments should set the {@code DC3_SECURITY_KEY} environment variable
     * or the {@code dc3.security.key} property instead.
     */
    public static final String DEFAULT_KEY = "io.github.pnoker.dc3";

    /**
     * Default password (fallback only).
     * <p>
     * Production deployments should set the {@code DC3_SECURITY_DEFAULT_PASSWORD} environment
     * variable or the {@code dc3.security.default-password} property instead.
     */
    public static final String DEFAULT_PASSWORD = "dc3dc3dc3";

    /**
     * Symmetric encryption algorithm: AES
     */
    public static final String ALGORITHM_AES = "AES";

    /**
     * Asymmetric encryption algorithm: RSA
     */
    public static final String ALGORITHM_RSA = "RSA";

    /**
     * Encryption algorithm: SHA256withRSA
     */
    public static final String ALGORITHM_SHA256_RSA = "SHA256withRSA";

    /**
     * Certificate type: X.509
     */
    public static final String CERTIFICATE_X509 = "X.509";

    /**
     * Certificate type: PKCS12
     */
    public static final String CERTIFICATE_PKCS12 = "PKCS12";

    /**
     * Certificate type: JKS (Java KeyStore)
     */
    public static final String CERTIFICATE_JKS = "jks";

    private AlgorithmConstant() {
        throw new IllegalStateException(BaseConstant.UTILITY_CLASS);
    }

}
