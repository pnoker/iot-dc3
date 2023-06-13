/*
 * Copyright 2016-present the original author or authors.
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

package io.github.pnoker.center.manager.service;

import io.github.pnoker.center.manager.entity.query.DevicePageQuery;
import io.github.pnoker.common.base.Service;
import io.github.pnoker.common.model.Device;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Device Interface
 *
 * @author pnoker
 * @since 2022.1.0
 */
public interface DeviceService extends Service<Device, DevicePageQuery> {

    /**
     * 根据 设备Name 和 租户Id 查询设备
     *
     * @param name     Device Name
     * @param tenantId 租户ID
     * @return Device
     */
    Device selectByName(String name, String tenantId);

    /**
     * 根据 驱动Id 查询该驱动下的全部设备
     *
     * @param driverId Driver ID
     * @return Device Array
     */
    List<Device> selectByDriverId(String driverId);

    /**
     * 根据 模板Id 查询该驱动下的全部设备
     *
     * @param profileId Profile ID
     * @return Device Array
     */
    List<Device> selectByProfileId(String profileId);

    /**
     * 根据 设备Id集 查询设备
     *
     * @param ids 设备ID Set
     * @return Device Array
     */
    List<Device> selectByIds(Set<String> ids);

    /**
     * 导入设备
     *
     * @param device        Device
     * @param multipartFile MultipartFile
     */
    void importDevice(Device device, MultipartFile multipartFile);

    /**
     * 生成导入设备模板
     *
     * @param device Device
     * @return File Path
     */
    Path generateImportTemplate(Device device);

    Long count();

    Long dataCount();

    List<Device> selectAllByDriverId(String id, String tenantId);
}
