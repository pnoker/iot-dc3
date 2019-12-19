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

package com.pnoker.transfer.rtmp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pnoker.common.bean.Response;
import com.pnoker.common.bean.Pages;
import com.pnoker.common.dto.transfer.RtmpDto;
import com.pnoker.common.entity.rtmp.Rtmp;

/**
 * <p>
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
public interface RtmpService {
    /**
     * 新增记录
     *
     * @param rtmp
     * @return true/false
     */
    Response<Rtmp> add(Rtmp rtmp);

    /**
     * 删除记录
     *
     * @param id
     * @return true/false
     */
    Response<Boolean> delete(Long id);

    /**
     * 更新记录
     *
     * @param rtmp
     * @return true/false
     */
    Response<Rtmp> update(Rtmp rtmp);

    /**
     * 通过ID查询记录
     *
     * @param id
     * @return type
     */
    Response<Rtmp> selectById(Long id);

    /**
     * 获取带分页、排序的记录
     *
     * @param rtmpDto
     * @return list
     */
    Response<Page<Rtmp>> list(RtmpDto rtmpDto);

    /**
     * 启动
     *
     * @param id
     * @return true/false
     */
    Response<Boolean> start(Long id);

    /**
     * 停止
     *
     * @param id
     * @return true/false
     */
    Response<Boolean> stop(Long id);
}
