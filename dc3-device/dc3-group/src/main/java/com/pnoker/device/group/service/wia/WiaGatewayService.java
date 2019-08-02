package com.pnoker.device.group.service.wia;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.pnoker.device.group.model.wia.WiaGateway;

import java.util.List;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: WiaGateway 服务接口
 */
public interface WiaGatewayService {
    List<WiaGateway> list(Wrapper<WiaGateway> wrapper);
}
