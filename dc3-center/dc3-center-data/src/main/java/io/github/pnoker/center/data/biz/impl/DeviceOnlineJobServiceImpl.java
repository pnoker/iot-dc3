package io.github.pnoker.center.data.biz.impl;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import io.github.pnoker.api.center.manager.*;
import io.github.pnoker.api.common.GrpcPageDTO;
import io.github.pnoker.center.data.biz.DeviceOnlineJobService;
import io.github.pnoker.center.data.biz.DriverOnlineJobService;
import io.github.pnoker.center.data.dal.DeviceStatusHistoryManager;
import io.github.pnoker.center.data.dal.DriverStatusHistoryManager;
import io.github.pnoker.center.data.entity.model.DeviceStatusHistoryDO;
import io.github.pnoker.center.data.entity.model.DriverStatusHistoryDO;
import io.github.pnoker.center.data.entity.query.DeviceQuery;
import io.github.pnoker.center.data.entity.query.DriverQuery;
import io.github.pnoker.common.constant.common.DefaultConstant;
import io.github.pnoker.common.constant.common.PrefixConstant;
import io.github.pnoker.common.constant.service.ManagerConstant;
import io.github.pnoker.common.entity.common.Pages;
import io.github.pnoker.common.enums.DriverStatusEnum;
import io.github.pnoker.common.redis.RedisService;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
public class DeviceOnlineJobServiceImpl implements DeviceOnlineJobService {
    @GrpcClient(ManagerConstant.SERVICE_NAME)
    private DeviceApiGrpc.DeviceApiBlockingStub deviceApiBlockingStub;

    @Resource
    private RedisService redisService;

    @Resource
    private DeviceStatusHistoryManager deviceStatusHistoryService;
    @Override
    public void deviceOnline() {
        DeviceQuery deviceQuery =new DeviceQuery();
        deviceQuery.setTenantId(1L);
        deviceQuery.setPage(new Pages());
        deviceQuery.getPage().setCurrent(1);
        deviceQuery.getPage().setSize(99999);
        GrpcPageDTO.Builder page = GrpcPageDTO.newBuilder()
                .setSize(deviceQuery.getPage().getSize())
                .setCurrent(deviceQuery.getPage().getCurrent());
        DeviceDTO.Builder builder = buildDTOByQuery(deviceQuery);
        GrpcPageDeviceQueryDTO.Builder query = GrpcPageDeviceQueryDTO.newBuilder()
                .setPage(page)
                .setDevice(builder);
        GrpcRPageDeviceDTO list = deviceApiBlockingStub.list(query.build());
        GrpcPageDeviceDTO data = list.getData();
        List<DeviceDTO> dataList = data.getDataList();
        if (ObjectUtils.isNotEmpty(dataList)){
            List<DeviceStatusHistoryDO> deviceStatusHistoryDOS = new ArrayList<>();
            dataList.forEach(driverDO->{
                String key = PrefixConstant.DEVICE_STATUS_KEY_PREFIX + driverDO.getBase().getId();
                String status = redisService.getKey(key);
                status = ObjectUtil.isNotNull(status) ? status : DriverStatusEnum.OFFLINE.getCode();
                DeviceStatusHistoryDO deviceStatusHistoryDO = new DeviceStatusHistoryDO();
                deviceStatusHistoryDO.setDriverId(driverDO.getDriverId());
                deviceStatusHistoryDO.setDeviceId(driverDO.getBase().getId());
                deviceStatusHistoryDO.setDeviceName(driverDO.getDeviceName());
                deviceStatusHistoryDO.setStatus(status);
                deviceStatusHistoryDOS.add(deviceStatusHistoryDO);
            });
            if (deviceStatusHistoryDOS!=null&&deviceStatusHistoryDOS.size()>0){
                deviceStatusHistoryService.saveBatch(deviceStatusHistoryDOS);
            }
        }
    }
    private static DeviceDTO.Builder buildDTOByQuery(DeviceQuery pageQuery) {
        DeviceDTO.Builder builder = DeviceDTO.newBuilder();
        if (CharSequenceUtil.isNotEmpty(pageQuery.getDeviceName())) {
            builder.setDeviceName(pageQuery.getDeviceName());
        }
        if (ObjectUtil.isNotEmpty(pageQuery.getDriverId())) {
            builder.setDriverId(pageQuery.getDriverId());
        } else {
            builder.setDriverId(DefaultConstant.DEFAULT_INT);
        }
        if (ObjectUtil.isNotNull(pageQuery.getEnableFlag())) {
            builder.setEnableFlag(pageQuery.getEnableFlag().getIndex());
        } else {
            builder.setEnableFlag(DefaultConstant.DEFAULT_INT);
        }
        if (ObjectUtil.isNotEmpty(pageQuery.getTenantId())) {
            builder.setTenantId(pageQuery.getTenantId());
        }
        return builder;
    }
}
