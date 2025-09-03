package com.devStudy.chatapp.gateway.service.Implementation;

import com.devStudy.chatapp.gateway.service.Interface.IJwtTokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ServerWebExchange;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class JwtTokenService implements IJwtTokenService {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenService.class);

    @Value("${chatroomApp.JWT_TOKEN_COOKIE_NAME}")
    private String JWT_TOKEN_COOKIE_NAME;
    
    @Value("${chatroomApp.jwt.secret}")
    private String secretKey;
    
    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Check if the token is valid and not expired, then extract the email (subject)
     */
    @Override
    public String validateTokenAndGetEmail(String token) {
        return executeWithExceptionHandling(
                () -> isNotExpired(token) ? getSubject(token) : null,
                "Failed to extract email from token"
        );
    }

    /**
     * Obtain the expiration date from the token
     */
    @Override
    public Date getExpirationDate(String token) {
        return executeWithExceptionHandling(
                () -> getClaimFromToken(token, Claims::getExpiration),
                "Failed to get expiration date from token"
        );
    }

    /**
     * Obtain the JWT token from the request cookies
     */
    @Override
    public String getTokenFromRequest(ServerWebExchange exchange) {
        return exchange.getRequest().getCookies()
                .getFirst(JWT_TOKEN_COOKIE_NAME)
                !=null ? Objects.requireNonNull(exchange.getRequest().getCookies()
                .getFirst(JWT_TOKEN_COOKIE_NAME)).getValue() : null;
    }

    /**
     * Exception handling wrapper
     */
    private <T> T executeWithExceptionHandling(Supplier<T> operation, String errorMessage) {
        try {
            return operation.get();
        } catch (ExpiredJwtException e) {
            LOGGER.warn("JWT token has expired: {}", e.getMessage());
        } catch (SignatureException e) {
            LOGGER.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            LOGGER.error("Malformed JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            LOGGER.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            LOGGER.error("JWT claims string is empty: {}", e.getMessage());
        } catch (JwtException e) {
            LOGGER.error("JWT processing error: {}", e.getMessage());
        } catch (Exception e) {
            LOGGER.error("{}: {}", errorMessage, e.getMessage());
        }
        return null;
    }

    /**
     * Obtain Claims from the token using the provided resolver function
     */
    private <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claimsResolver.apply(claims);
    }

    private String getSubject(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    private boolean isNotExpired(String token) {
        Date expiration = getExpirationDate(token);
        return expiration != null && !expiration.before(Date.from(Instant.now()));
    }
}