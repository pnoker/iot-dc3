package com.pnoker.common.bean.encryp;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: AES & RSA 算法密钥实体类
 */
public class Keys {

    /**
     * RSA 密钥对
     */
    @Data
    @AllArgsConstructor
    public class Rsa {
        private String publicKey;
        private String privateKey;
    }

    /**
     * Aes 密钥
     */
    @Data
    @AllArgsConstructor
    public class Aes {
        private String privateKey;
    }
}
