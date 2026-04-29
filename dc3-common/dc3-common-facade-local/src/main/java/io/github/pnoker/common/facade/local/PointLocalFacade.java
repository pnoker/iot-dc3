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
import io.github.pnoker.common.facade.api.PointFacade;
import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;
import io.github.pnoker.common.facade.local.builder.FacadePointBuilder;
import io.github.pnoker.common.manager.entity.bo.PointBO;
import io.github.pnoker.common.manager.entity.query.PointQuery;
import io.github.pnoker.common.manager.service.PointService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * In-process PointFacade implementation.
 *
 * @author pnoker
 * @since 2026.4.29
 */
@Slf4j
@Component
public class PointLocalFacade implements PointFacade {

    @Resource
    private PointService pointService;

    @Resource
    private FacadePointBuilder facadePointBuilder;

    @Override
    public FacadePointBO selectById(Long id) {
        PointBO managerBO = pointService.selectById(id);
        return Objects.isNull(managerBO) ? null : facadePointBuilder.toFacadeBO(managerBO);
    }

    @Override
    public FacadePage<FacadePointBO> selectByPage(FacadePointQuery query) {
        PointQuery managerQuery = facadePointBuilder.toManagerQuery(query);
        Page<PointBO> page = pointService.selectByPage(managerQuery);
        if (Objects.isNull(page)) {
            return FacadePage.empty();
        }

        List<FacadePointBO> records = page.getRecords().stream()
                .map(facadePointBuilder::toFacadeBO)
                .toList();
        return new FacadePage<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
    }
}
