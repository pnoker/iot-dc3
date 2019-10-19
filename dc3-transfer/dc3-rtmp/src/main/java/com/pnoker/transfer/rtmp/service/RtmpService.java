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

import com.pnoker.common.model.rtmp.Rtmp;
import com.pnoker.common.utils.Response;
import com.pnoker.common.dto.transfer.RtmpDto;

import java.util.List;

public interface RtmpService {
    /**
     * 获取 Rtmp 列表
     *
     * @return
     */
    List<Rtmp> getRtmpList(RtmpDto rtmpDto);

    Response addRtmp(Rtmp rtmp);

    Response startTask(Rtmp rtmp);

    Response stopTask(String id);
}
