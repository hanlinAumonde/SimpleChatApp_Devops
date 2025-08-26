package com.devStudy.chatapp.auth.service;

import io.jsonwebtoken.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.crypto.SecretKey;

import static com.devStudy.chatapp.auth.utils.ConstantValues.JWT_TOKEN_COOKIE_NAME;
import static com.devStudy.chatapp.auth.utils.ConstantValues.TOKEN_FLAG_LOGIN;

@Service
public class JwtTokenService {
	private static final Logger LOGGER = LoggerFactory.getLogger(JwtTokenService.class);
	
	@Value("${chatroomApp.jwt.secret}")
	private String secretKey;
	
	@Value("${chatroomApp.jwt.resetPwdTokenExpirationTime}")
	private Long resetPwdTokenExpirationTime;

	@Value("${chatroomApp.jwt.loginTokenExpirationTime}")
	private Long loginTokenExpirationTime;
    
    private SecretKey getSecretKey() {
    	byte[] keyBytes = Decoders.BASE64.decode(secretKey);
    	return Keys.hmacShaKeyFor(keyBytes);
    }

	public String generateJwtToken(String email, String tokenFlag) {
		Instant now = Instant.now();
		Instant expiration = now.plusMillis(60 * 1000
							* (Objects.equals(tokenFlag, TOKEN_FLAG_LOGIN) ? loginTokenExpirationTime : resetPwdTokenExpirationTime)
						);

        return Jwts.builder()
                .subject(email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSecretKey())
                .compact();
	}

    /**
     * 验证令牌是否有效
     */
    public boolean validateToken(String token) {
        return executeWithExceptionHandling(
                () -> isNotExpired(token),
                "Failed to validate token",
                false
        );
    }

    /**
     * 验证令牌并获取邮箱
     */
    public String validateTokenAndGetEmail(String token) {
        return executeWithExceptionHandling(
                () -> isNotExpired(token) ? getSubject(token) : null,
                "Failed to extract email from token",
                null
        );
    }

    /**
     * 获取令牌过期时间
     */
    public Date getExpirationDate(String token) {
        return executeWithExceptionHandling(
                () -> getClaimFromToken(token, Claims::getExpiration),
                "Failed to get expiration date from token",
                null
        );
    }

    /**
     * 从Cookie中获取JWT令牌
     */
    public String getTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> JWT_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    /**
     * 统一的异常处理方法
     */
    private <T> T executeWithExceptionHandling(Supplier<T> operation, String errorMessage, T defaultValue) {
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
        return defaultValue;
    }

    /**
     * 从令牌中获取Claims
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