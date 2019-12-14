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

package com.pnoker.common.tool;

import cn.hutool.core.util.ReUtil;
import com.google.common.base.Charsets;
import com.pnoker.common.dto.NodeDto;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * <p>自定义工具类集合
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
 */
public class Dc3Tools {
    /**
     * 将字符串进行Base64编码
     *
     * @param str
     * @return 返回字节流
     */
    public static byte[] encode(String str) {
        return Base64.getEncoder().encode(str.getBytes(Charsets.UTF_8));
    }

    /**
     * 将字节流进行Base64编码
     *
     * @param bytes
     * @return 返回字符串
     */
    public static String encodeToString(byte[] bytes) {
        return Base64.getEncoder().encodeToString(bytes);
    }

    /**
     * 必须配合encode使用，用于encode编码之后解码
     *
     * @param str 字符串
     * @return 返回字节流
     */
    public static byte[] decode(String str) {
        return Base64.getDecoder().decode(str);
    }

    /**
     * 必须配合encode使用，用于encode编码之后解码
     *
     * @param input 字节流
     * @return 返回字节流
     */
    public static byte[] decode(byte[] input) {
        return Base64.getDecoder().decode(input);
    }

    /**
     * @param username
     * @return 0：无效用户名，1：用户名，2：手机号，3：邮箱
     */
    public static int usernameType(String username) {
        if (isUsername(username)) {
            return 1;
        }
        if (isPhone(username)) {
            return 2;
        }
        if (isMail(username)) {
            return 3;
        }
        return 0;
    }

    /**
     * 判断字符串是否为 用户名格式（3-16）
     *
     * @param username
     * @return
     */
    public static boolean isUsername(String username) {
        String regex = "^[a-zA-Z]\\w{2,15}$";
        return ReUtil.isMatch(regex, username);
    }

    /**
     * 判断字符串是否为 手机号码格式
     *
     * @param phone
     * @return
     */
    public static boolean isPhone(String phone) {
        String regex = "^(13[0-9]|14[5|7]|15[0|1|2|3|4|5|6|7|8|9]|18[0|1|2|3|5|6|7|8|9])\\d{8}$";
        return ReUtil.isMatch(regex, phone);
    }

    /**
     * 判断字符串是否为 邮箱地址格式
     *
     * @param mail
     * @return
     */
    public static boolean isMail(String mail) {
        String regex = "^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$";
        return ReUtil.isMatch(regex, mail);
    }

    /**
     * 判断密码级别
     *
     * @param password
     * @return 0：无效密码，1：弱密码，2：普通密码，3：强密码
     */
    public static int passwordType(String password) {
        if (isStrongPassword(password)) {
            return 3;
        }
        if (isPassword(password)) {
            return 2;
        }
        if (isWeakPassword(password)) {
            return 1;
        }
        return 0;
    }

    /**
     * 判断字符串是否为 弱密码格式（8-16）
     *
     * @param password
     * @return
     */
    public static boolean isWeakPassword(String password) {
        String regex = "^[a-zA-Z]\\w{7,15}$";
        return ReUtil.isMatch(regex, password);
    }

    /**
     * 判断字符串是否为 密码格式（8-16）
     *
     * @param password
     * @return
     */
    public static boolean isPassword(String password) {
        String regex = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9]{8,16}$";
        return ReUtil.isMatch(regex, password);
    }

    /**
     * 判断字符串是否为 强密码格式（8-16）
     *
     * @param password
     * @return
     */
    public static boolean isStrongPassword(String password) {
        String regex = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,16}$";
        return ReUtil.isMatch(regex, password);
    }

    /**
     * 两层循环实现建树
     *
     * @param treeNodes 传入的树节点列表
     * @return
     */
    public <T extends NodeDto> List<T> buildByLoop(List<T> treeNodes, Object root) {
        List<T> trees = new ArrayList<>();
        for (T treeNode : treeNodes) {
            if (root.equals(treeNode.getParentId())) {
                trees.add(treeNode);
            }
            for (T it : treeNodes) {
                if (it.getParentId() == treeNode.getId()) {
                    if (treeNode.getChildren() == null) {
                        treeNode.setChildren(new ArrayList<>());
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
     * @param treeNodes
     * @return
     */
    public <T extends NodeDto> List<T> buildByRecursive(List<T> treeNodes, Object root) {
        List<T> trees = new ArrayList<T>();
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
     * @param treeNodes
     * @return
     */
    public <T extends NodeDto> T findChildren(T treeNode, List<T> treeNodes) {
        for (T it : treeNodes) {
            if (treeNode.getId() == it.getParentId()) {
                if (treeNode.getChildren() == null) {
                    treeNode.setChildren(new ArrayList<>());
                }
                treeNode.add(findChildren(it, treeNodes));
            }
        }
        return treeNode;
    }

}
