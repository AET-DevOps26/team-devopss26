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
 * <b>Key loading strategy:</b>
 * <ol>
 *   <li><b>Config-based (preferred):</b> If both {@code jwt.private-key} and {@code jwt.public-key}
 *       environment variables / config properties are set to Base64-encoded DER values, they are
 *       decoded and used to construct the RSA key pair. This is the production path — operations
 *       teams pre-provision keys so all instances share the same pair, enabling cross-instance
 *       token validation without shared secrets.</li>
 *   <li><b>Fallback (local development):</b> When keys are absent, a fresh 2048-bit RSA key pair
 *       is generated at startup. This is convenient for testing but means tokens signed by one
 *       instance cannot be verified by another (each restart generates new keys).</li>
 * </ol>
 * </p>
 *
 * <p><b>Token format:</b>
 * Generated tokens carry {@code sub} = user ID (as string), {@code name} = username claim,
 * an issued-at timestamp, and an expiration timestamp.
 * </p>
 *
 * <p><b>EXPIRATION_MS = 24 hours:</b>
 * A 24-hour window balances user convenience (infrequent re-login) against the security risk
 * of a stolen token. For higher-security deployments this should be reduced and paired with
 * a refresh-token mechanism.
 * </p>
 *
 * <p><b>Signature algorithm — RS256:</b>
 * RS256 (RSA with SHA-256) is chosen over HS256 (HMAC) because the asymmetric nature allows
 * any downstream service holding only the public key to verify tokens without possessing the
 * signing secret. This is essential in a microservice architecture where multiple services
 * need to authenticate requests but must not all hold the private key.
 * </p>
 */
@Service
public final class JwtService {

    private static final long EXPIRATION_MS = 24 * 60 * 60 * 1000L;
    private final PrivateKey privateKey;
    @Getter
    private final PublicKey publicKey;

    /**
     * Constructs the service by attempting to load RSA keys from configuration first,
     * falling back to runtime generation.
     *
     * @param privateKeyBase64 Base64-encoded PKCS#8 private key (from {@code jwt.private-key}),
     *                         or empty to trigger fallback generation
     * @param publicKeyBase64  Base64-encoded X.509 public key (from {@code jwt.public-key}),
     *                         or empty to trigger fallback generation
     * @throws RuntimeException if config-based loading fails (malformed key data) or
     *                          if the RSA algorithm is not available in the JRE
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

    /**
     * Creates a signed JWT carrying the user's ID as subject and the username as a custom claim.
     *
     * @param userId   the user's primary key, embedded as the JWT {@code sub} claim
     * @param username the user's login name, embedded as a custom {@code name} claim
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
     * Extracts the username claim from the given JWT token.
     *
     * @param token the JWT token string
     * @return the username stored in the token's {@code name} claim
     */
    public String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("name", String.class));
    }

    /**
     * Extracts the expiration date from the given JWT token.
     *
     * @param token the JWT token string
     * @return the token's expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim from the JWT token using the given resolver function.
     *
     * @param <T>            the type of the claim value
     * @param token          the JWT token string
     * @param claimsResolver a function that picks a specific claim from the parsed payload
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
     * Validates the given JWT token by verifying its signature, expiration, and structure.
     * We catch both {@link JwtException} (signature failure, malformed token)
     * and {@link IllegalArgumentException} (null/empty token) uniformly to avoid leaking
     * the reason for rejection to callers — this prevents attackers from probing token structure.
     *
     * @param token the JWT token string to validate
     * @return {@code true} if the token is valid and not expired, {@code false} otherwise
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
