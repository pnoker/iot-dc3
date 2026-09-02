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

import io.github.pnoker.common.enums.NotifyHistoryStatusEnum;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Channel sending result.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class NotifySendResult {

    private NotifyHistoryStatusEnum statusFlag;

    private String target;

    private Integer statusCode;

    private String statusMessage;

    private String providerMessageId;

    private Map<String, Object> responsePayload;

    private String errorMessage;

    /**
     * Success.
     *
     * @param target          target
     * @param statusCode      status code
     * @param statusMessage   status message
     * @param responsePayload response payload
     * @return success result
     */
    public static NotifySendResult success(
            String target, Integer statusCode, String statusMessage, Map<String, Object> responsePayload) {
        return new NotifySendResult(
                NotifyHistoryStatusEnum.SUCCESS, target, statusCode, statusMessage, null, responsePayload, null);
    }

    /**
     * Failed.
     *
     * @param target       target
     * @param errorMessage error message
     * @return failed result
     */
    public static NotifySendResult failed(String target, String errorMessage) {
        return new NotifySendResult(NotifyHistoryStatusEnum.FAILED, target, null, null, null, Map.of(), errorMessage);
    }

    /**
     * Skipped.
     *
     * @param target target
     * @param reason reason
     * @return skipped result
     */
    public static NotifySendResult skipped(String target, String reason) {
        return new NotifySendResult(NotifyHistoryStatusEnum.SKIPPED, target, null, null, null, Map.of(), reason);
    }
}
