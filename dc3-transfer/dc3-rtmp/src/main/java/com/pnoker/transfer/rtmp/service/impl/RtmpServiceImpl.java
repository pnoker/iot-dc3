package com.pnoker.transfer.rtmp.service.impl;

import com.alibaba.fastjson.JSON;
import com.pnoker.common.bean.base.ResponseBean;
import com.pnoker.common.model.rtmp.Rtmp;
import com.pnoker.transfer.rtmp.feign.RtmpFeignApi;
import com.pnoker.transfer.rtmp.service.RtmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class RtmpServiceImpl implements RtmpService {
    //重连时间间隔 & 统计重连次数
    private int interval = 1000 * 5;
    private volatile int times = 0;

    @Autowired
    private RtmpFeignApi rtmpFeignApi;

    @Override
    public List<Rtmp> getRtmpList() {
        List<Rtmp> list = null;

        Map<String, Object> condition = new HashMap<>(2);
        condition.put("auto_start", false);
        ResponseBean responseBean = rtmpFeignApi.list(JSON.toJSONString(condition));
        if (responseBean.isOk()) {
            list = (List<Rtmp>) responseBean.getResult();
        } else {
            log.error(responseBean.getMessage());
            reconnect();
        }
        if (null == list) {
            list = new ArrayList<>();
        }
        return list;
    }

    public void reconnect() {
        // 3 次重连机会
        if (times > 3) {
            log.info("一共重连 {} 次,退出重连", times);
            return;
        }
        log.info("第 {} 次重连", times);
        times++;
        try {
            Thread.sleep(interval * times);
            getRtmpList();
        } catch (Exception e) {
        }
    }
}
