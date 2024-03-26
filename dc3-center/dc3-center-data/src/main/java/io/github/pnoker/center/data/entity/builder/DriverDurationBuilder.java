package io.github.pnoker.center.data.entity.builder;

import io.github.pnoker.center.data.entity.bo.DriverRunBO;
import io.github.pnoker.center.data.entity.model.DriverRunDO;
import io.github.pnoker.center.data.entity.vo.DriverRunVO;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DriverDurationBuilder {

    DriverRunVO buildVOByBOList(DriverRunBO duration);
}