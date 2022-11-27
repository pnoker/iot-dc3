/*
 * Copyright 2016-present Pnoker All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      https://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.pnoker.common.utils;

import cn.hutool.core.util.ReUtil;
import io.github.pnoker.common.bean.common.TreeNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.*;
import java.math.BigDecimal;
import java.net.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dc3 平台自定义工具类集合
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Slf4j
public class Dc3Util {

    /**
     * 判断字符串是否为 数字格式
     *
     * @param content 字符串
     * @return boolean
     */
    public static boolean isNumeric(String content) {
        String regex = "-?[0-9]+(\\.[0-9]+)?";
        try {
            return ReUtil.isMatch(regex, new BigDecimal(content).toString());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断字符串是否为 用户名格式（2-64）
     *
     * @param name String
     * @return boolean
     */
    public static boolean isName(String name) {
        String regex = "^[A-Za-z0-9\\u4e00-\\u9fa5][A-Za-z0-9\\u4e00-\\u9fa5-_]{1,63}$";
        return ReUtil.isMatch(regex, name);
    }

    /**
     * 判断字符串是否为 手机号码格式
     *
     * @param phone String
     * @return boolean
     */
    public static boolean isPhone(String phone) {
        String regex = "^1([3-9])\\d{9}$";
        return ReUtil.isMatch(regex, phone);
    }

    /**
     * 判断字符串是否为 邮箱地址格式
     *
     * @param mail String
     * @return boolean
     */
    public static boolean isMail(String mail) {
        String regex = "^[a-zA-Z0-9_.-]+@[a-zA-Z0-9]+\\.[a-zA-Z0-9]+$";
        return ReUtil.isMatch(regex, mail);
    }

    /**
     * 判断字符串是否为 密码格式（8-16）
     *
     * @param password String
     * @return boolean
     */
    public static boolean isPassword(String password) {
        String regex = "^[a-zA-Z]\\w{7,15}$";
        return ReUtil.isMatch(regex, password);
    }

    /**
     * 判断字符串是否为 Host格式
     *
     * @param host String
     * @return boolean
     */
    public static boolean isHost(String host) {
        String regex = "^((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})(\\.((2(5[0-5]|[0-4]\\d))|[0-1]?\\d{1,2})){3}$";
        return ReUtil.isMatch(regex, host);
    }

    /**
     * 判断字符串是否为 驱动端口格式
     *
     * @param port Integer
     * @return boolean
     */
    public static boolean isDriverPort(int port) {
        String regex = "^8[6-7]\\d{2}$";
        return ReUtil.isMatch(regex, String.valueOf(port));
    }

    /**
     * InputStream 转 String
     * 此方法可以防止中文乱码
     *
     * @param inputStream InputStream
     * @return String
     */
    public static String inputStreamToString(InputStream inputStream) {
        try (ByteArrayOutputStream boa = new ByteArrayOutputStream()) {
            int length;
            byte[] buffer = new byte[1024];
            while ((length = inputStream.read(buffer)) > -1) {
                boa.write(buffer, 0, length);
            }
            byte[] result = boa.toByteArray();
            String temp = new String(result);
            if (temp.contains("gb2312")) {
                return new String(result, "gb2312");
            } else {
                return DecodeUtil.byteToString(result);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * File 转 MultipartFile
     *
     * @param fileInputStream FileInputStream
     * @return MultipartFile
     */
    public static MultipartFile fileInputStreamToMultipartFile(FileInputStream fileInputStream) {
        FileItemFactory factory = new DiskFileItemFactory(16, null);
        String textFieldName = "file";
        FileItem item = factory.createItem(textFieldName, "text/plain", true, "Dc3MultipartFile");
        try {
            int length = 0;
            byte[] buffer = new byte[1024];
            OutputStream outputStream = item.getOutputStream();
            while ((length = fileInputStream.read(buffer)) > -1) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            fileInputStream.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return new CommonsMultipartFile(item);
    }

    /**
     * Destroy Process With Command
     *
     * @param process Process
     * @param cmd     Exit Command
     */
    public static void destroyProcessWithCmd(Process process, String cmd) {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        try {
            if (!cmd.equals("")) {
                writer.write(cmd);
                writer.flush();
                writer.close();
            }
            process.destroyForcibly();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 两层循环实现建树
     *
     * @param treeNodes 传入的树节点列表
     * @return T Array
     */
    public <T extends TreeNode> List<T> buildByLoop(List<T> treeNodes, Object root) {
        List<T> trees = new ArrayList<>(16);
        for (T treeNode : treeNodes) {
            if (root.equals(treeNode.getParentId())) {
                trees.add(treeNode);
            }
            for (T it : treeNodes) {
                if (it.getParentId() == treeNode.getId()) {
                    if (treeNode.getChildren() == null) {
                        treeNode.setChildren(new ArrayList<>(16));
                    }
                    treeNode.add(it);
                }
            }
        }
        return trees;
    }

    /**
     * 使用递归方法建树
     *
     * @param treeNodes 传入的树节点列表
     * @return T Array
     */
    public <T extends TreeNode> List<T> buildByRecursive(List<T> treeNodes, Object root) {
        List<T> trees = new ArrayList<>(16);
        for (T treeNode : treeNodes) {
            if (root.equals(treeNode.getParentId())) {
                trees.add(findChildren(treeNode, treeNodes));
            }
        }
        return trees;
    }

    /**
     * 递归查找子节点
     *
     * @param treeNodes 传入的树节点列表
     * @return T
     */
    public <T extends TreeNode> T findChildren(T treeNode, List<T> treeNodes) {
        for (T it : treeNodes) {
            if (treeNode.getId() == it.getParentId()) {
                if (treeNode.getChildren() == null) {
                    treeNode.setChildren(new ArrayList<>(16));
                }
                treeNode.add(findChildren(it, treeNodes));
            }
        }
        return treeNode;
    }

    /**
     * 获取当前主机的 Local Host
     *
     * @return String
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
                return macList.stream().distinct().collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to get local mac address");
        }
        return macList;
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
     * @param includeLoopback if {@code true} loopback addresses will be included in the returned set.
     * @return the addresses and hostnames that were resolved from {@code address}.
     */
    public static Set<String> getHostNames(String address, boolean includeLoopback) {
        Set<String> hostNames = new HashSet<>(16);

        try {
            InetAddress inetAddress = InetAddress.getByName(address);

            if (inetAddress.isAnyLocalAddress()) {
                loopbackAddresses(hostNames, includeLoopback);
            } else {
                boolean loopback = inetAddress.isLoopbackAddress();

                if (!loopback || includeLoopback) {
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
     * 从 Request 中获取指定 Key 的 Header 值
     *
     * @param httpServletRequest HttpServletRequest
     * @param key                String
     * @return String
     */
    public static String getRequestHeader(HttpServletRequest httpServletRequest, String key) {
        return httpServletRequest.getHeader(key);
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

    /**
     * Get loopback addresses
     *
     * @param hostNames       HostName Set
     * @param includeLoopback includeLoopback if {@code true} loopback addresses will be included in the returned set.
     * @throws SocketException SocketException
     */
    private static void loopbackAddresses(Set<String> hostNames, boolean includeLoopback) throws SocketException {
        Enumeration<NetworkInterface> interfaceEnumeration = NetworkInterface.getNetworkInterfaces();

        for (NetworkInterface networkInterface : Collections.list(interfaceEnumeration)) {
            Collections.list(networkInterface.getInetAddresses()).forEach(inetAddress -> {
                if (inetAddress instanceof Inet4Address) {
                    boolean loopback = inetAddress.isLoopbackAddress();

                    if (!loopback || includeLoopback) {
                        hostNames.add(inetAddress.getHostName());
                        hostNames.add(inetAddress.getHostAddress());
                        hostNames.add(inetAddress.getCanonicalHostName());
                    }
                }
            });
        }
    }
}
