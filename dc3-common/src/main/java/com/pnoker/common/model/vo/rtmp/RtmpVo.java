package com.pnoker.common.model.vo.rtmp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * <p>Rtmp Vo
 *
 * @author : pnoker
 * @email : pnokers@icloud.com
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
    private Boolean autoStart;
}
