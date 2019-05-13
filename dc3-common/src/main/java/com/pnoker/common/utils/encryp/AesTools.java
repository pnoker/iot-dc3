package com.pnoker.common.utils.encryp;

import com.google.common.base.Charsets;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * @author: Pnoker
 * @email: pnokers@gmail.com
 * @project: eidps
 * @copyright: Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>系统激活工具类</p>
 */
public class AesTools {

    public static final String KEY_ALGORITHM = "AES";

    public static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

    public static byte[] initkey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        keyGenerator.init(192);
        SecretKey secretKey = keyGenerator.generateKey();
        return secretKey.getEncoded();
    }

    public static Key toKey(byte[] key) throws Exception {
        SecretKey secretKey = new SecretKeySpec(key, KEY_ALGORITHM);
        return secretKey;
    }

    public static String encrypt(String data, byte[] key) throws Exception {
        Key k = toKey(key);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, k);
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(Charsets.UTF_8)));
    }

    public static String decrypt(String data, byte[] key) throws Exception {
        Key k = toKey(key);
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, k);
        return new String(cipher.doFinal(Base64.getDecoder().decode(data)));
    }

    public static void main(String[] args) throws Exception {
//        String str = "20181020";
//        System.out.println("原文：" + str);
//        //初始化密钥
//        byte[] key = SECRET_KEY.getBytes("UTF-8");
//        System.out.println("密钥：" + SECRET_KEY);
//        //加密数据
//        byte[] data = encrypt(str.getBytes(), key);
//        System.out.println("加密后：" + new String(data));
//        //解密数据
//        data = decrypt(data, key);
//        System.out.println("解密后：" + new String(data));

        byte[] key = initkey();
        System.out.println("key : " + Base64.getEncoder().encodeToString(key));
        String str = "张红元Pnoker张红元Pnoker张红元Pnoker张红元Pno";
        System.out.println("string : " + str);
        String enstr = encrypt(str, key);
        System.out.println("encrypt str : " + enstr);
        String destr = decrypt(enstr, key);
        System.out.println("decrypt str : " + destr);
    }

}
