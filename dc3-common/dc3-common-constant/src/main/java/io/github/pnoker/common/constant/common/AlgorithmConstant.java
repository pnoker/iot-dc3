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
 * 算法 相关常量
 *
 * @author pnoker
 * @version 2025.6.0
 * @since 2022.1.0
 */
public class AlgorithmConstant {

    /**
     * 默认密钥
     */
    public static final String DEFAULT_KEY = "io.github.pnoker.dc3";
    /**
     * 默认密码
     */
    public static final String DEFAULT_PASSWORD = "dc3dc3dc3";
    /**
     * 加密算法 对称AES
     */
    public static final String ALGORITHM_AES = "AES";
    /**
     * 加密算法 非对称RSA
     */
    public static final String ALGORITHM_RSA = "RSA";
    /**
     * 加密算法 SHA256withRSA
     */
    public static final String ALGORITHM_SHA256_RSA = "SHA256withRSA";
    /**
     * 证书类型 X.509
     */
    public static final String CERTIFICATE_X509 = "X.509";
    /**
     * 证书类型 PKCS12
     */
    public static final String CERTIFICATE_PKCS12 = "PKCS12";
    /**
     * 证书类型 JKS
     */
    public static final String CERTIFICATE_JKS = "jks";

    private AlgorithmConstant() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }
}
