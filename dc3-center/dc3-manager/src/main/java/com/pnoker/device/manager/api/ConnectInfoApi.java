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

package com.pnoker.device.manager.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.device.manager.feign.ConnectInfoClient;
import com.pnoker.common.bean.R;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.ConnectInfoDto;
import com.pnoker.common.model.ConnectInfo;
import com.pnoker.common.model.Dic;
import com.pnoker.device.manager.service.ConnectInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>驱动连接配置信息 Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_MANAGER_CONNECT_INFO_URL_PREFIX)
public class ConnectInfoApi implements ConnectInfoClient {

    @Resource
    private ConnectInfoService connectInfoService;

    @Override
    public R<ConnectInfo> add(ConnectInfo connectInfo) {
        try {
            ConnectInfo add = connectInfoService.add(connectInfo);
            if (null != add) {
                return R.ok(add);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Boolean> delete(Long id) {
        try {
            return connectInfoService.delete(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<ConnectInfo> update(ConnectInfo connectInfo) {
        try {
            ConnectInfo update = connectInfoService.update(connectInfo);
            if (null != update) {
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<ConnectInfo> selectById(Long id) {
        try {
            ConnectInfo select = connectInfoService.selectById(id);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<ConnectInfo> selectByName(String name) {
        try {
            ConnectInfo select = connectInfoService.selectByName(name);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Page<ConnectInfo>> list(ConnectInfoDto connectInfoDto) {
        try {
            Page<ConnectInfo> page = connectInfoService.list(connectInfoDto);
            if (null != page) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
