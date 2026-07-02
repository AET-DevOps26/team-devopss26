package de.tum.devopss26.shared.security;

import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.method.HandlerMethod;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TokenValidationInterceptorTest {

    private TokenValidationInterceptor interceptor;
    private RestClient restClient;
    private RestClient.ResponseSpec responseSpec;

    private KeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception {
        interceptor = new TokenValidationInterceptor("http://localhost:8001");

        restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec<?> requestHeadersUriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec<?> requestHeadersSpec = mock(RestClient.RequestHeadersSpec.class);
        responseSpec = mock(RestClient.ResponseSpec.class);

        doReturn(requestHeadersUriSpec).when(restClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(anyString());
        doReturn(responseSpec).when(requestHeadersSpec).retrieve();

        ReflectionTestUtils.setField(interceptor, "restClient", restClient);

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.generateKeyPair();
    }

    @Test
    void preHandle_NoAnnotation_ReturnsTrue() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        when(handlerMethod.hasMethodAnnotation(RequireTokenValidation.class)).thenReturn(false);
        when(handlerMethod.getBeanType()).thenAnswer(inv -> Object.class);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertTrue(result);
        verifyNoInteractions(restClient);
    }

    @Test
    void preHandle_NotHandlerMethod_ReturnsTrue() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Object handler = new Object();

        boolean result = interceptor.preHandle(request, response, handler);

        assertTrue(result);
        verifyNoInteractions(restClient);
    }

    @Test
    void preHandle_MissingAuthorizationHeader_ReturnsFalseAnd401() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        when(handlerMethod.hasMethodAnnotation(RequireTokenValidation.class)).thenReturn(true);
        when(handlerMethod.getBeanType()).thenAnswer(inv -> Object.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
    }

    @Test
    void preHandle_InvalidHeaderFormat_ReturnsFalseAnd401() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        when(handlerMethod.hasMethodAnnotation(RequireTokenValidation.class)).thenReturn(true);
        when(handlerMethod.getBeanType()).thenAnswer(inv -> Object.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer"); // no token content

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
    }

    @Test
    void preHandle_PublicKeyFetchFails_ReturnsFalseAnd500() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        when(handlerMethod.hasMethodAnnotation(RequireTokenValidation.class)).thenReturn(true);
        when(handlerMethod.getBeanType()).thenAnswer(inv -> Object.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer sampletoken");
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("Connection refused"));

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Token validation service unavailable (public key not fetched)");
    }

    @Test
    void preHandle_InvalidTokenSignature_ReturnsFalseAnd401() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        // Generate token signed by a different key
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair anotherKeyPair = keyGen.generateKeyPair();
        String token = Jwts.builder()
                .subject("testuser")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(anotherKeyPair.getPrivate())
                .compact();

        when(handlerMethod.hasMethodAnnotation(RequireTokenValidation.class)).thenReturn(true);
        when(handlerMethod.getBeanType()).thenAnswer(inv -> Object.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        String base64PublicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        when(responseSpec.body(String.class)).thenReturn(base64PublicKey);

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertFalse(result);
        verify(response).sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
    }

    @Test
    void preHandle_ValidToken_ReturnsTrueAndSetsAttributes() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HandlerMethod handlerMethod = mock(HandlerMethod.class);

        String token = Jwts.builder()
                .subject("testuser")
                .expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(keyPair.getPrivate())
                .compact();

        when(handlerMethod.hasMethodAnnotation(RequireTokenValidation.class)).thenReturn(true);
        when(handlerMethod.getBeanType()).thenAnswer(inv -> Object.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);

        String base64PublicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        when(responseSpec.body(String.class)).thenReturn(base64PublicKey);

        // First invocation: fetches public key
        boolean result1 = interceptor.preHandle(request, response, handlerMethod);
        assertTrue(result1);
        verify(request).setAttribute(eq("username"), eq("testuser"));
        verify(request).setAttribute(eq("jwtClaims"), any());

        // Second invocation: uses cached public key (verify restClient get was only called once)
        boolean result2 = interceptor.preHandle(request, response, handlerMethod);
        assertTrue(result2);
        verify(restClient, times(1)).get();
    }
}
