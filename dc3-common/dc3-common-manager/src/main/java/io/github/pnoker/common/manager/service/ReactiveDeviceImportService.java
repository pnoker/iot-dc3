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

import io.github.pnoker.common.manager.entity.bo.DeviceBO;
import io.github.pnoker.common.manager.entity.operation.OperationView;
import io.github.pnoker.db.r2dbc.core.operation.OperationAccepted;
import java.util.UUID;
import reactor.core.publisher.Mono;
/** Business service covering device import use cases. */

public interface ReactiveDeviceImportService {

    /** Submit the device import workbook as a tracked async operation. */
    Mono<OperationAccepted> submit(DeviceBO context, String fileName, byte[] content, String idempotencyKey);

    /** Generate the import workbook template for the driver/profile pair. */
    Mono<byte[]> generateTemplate(Long tenantId, Long driverId, Long profileId);

    /** Load the device import operation view by id. */
    Mono<OperationView> getOperation(Long tenantId, UUID operationId);
}
