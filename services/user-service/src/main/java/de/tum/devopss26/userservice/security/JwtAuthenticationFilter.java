package de.tum.devopss26.userservice.security;

import de.tum.devopss26.userservice.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * <b>Token extraction:</b>
 * The filter reads the {@code Authorization} header and checks for the {@code Bearer } prefix.
 * We <em>must</em> check the prefix rather than simply attempting to parse any non-null header
 * because other auth mechanisms (e.g., Basic auth) also use the same header with a different
 * format. Prematurely attempting JWT parsing on a Basic token would produce confusing errors.
 *
 * <p><b>Why {@code null} credentials?</b>
 * The second argument to {@code UsernamePasswordAuthenticationToken} is the credentials
 * (typically a password). For token-based auth, the JWT itself is the credential and is
 * already verified — there is no need to carry it in the authentication object. Passing
 * {@code null} avoids exposing the token string through the security context.
 *
 * <p><b>Empty authorities:</b>
 * Currently no roles are assigned (see {@code Collections.emptyList()} in
 * {@link de.tum.devopss26.userservice.service.UserDetailsServiceImpl}). This means all
 * authenticated users have equal permissions. Adding role-based access requires populating
 * the authority list here or in the {@code UserDetailsService}.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        if (jwtService.isTokenValid(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(jwtService.extractUsername(jwt));
            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                    userDetails,
                    null,
                    userDetails.getAuthorities()
            );
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
        filterChain.doFilter(request, response);
    }
}
