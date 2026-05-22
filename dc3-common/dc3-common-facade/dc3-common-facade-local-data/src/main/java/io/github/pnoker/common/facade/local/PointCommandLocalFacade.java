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

package io.github.pnoker.common.facade.local;

import io.github.pnoker.common.data.biz.PointCommandService;
import io.github.pnoker.common.data.entity.vo.PointCommandReadVO;
import io.github.pnoker.common.data.entity.vo.PointCommandWriteVO;
import io.github.pnoker.common.facade.api.PointCommandFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * In-process implementation: routes command calls straight into
 * {@link PointCommandService}.
 * <p>
 * Selected when {@code dc3.facade.mode=local}. Carries zero serialization cost — the same
 * JVM handles both caller and service.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PointCommandLocalFacade implements PointCommandFacade {

    private final PointCommandService pointCommandService;

    @Override
    public boolean submitRead(Long tenantId, Long deviceId, Long pointId) {
        PointCommandReadVO readVO = new PointCommandReadVO();
        readVO.setDeviceId(deviceId);
        readVO.setPointId(pointId);
        pointCommandService.read(tenantId, readVO);
        return true;
    }

    @Override
    public boolean submitWrite(Long tenantId, Long deviceId, Long pointId, String value) {
        PointCommandWriteVO writeVO = new PointCommandWriteVO();
        writeVO.setDeviceId(deviceId);
        writeVO.setPointId(pointId);
        writeVO.setValue(value);
        pointCommandService.write(tenantId, writeVO);
        return true;
    }

}
