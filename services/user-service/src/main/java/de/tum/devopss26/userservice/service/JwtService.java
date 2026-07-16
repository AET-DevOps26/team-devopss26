package de.tum.devopss26.userservice.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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

/**
 * Service for handling JWT token operations including generation, validation,
 * and claim extraction. Uses RSA-256 signing with configurable or auto-generated keys.
 */
@Service
public final class JwtService {

    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L;
    private final PrivateKey privateKey;
    @Getter
    private final PublicKey publicKey;

    /**
     * Constructs a JwtService with RSA key configuration.
     * If Base64-encoded private and public keys are provided via configuration properties
     * ({@code jwt.private-key} and {@code jwt.public-key}), they are decoded and used.
     * Otherwise, a new 2048-bit RSA key pair is generated dynamically (fallback for local testing).
     *
     * @param privateKeyBase64 the Base64-encoded PKCS#8 private key (may be empty for auto-generation)
     * @param publicKeyBase64  the Base64-encoded X.509 public key (may be empty for auto-generation)
     * @throws RuntimeException if key loading or generation fails
     */
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

    /**
     * Generates a JWT token for the given user.
     * The token is signed with the RSA private key and expires after 24 hours.
     *
     * @param userId   the user's ID (stored as the token subject)
     * @param username the user's username (stored as a custom claim "name")
     * @return the signed JWT token string
     */
	public String generateToken(long userId, String username) {
        return Jwts.builder()
                .subject(Long.toString(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(privateKey, Jwts.SIG.RS256)
                .claim("name", username)
                .compact();
    }

    /**
     * Extracts the username ("name" claim) from the given JWT token.
     *
     * @param token the JWT token string
     * @return the username stored in the token
     */
    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("name", String.class));
    }

    /**
     * Extracts the expiration date from the given JWT token.
     *
     * @param token the JWT token string
     * @return the expiration {@link Date}
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from the JWT token using the provided resolver function.
     *
     * @param <T>            the type of the claim
     * @param token          the JWT token string
     * @param claimsResolver a function to extract the desired claim from the parsed {@link Claims}
     * @return the extracted claim value
     */
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

    /**
     * Validates whether the given JWT token is valid (not expired and has a valid username claim).
     *
     * @param token the JWT token string
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    public boolean isTokenValid(String token) {
        try {
            final String username = extractUsername(token);
            return username != null && !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
}
