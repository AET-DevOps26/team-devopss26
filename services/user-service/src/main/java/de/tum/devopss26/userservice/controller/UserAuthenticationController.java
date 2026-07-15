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

/**
 * Implements the OpenAPI-generated {@link UserAuthenticationApi} interface, acting as
 * a thin HTTP façade that delegates all business logic to {@link UserAuthenticationService}.
 * This separation keeps generated interface code decoupled from implementation details.
 *
 * <p><b>Endpoints:</b>
 * <ul>
 *   <li>{@code POST /register} — account creation</li>
 *   <li>{@code GET /login} — password-based login via Basic auth</li>
 *   <li>{@code GET /check-token} — JWT validity check</li>
 *   <li>{@code GET /public-key} — RSA public key distribution to clients</li>
 * </ul>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class UserAuthenticationController implements UserAuthenticationApi {

    private final UserAuthenticationService authService;
    private final HttpServletRequest request;
    private final JwtService jwtService;

    @Override
    public ResponseEntity<Void> registerUser(RegisterUserRequest createUserRequest) {
        authService.registerUser(createUserRequest);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<LoginResponse> loginUser() {
        String token = authService.loginUser();
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        return ResponseEntity.ok(response);
    }

    /**
     * We read the header from the injected {@code request} rather than from a controller
     * parameter to keep the generated OpenAPI interface signature unchanged.
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
     * Exposes the RSA public key (Base64-encoded X.509 format) so external services
     * can verify tokens issued by this service without sharing private key material.
     * <p>
     * This is necessary because tokens are signed with RS256 (asymmetric), meaning
     * any consumer that possesses the public key can validate token authenticity.
     * </p>
     */
    @Override
    public ResponseEntity<PublicKeyResponse> publicKey() {
        PublicKeyResponse response = new PublicKeyResponse()
                .publicKey(Base64.getEncoder().encodeToString(jwtService.getPublicKey().getEncoded()));
        return ResponseEntity.ok(response);
    }
}
