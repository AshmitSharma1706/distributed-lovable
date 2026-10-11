package com.project.distributed_lovable.common_lib.security;

import com.project.distributed_lovable.common_lib.filter.CorrelationIdFilter;
import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

@AutoConfiguration
public class SharedSecurityAutoConfiguration {

    @Bean
    public CorrelationIdFilter correlationIdFilter() {
        return new CorrelationIdFilter();
    }

    @Bean
    public AuthUtil authUtil(){
        return new AuthUtil();
    }

    @Bean
    public JwtAuthFilter JwtAuthFilter(AuthUtil authUtil,
                                       @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver){
        return new JwtAuthFilter(authUtil, handlerExceptionResolver);
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getCredentials() instanceof String token) {
                requestTemplate.header("Authorization", "Bearer " + token);
            }

            String requestId = MDC.get(CorrelationIdFilter.MDC_KEY);
            if (requestId != null && !requestId.isBlank()) {
                requestTemplate.header(CorrelationIdFilter.CORRELATION_ID_HEADER, requestId);
            }
        };
    }
}
