package de.tum.devopss26.userservice.service;

import org.openapitools.model.RegisterUserRequest;

/**
 * Service interface for user authentication operations including registration,
 * login, and token validation.
 */
public interface UserAuthenticationService {

	/**
	 * Registers a new user with the given details.
	 *
	 * @param request the registration request containing username and password
	 */
	void registerUser(RegisterUserRequest request);

	/**
	 * Authenticates the currently logged-in user and returns a JWT token.
	 *
	 * @return the generated JWT token string
	 */
	String loginUser();

	/**
	 * Validates the JWT token from the Authorization header.
	 *
	 * @param authHeader the Authorization header value (expected format: "Bearer &lt;token&gt;")
	 * @return {@code true} if the token is valid, {@code false} otherwise
	 */
	boolean checkToken(String authHeader);

}
