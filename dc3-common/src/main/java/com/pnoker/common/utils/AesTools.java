/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.pnoker.common.utils;

import com.google.common.base.Charsets;
import com.pnoker.common.model.dto.Keys;
import com.pnoker.common.utils.Tools;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
public class AesTools {

    public static final String KEY_ALGORITHM = "AES";

    /**
     * 生成AES密钥
     *
     * @return Keys.Aes
     * @throws NoSuchAlgorithmException
     */
    public static Keys.Aes genKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGenerator.init(128);
        SecretKey secretKey = keyGenerator.generateKey();
        Keys.Aes aes = new Keys().new Aes(Tools.encodeToString(secretKey.getEncoded()));
        return aes;
    }

    /**
     * AES 私钥加密
     *
     * @param str
     * @param privateKey
     * @return
     * @throws Exception
     */
    public static String encrypt(String str, String privateKey) throws Exception {
        //base64编码的私钥
        byte[] keyBytes = Tools.decode(privateKey);
        Key key = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        //AES加密
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        String outStr = Tools.encodeToString(cipher.doFinal(str.getBytes(Charsets.UTF_8)));
        return outStr;
    }

    /**
     * AES 私钥解密
     *
     * @param str
     * @param privateKey
     * @return
     * @throws Exception
     */
    public static String decrypt(String str, String privateKey) throws Exception {
        //base64编码的私钥
        byte[] keyBytes = Tools.decode(privateKey);
        Key key = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        //AES解密
        Cipher cipher = Cipher.getInstance(KEY_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        //64位解码加密后的字符串
        byte[] inputByte = Tools.decode(str.getBytes(Charsets.UTF_8));
        return new String(cipher.doFinal(inputByte));
    }
}
