package com.pnoker.common.model.vo.rtmp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>Copyright(c) 2019. Pnoker All Rights Reserved.
 * <p>@Author     : Pnoker
 * <p>Email      : pnokers@gmail.com
 * <p>Description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RtmpVo {
    private long id;
    private String name;
    private String rtspUrl;
    private String rtmpUrl;
    private String command;
    private short videoType;
    private boolean autoStart;
}
