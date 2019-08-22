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
package com.pnoker.center.dbs.controller;

import com.pnoker.center.dbs.service.RtmpService;
import com.pnoker.common.base.BaseController;
import com.pnoker.common.bean.base.ResponseBean;
import com.pnoker.common.model.rtmp.Rtmp;
import com.pnoker.common.utils.Tools;
import com.pnoker.transfer.rtmp.feign.RtmpFeignApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
@RestController
public class RtmpController extends BaseController implements RtmpFeignApi {
    @Autowired
    private RtmpService rtmpService;

    @Override
    public ResponseBean add(String json) {
        return ok();
    }

    @Override
    public ResponseBean delete(String json) {
        return ok();
    }

    @Override
    public ResponseBean update(String json) {
        return ok();
    }

    @Override
    public ResponseBean list(String json) {
        //进行数据校验
        if (null == json || "".equals(json)) {
            json = "{}";
        }
        if (!Tools.isJson(json)) {
            return fail("Not Json Format");
        }
        List<Rtmp> list = rtmpService.list(json);
        return ok(list);
    }

    @RequestMapping("/insert")
    public ResponseBean insert() {
        for (int i = 5; i < 100; i++) {
            Rtmp wiaData = new Rtmp(i);
            rtmpService.insert(wiaData);
            if (i % 100 == 0) {
                log.info("完成：{},{}%", i, i / 1000000 * 10);
            }
        }
        return ok();
    }
}
