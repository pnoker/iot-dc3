package com.pnoker.transfer.rtmp.model.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
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
@ApiModel("Rtmp转码任务实体类")
@NoArgsConstructor
@AllArgsConstructor
public class RtmpVo {
    private long id;
    @ApiModelProperty(value = "rtsp视频流地址", name = "rtspUrl", required = true)
    private String rtspUrl;
    @ApiModelProperty(value = "rtmp推流地址", name = "rtmpUrl", required = true)
    private String rtmpUrl;
    @ApiModelProperty(value = "command配置", name = "command", required = true)
    private String command;
    @ApiModelProperty(value = "rtsp视频类型", name = "videoType", required = true)
    private short videoType;
    @ApiModelProperty(value = "是否自动启动任务", name = "autoStart", required = true)
    private boolean autoStart;
}
