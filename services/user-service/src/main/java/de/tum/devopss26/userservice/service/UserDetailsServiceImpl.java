package de.tum.devopss26.userservice.service;

import de.tum.devopss26.userservice.entity.User;
import de.tum.devopss26.userservice.repository.UserRepository;
import lombok.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * <b>Case-insensitive lookup:</b> We use {@code findByUsernameIgnoreCase} so that users
 * can authenticate with any casing (e.g., "Alice", "alice", "ALICE") — which is more
 * user-friendly while the {@code username} column's {@code unique} constraint is case-sensitive
 * in PostgreSQL by default. The actual stored username (original casing) is returned as the
 * principal name.
 *
 * <p><b>Authorities:</b> An empty list is returned because this service does not yet implement
 * role-based access control. Every authenticated user receives the same effective permissions.
 * When roles are introduced, they should be loaded here and mapped to Spring Security
 * {@code GrantedAuthority} objects.
 *
 * <p>This service is consumed primarily by Spring Security's {@code DaoAuthenticationProvider}
 * (enabled implicitly by {@code httpBasic()}) and by {@link de.tum.devopss26.userservice.security.JwtAuthenticationFilter}
 * when reconstructing a {@code UserDetails} object from a validated JWT.</p>
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the user by username (case-insensitive lookup).
     *
     * @param username the username to look up
     * @return the {@link UserDetails} for the given username
     * @throws UsernameNotFoundException if no user with the given username exists
     */
    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPasswordHash(),
                Collections.emptyList()
        );
    }
}
