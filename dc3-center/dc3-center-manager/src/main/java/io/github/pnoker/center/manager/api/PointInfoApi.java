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

package io.github.pnoker.center.manager.api;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.feign.PointInfoClient;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.center.manager.service.PointInfoService;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.CommonConstant;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.PointAttributeDto;
import io.github.pnoker.common.dto.PointInfoDto;
import io.github.pnoker.common.model.PointInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * 位号配置信息 Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(ServiceConstant.Manager.POINT_INFO_URL_PREFIX)
public class PointInfoApi implements PointInfoClient {

    @Resource
    private PointInfoService pointInfoService;

    @Resource
    private NotifyService notifyService;

    @Override
    public R<PointInfo> add(PointInfo pointInfo) {
        try {
            PointInfo add = pointInfoService.add(pointInfo);
            if (ObjectUtil.isNotNull(add)) {
                notifyService.notifyDriverPointInfo(CommonConstant.Driver.PointInfo.ADD, add);
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
            PointInfo pointInfo = pointInfoService.selectById(id);
            if (ObjectUtil.isNotNull(pointInfo) && pointInfoService.delete(id)) {
                notifyService.notifyDriverPointInfo(CommonConstant.Driver.PointInfo.DELETE, pointInfo);
                return R.ok();
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<PointInfo> update(PointInfo pointInfo) {
        try {
            PointInfo update = pointInfoService.update(pointInfo);
            if (null != update) {
                notifyService.notifyDriverPointInfo(CommonConstant.Driver.PointInfo.UPDATE, update);
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<PointInfo> selectById(String id) {
        try {
            PointInfo select = pointInfoService.selectById(id);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<PointInfo> selectByAttributeIdAndDeviceIdAndPointId(String attributeId, String deviceId, String pointId) {
        try {
            PointInfo select = pointInfoService.selectByAttributeIdAndDeviceIdAndPointId(attributeId, deviceId, pointId);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<PointInfo>> selectByDeviceIdAndPointId(String deviceId, String pointId) {
        try {
            List<PointInfo> select = pointInfoService.selectByDeviceIdAndPointId(deviceId, pointId);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<PointInfo>> selectByDeviceId(String deviceId) {
        try {
            List<PointInfo> select = pointInfoService.selectByDeviceId(deviceId);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Page<PointInfo>> list(PointInfoDto pointInfoDto) {
        try {
            if (ObjectUtil.isEmpty(pointInfoDto)) {
                pointInfoDto = new PointInfoDto();
            }
            Page<PointInfo> page = pointInfoService.list(pointInfoDto);
            if (null != page) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
