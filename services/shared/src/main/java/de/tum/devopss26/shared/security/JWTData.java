package de.tum.devopss26.shared.security;

import io.jsonwebtoken.Claims;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class JWTData {
	
	private final long userId;
	private final Claims claims;
	
}
