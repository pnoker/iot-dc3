package io.github.pnoker.common.auth.biz;

import io.github.pnoker.common.auth.entity.bean.TokenValid;
import reactor.core.publisher.Mono;

/** Fully reactive token lifecycle contract. */
public interface ReactiveTokenService {
    Mono<String> generateSalt(String loginName, String tenantCode);
    Mono<String> generateToken(String loginName, String password, String tenantCode);
    Mono<Void> changePassword(String loginName, String currentPassword, String newPassword, String tenantCode);
    Mono<Boolean> tryCancelToken(String loginName, String tenantCode);
    Mono<TokenValid> checkValid(String loginName, String token, String tenantCode);
}
