package de.tum.devopss26.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Value;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.function.Function;

@Service
public final class JwtService {

    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L;
    private final PrivateKey privateKey;
    @Getter
    private final PublicKey publicKey;

    public JwtService(
            @Value("${jwt.private-key:}") String privateKeyBase64,
            @Value("${jwt.public-key:}") String publicKeyBase64) {
        
        if (privateKeyBase64 != null && !privateKeyBase64.trim().isEmpty() &&
            publicKeyBase64 != null && !publicKeyBase64.trim().isEmpty()) {
            try {
                byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64.trim());
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                this.privateKey = keyFactory.generatePrivate(privateKeySpec);

                byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64.trim());
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
                this.publicKey = keyFactory.generatePublic(publicKeySpec);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load RSA key pair from configuration", e);
            }
        } else {
            // Fallback to dynamic generation (e.g. for local testing without env vars)
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(2048);
                KeyPair kp = kpg.generateKeyPair();
                this.privateKey = kp.getPrivate();
                this.publicKey = kp.getPublic();
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Failed to initialize RSA key generator", e);
            }
        }
    }

	public String generateToken(long userId, String username) {
        return Jwts.builder()
                .subject(Long.toString(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(privateKey, Jwts.SIG.RS256)
                .claim("name", username)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("name", String.class));
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        final String username = extractUsername(token);
        return username != null && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
