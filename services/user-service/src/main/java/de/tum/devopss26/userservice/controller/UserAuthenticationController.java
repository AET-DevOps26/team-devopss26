package de.tum.devopss26.userservice.controller;

import de.tum.devopss26.userservice.exception.UserAlreadyExistsException;
import de.tum.devopss26.userservice.service.UserAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.openapitools.api.UserAuthenticationApi;
import org.openapitools.model.LoginResponse;
import org.openapitools.model.RegisterUserRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class UserAuthenticationController implements UserAuthenticationApi {

	private static final Logger LOG = LoggerFactory.getLogger(UserAuthenticationController.class);
	private final UserAuthenticationService authService;
	private final HttpServletRequest request;

	@Override
	public ResponseEntity<Void> registerUser(RegisterUserRequest createUserRequest) {
		try {
			authService.registerUser(createUserRequest);
		} catch (UserAlreadyExistsException e) {
			LOG.atError().setCause(e).log();
			return ResponseEntity.status(HttpStatus.CONFLICT).body(null);
		} catch (Exception e) {
			LOG.atError().setCause(e).log();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
		}
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@Override
	public ResponseEntity<LoginResponse> loginUser() {
		try {
			String token = authService.loginUser();
			LoginResponse response = new LoginResponse();
			response.setToken(token);
			return ResponseEntity.ok(response);
		} catch (Exception e) {
			LOG.atError().setCause(e).log();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@Override
	public ResponseEntity<Void> checkToken() {
		try {
			String authHeader = request.getHeader("Authorization");
			if (authService.checkToken(authHeader)) {
				return ResponseEntity.ok().build();
			}
			return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
		} catch (Exception e) {
			LOG.atWarn().log("Token validation failed: {}", e.getMessage());
			return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
		}
	}
}
