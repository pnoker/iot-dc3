package com.pnoker.transfer.opc.service;

import com.pnoker.transfer.opc.bean.OpcInfo;
import com.pnoker.transfer.opc.bean.OpcItem;
import com.pnoker.transfer.opc.bean.OpcNodes;
import com.pnoker.transfer.opc.bean.OpcServer;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.common.AlreadyConnectedException;

import java.net.UnknownHostException;
import java.util.List;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description: Opc 服务接口
 */
public interface OpcService {
    List<OpcServer> opcServerList(OpcInfo info) throws JIException, UnknownHostException;

    OpcNodes opcItemList(OpcInfo info) throws AlreadyConnectedException, JIException, UnknownHostException;

    OpcItem syncRead();

    OpcItem asyncRead();

    int syncWrite();

    int asyncWrite();
}
