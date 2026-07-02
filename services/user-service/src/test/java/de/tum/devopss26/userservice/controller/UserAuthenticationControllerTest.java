package de.tum.devopss26.userservice.controller;

import de.tum.devopss26.userservice.exception.UserAlreadyExistsException;
import de.tum.devopss26.userservice.service.UserAuthenticationService;
import org.junit.jupiter.api.Test;
import org.openapitools.model.RegisterUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.tum.devopss26.userservice.config.SecurityConfig;
import org.springframework.context.annotation.Import;

import java.util.Collections;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAuthenticationController.class)
@Import(SecurityConfig.class)
class UserAuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAuthenticationService authService;

    @MockitoBean
    private de.tum.devopss26.userservice.service.JwtService jwtService;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    @Test
    void registerUser_CREATED() throws Exception {
        doNothing().when(authService).registerUser(any(RegisterUserRequest.class));

        String registerRequestJson = """
                {
                  "username": "testuser",
                  "password": "securepassword"
                }
                """;

        mockMvc.perform(post("/api/v1/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andExpect(status().isCreated())
                .andExpect(openApi().isValid("user-service.yaml"));
    }

    @Test
    void registerUser_CONFLICT() throws Exception {
        doThrow(new UserAlreadyExistsException("testuser")).when(authService).registerUser(any(RegisterUserRequest.class));

        String registerRequestJson = """
                {
                  "username": "testuser",
                  "password": "securepassword"
                }
                """;

        mockMvc.perform(post("/api/v1/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andExpect(status().isConflict())
                .andExpect(openApi().isValid("user-service.yaml"));
    }

    @Test
    void registerUser_INTERNAL_SERVER_ERRROR() throws Exception {
        doThrow(new RuntimeException("forced error")).when(authService).registerUser(any(RegisterUserRequest.class));

        String registerRequestJson = """
                {
                  "username": "testuser",
                  "password": "securepassword"
                }
                """;

        mockMvc.perform(post("/api/v1/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerRequestJson))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("user-service.yaml"));
    }

    @Test
    void loginUser_SUCCESS() throws Exception {
        String rawPassword = "securepassword";
        String passwordHash = passwordEncoder.encode(rawPassword);

        var mockUser = User
                .withUsername("testuser")
                .password(passwordHash)
                .authorities(Collections.emptyList())
                .build();

        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(mockUser);
        when(authService.loginUser()).thenReturn("mocked-jwt-token");

        mockMvc.perform(post("/api/v1/users/auth/login")
                        .with(httpBasic("testuser", rawPassword)))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("user-service.yaml"));
    }

    @Test
    void loginUser_UNAUTHORIZED_wrongPassword() throws Exception {
        String rawPassword = "securepassword";
        String passwordHash = passwordEncoder.encode(rawPassword);

        var mockUser = User
                .withUsername("testuser")
                .password(passwordHash)
                .authorities(Collections.emptyList())
                .build();

        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(mockUser);

        mockMvc.perform(post("/api/v1/users/auth/login")
                        .with(httpBasic("testuser", "wrongpassword")))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("user-service.yaml"));
    }

    @Test
    void loginUser_UNAUTHORIZED_unknownUser() throws Exception {
        when(userDetailsService.loadUserByUsername("unknownuser"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        mockMvc.perform(post("/api/v1/users/auth/login")
                        .with(httpBasic("unknownuser", "somepassword")))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("user-service.yaml"));
    }

    @Test
    void loginUser_UNAUTHORIZED_noCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/users/auth/login"))
                .andExpect(status().isUnauthorized())
                .andExpect(openApi().isValid("user-service.yaml"));
    }

    @Test
    void loginUser_INTERNAL_SERVER_ERROR() throws Exception {
        String rawPassword = "testpassword";
        String passwordHash = passwordEncoder.encode(rawPassword);

        var mockUser = User
                .withUsername("testuser")
                .password(passwordHash)
                .authorities(Collections.emptyList())
                .build();

        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(mockUser);
        doThrow(new RuntimeException("forced error")).when(authService).loginUser();

        mockMvc.perform(post("/api/v1/users/auth/login")
                        .with(httpBasic("testuser", rawPassword)))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("user-service.yaml"));
    }

    @Test
    void checkToken_SUCCESS() throws Exception {
        var mockUser = User
                .withUsername("testuser")
                .password("password")
                .authorities(Collections.emptyList())
                .build();

        when(jwtService.extractUsername("valid-token")).thenReturn("testuser");
        when(jwtService.isTokenValid("valid-token", "testuser")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(mockUser);
        when(authService.checkToken("Bearer valid-token")).thenReturn(true);

        mockMvc.perform(get("/api/v1/users/auth/check-token")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("user-service.yaml"));
    }

    @Test
    void checkToken_NOT_ACCEPTABLE_invalidToken() throws Exception {
        when(jwtService.extractUsername("invalid-token")).thenThrow(new RuntimeException("invalid token"));
        when(authService.checkToken("Bearer invalid-token")).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/auth/check-token")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isNotAcceptable())
                .andExpect(openApi().isValid("user-service.yaml"));
    }

    @Test
    void checkToken_NOT_ACCEPTABLE_missingHeader() throws Exception {
        when(authService.checkToken(null)).thenReturn(false);

        mockMvc.perform(get("/api/v1/users/auth/check-token"))
                .andExpect(status().isNotAcceptable())
                .andExpect(openApi().isValid("user-service.yaml"));
    }

    @Test
    void checkToken_INTERNAL_SERVER_ERROR() throws Exception {
        doThrow(new RuntimeException("forced error")).when(authService).checkToken(anyString());

        mockMvc.perform(get("/api/v1/users/auth/check-token")
                        .header("Authorization", "Bearer ignored-token"))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("user-service.yaml"));
    }

    @Test
    void publicKey_SUCCESS() throws Exception {
        java.security.PublicKey mockPublicKey = mock(java.security.PublicKey.class);
        when(mockPublicKey.getEncoded()).thenReturn(new byte[]{1, 2, 3});
        when(jwtService.getPublicKey()).thenReturn(mockPublicKey);

        mockMvc.perform(get("/api/v1/users/auth/public-key"))
                .andExpect(status().isOk())
                .andExpect(openApi().isValid("user-service.yaml"));
    }

    @Test
    void publicKey_INTERNAL_SERVER_ERROR() throws Exception {
        doThrow(new RuntimeException("forced error")).when(jwtService).getPublicKey();

        mockMvc.perform(get("/api/v1/users/auth/public-key"))
                .andExpect(status().isInternalServerError())
                .andExpect(openApi().isValid("user-service.yaml"));
    }
}
