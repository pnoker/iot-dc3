package io.github.pnoker.center.manager.entity.query;

import io.github.pnoker.common.entity.common.Pages;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Topic Query
 *
 * @author pnoker
 * @since 2022.1.0
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Pages page;

    /**
     * 主题
     */

    private String topic;


    /**
     * 设备名称
     */

    private String deviceName;
}
