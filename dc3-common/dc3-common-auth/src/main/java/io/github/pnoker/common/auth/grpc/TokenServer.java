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

package io.github.pnoker.common.auth.grpc;

import io.github.pnoker.api.center.auth.GrpcLoginQuery;
import io.github.pnoker.api.center.auth.GrpcTokenValidationDTO;
import io.github.pnoker.api.center.auth.TokenApiGrpc;
import io.github.pnoker.common.auth.biz.ReactiveTokenService;
import io.github.pnoker.common.auth.entity.bean.TokenValid;
import io.github.pnoker.common.utils.TimeUtil;
import com.google.common.util.concurrent.MoreExecutors;
import io.grpc.Context;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * gRPC server handling token facade requests.
 *
 * @author pnoker
 * @since 2016.10.1
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenServer extends TokenApiGrpc.TokenApiImplBase {

    private final ReactiveTokenService tokenService;

    @Override
    public void checkValid(GrpcLoginQuery request, StreamObserver<GrpcTokenValidationDTO> responseObserver) {
        Context context = Context.current();
        AtomicBoolean terminated = new AtomicBoolean();
        reactor.core.Disposable subscription = tokenService.checkValid(request.getName(), request.getToken(), request.getTenant())
                .subscribe(entity -> {
                    if (context.isCancelled() || !terminated.compareAndSet(false, true)) return;
                    GrpcTokenValidationDTO.Builder builder = GrpcTokenValidationDTO.newBuilder()
                            .setValid(entity.isValid());
                    if (entity.isValid()) builder.setExpireTime(TimeUtil.completeFormat(entity.getExpireTime()));
                    responseObserver.onNext(builder.build());
                    responseObserver.onCompleted();
                }, error -> {
                    if (context.isCancelled() || !terminated.compareAndSet(false, true)) return;
                    log.warn("checkValid failed", error);
                    responseObserver.onError(io.grpc.Status.INTERNAL.withDescription("checkValid failed")
                            .withCause(error).asRuntimeException());
                });
        context.addListener(ignored -> subscription.dispose(), MoreExecutors.directExecutor());
    }

}
