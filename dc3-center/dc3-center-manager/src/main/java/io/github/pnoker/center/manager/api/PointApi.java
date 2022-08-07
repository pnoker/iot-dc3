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

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.api.center.manager.feign.PointClient;
import io.github.pnoker.center.manager.service.NotifyService;
import io.github.pnoker.center.manager.service.PointService;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.CommonConstant;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.LabelDto;
import io.github.pnoker.common.dto.PointDto;
import io.github.pnoker.common.model.Point;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public R<Point> add(Point point, String tenantId) {
        try {
            Point add = pointService.add(point.setTenantId(tenantId));
            if (ObjectUtil.isNotNull(add)) {
                notifyService.notifyDriverPoint(CommonConstant.Driver.Point.ADD, add);
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
            Point point = pointService.selectById(id);
            if (ObjectUtil.isNotNull(point) && pointService.delete(id)) {
                notifyService.notifyDriverPoint(CommonConstant.Driver.Point.DELETE, point);
                return R.ok();
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Point> update(Point point, String tenantId) {
        try {
            Point update = pointService.update(point.setTenantId(tenantId));
            if (ObjectUtil.isNotNull(update)) {
                notifyService.notifyDriverPoint(CommonConstant.Driver.Point.UPDATE, update);
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Point> selectById(String id) {
        try {
            Point select = pointService.selectById(id);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Map<String, Point>> selectByIds(Set<String> pointIds) {
        try {
            List<Point> points = pointService.selectByIds(pointIds);
            Map<String, Point> pointMap = points.stream().collect(Collectors.toMap(Point::getId, Function.identity()));
            return R.ok(pointMap);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<List<Point>> selectByProfileId(String profileId) {
        try {
            List<Point> select = pointService.selectByProfileId(profileId);
            if (CollectionUtil.isNotEmpty(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<Point>> selectByDeviceId(String deviceId) {
        try {
            List<Point> select = pointService.selectByDeviceId(deviceId);
            if (CollectionUtil.isNotEmpty(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Page<Point>> list(PointDto pointDto, String tenantId) {
        try {
            if (ObjectUtil.isEmpty(pointDto)) {
                pointDto = new PointDto();
            }
            pointDto.setTenantId(tenantId);
            Page<Point> page = pointService.list(pointDto);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Map<String, String>> unit(Set<String> pointIds) {
        try {
            Map<String, String> units = pointService.unit(pointIds);
            if (null != units) {
                return R.ok(units);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
