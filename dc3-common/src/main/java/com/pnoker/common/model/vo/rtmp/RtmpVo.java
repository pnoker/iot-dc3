package com.pnoker.common.model.vo.rtmp;

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
    @ApiModelProperty(hidden = true)
    private long id;
    @ApiModelProperty(value = "rtsp视频流地址", required = true)
    private String rtspUrl;
    @ApiModelProperty(value = "rtmp推流地址", required = true)
    private String rtmpUrl;
    @ApiModelProperty(value = "command配置", required = true)
    private String command;
    @ApiModelProperty(value = "rtsp视频类型", required = true)
    private short videoType;
    @ApiModelProperty(value = "是否自动启动任务", required = true)
    private boolean autoStart;
}
