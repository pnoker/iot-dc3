package com.pnoker.device.group.service.wia.impl;

import com.pnoker.device.group.dao.wia.WiaGatewayMapper;
import com.pnoker.device.group.model.wia.WiaGateway;
import com.pnoker.device.group.service.wia.WiaGatewayService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: WiaGateway 接口实现
 */
@Service
public class WiaGatewayServiceImpl implements WiaGatewayService {
    @Autowired
    private WiaGatewayMapper wiaGatewayMapper;

    @Override
    public List<WiaGateway> list() {
        return wiaGatewayMapper.list();
    }
}
