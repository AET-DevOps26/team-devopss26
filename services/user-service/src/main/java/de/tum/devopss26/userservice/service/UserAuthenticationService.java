package de.tum.devopss26.userservice.service;

import org.openapitools.model.RegisterUserRequest;

/**
 * The interface is intentionally decoupled from any HTTP transport concerns; callers
 * (typically the REST controller) are responsible for header extraction and response formatting.
 */
public interface UserAuthenticationService {

    /**
     * @throws de.tum.devopss26.userservice.exception.UserAlreadyExistsException if a user
     *                                                                            with the same username already exists
     */
    void registerUser(RegisterUserRequest request);

    /**
     * Reads the already-authenticated principal from the security context rather than accepting
     * raw credentials — this works because Spring Security's {@code httpBasic()} filter populates
     * the context before the controller is reached.
     */
    String loginUser();

    /**
     * Returns {@code false} for missing, malformed, expired, or incorrectly signed tokens
     * without throwing exceptions.
     *
     * @param authHeader the raw {@code Authorization} header value (expected format: {@code Bearer <jwt>})
     */
    boolean checkToken(String authHeader);

}
