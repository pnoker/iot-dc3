/*
 * Copyright 2022 Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.transfer.rtmp.api;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.transfer.rtmp.feign.RtmpClient;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.ProfileDto;
import io.github.pnoker.common.dto.RtmpDto;
import io.github.pnoker.common.model.Rtmp;
import io.github.pnoker.transfer.rtmp.service.RtmpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * Rest接口控制器
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(ServiceConstant.Rtmp.URL_PREFIX)
public class RtmpApi implements RtmpClient {

    @Resource
    private RtmpService rtmpService;

    @Override
    public R<Rtmp> add(Rtmp rtmp, String tenantId) {
        try {
            Rtmp add = rtmpService.add(rtmp.setTenantId(tenantId));
            if (null != add) {
                return R.ok(add);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Boolean> delete(String id) {
        try {
            return rtmpService.delete(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Rtmp> update(Rtmp rtmp, String tenantId) {
        try {
            Rtmp update = rtmpService.update(rtmp.setTenantId(tenantId));
            if (null != update) {
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Rtmp> selectById(String id) {
        try {
            Rtmp select = rtmpService.selectById(id);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Page<Rtmp>> list(RtmpDto rtmpDto, String tenantId) {
        try {
            if (ObjectUtil.isEmpty(rtmpDto)) {
                rtmpDto = new RtmpDto();
            }
            rtmpDto.setTenantId(tenantId);
            Page<Rtmp> page = rtmpService.list(rtmpDto);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Boolean> start(String id) {
        try {
            return rtmpService.start(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<Boolean> stop(String id) {
        try {
            return rtmpService.stop(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

}
