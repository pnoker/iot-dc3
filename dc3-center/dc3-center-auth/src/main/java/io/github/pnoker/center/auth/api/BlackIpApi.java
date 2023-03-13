/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.center.auth.api;


import io.github.pnoker.api.center.auth.BlackIpApiGrpc;
import io.github.pnoker.api.center.auth.IpQuery;
import io.github.pnoker.api.center.auth.RBlackIpDTO;
import io.github.pnoker.api.common.RDTO;
import io.github.pnoker.center.auth.service.BlackIpService;
import io.github.pnoker.common.enums.ResponseEnum;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.Resource;

/**
 * BlackIp Api
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
@GrpcService
public class BlackIpApi extends BlackIpApiGrpc.BlackIpApiImplBase {

    @Resource
    private BlackIpService blackIpService;

    @Override
    public void checkBlackIpValid(IpQuery request, StreamObserver<RBlackIpDTO> responseObserver) {
        RBlackIpDTO.Builder builder = RBlackIpDTO.newBuilder();
        RDTO.Builder rBuilder = RDTO.newBuilder();
        Boolean ipValid = blackIpService.checkBlackIpValid(request.getIp());
        if (!Boolean.TRUE.equals(ipValid)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.IP_INVALID.getCode());
            rBuilder.setMessage(ResponseEnum.IP_INVALID.getMessage());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getMessage());
            builder.setData(true);
        }

        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}
