package io.github.pnoker.common.auth.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** RFC 6749/8414 protocol error carrying a stable OAuth error code. */
@Getter
public class OAuthProtocolException extends ResponseStatusException {

    private final String error;
    private final String description;

    public OAuthProtocolException(int status, String error, String description) {
        super(HttpStatus.valueOf(status), description);
        this.error = error;
        this.description = description;
    }
}
