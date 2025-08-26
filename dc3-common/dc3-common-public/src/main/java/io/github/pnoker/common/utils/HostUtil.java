/*
 * Copyright 2016-present the IoT DC3 original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
 * @version 2025.6.0
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
