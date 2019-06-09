package com.pnoker.common;

import com.alibaba.fastjson.JSON;
import com.pnoker.common.bean.encryp.Keys;
import com.pnoker.common.utils.encryp.AesTools;
import com.pnoker.common.utils.encryp.RsaTools;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: AES\RSA 加密算法测试
 */
@Slf4j
public class TestEncrypt {
    @Test
    public void aesTest() throws Exception {
        Keys.Aes aes = AesTools.genKey();
        log.info(" aes key : {}", JSON.toJSONString(aes));
        String str = "zhanghongyuan@ks.sia.cn,zhanghongyuan@ks.sia.cn";
        log.info("str : {}", str);
        String ens = AesTools.encrypt(str, aes.getPrivateKey());
        log.info("ens : {}", ens);
        String des = AesTools.decrypt(ens, aes.getPrivateKey());
        log.info("des : {}", des);
    }

    @Test
    public void rsaTest() throws Exception {
        Keys.Rsa rsa = RsaTools.genKey();
        log.info(" rsa key : {}", JSON.toJSONString(rsa));
        log.info("pk:{},sk:{}",rsa.getPublicKey().length(),rsa.getPrivateKey().length());
        String str = "zhanghongyuan@ks.sia.cn,zhanghongyuan@ks.sia.cn";
        log.info("str : {}", str);
        String ens = RsaTools.encrypt(str, rsa.getPublicKey());
        log.info("ens : {}", ens);
        String des = RsaTools.decrypt(ens, rsa.getPrivateKey());
        log.info("des : {}", des);
    }
}
