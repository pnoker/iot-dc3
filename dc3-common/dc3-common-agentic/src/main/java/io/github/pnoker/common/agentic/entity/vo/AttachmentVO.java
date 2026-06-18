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
package io.github.pnoker.common.agentic.entity.vo;

import io.github.pnoker.common.entity.base.BaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * View object for agentic attachment API responses.
 *
 * @author pnoker
 * @version 2026.5.10
 * @since 2026.5.10
 */
@Getter
@Setter
@ToString(callSuper = true)
@Schema(description = "Attachment view object")
public class AttachmentVO extends BaseVO {

    @Schema(description = "ID of the conversation session this attachment belongs to.", example = "conv_xyz789")
    private String conversationId;

    @Schema(description = "Original file name of the uploaded attachment.", example = "report.pdf")
    private String fileName;

    @Schema(description = "MIME content type of the attachment (e.g. application/pdf, image/png).", example = "application/pdf")
    private String contentType;

    @Schema(description = "File size of the attachment in bytes.", example = "10240")
    private Long size;

    @Schema(description = "Server-side storage path where the attachment file is persisted.")
    private String filePath;

}
