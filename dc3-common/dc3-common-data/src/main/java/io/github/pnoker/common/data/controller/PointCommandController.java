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

package io.github.pnoker.common.data.controller;

import io.github.pnoker.common.base.BaseController;
import io.github.pnoker.common.constant.service.DataConstant;
import io.github.pnoker.common.data.biz.PointCommandService;
import io.github.pnoker.common.data.entity.builder.PointCommandBuilder;
import io.github.pnoker.common.data.entity.vo.PointCommandReadVO;
import io.github.pnoker.common.data.entity.vo.PointCommandWriteVO;
import io.github.pnoker.common.entity.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.extensions.Extension;
import io.swagger.v3.oas.annotations.extensions.ExtensionProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * REST controller exposing point command management endpoints.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Tag(name = "point_command", description = "Point-level command dispatch: send commands to individual device data points and track their execution status in real time")
@Slf4j
@RestController
@RequestMapping(DataConstant.POINT_COMMAND_URL_PREFIX)
@RequiredArgsConstructor
public class PointCommandController implements BaseController {

    private final PointCommandService pointCommandService;

    private final PointCommandBuilder pointCommandBuilder;

    /**
     * Dispatch a downward read command to fetch the current value of a single point.
     *
     * @param entityVO read-command payload identifying the device and point to read
     * @return the commandId assigned to the dispatched command, for polling execution status
     */
    @PreAuthorize("@perm.can('point_command', 'list')")
    @Operation(summary = "Send Point Read Command", description = "Dispatch a downward read command to fetch the current value of a single point on the tenant's device " +
            "and return its command ID. Poll the command history with the returned ID to track execution status; the optional commandId makes submission idempotent.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "LOW"),
                    @ExtensionProperty(name = "destructive", value = "false"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "true")
            }))
    @PostMapping("/read")
    public Mono<R<String>> read(@Validated @RequestBody PointCommandReadVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            String commandId = pointCommandService.read(tenantId, pointCommandBuilder.buildBOByVO(entityVO));
            R<String> result = R.ok();
            result.setData(commandId);
            return result;
        }));
    }

    /**
     * Dispatch a downward write command to push a new value onto a single point.
     *
     * @param entityVO write-command payload identifying the device, point and value to write
     * @return the commandId assigned to the dispatched command, for polling delivery status
     */
    @PreAuthorize("@perm.can('point_command', 'list')")
    @Operation(summary = "Send Point Write Command", description = "Dispatch a downward write command to push a new value onto a single point on the tenant's device " +
            "and return its command ID. Poll the command history with the returned ID to confirm whether the write reached the device; the optional commandId makes submission idempotent.",
            extensions = @Extension(name = "x-dc3-ai", properties = {
                    @ExtensionProperty(name = "riskLevel", value = "HIGH"),
                    @ExtensionProperty(name = "destructive", value = "true"),
                    @ExtensionProperty(name = "idempotent", value = "true"),
                    @ExtensionProperty(name = "openWorld", value = "true")
            }))
    @PostMapping("/write")
    public Mono<R<String>> write(@Validated @RequestBody PointCommandWriteVO entityVO) {
        return getTenantId().flatMap(tenantId -> async(() -> {
            String commandId = pointCommandService.write(tenantId, pointCommandBuilder.buildBOByVO(entityVO));
            R<String> result = R.ok();
            result.setData(commandId);
            return result;
        }));
    }

}
