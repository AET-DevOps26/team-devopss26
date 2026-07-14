package de.tum.devopss26.userservice;

import de.tum.devopss26.shared.it.AbstractIntegrationTest;
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
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public void setMockMvc(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Autowired
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Autowired(required = false)
    public void setObjectMapper(ObjectMapper objectMapper) {
        if (objectMapper != null) {
            this.objectMapper = objectMapper;
        }
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    private void registerUser(String username, String password) throws Exception {
        String registerJson = String.format("""
                {
                  "username": "%s",
                  "password": "%s"
                }
                """, username, password);

        mockMvc.perform(post("/api/v1/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated());
    }

    private String loginUser(String username, String password) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/users/auth/login")
                        .with(httpBasic(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String tokenResponseStr = loginResult.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(tokenResponseStr, LoginResponse.class);
        return loginResponse.getToken();
    }

    @Test
    void testRegisterUser() throws Exception {
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
    }

    @Test
    void testRegisterDuplicateUser() throws Exception {
        registerUser("testuser", "securepassword");

        String registerJson = """
                {
                  "username": "testuser",
                  "password": "securepassword"
                }
                """;

        mockMvc.perform(post("/api/v1/users/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isConflict());
    }

    @Test
    void testLoginSuccess() throws Exception {
        registerUser("testuser", "securepassword");

        MvcResult loginResult = mockMvc.perform(post("/api/v1/users/auth/login")
                        .with(httpBasic("testuser", "securepassword")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andReturn();

        String tokenResponseStr = loginResult.getResponse().getContentAsString();
        LoginResponse loginResponse = objectMapper.readValue(tokenResponseStr, LoginResponse.class);
        String token = loginResponse.getToken();
        assertThat(token).isNotEmpty();
    }

    @Test
    void testLoginWrongPassword() throws Exception {
        registerUser("testuser", "securepassword");

        mockMvc.perform(post("/api/v1/users/auth/login")
                        .with(httpBasic("testuser", "wrongpassword")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCheckTokenSuccess() throws Exception {
        registerUser("testuser", "securepassword");
        String token = loginUser("testuser", "securepassword");

        mockMvc.perform(get("/api/v1/users/auth/check-token")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void testCheckTokenInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/users/auth/check-token")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCheckTokenMissing() throws Exception {
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
