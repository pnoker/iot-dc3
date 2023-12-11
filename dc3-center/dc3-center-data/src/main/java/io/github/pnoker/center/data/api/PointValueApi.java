/*
 * Copyright 2016-present the IoT DC3 original author or authors.
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

package io.github.pnoker.center.data.api;

import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.data.PointValueApiGrpc;
import io.github.pnoker.api.center.data.PointValueDTO;
import io.github.pnoker.api.center.data.PointValueQuery;
import io.github.pnoker.api.center.data.RPointValueDTO;
import io.github.pnoker.api.common.GrpcRDTO;
import io.github.pnoker.center.data.service.PointValueService;
import io.github.pnoker.center.data.entity.point.PointValue;
import io.github.pnoker.common.constant.enums.ResponseEnum;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import javax.annotation.Resource;

@Slf4j
@GrpcService
public class PointValueApi extends PointValueApiGrpc.PointValueApiImplBase {

    @Resource
    PointValueService pointValueService;

    @Override
    public void lastValue(PointValueQuery request, StreamObserver<RPointValueDTO> responseObserver) {
        RPointValueDTO.Builder builder = RPointValueDTO.newBuilder();
        RDTO.Builder rBuilder = RDTO.newBuilder();
        PointValue pointValue = pointValueService.latest(request);
        if (ObjectUtil.isNull(pointValue)) {
            rBuilder.setOk(false);
            rBuilder.setCode(ResponseEnum.NO_RESOURCE.getCode());
            rBuilder.setMessage(ResponseEnum.NO_RESOURCE.getMessage());
        } else {
            rBuilder.setOk(true);
            rBuilder.setCode(ResponseEnum.OK.getCode());
            rBuilder.setMessage(ResponseEnum.OK.getMessage());
            builder.setData(buildDTOByDO(pointValue));
        }
        builder.setResult(rBuilder);
        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

    private PointValueDTO buildDTOByDO(PointValue pointValue) {
        PointValueDTO.Builder builder = PointValueDTO.newBuilder();
        builder.setDeviceId(pointValue.getDeviceId());
        builder.setPointId(pointValue.getPointId());
        builder.setValue(pointValue.getValue());
        builder.setRawValue(pointValue.getRawValue());
        builder.setCreateTime(pointValue.getCreateTime().getTime());
        builder.setOriginTime(pointValue.getOriginTime().getTime());
        return builder.build();
    }

}
