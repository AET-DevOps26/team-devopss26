package de.tum.devopss26.userservice.controller;

import de.tum.devopss26.userservice.exception.UserAlreadyExistsException;
import de.tum.devopss26.userservice.service.UserAuthenticationService;
import org.junit.jupiter.api.Test;
import org.openapitools.model.RegisterUserRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers.openApi;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAuthenticationController.class)
class UserAuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserAuthenticationService authService;

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
}
