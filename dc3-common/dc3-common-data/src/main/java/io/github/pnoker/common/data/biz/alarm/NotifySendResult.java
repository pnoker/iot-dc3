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

package io.github.pnoker.common.data.biz.alarm;

import io.github.pnoker.common.enums.NotifyRecordStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

/**
 * Channel sending result.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NotifySendResult {

    private NotifyRecordStatusEnum statusFlag;

    private String target;

    private Integer statusCode;

    private String statusMessage;

    private String providerMessageId;

    private Map<String, Object> responsePayload;

    private String errorMessage;

    public static NotifySendResult success(String target, Integer statusCode, String statusMessage,
                                           Map<String, Object> responsePayload) {
        return new NotifySendResult(NotifyRecordStatusEnum.SUCCESS, target, statusCode, statusMessage, null,
                responsePayload, null);
    }

    public static NotifySendResult failed(String target, String errorMessage) {
        return new NotifySendResult(NotifyRecordStatusEnum.FAILED, target, null, null, null, Map.of(),
                errorMessage);
    }

    public static NotifySendResult skipped(String target, String reason) {
        return new NotifySendResult(NotifyRecordStatusEnum.SKIPPED, target, null, null, null, Map.of(), reason);
    }

}
