package com.pnoker.center.dbs.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pnoker.center.dbs.service.RtmpService;
import com.pnoker.common.base.BaseController;
import com.pnoker.common.model.rtmp.Rtmp;
import com.pnoker.transfer.rtmp.feign.RtmpFeignApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
    private Environment environment;
    @Autowired
    private RtmpService rtmpService;

    @Override
    public String api() {
        return environment.getProperty("server.port");
    }

    @Override
    public String add(String json) {
        return null;
    }

    @Override
    public String delete(String json) {
        return null;
    }

    @Override
    public String update(String json) {
        return null;
    }

    @Override
    public List<Rtmp> list() {
        QueryWrapper<Rtmp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("auto_start", true);
        List<Rtmp> list = rtmpService.list(queryWrapper);
        return list;
    }

    @RequestMapping("/insert")
    public void insert() {
        for (int i = 0; i < 1000000; i++) {
            Rtmp wiaData = new Rtmp(i);
            rtmpService.insert(wiaData);
            if (i % 100 == 0) {
                log.info("完成：{},{}%", i, i / 1000000 * 10);
            }
        }
    }
}
