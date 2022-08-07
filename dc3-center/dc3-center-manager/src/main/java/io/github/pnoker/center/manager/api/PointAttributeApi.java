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
import io.github.pnoker.api.center.manager.feign.PointAttributeClient;
import io.github.pnoker.center.manager.service.PointAttributeService;
import io.github.pnoker.common.bean.R;
import io.github.pnoker.common.constant.ServiceConstant;
import io.github.pnoker.common.dto.PointAttributeDto;
import io.github.pnoker.common.dto.PointDto;
import io.github.pnoker.common.exception.NotFoundException;
import io.github.pnoker.common.model.PointAttribute;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 驱动属性配置信息 Client 接口实现
 *
 * @author pnoker
 */
@Slf4j
@RestController
@RequestMapping(ServiceConstant.Manager.POINT_ATTRIBUTE_URL_PREFIX)
public class PointAttributeApi implements PointAttributeClient {

    @Resource
    private PointAttributeService pointAttributeService;

    @Override
    public R<PointAttribute> add(PointAttribute pointAttribute) {
        try {
            PointAttribute add = pointAttributeService.add(pointAttribute);
            if (ObjectUtil.isNotNull(add)) {
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
            return pointAttributeService.delete(id) ? R.ok() : R.fail();
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @Override
    public R<PointAttribute> update(PointAttribute pointAttribute) {
        try {
            PointAttribute update = pointAttributeService.update(pointAttribute);
            if (ObjectUtil.isNotNull(update)) {
                return R.ok(update);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<PointAttribute> selectById(String id) {
        try {
            PointAttribute select = pointAttributeService.selectById(id);
            if (ObjectUtil.isNotNull(select)) {
                return R.ok(select);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<List<PointAttribute>> selectByDriverId(String id) {
        try {
            List<PointAttribute> select = pointAttributeService.selectByDriverId(id);
            if (CollectionUtil.isNotEmpty(select)) {
                return R.ok(select);
            }
        } catch (NotFoundException ne) {
            return R.ok(new ArrayList<>());
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

    @Override
    public R<Page<PointAttribute>> list(PointAttributeDto pointAttributeDto) {
        try {
            if (ObjectUtil.isEmpty(pointAttributeDto)) {
                pointAttributeDto = new PointAttributeDto();
            }
            Page<PointAttribute> page = pointAttributeService.list(pointAttributeDto);
            if (ObjectUtil.isNotNull(page)) {
                return R.ok(page);
            }
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
        return R.fail();
    }

}
