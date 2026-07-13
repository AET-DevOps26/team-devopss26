package de.tum.devopss26.userservice;

import de.tum.devopss26.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openapitools.model.LoginResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
public class UserAuthenticationIT extends AbstractIntegrationTest {

    private MockMvc mockMvc;
    private UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public void setMockMvc(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void testRegisterUserAndLogin() throws Exception {
        String registerJson = """
                {
                  "username": "testuser",
                  "password": "securepassword"
                }
                """;

        mockMvc.perform(post("/api/v1/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated());

        assertThat(userRepository.existsByUsername("testuser")).isTrue();

        mockMvc.perform(post("/api/v1/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isConflict());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/users/auth/login")
                        .with(httpBasic("testuser", "securepassword")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String tokenResponseStr = loginResult.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(tokenResponseStr, LoginResponse.class);
        String token = loginResponse.getToken();
        assertThat(token).isNotEmpty();

        mockMvc.perform(post("/api/v1/users/auth/login")
                        .with(httpBasic("testuser", "wrongpassword")))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/auth/check-token")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/auth/check-token")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/users/auth/check-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testGetPublicKey() throws Exception {
        mockMvc.perform(get("/api/v1/users/auth/public-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicKey").exists());
    }
}
