package de.tum.devopss26.userservice.controller;

import de.tum.devopss26.userservice.service.JwtService;
import de.tum.devopss26.userservice.service.UserAuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.api.UserAuthenticationApi;
import org.openapitools.model.LoginResponse;
import org.openapitools.model.PublicKeyResponse;
import org.openapitools.model.RegisterUserRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import java.util.Base64;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserAuthenticationController implements UserAuthenticationApi {

    private final UserAuthenticationService authService;
    private final HttpServletRequest request;
    private final JwtService jwtService;

    /**
     * Registers a new user account.
     *
     * @param createUserRequest the request containing the user registration details
     * @return {@code 201 Created} if registration is successful
     */
    @Override
    public ResponseEntity<Void> registerUser(RegisterUserRequest createUserRequest) {
        authService.registerUser(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Authenticates the user using HTTP Basic Authentication and returns a JWT token.
     *
     * @return a {@link LoginResponse} containing the JWT token
     */
    @Override
    public ResponseEntity<LoginResponse> loginUser() {
        String token = authService.loginUser();
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        return ResponseEntity.ok(response);
    }

    /**
     * Validates the JWT token provided in the Authorization header.
     *
     * @return {@code 200 OK} if the token is valid, {@code 401 Unauthorized} otherwise
     */
    @Override
    public ResponseEntity<Void> checkToken() {
        String authHeader = request.getHeader("Authorization");
        if (authService.checkToken(authHeader)) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    /**
     * Returns the RSA public key used for JWT verification.
     *
     * @return a {@link PublicKeyResponse} containing the Base64-encoded public key
     */
    @Override
    public ResponseEntity<PublicKeyResponse> publicKey() {
        PublicKeyResponse response = new PublicKeyResponse()
                .publicKey(Base64.getEncoder().encodeToString(jwtService.getPublicKey().getEncoded()));
        return ResponseEntity.ok(response);
    }
}
