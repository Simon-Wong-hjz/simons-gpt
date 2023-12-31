package com.simwong.simonsgpt.utils;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import reactor.core.publisher.Mono;

public class SecurityUtils {
    public static Mono<Boolean> isAuthenticated() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .map(authentication -> authentication != null && authentication.isAuthenticated())
                .defaultIfEmpty(false);
    }
}
