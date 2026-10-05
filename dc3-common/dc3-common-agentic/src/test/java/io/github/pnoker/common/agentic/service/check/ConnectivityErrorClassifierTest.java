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
package io.github.pnoker.common.agentic.service.check;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.pnoker.common.enums.ConnectivityErrorTypeEnum;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.Test;

class ConnectivityErrorClassifierTest {

    @Test
    void classifiesTransportExceptions() {
        assertEquals(
                ConnectivityErrorTypeEnum.UNREACHABLE,
                ConnectivityErrorClassifier.classify(
                        null, new IOException(new UnknownHostException("api.example.com")), null));
        assertEquals(
                ConnectivityErrorTypeEnum.UNREACHABLE,
                ConnectivityErrorClassifier.classify(null, new ConnectException("refused"), null));
        assertEquals(
                ConnectivityErrorTypeEnum.TLS_ERROR,
                ConnectivityErrorClassifier.classify(null, new SSLHandshakeException("bad cert"), null));
        assertEquals(
                ConnectivityErrorTypeEnum.TIMEOUT,
                ConnectivityErrorClassifier.classify(null, new SocketTimeoutException("read timeout"), null));
        assertEquals(
                ConnectivityErrorTypeEnum.TIMEOUT,
                ConnectivityErrorClassifier.classify(null, new TimeoutException("did not complete"), null));
    }

    @Test
    void classifiesUpstreamStatuses() {
        assertEquals(ConnectivityErrorTypeEnum.AUTH_FAILED, ConnectivityErrorClassifier.classify(401, null, null));
        assertEquals(ConnectivityErrorTypeEnum.AUTH_FAILED, ConnectivityErrorClassifier.classify(403, null, null));
        assertEquals(ConnectivityErrorTypeEnum.MODEL_NOT_FOUND, ConnectivityErrorClassifier.classify(404, null, null));
        assertEquals(ConnectivityErrorTypeEnum.RATE_LIMITED, ConnectivityErrorClassifier.classify(429, null, null));
        assertEquals(ConnectivityErrorTypeEnum.UPSTREAM_ERROR, ConnectivityErrorClassifier.classify(503, null, null));
        assertEquals(ConnectivityErrorTypeEnum.BAD_RESPONSE, ConnectivityErrorClassifier.classify(400, null, null));
    }

    @Test
    void classifiesBodyKeywordsIncludingChineseRelayMessages() {
        assertEquals(
                ConnectivityErrorTypeEnum.QUOTA_EXCEEDED,
                ConnectivityErrorClassifier.classify(null, null, "无可用渠道，请检查分组"));
        assertEquals(
                ConnectivityErrorTypeEnum.QUOTA_EXCEEDED,
                ConnectivityErrorClassifier.classify(null, null, "账户余额不足，请充值"));
        assertEquals(
                ConnectivityErrorTypeEnum.QUOTA_EXCEEDED,
                ConnectivityErrorClassifier.classify(null, null, "You exceeded your current quota"));
        assertEquals(
                ConnectivityErrorTypeEnum.MODEL_NOT_FOUND,
                ConnectivityErrorClassifier.classify(null, null, "The model 'x' does not exist"));
        assertEquals(
                ConnectivityErrorTypeEnum.AUTH_FAILED,
                ConnectivityErrorClassifier.classify(null, null, "invalid_api_key provided"));
    }

    @Test
    void fallsBackToUpstreamError() {
        assertEquals(ConnectivityErrorTypeEnum.UPSTREAM_ERROR, ConnectivityErrorClassifier.classify(null, null, null));
        assertEquals(
                ConnectivityErrorTypeEnum.UPSTREAM_ERROR,
                ConnectivityErrorClassifier.classify(null, new RuntimeException("boom"), null));
    }

    @Test
    void sanitizesMessagesForStorage() {
        String sanitized =
                ConnectivityErrorClassifier.sanitizeMessage("line1\r\nline2 sk-secret-key tail", "sk-secret-key");
        assertFalse(sanitized.contains("sk-secret-key"));
        assertTrue(sanitized.contains("***"));
        assertFalse(sanitized.contains("\r"));
        assertFalse(sanitized.contains("\n"));

        StringBuilder longMessage = new StringBuilder();
        for (int i = 0; i < 60; i++) longMessage.append("chunk-").append(i).append(' ');
        assertEquals(
                255,
                ConnectivityErrorClassifier.sanitizeMessage(longMessage.toString(), null)
                        .length());
        assertEquals("", ConnectivityErrorClassifier.sanitizeMessage(null, null));
    }
}
