/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
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

package com.dc3.center.auth.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.auth.blackIp.feign.BlackIpClient;
import com.dc3.center.auth.service.BlackIpService;
import com.dc3.common.bean.R;
import com.dc3.common.constant.Common;
import com.dc3.common.dto.BlackIpDto;
import com.dc3.common.model.BlackIp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Ip 黑名单 Feign Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(Common.Service.DC3_AUTH_BLACK_IP_URL_PREFIX)
public class BlackIpApi implements BlackIpClient {

    @Resource
    private BlackIpService blackIpService;

    @Override
    public R<BlackIp> add(BlackIp blackIp) {
        try {
            BlackIp add = blackIpService.add(blackIp);
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
            return blackIpService.delete(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<BlackIp> update(BlackIp blackIp) {
        try {
            BlackIp update = blackIpService.update(blackIp);
            if (null != update) {
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<BlackIp> selectById(Long id) {
        try {
            BlackIp select = blackIpService.selectById(id);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail("Resource does not exist");
    }

    @Override
    public R<BlackIp> selectByIp(String ip) {
        try {
            BlackIp select = blackIpService.selectByIp(ip);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail("Resource does not exist");
    }

    @Override
    public R<Page<BlackIp>> list(BlackIpDto blackIpDto) {
        try {
            Page<BlackIp> page = blackIpService.list(blackIpDto);
            if (null != page) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail("Resource does not exist");
    }

    @Override
    public R<Boolean> checkBlackIpValid(String ip) {
        try {
            return blackIpService.checkBlackIpValid(ip) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}
