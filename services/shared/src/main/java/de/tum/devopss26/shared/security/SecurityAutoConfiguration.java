package de.tum.devopss26.shared.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Services that include this module as a dependency automatically get JWT validation on any
 * controller method or class annotated with {@link RequireTokenValidation} — no manual
 * {@code WebMvcConfigurer} setup is required. Registration is driven by the
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} file.
 */
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
