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

package io.github.pnoker.common.data.biz;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.github.pnoker.common.data.entity.model.CommandRecordDO;
import io.github.pnoker.common.data.entity.vo.CommandCallVO;
import io.github.pnoker.common.data.entity.vo.CommandRecordQueryVO;

/**
 * Business service for custom command call operations.
 *
 * @author pnoker
 * @version 2026.5.23
 * @since 2026.5.23
 */
public interface CommandRecordService {

    String call(Long tenantId, CommandCallVO entityVO);

    CommandRecordDO getByRecordId(String recordId);

    Page<CommandRecordDO> list(Long tenantId, CommandRecordQueryVO queryVO);

}
