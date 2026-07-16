package de.tum.devopss26.userservice.config;

import de.tum.devopss26.userservice.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for the User Service.
 * Defines security filter chains for login (HTTP Basic) and API (JWT-based) endpoints.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Security filter chain for the login endpoint ({@code /api/v1/users/auth/login}).
     * Uses HTTP Basic Authentication and stateless session management.
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the built {@link SecurityFilterChain}
     */
    @Bean
    @Order(1)
    public SecurityFilterChain loginSecurityFilterChain(HttpSecurity http) {
        http
                .securityMatcher("/api/v1/users/auth/login")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Security filter chain for all other API endpoints.
     * Permits unauthenticated access to registration, token check, and public key endpoints.
     * All other requests require a valid JWT token via {@link JwtAuthenticationFilter}.
     *
     * @param http           the {@link HttpSecurity} to configure
     * @param jwtAuthFilter  the JWT authentication filter to add before the username/password filter
     * @return the built {@link SecurityFilterChain}
     */
    @Bean
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthFilter) {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/users/auth/register", "/api/v1/users/auth/check-token", "/api/v1/users/auth/public-key").permitAll()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Provides a BCrypt password encoder with strength 12 for hashing user passwords.
     *
     * @return the {@link PasswordEncoder} instance
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
