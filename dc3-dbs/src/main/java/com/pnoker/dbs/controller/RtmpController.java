package com.pnoker.dbs.controller;

import com.pnoker.api.dbs.feign.RtmpFeignApi;
import com.pnoker.common.base.BaseController;
import com.pnoker.common.model.rtmp.Rtmp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
@RestController
public class RtmpController extends BaseController implements RtmpFeignApi {
    @Autowired
    private Environment environment;

    @Override
    public String api() {

        return environment.getProperty("server.port");
    }

    @Override
    public Rtmp list() {
        Rtmp rtmp = new Rtmp(1);
        return rtmp;
    }
}
