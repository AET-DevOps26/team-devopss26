package de.tum.devopss26.shared.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;

public final class JWTHelper {
	
	private JWTHelper() {
	    throw new IllegalAccessError("Illegal access of JWTHelper - No instantiation!");
	}
	
	public static JWTData extractFrom(HttpServletRequest request) {
		return new JWTData(
				Long.parseLong(request.getAttribute("userId").toString()),
				(Claims) request.getAttribute("jwtClaims")
		);
	}
	
}
