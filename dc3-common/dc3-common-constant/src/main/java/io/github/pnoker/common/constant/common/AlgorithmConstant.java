/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.constant.common;

/**
 * 算法 相关常量
 *
 * @author pnoker
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
