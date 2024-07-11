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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.constant.cache.TimeoutConstant;
import io.github.pnoker.common.constant.common.AlgorithmConstant;
import io.github.pnoker.common.constant.common.ExceptionConstant;
import io.github.pnoker.common.constant.common.SymbolConstant;
import io.github.pnoker.common.entity.auth.Keys;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;

/**
 * AES, RSA, JWT 相关工具类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
public class KeyUtil {

    private KeyUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 生成AES密钥
     *
     * @return Keys.Aes
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    public static Keys.Aes genAesKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AlgorithmConstant.ALGORITHM_AES);
        keyGenerator.init(256);
        SecretKey secretKey = keyGenerator.generateKey();
        return new Keys.Aes(DecodeUtil.byteToString(DecodeUtil.encode(secretKey.getEncoded())));
    }

    /**
     * 使用AES密钥加密
     *
     * @param content    String
     * @param privateKey Private Key
     * @return Encrypt Aes
     * @throws NoSuchPaddingException    NoSuchPaddingException
     * @throws NoSuchAlgorithmException  NoSuchAlgorithmException
     * @throws InvalidKeyException       InvalidKeyException
     * @throws IllegalBlockSizeException IllegalBlockSizeException
     * @throws BadPaddingException       BadPaddingException
     */
    public static String encryptAes(String content, String privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        //base64编码的私钥
        byte[] keyBytes = DecodeUtil.decode(privateKey);
        Key key = new SecretKeySpec(keyBytes, AlgorithmConstant.ALGORITHM_AES);
        //AES加密
        Cipher cipher = Cipher.getInstance(AlgorithmConstant.ALGORITHM_AES);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return DecodeUtil.byteToString(DecodeUtil.encode(cipher.doFinal(DecodeUtil.stringToByte(content))));
    }

    /**
     * 使用AES密钥解密
     *
     * @param content    String
     * @param privateKey Private Key
     * @return Decrypt Aes
     * @throws NoSuchPaddingException    NoSuchPaddingException
     * @throws NoSuchAlgorithmException  NoSuchAlgorithmException
     * @throws InvalidKeyException       InvalidKeyException
     * @throws IllegalBlockSizeException IllegalBlockSizeException
     * @throws BadPaddingException       BadPaddingException
     */
    public static String decryptAes(String content, String privateKey) throws NoSuchPaddingException, NoSuchAlgorithmException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        //base64编码的私钥
        byte[] keyBytes = DecodeUtil.decode(privateKey);
        Key key = new SecretKeySpec(keyBytes, AlgorithmConstant.ALGORITHM_AES);
        //AES解密
        Cipher cipher = Cipher.getInstance(AlgorithmConstant.ALGORITHM_AES);
        cipher.init(Cipher.DECRYPT_MODE, key);
        //64位解码加密后的字符串
        byte[] inputByte = DecodeUtil.decode(DecodeUtil.stringToByte(content));
        return DecodeUtil.byteToString(cipher.doFinal(inputByte));
    }

    /**
     * 生成RSA密钥对
     *
     * @return Keys.Rsa
     * @throws NoSuchAlgorithmException NoSuchAlgorithmException
     */
    public static Keys.Rsa genRsaKey() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(AlgorithmConstant.ALGORITHM_RSA);
        keyPairGen.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGen.generateKeyPair();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        String publicKeyString = DecodeUtil.byteToString(DecodeUtil.encode(publicKey.getEncoded()));
        String privateKeyString = DecodeUtil.byteToString(DecodeUtil.encode((privateKey.getEncoded())));
        return new Keys.Rsa(publicKeyString, privateKeyString);
    }

    /**
     * 使用RSA公钥加密
     *
     * @param content   String
     * @param publicKey Public Key
     * @return Encrypt Rsa
     * @throws NoSuchAlgorithmException  NoSuchAlgorithmException
     * @throws NoSuchPaddingException    NoSuchPaddingException
     * @throws InvalidKeyException       InvalidKeyException
     * @throws IllegalBlockSizeException IllegalBlockSizeException
     * @throws BadPaddingException       BadPaddingException
     * @throws InvalidKeySpecException   InvalidKeySpecException
     */
    public static String encryptRsa(String content, String publicKey) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
        //base64编码的公钥
        byte[] keyBytes = DecodeUtil.decode(publicKey);
        KeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance(AlgorithmConstant.ALGORITHM_RSA).generatePublic(keySpec);
        //RSA加密
        Cipher cipher = Cipher.getInstance(AlgorithmConstant.ALGORITHM_AES);
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return DecodeUtil.byteToString(DecodeUtil.encode(cipher.doFinal(DecodeUtil.stringToByte(content))));
    }

    /**
     * 使用RSA密钥解密
     *
     * @param content    String
     * @param privateKey Private Key
     * @return Decrypt Rsa
     * @throws NoSuchAlgorithmException  NoSuchAlgorithmException
     * @throws NoSuchPaddingException    NoSuchPaddingException
     * @throws InvalidKeyException       InvalidKeyException
     * @throws IllegalBlockSizeException IllegalBlockSizeException
     * @throws BadPaddingException       BadPaddingException
     * @throws InvalidKeySpecException   InvalidKeySpecException
     */
    public static String decryptRsa(String content, String privateKey) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, InvalidKeySpecException {
        //base64编码的私钥
        byte[] keyBytes = DecodeUtil.decode(privateKey);
        KeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance(AlgorithmConstant.ALGORITHM_RSA).generatePrivate(keySpec);
        //RSA解密
        Cipher cipher = Cipher.getInstance(AlgorithmConstant.ALGORITHM_RSA);
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        //64位解码加密后的字符串
        byte[] inputByte = DecodeUtil.decode(DecodeUtil.stringToByte(content));
        return DecodeUtil.byteToString(cipher.doFinal(inputByte));
    }

    /**
     * 生成Token令牌
     *
     * @param userName 用户名称
     * @param salt     Salt
     * @param tenantId 租户ID
     * @return Token String
     */
    public static String generateToken(String userName, String salt, Long tenantId) {
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(DecodeUtil.stringToByte(AlgorithmConstant.DEFAULT_KEY + SymbolConstant.COLON + salt));
        JwtBuilder builder = Jwts.builder()
                .issuer(AlgorithmConstant.DEFAULT_KEY + SymbolConstant.COLON + tenantId)
                .subject(AlgorithmConstant.DEFAULT_KEY + SymbolConstant.COLON + userName)
                .issuedAt(new Date())
                .signWith(key, Jwts.SIG.HS256)
                .expiration(TimeUtil.expireTime(TimeoutConstant.TOKEN_CACHE_TIMEOUT, Calendar.HOUR));
        return builder.compact();
    }

    /**
     * 解析Token令牌
     *
     * @param userName 用户名称
     * @param salt     Salt
     * @param token    Token
     * @param tenantId 租户ID
     * @return Claims
     */
    public static Claims parserToken(String userName, String salt, String token, Long tenantId) {
        SecretKey key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(DecodeUtil.stringToByte(AlgorithmConstant.DEFAULT_KEY + SymbolConstant.COLON + salt));
        JwtParser parser = Jwts.parser()
                .requireIssuer(AlgorithmConstant.DEFAULT_KEY + SymbolConstant.COLON + tenantId)
                .requireSubject(AlgorithmConstant.DEFAULT_KEY + SymbolConstant.COLON + userName)
                .verifyWith(key)
                .build();
        return parser.parseSignedClaims(token).getPayload();
    }

}
