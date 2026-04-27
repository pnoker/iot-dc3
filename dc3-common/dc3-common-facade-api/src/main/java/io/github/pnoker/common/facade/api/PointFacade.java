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

package io.github.pnoker.common.facade.api;

import io.github.pnoker.common.facade.entity.bo.FacadePointBO;
import io.github.pnoker.common.facade.entity.common.FacadePage;
import io.github.pnoker.common.facade.entity.query.FacadePointQuery;

/**
 * Protocol-neutral point facade. Mirrors the two RPCs on
 * {@code api.center.manager.PointApi}.
 *
 * @author pnoker
 * @since 2026.4.19
 */
public interface PointFacade {

    /**
     * @return the point, or {@code null} when it does not exist.
     */
    FacadePointBO selectById(Long id);

    /**
     * @return a page of points (never {@code null}; empty page when nothing matches).
     */
    FacadePage<FacadePointBO> selectByPage(FacadePointQuery query);
}
