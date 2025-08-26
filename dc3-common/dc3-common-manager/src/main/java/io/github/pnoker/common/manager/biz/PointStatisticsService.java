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

package io.github.pnoker.common.manager.biz;

import java.time.LocalDateTime;

/**
 * 积分统计服务
 *
 * @Author fukq
 * create by 2024/3/5 14:40
 * @Version 1.0
 * @date 2024/03/05
 */
public interface PointStatisticsService {
    /**
     * 统计点历史
     */
    void statisticsPointHistory(LocalDateTime datetime);
}
