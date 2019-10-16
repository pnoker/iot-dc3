/*
 * Copyright 2019 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pnoker.transfer.opc.service;

import com.pnoker.transfer.opc.bean.OpcInfo;
import com.pnoker.transfer.opc.bean.OpcItem;
import com.pnoker.transfer.opc.bean.OpcNodes;
import com.pnoker.transfer.opc.bean.OpcServer;
import org.jinterop.dcom.common.JIException;
import org.openscada.opc.lib.common.AlreadyConnectedException;

import java.net.UnknownHostException;
import java.util.List;

public interface OpcService {
    /**
     * 查询指定主机 Opc 服务列表
     *
     * @param info
     * @return
     * @throws JIException
     * @throws UnknownHostException
     */
    List<OpcServer> opcServerList(OpcInfo info) throws JIException, UnknownHostException;

    /**
     * 查询指定 Opc 服务器节点列表
     *
     * @param info
     * @return
     * @throws AlreadyConnectedException
     * @throws JIException
     * @throws UnknownHostException
     */
    OpcNodes opcItemList(OpcInfo info) throws AlreadyConnectedException, JIException, UnknownHostException;

    /**
     * 同步读 Opc 服务器节点
     *
     * @param items
     * @return
     */
    OpcItem syncRead(List<String> items);

    /**
     * 异步读 Opc 服务器节点
     *
     * @param items
     * @return
     */
    OpcItem asyncRead(List<String> items);

    /**
     * 同步写 Opc 服务器节点
     *
     * @param items
     * @return
     */
    int syncWrite(List<OpcItem> items);

    /**
     * 异步写 Opc 服务器节点
     *
     * @param items
     * @return
     */
    int asyncWrite(List<OpcItem> items);
}
