package io.github.pnoker.center.manager.entity.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 
 * @TableName dc3_connection_profile
 */
@Data
@TableName(value ="dc3_connection_profile")
public class ConnectionProfile implements Serializable {
    /**
     * 
     */
    @TableId
    private Integer id;

    /**
     * 
     */
    private String profileName;

    /**
     * 
     */
    private String profileType;

    /**
     * 
     */
    private String brokerAddress;

    /**
     * 
     */
    private Integer brokerPort;

    /**
     * 
     */
    private String clientId;

    /**
     * 
     */
    private Integer connectionTimeout;

    /**
     * 
     */
    private Integer keepAliveInterval;

    /**
     * 
     */
    private Integer maxInflight;

    /**
     * 
     */
    private String mqttVersion;
    private String username;
    private String password;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;


}