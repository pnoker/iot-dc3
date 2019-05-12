package com.pnoker.common.utils;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

/**
 * @author: Pnoker
 * @email: pnokers@gmail.com
 * @project: eidps
 * @copyright: Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>系统激活工具类</p>
 */
public class ActiveTools {
    /**
     * 激活状态，默认激活状态为 false
     */
    public static volatile boolean ACTIVE_STATE = false;

    /**
     * 密钥
     */
    public static String SECRET_KEY = "8kCTm2VdamnhwFliT6oLiw==";

    /**
     * 密钥算法
     * java6支持56位密钥，bouncycastle支持64位
     */
    public static final String KEY_ALGORITHM = "AES";

    /**
     * 加密/解密算法/工作模式/填充方式
     * <p>
     * JAVA6 支持PKCS5PADDING填充方式
     * Bouncy castle支持PKCS7Padding填充方式
     */
    public static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

    public static void verificate() {
        ACTIVE_STATE = true;
    }

    /**
     * 生成密钥，java6只支持56位密钥，bouncycastle支持64位密钥
     *
     * @return byte[] 二进制密钥
     */
    public static byte[] initkey() {
        KeyGenerator kg = null;
        try {
            kg = KeyGenerator.getInstance(KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        kg.init(128);
        SecretKey secretKey = kg.generateKey();
        return secretKey.getEncoded();
    }

    /**
     * 转换密钥
     *
     * @param key 二进制密钥
     * @return Key 密钥
     */
    public static Key toKey(byte[] key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
        return secretKey;
    }

    /**
     * 加密数据
     *
     * @param data 待加密数据
     * @param key  密钥
     * @return byte[] 加密后的数据
     */
    public static byte[] encrypt(byte[] data, byte[] key) throws Exception {
        Key k = toKey(key);
        /**
         * 实例化
         * 使用 PKCS7PADDING 填充方式，按如下方式实现,就是调用bouncycastle组件实现
         * Cipher.getInstance(CIPHER_ALGORITHM,"BC")
         */
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, k);
        return cipher.doFinal(data);
    }

    /**
     * 解密数据
     *
     * @param data 待解密数据
     * @param key  密钥
     * @return byte[] 解密后的数据
     */
    public static byte[] decrypt(byte[] data, byte[] key) throws Exception {
        Key k = toKey(key);
        /**
         * 实例化
         * 使用 PKCS7PADDING 填充方式，按如下方式实现,就是调用bouncycastle组件实现
         * Cipher.getInstance(CIPHER_ALGORITHM,"BC")
         */
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, k);
        return cipher.doFinal(data);
    }

    public static void main(String[] args) throws Exception {
        String str = "20181020";
        System.out.println("原文：" + str);
        //初始化密钥
        byte[] key = SECRET_KEY.getBytes("UTF-8");
        System.out.println("密钥：" + SECRET_KEY);
        //加密数据
        byte[] data = encrypt(str.getBytes(), key);
        System.out.println("加密后：" + new String(data));
        //解密数据
        data = decrypt(data, key);
        System.out.println("解密后：" + new String(data));
    }

}
