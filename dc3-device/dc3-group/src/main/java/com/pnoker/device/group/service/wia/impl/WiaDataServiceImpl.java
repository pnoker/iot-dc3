/*
 * Copyright 2018 Google LLC. All Rights Reserved.
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
package com.pnoker.device.group.service.wia.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pnoker.device.group.mapper.wia.WiaDataMapper;
import com.pnoker.device.group.model.wia.WiaData;
import com.pnoker.device.group.service.wia.WiaDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>Copyright(c) 2018. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Rtmp 接口实现
 */
@Service
public class WiaDataServiceImpl implements WiaDataService {
    @Autowired
    private WiaDataMapper wiaDataMapper;

    @Override
    public List<WiaData> list(Wrapper<WiaData> wrapper) {
        return wiaDataMapper.selectList(wrapper);
    }

    @Override
    public int insert(WiaData wiaData) {
        return wiaDataMapper.insert(wiaData);
    }
}
