package de.tum.devopss26.userservice.service;

import org.openapitools.model.RegisterUserRequest;

public interface UserAuthenticationService {

	void registerUser(RegisterUserRequest request);

}
