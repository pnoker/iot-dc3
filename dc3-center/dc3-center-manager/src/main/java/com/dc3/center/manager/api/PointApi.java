/*
 * Copyright (c) 2022. Pnoker. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.center.manager.api;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dc3.api.center.manager.feign.PointClient;
import com.dc3.center.manager.service.NotifyService;
import com.dc3.center.manager.service.PointService;
import com.dc3.common.bean.R;
import com.dc3.common.constant.CommonConstant;
import com.dc3.common.constant.ServiceConstant;
import com.dc3.common.dto.PointDto;
import com.dc3.common.model.Point;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 位号 Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(ServiceConstant.Manager.POINT_URL_PREFIX)
public class PointApi implements PointClient {

    @Resource
    private PointService pointService;
    @Resource
    private NotifyService notifyService;

    @Override
    public R<Point> add(Point point, Long tenantId) {
        try {
            Point add = pointService.add(point.setTenantId(tenantId));
            if (null != add) {
                notifyService.notifyDriverPoint(CommonConstant.Driver.Point.ADD, add);
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
            Point point = pointService.selectById(id);
            if (null != point && pointService.delete(id)) {
                notifyService.notifyDriverPoint(CommonConstant.Driver.Point.DELETE, point);
                return R.ok();
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Point> update(Point point, Long tenantId) {
        try {
            Point update = pointService.update(point.setTenantId(tenantId));
            if (null != update) {
                notifyService.notifyDriverPoint(CommonConstant.Driver.Point.UPDATE, update);
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Point> selectById(Long id) {
        try {
            Point select = pointService.selectById(id);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Point>> selectByProfileId(Long profileId) {
        try {
            List<Point> select = pointService.selectByProfileId(profileId);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Point>> selectByDeviceId(Long deviceId) {
        try {
            List<Point> select = pointService.selectByDeviceId(deviceId);
            if (null != select) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Page<Point>> list(PointDto pointDto, Long tenantId) {
        try {
            pointDto.setTenantId(tenantId);
            Page<Point> page = pointService.list(pointDto);
            if (null != page) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Map<Long, String>> unit(Set<Long> pointIds) {
        try {
            Map<Long, String> units = pointService.unit(pointIds);
            if (null != units) {
                return R.ok(units);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
