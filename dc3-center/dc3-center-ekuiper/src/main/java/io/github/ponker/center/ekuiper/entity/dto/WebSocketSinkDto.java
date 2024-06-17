package io.github.ponker.center.ekuiper.entity.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * @author : Zhen
 * @date : 2024/5/22
 */
@Data
public class WebSocketSinkDto {

    @NotBlank
    private String addr;

    @NotBlank
    private String path;

    private Boolean insecureSkipVerify;

    @JsonProperty("certificationPath")
    private String certificationPath;

    @JsonProperty("privateKeyPath")
    private String privateKeyPath;

    @JsonProperty("rootCaPath")
    private String rootCaPath;

    @JsonProperty("certficationRaw")
    private String certficationRaw;

    @JsonProperty("privateKeyRaw")
    private String privateKeyRaw;

    @JsonProperty("rootCARaw")
    private String rootCARaw;

    @JsonProperty("checkConnection")
    private Boolean checkConnection;

    private Boolean sendSingle;

    private Boolean omitIfEmpty;

    private String format;
}
