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

import io.github.pnoker.common.data.biz.PointValueCommandService;
import io.github.pnoker.common.data.entity.vo.PointValueReadVO;
import io.github.pnoker.common.data.entity.vo.PointValueWriteVO;
import io.github.pnoker.common.facade.api.PointValueCommandFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * In-process implementation: routes command calls straight into
 * {@link PointValueCommandService}.
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
public class PointValueCommandLocalFacade implements PointValueCommandFacade {

    private final PointValueCommandService pointValueCommandService;

    @Override
    public boolean dispatchRead(Long tenantId, Long deviceId, Long pointId) {
        PointValueReadVO readVO = new PointValueReadVO();
        readVO.setDeviceId(deviceId);
        readVO.setPointId(pointId);
        pointValueCommandService.read(tenantId, readVO);
        return true;
    }

    @Override
    public boolean dispatchWrite(Long tenantId, Long deviceId, Long pointId, String value) {
        PointValueWriteVO writeVO = new PointValueWriteVO();
        writeVO.setDeviceId(deviceId);
        writeVO.setPointId(pointId);
        writeVO.setValue(value);
        pointValueCommandService.write(tenantId, writeVO);
        return true;
    }

}
