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

package com.pnoker.center.manager.service;

import com.pnoker.common.base.Service;
import com.pnoker.common.dto.PointInfoDto;
import com.pnoker.common.model.PointInfo;

/**
 * <p>PointInfo Interface
 *
 * @author pnoker
 */
public interface PointInfoService extends Service<PointInfo, PointInfoDto> {
    /**
     * 根据位号配置信息 ID 查询
     *
     * @param id
     * @return
     */
    PointInfo selectByPointAttributeID(Long id);
}
