package de.tum.devopss26.userservice.exception;

import de.tum.devopss26.shared.exception.ConflictException;

/**
 * Exception thrown when an attempt is made to register a user with a username
 * that already exists in the system.
 */
public class UserAlreadyExistsException extends ConflictException {

	/**
	 * Constructs a new exception for the given username.
	 *
	 * @param username the duplicate username
	 */
	public UserAlreadyExistsException(String username) {
		super("User with username " + username + " already exists");
	}

}
