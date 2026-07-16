package de.tum.devopss26.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * <b>Caching strategy:</b> The RSA public key is fetched lazily from the user-service and cached
 * in-memory. If signature verification fails ({@link io.jsonwebtoken.security.SignatureException})
 * and at least 30&nbsp;s have elapsed since the last fetch, the cache is cleared and the key is
 * refetched — this self-heals transparently after key rotation or user-service restarts.
 * <p>On success, {@code userId} (the token subject) and {@code jwtClaims} are injected as request
 * attributes so downstream handlers can access them without re-parsing the token.
 */
@Component
public class TokenValidationInterceptor implements HandlerInterceptor {

	private static final Logger log = LoggerFactory.getLogger(TokenValidationInterceptor.class);

	private final RestClient restClient;
	private PublicKey publicKey;
	private Instant lastFetchTime = Instant.MIN;
	private static final Duration MIN_REFETCH_INTERVAL = Duration.ofSeconds(30);

	/**
	 * The built-in {@link RestClient} uses a 2-second connect/read timeout so a stalled
	 * user-service does not block request threads.
	 */
	public TokenValidationInterceptor(@Value("${user-service.url:http://localhost:8001}") String userServiceUrl) {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(Duration.ofSeconds(2));
		requestFactory.setReadTimeout(Duration.ofSeconds(2));
		this.restClient = RestClient.builder()
				.baseUrl(userServiceUrl)
				.requestFactory(requestFactory)
				.build();
	}

	private synchronized PublicKey getOrFetchPublicKey() {
		if (publicKey != null) {
			return publicKey;
		}
		try {
			Map<String, String> response = restClient.get()
					.uri("/api/v1/users/auth/public-key")
					.retrieve()
					.body(new ParameterizedTypeReference<>() {
                    });

			String base64Key = response != null ? response.get("publicKey") : null;

			if (base64Key != null) {
				byte[] keyBytes = Base64.getDecoder().decode(base64Key);
				X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
				KeyFactory kf = KeyFactory.getInstance("RSA");
				this.publicKey = kf.generatePublic(spec);
				this.lastFetchTime = Instant.now();
			}
		} catch (Exception e) {
			// Self-healing: if fetch fails, print error and we'll retry on next request
			log.atError().setCause(e).log("Failed to fetch JWT public key from user-service");
		}
		return publicKey;
	}

	private synchronized boolean tryClearCachedPublicKeyForRefetch() {
		if (Duration.between(lastFetchTime, Instant.now()).compareTo(MIN_REFETCH_INTERVAL) > 0) {
			this.publicKey = null;
			return true;
		}
		return false;
	}

	private Claims validateToken(String token, PublicKey key) {
		return Jwts.parser()
				.verifyWith(key)
				.build()
				.parseSignedClaims(token)
				.getPayload();
	}

	/**
	 * Intercepts incoming requests to validate JWT tokens on endpoints annotated with
	 * {@link RequireTokenValidation}. The validation flow is documented in the class-level
	 * JavaDoc.
	 *
	 * @param request  the incoming HTTP request
	 * @param response the outgoing HTTP response
	 * @param handler  the handler object chosen for this request
	 * @return {@code true} if the request should proceed, {@code false} if a response was sent
	 */
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
			Claims claims = validateToken(token, activePublicKey);

			// Inject attributes
			request.setAttribute("userId", claims.getSubject());
			request.setAttribute("jwtClaims", claims);

			return true;
		} catch (io.jsonwebtoken.security.SignatureException e) {
			// Signature failed. The public key might have changed (e.g. user-service restarted).
			// Try to refetch the key if we haven't done so recently.
			if (tryClearCachedPublicKeyForRefetch()) {
				log.info("JWT signature verification failed. Attempting to refetch public key...");
				PublicKey newPublicKey = getOrFetchPublicKey();
				if (newPublicKey != null && newPublicKey != activePublicKey) {
					try {
						Claims claims = validateToken(token, newPublicKey);
						request.setAttribute("userId", claims.getSubject());
						request.setAttribute("jwtClaims", claims);
						return true;
					} catch (Exception retryEx) {
						// Fall through to 401
					}
				}
			}
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
			return false;
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT token");
			return false;
		}
	}
}
