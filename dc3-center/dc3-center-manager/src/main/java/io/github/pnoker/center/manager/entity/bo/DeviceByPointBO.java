package io.github.pnoker.center.manager.entity.bo;

import io.github.pnoker.center.manager.entity.model.DeviceDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeviceByPointBO {

    /**
     * 数量
     */
    private Long count;

    /**
     * 设备集合
     */
    private List<DeviceDO> devices;

}
