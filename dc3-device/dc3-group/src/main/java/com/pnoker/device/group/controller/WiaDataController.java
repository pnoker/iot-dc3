/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.pnoker.device.group.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.pnoker.common.base.BaseController;
import com.pnoker.device.group.model.wia.WiaData;
import com.pnoker.device.group.service.wia.WiaDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
@Slf4j
@RestController
public class WiaDataController extends BaseController {
    @Autowired
    private WiaDataService wiaDataService;

    @RequestMapping("/list")
    public List<WiaData> list() {
        QueryWrapper<WiaData> queryWrapper = new QueryWrapper<>();
        List<WiaData> list = wiaDataService.list(queryWrapper);
        return list;
    }

    @Transactional
    @RequestMapping("/insert/{num}")
    public String insert(@PathVariable int num) {
        LinkedBlockingQueue blockingQueue = new LinkedBlockingQueue(5000);
        if(blockingQueue.size()>1000){
            
        }
        long start = System.currentTimeMillis();
        for (int i = 0; i < num; i++) {
            WiaData wiaData = new WiaData();
            wiaData.setId(i);
            wiaData.setVariableId(0);
            wiaData.setValue(1);
            wiaData.setTime(System.currentTimeMillis());
            wiaDataService.insert(wiaData);
            log.info("完成：{}", i);
        }
        long end = System.currentTimeMillis();
        return ((end - start) / 1000) + "";
    }
}
