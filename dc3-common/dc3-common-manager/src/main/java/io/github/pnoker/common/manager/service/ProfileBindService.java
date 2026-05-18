/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package io.github.pnoker.common.manager.service;

import io.github.pnoker.common.base.service.BaseService;
import io.github.pnoker.common.manager.entity.bo.ProfileBindBO;
import io.github.pnoker.common.manager.entity.query.ProfileBindQuery;

import java.util.List;

/**
 * ProfileBind Interface
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public interface ProfileBindService extends BaseService<ProfileBindBO, ProfileBindQuery> {

    /**
     * Remove all profile bindings for the given device.
     *
     * @param deviceId Device ID
     * @throws io.github.pnoker.common.exception.DeleteException when removal fails
     */
    void removeByDeviceId(Long deviceId);

    /**
     * Remove the profile binding for the given device and profile.
     *
     * @param deviceId  Device ID
     * @param profileId Profile ID
     * @throws io.github.pnoker.common.exception.DeleteException when removal fails
     */
    void removeByDeviceIdAndProfileId(Long deviceId, Long profileId);

    /**
     * Device ID ID
     *
     * @param deviceId  Device ID
     * @param profileId Point ID
     * @return ProfileBind
     */
    ProfileBindBO getByDeviceIdAndProfileId(Long deviceId, Long profileId);

    /**
     * ID Device ID
     *
     * @param profileId Point ID
     * @return Device ID
     */
    List<Long> selectDeviceIdsByProfileId(Long profileId);

    /**
     * Device ID ID
     *
     * @param deviceId Device ID
     * @return ID
     */
    List<Long> selectProfileIdsByDeviceId(Long deviceId);

}
