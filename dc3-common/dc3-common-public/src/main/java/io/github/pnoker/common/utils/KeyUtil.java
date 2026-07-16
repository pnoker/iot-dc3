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

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;

/**
 * AES/RSA encryption and JWT token utility.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
public class KeyUtil {

    /**
     * GCM initialization vector length in bytes (96-bit, the recommended size).
     */
    private static final int GCM_IV_LENGTH = 12;

    /**
     * GCM authentication tag length in bits.
     */
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private KeyUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * Generate an AES key.
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
     * Encrypt content using an AES key (AES/GCM/NoPadding with a random IV).
     * <p>
     * The output is Base64({@code IV || ciphertext+tag}); a fresh random IV is prepended so
     * every call produces distinct ciphertext for the same plaintext.
     *
     * @param content    String
     * @param privateKey Private key in Base64 encoding
     * @return Encrypted AES content
     * @throws NoSuchPaddingException             NoSuchPaddingException
     * @throws NoSuchAlgorithmException            NoSuchAlgorithmException
     * @throws InvalidKeyException                InvalidKeyException
     * @throws IllegalBlockSizeException          IllegalBlockSizeException
     * @throws BadPaddingException                BadPaddingException
     * @throws InvalidAlgorithmParameterException InvalidAlgorithmParameterException
     */
    public static String encryptAes(String content, String privateKey) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException {
        // Base64-encoded private key
        byte[] keyBytes = DecodeUtil.decode(privateKey);
        Key key = new SecretKeySpec(keyBytes, AlgorithmConstant.ALGORITHM_AES);
        // Random IV per encryption
        byte[] iv = new byte[GCM_IV_LENGTH];
        SECURE_RANDOM.nextBytes(iv);
        // AES-GCM encryption
        Cipher cipher = Cipher.getInstance(AlgorithmConstant.TRANSFORM_AES_GCM);
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        byte[] ciphertext = cipher.doFinal(DecodeUtil.stringToByte(content));
        // Prepend IV so the ciphertext is self-contained: IV || ciphertext+tag
        byte[] output = ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array();
        return DecodeUtil.byteToString(DecodeUtil.encode(output));
    }

    /**
     * Decrypt content using an AES key (AES/GCM/NoPadding with a prepended IV).
     *
     * @param content    String
     * @param privateKey Private key in Base64 encoding
     * @return Decrypted AES content
     * @throws NoSuchPaddingException             NoSuchPaddingException
     * @throws NoSuchAlgorithmException            NoSuchAlgorithmException
     * @throws InvalidKeyException                InvalidKeyException
     * @throws IllegalBlockSizeException          IllegalBlockSizeException
     * @throws BadPaddingException                BadPaddingException
     * @throws InvalidAlgorithmParameterException InvalidAlgorithmParameterException
     */
    public static String decryptAes(String content, String privateKey) throws NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException {
        // Base64-encoded private key
        byte[] keyBytes = DecodeUtil.decode(privateKey);
        Key key = new SecretKeySpec(keyBytes, AlgorithmConstant.ALGORITHM_AES);
        // Decode the encrypted string from Base64 and split IV || ciphertext+tag
        byte[] inputByte = DecodeUtil.decode(DecodeUtil.stringToByte(content));
        ByteBuffer buffer = ByteBuffer.wrap(inputByte);
        byte[] iv = new byte[GCM_IV_LENGTH];
        buffer.get(iv);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);
        // AES-GCM decryption
        Cipher cipher = Cipher.getInstance(AlgorithmConstant.TRANSFORM_AES_GCM);
        cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return DecodeUtil.byteToString(cipher.doFinal(ciphertext));
    }

    /**
     * Generate an RSA key pair.
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
     * Encrypt content using an RSA public key.
     *
     * @param content   String
     * @param publicKey Public key in Base64 encoding
     * @return Encrypted RSA content
     * @throws NoSuchAlgorithmException  NoSuchAlgorithmException
     * @throws NoSuchPaddingException    NoSuchPaddingException
     * @throws InvalidKeyException       InvalidKeyException
     * @throws IllegalBlockSizeException IllegalBlockSizeException
     * @throws BadPaddingException       BadPaddingException
     * @throws InvalidKeySpecException   InvalidKeySpecException
     */
    public static String encryptRsa(String content, String publicKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeySpecException {
        // Base64-encoded public key
        byte[] keyBytes = DecodeUtil.decode(publicKey);
        KeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance(AlgorithmConstant.ALGORITHM_RSA)
                .generatePublic(keySpec);
        // RSA encryption
        Cipher cipher = Cipher.getInstance(AlgorithmConstant.TRANSFORM_RSA_OAEP);
        cipher.init(Cipher.ENCRYPT_MODE, pubKey);
        return DecodeUtil.byteToString(DecodeUtil.encode(cipher.doFinal(DecodeUtil.stringToByte(content))));
    }

    /**
     * Decrypt content using an RSA private key.
     *
     * @param content    String
     * @param privateKey Private key in Base64 encoding
     * @return Decrypted RSA content
     * @throws NoSuchAlgorithmException  NoSuchAlgorithmException
     * @throws NoSuchPaddingException    NoSuchPaddingException
     * @throws InvalidKeyException       InvalidKeyException
     * @throws IllegalBlockSizeException IllegalBlockSizeException
     * @throws BadPaddingException       BadPaddingException
     * @throws InvalidKeySpecException   InvalidKeySpecException
     */
    public static String decryptRsa(String content, String privateKey)
            throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, InvalidKeySpecException {
        // Base64-encoded private key
        byte[] keyBytes = DecodeUtil.decode(privateKey);
        KeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance(AlgorithmConstant.ALGORITHM_RSA)
                .generatePrivate(keySpec);
        // RSA decryption
        Cipher cipher = Cipher.getInstance(AlgorithmConstant.TRANSFORM_RSA_OAEP);
        cipher.init(Cipher.DECRYPT_MODE, priKey);
        // Decode the encrypted string from Base64
        byte[] inputByte = DecodeUtil.decode(DecodeUtil.stringToByte(content));
        return DecodeUtil.byteToString(cipher.doFinal(inputByte));
    }

    /**
     * Resolve the JWT signing key from the {@code DC3_SECURITY_KEY} environment variable
     * or the {@code dc3.security.key} system property, throwing when neither is set.
     *
     * @return the security key
     */
    private static String getSecurityKey() {
        String key = System.getenv("DC3_SECURITY_KEY");
        if (key == null || key.isBlank()) {
            key = System.getProperty("dc3.security.key");
        }
        if (key == null || key.isBlank()) {
            throw new IllegalStateException(
                    "DC3_SECURITY_KEY environment variable or dc3.security.key property is required but not configured");
        }
        return key;
    }

    /**
     * Generate a JWT token.
     *
     * @param subject  token subject, normally principal ID
     * @param salt     Salt
     * @param tenantId Tenant ID
     * @return Token string
     */
    public static String generateToken(String subject, String salt, Long tenantId) {
        String securityKey = getSecurityKey();
        SecretKey key = io.jsonwebtoken.security.Keys
                .hmacShaKeyFor(DecodeUtil.stringToByte(securityKey + SymbolConstant.COLON + salt));
        JwtBuilder builder = Jwts.builder()
                .issuer(securityKey + SymbolConstant.COLON + tenantId)
                .subject(securityKey + SymbolConstant.COLON + subject)
                .issuedAt(new Date())
                .signWith(key, Jwts.SIG.HS256)
                .expiration(TimeUtil.expireTime(TimeoutConstant.TOKEN_CACHE_TIMEOUT, Calendar.HOUR));
        return builder.compact();
    }

    /**
     * Parse and validate a JWT token.
     *
     * @param subject  token subject, normally principal ID
     * @param salt     Salt
     * @param token    Token string
     * @param tenantId Tenant ID
     * @return Claims
     */
    public static Claims parserToken(String subject, String salt, String token, Long tenantId) {
        String securityKey = getSecurityKey();
        SecretKey key = io.jsonwebtoken.security.Keys
                .hmacShaKeyFor(DecodeUtil.stringToByte(securityKey + SymbolConstant.COLON + salt));
        JwtParser parser = Jwts.parser()
                .requireIssuer(securityKey + SymbolConstant.COLON + tenantId)
                .requireSubject(securityKey + SymbolConstant.COLON + subject)
                .verifyWith(key)
                .build();
        return parser.parseSignedClaims(token).getPayload();
    }

}
