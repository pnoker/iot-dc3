package com.pnoker.common;

import com.alibaba.fastjson.JSON;
import com.pnoker.common.dto.Keys;
import com.pnoker.common.utils.Dc3Tools;
import com.pnoker.common.utils.AesTools;
import com.pnoker.common.utils.RsaTools;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

/**
 * <p>AES\RSA 加密算法测试
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
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
        log.info("pk:{},sk:{}", rsa.getPublicKey().length(), rsa.getPrivateKey().length());
        String str = "zhanghongyuan@ks.sia.cn,zhanghongyuan@ks.sia.cn";
        log.info("str : {}", str);
        String ens = RsaTools.encrypt(str, rsa.getPublicKey());
        log.info("ens : {}", ens);
        String des = RsaTools.decrypt(ens, rsa.getPrivateKey());
        log.info("des : {}", des);
    }

    @Test
    public void uuidTest(){
        String uuid = Dc3Tools.uuid();
        log.info(uuid);
    }
}
