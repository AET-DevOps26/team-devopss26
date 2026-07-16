package de.tum.devopss26.shared.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

public final class JWTHelper {
	
	private JWTHelper() {
	    throw new IllegalAccessError("Illegal access of JWTHelper - No instantiation!");
	}
	
	/**
	 * Extracts JWT data from request attributes injected by {@code TokenValidationInterceptor}.
	 *
	 * @param request the current HTTP request containing {@code userId} and {@code jwtClaims} attributes
	 * @return a {@link JWTData} holding the user ID and parsed JWT claims
	 */
	public static JWTData extractFrom(HttpServletRequest request) {
		return new JWTData(
				Long.parseLong(request.getAttribute("userId").toString()),
				(Claims) request.getAttribute("jwtClaims")
		);
	}
	
}
