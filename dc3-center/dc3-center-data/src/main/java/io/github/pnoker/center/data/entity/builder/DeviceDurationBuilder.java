package io.github.pnoker.center.data.entity.builder;

import io.github.pnoker.center.data.entity.bo.DeviceRunBO;
import io.github.pnoker.center.data.entity.model.DeviceRunDO;
import io.github.pnoker.center.data.entity.vo.DeviceRunVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeviceDurationBuilder {

    DeviceRunVO buildVOByBOList(DeviceRunBO duration);
}
