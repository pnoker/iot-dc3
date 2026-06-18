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

package io.github.pnoker.api.common;

import io.github.pnoker.common.enums.ErrorCode;
import io.github.pnoker.common.enums.ResponseCode;
import io.github.pnoker.common.enums.SuccessCode;

/**
 * Factory for the gRPC {@link GrpcR} result envelope.
 * <p>
 * gRPC servers used to hand-build the same {@code newBuilder().setOk().setCode().setMessage()}
 * triple in every RPC. These helpers centralize that boilerplate so a server states intent
 * ({@code ok()} / {@code fail(code)} / {@code notFound()}) and the wire fields stay consistent
 * with the HTTP envelope's {@link ResponseCode} set.
 *
 * @author pnoker
 * @version 2025.9.0
 * @since 2016.10.1
 */
public final class GrpcRFactory {

    private GrpcRFactory() {
    }

    /**
     * Successful result carrying {@link SuccessCode#OK}.
     *
     * @return GrpcR
     */
    public static GrpcR ok() {
        return of(true, SuccessCode.OK);
    }

    /**
     * Successful result carrying a specific {@link SuccessCode}.
     *
     * @param code success code
     * @return GrpcR
     */
    public static GrpcR ok(SuccessCode code) {
        return of(true, code);
    }

    /**
     * Failed result carrying a specific {@link ErrorCode} and its default remark.
     *
     * @param code error code
     * @return GrpcR
     */
    public static GrpcR fail(ErrorCode code) {
        return of(false, code);
    }

    /**
     * Failed result carrying a specific {@link ErrorCode} and a custom message.
     *
     * @param code    error code
     * @param message custom message
     * @return GrpcR
     */
    public static GrpcR fail(ErrorCode code, String message) {
        return GrpcR.newBuilder().setOk(false).setCode(code.getCode()).setMessage(message).build();
    }

    /**
     * Failed result carrying {@link ErrorCode#NOT_FOUND}.
     *
     * @return GrpcR
     */
    public static GrpcR notFound() {
        return of(false, ErrorCode.NOT_FOUND);
    }

    private static GrpcR of(boolean ok, ResponseCode code) {
        return GrpcR.newBuilder().setOk(ok).setCode(code.getCode()).setMessage(code.getRemark()).build();
    }

}
