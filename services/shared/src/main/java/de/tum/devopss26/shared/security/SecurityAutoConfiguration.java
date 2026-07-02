package de.tum.devopss26.shared.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Import(TokenValidationInterceptor.class)
public class SecurityAutoConfiguration implements WebMvcConfigurer {

    private final TokenValidationInterceptor tokenValidationInterceptor;

    public SecurityAutoConfiguration(TokenValidationInterceptor tokenValidationInterceptor) {
        this.tokenValidationInterceptor = tokenValidationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenValidationInterceptor);
    }
}
