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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.biz.PointValueService;
import io.github.pnoker.common.entity.bo.PointValueBO;
import io.github.pnoker.common.entity.query.PointValueQuery;
import io.github.pnoker.common.facade.api.PointValueFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointValueBO;
import io.github.pnoker.common.facade.local.builder.FacadePointValueBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * In-process implementation: routes each call straight into {@link PointValueService}.
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
public class PointValueLocalFacade implements PointValueFacade {

    private final PointValueService pointValueService;

    private final FacadePointValueBuilder facadePointValueBuilder;

    @Override
    public FacadePointValueBO lastValue(Long tenantId, Long deviceId, Long pointId) {
        PointValueQuery query = PointValueQuery.builder()
                .tenantId(tenantId)
                .deviceId(deviceId)
                .pointId(pointId)
                .build();
        Page<PointValueBO> page = pointValueService.latest(query);
        if (Objects.isNull(page) || page.getRecords().isEmpty()) {
            return null;
        }
        return facadePointValueBuilder.toFacadeBO(page.getRecords().getFirst());
    }

    @Override
    public List<String> history(Long tenantId, Long deviceId, Long pointId, int count) {
        List<String> result = pointValueService.history(tenantId, deviceId, pointId, count);
        if (Objects.isNull(result) || result.isEmpty()) {
            return Collections.emptyList();
        }
        return result;
    }

}
