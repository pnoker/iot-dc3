package com.pnoker.dbs;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pnoker.dbs.mapper.RtmpMapper;
import com.pnoker.dbs.model.Rtmp;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Slf4j
@SpringBootTest
@RunWith(SpringRunner.class)
public class TestRtmp {
    @Autowired
    private RtmpMapper rtmpMapper;

    @Test
    public void testSelect() {
        log.info("test rtmp select");
        QueryWrapper<Rtmp> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(Rtmp.ID, 1);
        List<Rtmp> rtmps = rtmpMapper.selectList(queryWrapper);
        rtmps.forEach(rtmp -> log.info(rtmp.toString()));
    }
}
