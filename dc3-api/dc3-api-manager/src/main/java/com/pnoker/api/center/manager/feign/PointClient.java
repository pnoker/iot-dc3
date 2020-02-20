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

package com.pnoker.api.center.manager.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.api.center.manager.hystrix.PointClientHystrix;
import com.pnoker.common.bean.R;
import com.pnoker.common.constant.Common;
import com.pnoker.common.dto.PointDto;
import com.pnoker.common.model.Dic;
import com.pnoker.common.model.Point;
import com.pnoker.common.valid.Insert;
import com.pnoker.common.valid.Update;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * <p>位号 FeignClient
 *
 * @author pnoker
 */
@FeignClient(path = Common.Service.DC3_MANAGER_POINT_URL_PREFIX, name = Common.Service.DC3_MANAGER, fallbackFactory = PointClientHystrix.class)
public interface PointClient {

    /**
     * 新增 Point
     *
     * @param point
     * @return Point
     */
    @PostMapping("/add")
    R<Point> add(@Validated(Insert.class) @RequestBody Point point);

    /**
     * 根据 Id 删除 Point
     *
     * @param id pointId
     * @return Boolean
     */
    @PostMapping("/delete/{id}")
    R<Boolean> delete(@PathVariable(value = "id") Long id);

    /**
     * 修改 Point
     *
     * @param point
     * @return Point
     */
    @PostMapping("/update")
    R<Point> update(@Validated(Update.class) @RequestBody Point point);

    /**
     * 根据 Id 查询 Point
     *
     * @param id
     * @return Point
     */
    @GetMapping("/id/{id}")
    R<Point> selectById(@PathVariable(value = "id") Long id);

    /**
     * 根据模板 ID & 位号 Name 查询 Point
     *
     * @param profileId
     * @param name
     * @return Point
     */
    @GetMapping("/profile/{profileId}/name/{name}")
    R<Point> selectByNameAndProfile(@PathVariable(value = "profileId") Long profileId, @PathVariable(value = "name") String name);

    /**
     * 分页查询 Point
     *
     * @param pointDto
     * @return Page<Point>
     */
    @PostMapping("/list")
    R<Page<Point>> list(@RequestBody(required = false) PointDto pointDto);

    /**
     * 查询 Point 字典
     *
     * @return List<Point>
     */
    @GetMapping("/dictionary")
    R<List<Dic>> dictionary();

}
