package com.pnoker.service.dbs.model;

import javax.persistence.*;

public class User {
    /**
     * id唯一标识
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名
     */
    private String name;

    /**
     * 密码
     */
    private String pass;

    /**
     * 获取id唯一标识
     *
     * @return id - id唯一标识
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置id唯一标识
     *
     * @param id id唯一标识
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户名
     *
     * @return name - 用户名
     */
    public String getName() {
        return name;
    }

    /**
     * 设置用户名
     *
     * @param name 用户名
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * 获取密码
     *
     * @return pass - 密码
     */
    public String getPass() {
        return pass;
    }

    /**
     * 设置密码
     *
     * @param pass 密码
     */
    public void setPass(String pass) {
        this.pass = pass;
    }
}