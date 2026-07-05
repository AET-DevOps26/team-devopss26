package de.tum.devopss26.userservice.exception;

import de.tum.devopss26.shared.exception.ConflictException;

public class UserAlreadyExistsException extends ConflictException {
	
	public UserAlreadyExistsException(String username) {
		super("User with username " + username + " already exists");
	}
	
}
