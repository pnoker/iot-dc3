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

package com.dc3.transfer.rtmp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dc3.common.model.Rtmp;
import org.apache.ibatis.annotations.Mapper;

/**
 * Rtmp 数据库操作接口
 *
 * @author pnoker
 */
@Mapper
public interface RtmpMapper extends BaseMapper<Rtmp> {
}
