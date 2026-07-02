package de.tum.devopss26.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Component
public class TokenValidationInterceptor implements HandlerInterceptor {

	private static final Logger LOG = LoggerFactory.getLogger(TokenValidationInterceptor.class);
	private final RestClient restClient;
	private PublicKey publicKey;

	public TokenValidationInterceptor(@Value("${user-service.url:http://localhost:8001}") String userServiceUrl) {
		this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
	}

	private synchronized PublicKey getOrFetchPublicKey() {
		if (publicKey != null) {
			return publicKey;
		}
		try {
			String base64Key = restClient.get()
					.uri("/api/v1/users/auth/public-key")
					.retrieve()
					.body(String.class);

			if (base64Key != null) {
				byte[] keyBytes = Base64.getDecoder().decode(base64Key.trim());
				X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
				KeyFactory kf = KeyFactory.getInstance("RSA");
				this.publicKey = kf.generatePublic(spec);
			}
		} catch (Exception e) {
			// Self-healing: if fetch fails, print error and we'll retry on next request
			LOG.atError().setCause(e).log("Failed to fetch JWT public key from user-service");
		}
		return publicKey;
	}

	@Override
	public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
		if (!(handler instanceof HandlerMethod handlerMethod)) {
			return true;
		}

		// Check if the annotation is present on the method or class
		boolean hasAnnotation = handlerMethod.hasMethodAnnotation(RequireTokenValidation.class) ||
		                        handlerMethod.getBeanType().isAnnotationPresent(RequireTokenValidation.class);

		if (!hasAnnotation) {
			return true;
		}

		String authHeader = request.getHeader("Authorization");
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization header");
			return false;
		}

		String token = authHeader.substring(7);

		PublicKey activePublicKey = getOrFetchPublicKey();
		if (activePublicKey == null) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Token validation service unavailable (public key not fetched)");
			return false;
		}

		try {
			// Validate signature, expiration date, and malformation using RSA public key
			Claims claims = Jwts.parser()
					.verifyWith(activePublicKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();

			// Inject attributes
			request.setAttribute("username", claims.getSubject());
			request.setAttribute("jwtClaims", claims);

			return true;
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
			return false;
		}
	}
}
