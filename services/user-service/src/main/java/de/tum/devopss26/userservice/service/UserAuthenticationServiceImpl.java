package de.tum.devopss26.userservice.service;

import de.tum.devopss26.userservice.entity.User;
import de.tum.devopss26.userservice.exception.UserAlreadyExistsException;
import de.tum.devopss26.userservice.mapper.UserMapper;
import de.tum.devopss26.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.RegisterUserRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * <b>Registration flow:</b> The plain-text password in {@link RegisterUserRequest} is hashed
 * by {@link UserMapper#toEntity} via MapStruct's {@code qualifiedByName} mechanism, which
 * invokes {@code BCryptPasswordEncoder} with strength 12. We check for duplicate usernames
 * <i>before</i> password hashing to avoid unnecessary BCrypt work on obviously invalid requests.
 *
 * <p><b>Login flow:</b> Rather than accepting raw credentials, this method reads the already-authenticated
 * principal from {@link SecurityContextHolder}. This works because Spring Security's {@code httpBasic()}
 * filter chain (see {@code loginSecurityFilterChain}) populates the security context before the
 * controller is reached — so by the time this service runs, the {@code Authentication} object is
 * guaranteed to exist and contain the verified username. This design avoids handling raw passwords
 * in application code.
 *
 * <p><b>Token check flow:</b> The raw {@code Authorization} header is parsed for the {@code Bearer}
 * prefix. Stripping the prefix here rather than in the controller keeps the controller a thin
 * HTTP layer. We catch all exceptions broadly to ensure that any parsing or validation failure
 * results in a clean {@code false} rather than propagating to the client as a 500 error.
 */
@Slf4j
@Service
@RequiredArgsConstructor
class UserAuthenticationServiceImpl implements UserAuthenticationService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final JwtService jwtService;

    /**
     * Registers a new user account.
     *
     * @param request the registration request containing username and password
     * @throws UserAlreadyExistsException if a user with the given username already exists
     */
    @Transactional
    @Override
    public void registerUser(RegisterUserRequest request) {
        if (repository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException(request.getUsername());
        }

        User mapped = mapper.toEntity(request);
        repository.save(mapped);
    }

    /**
     * Authenticates the currently logged-in user and generates a JWT token.
     * Expects the security context to have been populated by Spring Security's Basic auth filter.
     *
     * @return the generated JWT token string
     * @throws UsernameNotFoundException if the authenticated user is not found in the database
     */
    @Transactional(readOnly = true)
    @Override
    public String loginUser() {
        String username = Objects.requireNonNull(SecurityContextHolder.getContext().getAuthentication()).getName();

        Optional<User> opt = repository.findByUsernameIgnoreCase(username);
        if (opt.isEmpty()) {
            throw new UsernameNotFoundException(username);
        }

        return jwtService.generateToken(opt.get().getId(), opt.get().getUsername());
    }

    /**
     * Validates a JWT token from the Authorization header.
     *
     * @param authHeader the raw {@code Authorization} header value (expected format: {@code Bearer <jwt>})
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    @Override
    public boolean checkToken(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return false;
            }
            String jwt = authHeader.substring(7);
            return jwtService.isTokenValid(jwt);
        } catch (Exception e) {
            log.atError().setCause(e).log("Error checking auth token");
            return false;
        }
    }
}
