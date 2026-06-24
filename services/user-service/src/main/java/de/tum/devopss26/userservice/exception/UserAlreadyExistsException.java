package de.tum.devopss26.userservice.exception;

public class UserAlreadyExistsException extends RuntimeException {
	
	public UserAlreadyExistsException(String username) {
		super("User with username " + username + " already exists");
	}
	
}
