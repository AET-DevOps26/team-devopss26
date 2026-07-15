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
 * <b>Why two filter chains?</b>
 * The login endpoint ({@code /login}) uses HTTP Basic authentication to verify credentials,
 * while all other auth endpoints use JWT Bearer tokens. These are fundamentally different
 * authentication mechanisms and must be handled by separate SecurityFilterChain beans with
 * explicit {@code @Order} to control evaluation priority.
 * </p>
 *
 * <p><b>Why stateless sessions everywhere?</b>
 * This is a REST API, not a traditional web application. Storing session state on the server
 * would require sticky sessions or a shared session store (Redis), which adds operational
 * complexity without benefit — each request carries its own authentication via JWT or Basic.
 * {@link SessionCreationPolicy#STATELESS} tells Spring Security never to create an
 * {@code HttpSession} and never to use one to obtain the {@code SecurityContext}.
 * </p>
 *
 * <p><b>Why CSRF disabled?</b>
 * CSRF protection guards against attacks that trick a browser into sending unwanted requests.
 * Since this service serves non-browser clients (mobile apps, other services) and uses token-based
 * auth instead of cookie-based sessions, CSRF is not applicable. Leaving it enabled would
 * require every request to include a CSRF token, which adds friction for API consumers.
 * </p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Uses HTTP Basic authentication: Spring Security's {@code BasicAuthenticationFilter}
     * extracts the {@code Authorization: Basic ...} header, validates credentials against
     * {@link de.tum.devopss26.userservice.service.UserDetailsServiceImpl}, and populates the
     * security context.
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
     * Registration, token-check, and public-key endpoints are publicly accessible (no auth
     * required). All other requests must include a valid JWT Bearer token, verified by
     * {@link JwtAuthenticationFilter} which is inserted <em>before</em>
     * {@link UsernamePasswordAuthenticationFilter} so that the JWT-derived authentication
     * is available before any downstream security checks.
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
     * BCrypt's strength factor is logarithmic — 12 means 2¹² = 4096 iterations.
     * This is a deliberate trade-off: higher values (e.g., 14+) provide stronger
     * resistance against GPU-based brute force but add noticeable latency (300-500ms)
     * per hash on typical server hardware, which hurts login UX. Strength 12 offers
     * a good balance for a web application on modern hardware (~100-150ms per hash).
     * </p>
     * <p>
     * BCrypt is preferred over SHA-based hashing because it includes a built-in salt
     * and is deliberately slow (computationally expensive), making pre-computed rainbow
     * table attacks infeasible even if the hash column is leaked.
     * </p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
