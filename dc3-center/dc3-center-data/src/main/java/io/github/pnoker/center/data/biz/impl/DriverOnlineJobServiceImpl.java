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

package io.github.pnoker.center.data.biz.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.GrpcPage;
import io.github.pnoker.center.data.biz.DriverOnlineJobService;
import io.github.pnoker.center.data.dal.DriverStatusHistoryManager;
import io.github.pnoker.center.data.entity.model.DriverStatusHistoryDO;
import io.github.pnoker.center.data.entity.query.DriverQuery;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.redis.service.RedisService;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class DriverOnlineJobServiceImpl implements DriverOnlineJobService {
    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DriverApiGrpc.DriverApiBlockingStub driverApiBlockingStub;

    @Resource
    private RedisService redisService;

    @Resource
    private DriverStatusHistoryManager driverStatusHistoryService;

    @Override
    public void driverOnline() {
        DriverQuery driverQuery = new DriverQuery();
        driverQuery.setTenantId(1L);
        driverQuery.setPage(new Pages());
        driverQuery.getPage().setCurrent(1);
        driverQuery.getPage().setSize(99999);
        GrpcPage.Builder page = GrpcPage.newBuilder()
                .setSize(driverQuery.getPage().getSize())
                .setCurrent(driverQuery.getPage().getCurrent());
        GrpcPageDriverQuery.Builder query = GrpcPageDriverQuery.newBuilder()
                .setPage(page);
        if (CharSequenceUtil.isNotEmpty(driverQuery.getDriverName())) {
            query.setDriverName(driverQuery.getDriverName());
        }
        if (CharSequenceUtil.isNotEmpty(driverQuery.getServiceName())) {
            query.setServiceName(driverQuery.getServiceName());
        }
        if (CharSequenceUtil.isNotEmpty(driverQuery.getServiceHost())) {
            query.setServiceHost(driverQuery.getServiceHost());
        }
        if (ObjectUtil.isNotNull(driverQuery.getDriverTypeFlag())) {
            query.setDriverTypeFlag(driverQuery.getDriverTypeFlag().getIndex());
        } else {
            query.setDriverTypeFlag(DefaultConstant.DEFAULT_INT);
        }
        if (ObjectUtil.isNotNull(driverQuery.getEnableFlag())) {
            query.setEnableFlag(driverQuery.getEnableFlag().getIndex());
        } else {
            query.setEnableFlag(DefaultConstant.DEFAULT_INT);
        }
        if (ObjectUtil.isNotEmpty(driverQuery.getTenantId())) {
            query.setTenantId(driverQuery.getTenantId());
        }
        GrpcRPageDriverDTO list = driverApiBlockingStub.list(query.build());
        GrpcPageDriverDTO data = list.getData();
        List<GrpcDriverDTO> dataList = data.getDataList();
        if (ObjectUtils.isNotEmpty(dataList)) {
            List<DriverStatusHistoryDO> driverStatusHistoryDOS = new ArrayList<>();
            dataList.forEach(driverDO -> {
                String key = PrefixConstant.DRIVER_STATUS_KEY_PREFIX + driverDO.getBase().getId();
                String status = redisService.getKey(key);
                status = ObjectUtil.isNotNull(status) ? status : DriverStatusEnum.OFFLINE.getCode();
                DriverStatusHistoryDO driverStatusHistoryDO = new DriverStatusHistoryDO();
                driverStatusHistoryDO.setDriverId(driverDO.getBase().getId());
                driverStatusHistoryDO.setDriverName(driverDO.getDriverName());
                driverStatusHistoryDO.setStatus(status);
                driverStatusHistoryDOS.add(driverStatusHistoryDO);
            });
            if (driverStatusHistoryDOS != null && driverStatusHistoryDOS.size() > 0) {
                driverStatusHistoryService.saveBatch(driverStatusHistoryDOS);
            }
        }
    }
}
