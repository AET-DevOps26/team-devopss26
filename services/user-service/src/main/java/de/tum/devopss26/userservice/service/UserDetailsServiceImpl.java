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
 * Implementation of {@link UserDetailsService} that loads user data from the database
 * using the {@link UserRepository}.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Constructs the service with the required user repository.
     *
     * @param userRepository the repository to load user data from
     */
    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by their username (case-insensitive lookup).
     *
     * @param username the username of the user to load
     * @return the {@link UserDetails} for the given username
     * @throws UsernameNotFoundException if no user is found with the given username
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
