/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.common.tool;

import com.google.common.base.Charsets;
import com.pnoker.common.bean.Keys;
import com.pnoker.common.constant.Common;

import javax.crypto.Cipher;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
public class RsaTools {

    /**
     * 生成RSA密钥对
     *
     * @return Keys.Rsa
     * @throws NoSuchAlgorithmException
     */
    public static Keys.Rsa genKey() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(Common.KEY_ALGORITHM_RSA);
        keyPairGen.initialize(1024, new SecureRandom());
        KeyPair keyPair = keyPairGen.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String publicKeyString = Dc3Tools.encodeToString(publicKey.getEncoded());
        String privateKeyString = Dc3Tools.encodeToString((privateKey.getEncoded()));
        Keys.Rsa rsa = new Keys().new Rsa(publicKeyString, privateKeyString);
        return rsa;
    }

    /**
     * RSA 公钥加密
     *
     * @param str
     * @param publicKey
     * @return
     * @throws Exception
     */
    public static String encrypt(String str, String publicKey) throws Exception {
        //base64编码的公钥
        byte[] keyBytes = Dc3Tools.decode(publicKey);
        KeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance(Common.KEY_ALGORITHM_RSA).generatePublic(keySpec);
        //RSA加密
        Cipher cipher = Cipher.getInstance(Common.KEY_ALGORITHM_RSA);
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        String outStr = Dc3Tools.encodeToString(cipher.doFinal(str.getBytes(Charsets.UTF_8)));
        return outStr;
    }

    /**
     * RSA 私钥解密
     *
     * @param str
     * @param privateKey
     * @return
     * @throws Exception
     */
    public static String decrypt(String str, String privateKey) throws Exception {
        //base64编码的私钥
        byte[] keyBytes = Dc3Tools.decode(privateKey);
        KeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance(Common.KEY_ALGORITHM_RSA).generatePrivate(keySpec);
        //RSA解密
        Cipher cipher = Cipher.getInstance(Common.KEY_ALGORITHM_RSA);
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        //64位解码加密后的字符串
        byte[] inputByte = Dc3Tools.decode(str.getBytes(Charsets.UTF_8));
        return new String(cipher.doFinal(inputByte));
    }
}
