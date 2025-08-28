package com.devStudy.chatapp.auth.security.loginVerificationCode;

import com.devStudy.chatapp.auth.service.Implementation.UserService;
import com.devStudy.chatapp.auth.service.Implementation.VerificationCodeService;
import io.jsonwebtoken.lang.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class VerificationCodeAuthenticationProvider implements AuthenticationProvider {
    private static final Logger logger = LoggerFactory.getLogger(VerificationCodeAuthenticationProvider.class);

    private final VerificationCodeService verificationCodeService;
    private final UserService userService;

    @Value("${chatroomApp.MAX_FAILED_ATTEMPTS}")
    private int MAX_FAILED_ATTEMPTS;

    @Autowired
    public VerificationCodeAuthenticationProvider(VerificationCodeService verificationCodeService, UserService userService) {
        this.verificationCodeService = verificationCodeService;
        this.userService = userService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(VerificationCodeAuthenticationToken.class, authentication,
                "Only VerificationCodeAuthenticationToken is supported");

        String userEmail = authentication.getPrincipal().toString();
        String verificationCode = authentication.getCredentials().toString();

        if(verificationCodeService.validateCode(userEmail, verificationCode)){
            UserDetails userDetails = userService.loadUserByUsername(userEmail);
            VerificationCodeAuthenticationToken authenticatedToken =
                    VerificationCodeAuthenticationToken.authenticated(userDetails, userDetails.getAuthorities());
            authenticatedToken.setDetails(authentication.getDetails());
            logger.info("L'utilisateur {} a été authentifié avec succès", userEmail);
            return authenticatedToken;
        }
        throw FailedLoginAttemptsException(userEmail);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(VerificationCodeAuthenticationToken.class);
    }

    private BadCredentialsException FailedLoginAttemptsException(String userEmail) {
        int attempts = verificationCodeService.incrementLoginAttempts(userEmail);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            verificationCodeService.invalideteCode(userEmail);
            logger.info("Trops de tentatives malveillantes, le code de vérification est devenu invalide, veuillez le redemander");
            return new BadCredentialsException("Trops de tentatives malveillantes, le code de vérification est devenu invalide, veuillez le redemander");
        }
        logger.info("Code incorrect. Plus que {} tentatives avant l'invalidation du code", MAX_FAILED_ATTEMPTS - attempts);
        return new BadCredentialsException("Code incorrect. Plus que " + (MAX_FAILED_ATTEMPTS - attempts) + " tentatives avant l'invalidation du code");
    }
}