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

package io.github.pnoker.common.utils;

import io.github.pnoker.common.constant.common.ExceptionConstant;
import lombok.extern.slf4j.Slf4j;

import java.net.*;
import java.util.*;

/**
 * Host 相关工具类
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
public class HostUtil {

    private HostUtil() {
        throw new IllegalStateException(ExceptionConstant.UTILITY_CLASS);
    }

    /**
     * 获取当前主机的 Local Host
     *
     * @return R of String
     */
    public static String localHost() {
        try {
            InetAddress address = InetAddress.getLocalHost();
            return address.getHostAddress();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Given an address resolve it to as many unique addresses or hostnames as can be found.
     *
     * @param address the address to resolve.
     * @return the addresses and hostnames that were resolved from {@code address}.
     */
    public static Set<String> getHostNames(String address) {
        return getHostNames(address, true);
    }

    /**
     * Given an address resolve it to as many unique addresses or hostnames as can be found.
     *
     * @param address         the address to resolve.
     * @param includeLoopBack if {@code true} loopback addresses will be included in the returned set.
     * @return the addresses and hostnames that were resolved from {@code address}.
     */
    public static Set<String> getHostNames(String address, boolean includeLoopBack) {
        Set<String> hostNames = new HashSet<>(4);

        try {
            InetAddress inetAddress = InetAddress.getByName(address);

            if (inetAddress.isAnyLocalAddress()) {
                loopBackAddresses(hostNames, includeLoopBack);
            } else {
                boolean loopback = inetAddress.isLoopbackAddress();

                if (!loopback || includeLoopBack) {
                    hostNames.add(inetAddress.getHostName());
                    hostNames.add(inetAddress.getHostAddress());
                    hostNames.add(inetAddress.getCanonicalHostName());
                }
            }
        } catch (UnknownHostException | SocketException e) {
            log.warn("Failed to get hostname for bind address: {}", address, e);
        }

        return hostNames;
    }

    /**
     * 获取电脑 Mac 物理地址列表
     *
     * @return Mac Array
     */
    public static List<String> localMacList() {
        ArrayList<String> macList = new ArrayList<>(16);
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                lookupLocalMac(macList, networkInterface);
            }
            if (!macList.isEmpty()) {
                return macList.stream().distinct().toList();
            }
        } catch (Exception e) {
            log.warn("Failed to get local mac address");
        }
        return macList;
    }

    /**
     * Get loopback addresses
     *
     * @param hostNames       HostName Set
     * @param includeLoopBack includeLoopBack if {@code true} loopback addresses will be included in the returned set.
     * @throws SocketException SocketException
     */
    private static void loopBackAddresses(Set<String> hostNames, boolean includeLoopBack) throws SocketException {
        Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();

        for (NetworkInterface networkInterface : Collections.list(interfaceEnumeration)) {
            Collections.list(networkInterface.getInetAddresses()).forEach(inetAddress -> {
                if (inetAddress instanceof Inet4Address) {
                    boolean loopback = inetAddress.isLoopbackAddress();

                    if (!loopback || includeLoopBack) {
                        hostNames.add(inetAddress.getHostName());
                        hostNames.add(inetAddress.getHostAddress());
                        hostNames.add(inetAddress.getCanonicalHostName());
                    }
                }
            });
        }
    }

    /**
     * Lookup local mac
     *
     * @param macList          Mac List
     * @param networkInterface NetworkInterface
     * @throws SocketException SocketException
     */
    private static void lookupLocalMac(ArrayList<String> macList, NetworkInterface networkInterface) throws SocketException {
        List<InterfaceAddress> interfaceAddressList = networkInterface.getInterfaceAddresses();
        for (InterfaceAddress interfaceAddress : interfaceAddressList) {
            InetAddress inetAddress = interfaceAddress.getAddress();
            NetworkInterface network = NetworkInterface.getByInetAddress(inetAddress);
            if (network == null) {
                continue;
            }

            byte[] mac = network.getHardwareAddress();
            if (mac != null) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    stringBuilder.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                }
                macList.add(stringBuilder.toString());
            }
        }
    }
}
