package de.tum.devopss26.shared.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for extracting JWT data from authenticated HTTP requests.
 * <p>
 * This class is not meant to be instantiated.
 * </p>
 */
public final class JWTHelper {
	
	private JWTHelper() {
	    throw new IllegalAccessError("Illegal access of JWTHelper - No instantiation!");
	}
	
	/**
	 * Extracts the {@link JWTData} (userId and claims) from the request attributes
	 * that were previously injected by {@link TokenValidationInterceptor}.
	 *
	 * @param request the HTTP request containing the JWT attributes
	 * @return the extracted JWT data
	 */
	public static JWTData extractFrom(HttpServletRequest request) {
		return new JWTData(
				Long.parseLong(request.getAttribute("userId").toString()),
				(Claims) request.getAttribute("jwtClaims")
		);
	}
	
}
