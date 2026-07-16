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

@Slf4j
@Service
@RequiredArgsConstructor
class UserAuthenticationServiceImpl implements UserAuthenticationService {

    private final UserRepository repository;
    private final UserMapper mapper;
    private final JwtService jwtService;

    /**
     * Registers a new user. Checks for duplicate usernames before persisting.
     *
     * @param request the registration request containing username and password
     * @throws UserAlreadyExistsException if a user with the same username already exists
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
     * Authenticates the currently logged-in user by extracting their username
     * from the security context and generating a JWT token.
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
     * Validates the JWT token from the Authorization header.
     * Returns {@code false} for any error or invalid token without throwing exceptions.
     *
     * @param authHeader the Authorization header value (expected format: "Bearer &lt;token&gt;")
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
