/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

package com.dc3.common.utils;

import com.dc3.common.bean.Keys;
import com.dc3.common.constant.Common;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;

/**
 * Dc3 平台密钥工具类
 *
 * @author pnoker
 */
public class KeyUtil {

    /**
     * 生成AES密钥
     *
     * @return Keys.Aes
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    public static Keys.Aes genAesKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(Common.KEY_ALGORITHM_AES);
        keyGenerator.init(128);
        SecretKey secretKey = keyGenerator.generateKey();
        return new Keys.Aes(Dc3Util.encode(secretKey.getEncoded()));
    }

    /**
     * AES 私钥加密
     *
     * @param str        String
     * @param privateKey Private Key
     * @return Encrypt Aes
     * @throws Exception Exception
     */
    public static String encryptAes(String str, String privateKey) throws Exception {
        //base64编码的私钥
        byte[] keyBytes = Dc3Util.decode(privateKey);
        Key key = new SecretKeySpec(keyBytes, Common.KEY_ALGORITHM_AES);
        //AES加密
        Cipher cipher = Cipher.getInstance(Common.KEY_ALGORITHM_AES);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Dc3Util.encode(cipher.doFinal(str.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * AES 私钥解密
     *
     * @param str        String
     * @param privateKey Private Key
     * @return Decrypt Aes
     * @throws Exception Exception
     */
    public static String decryptAes(String str, String privateKey) throws Exception {
        //base64编码的私钥
        byte[] keyBytes = Dc3Util.decode(privateKey);
        Key key = new SecretKeySpec(keyBytes, Common.KEY_ALGORITHM_AES);
        //AES解密
        Cipher cipher = Cipher.getInstance(Common.KEY_ALGORITHM_AES);
        cipher.init(Cipher.DECRYPT_MODE, key);
        //64位解码加密后的字符串
        byte[] inputByte = Dc3Util.decode(str.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(inputByte), StandardCharsets.UTF_8);
    }

    /**
     * 生成RSA密钥对
     *
     * @return Keys.Rsa
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    public static Keys.Rsa genRsaKey() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(Common.KEY_ALGORITHM_RSA);
        keyPairGen.initialize(1024, new SecureRandom());
        KeyPair keyPair = keyPairGen.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String publicKeyString = Dc3Util.encode(publicKey.getEncoded());
        String privateKeyString = Dc3Util.encode((privateKey.getEncoded()));
        return new Keys.Rsa(publicKeyString, privateKeyString);
    }

    /**
     * RSA 公钥加密
     *
     * @param str       String
     * @param publicKey Public Key
     * @return Encrypt Rsa
     * @throws Exception Exception
     */
    public static String encryptRsa(String str, String publicKey) throws Exception {
        //base64编码的公钥
        byte[] keyBytes = Dc3Util.decode(publicKey);
        KeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance(Common.KEY_ALGORITHM_RSA).generatePublic(keySpec);
        //RSA加密
        Cipher cipher = Cipher.getInstance(Common.KEY_ALGORITHM_RSA);
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return Dc3Util.encode(cipher.doFinal(str.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * RSA 私钥解密
     *
     * @param str        String
     * @param privateKey Private Key
     * @return Decrypt Rsa
     * @throws Exception Exception
     */
    public static String decryptRsa(String str, String privateKey) throws Exception {
        //base64编码的私钥
        byte[] keyBytes = Dc3Util.decode(privateKey);
        KeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance(Common.KEY_ALGORITHM_RSA).generatePrivate(keySpec);
        //RSA解密
        Cipher cipher = Cipher.getInstance(Common.KEY_ALGORITHM_RSA);
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        //64位解码加密后的字符串
        byte[] inputByte = Dc3Util.decode(str.getBytes(StandardCharsets.UTF_8));
        return new String(cipher.doFinal(inputByte), StandardCharsets.UTF_8);
    }

    /**
     * 生成Token令牌
     *
     * @param name String
     * @return String
     */
    public static String generateToken(String name) {
        JwtBuilder builder = Jwts.builder()
                .setId(name)
                .setIssuedAt(new Date())
                .setExpiration(Dc3Util.expireTime(12, Calendar.HOUR))
                .signWith(SignatureAlgorithm.HS256, Common.KEY);
        return builder.compact();
    }

    /**
     * 解析Token令牌
     *
     * @param token String
     * @return Claims
     */
    public static Claims parserToken(String token) {
        return Jwts.parser()
                .setSigningKey(Common.KEY)
                .parseClaimsJws(token)
                .getBody();
    }
}
