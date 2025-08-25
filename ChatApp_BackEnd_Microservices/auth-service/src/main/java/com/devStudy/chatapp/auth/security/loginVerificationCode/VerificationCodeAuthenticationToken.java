package com.devStudy.chatapp.auth.security.loginVerificationCode;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class VerificationCodeAuthenticationToken extends AbstractAuthenticationToken {

    private final Object principal;
    private String verificationCode;

    public VerificationCodeAuthenticationToken(Collection<? extends GrantedAuthority> authorities, String verificationCode, Object principal) {
        super(authorities);
        this.verificationCode = verificationCode;
        this.principal = principal;
        super.setAuthenticated(true);
    }

    public VerificationCodeAuthenticationToken(String verificationCode, Object principal) {
        super(null);
        this.verificationCode = verificationCode;
        this.principal = principal;
        setAuthenticated(false);
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException("Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        super.setAuthenticated(false);
    }

    @Override
    public Object getCredentials() {
        return verificationCode;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public void eraseCredentials() {
        super.eraseCredentials();
        this.verificationCode = null;
    }

    public static VerificationCodeAuthenticationToken authenticated(Object principal, Collection<? extends GrantedAuthority> authorities) {
        return new VerificationCodeAuthenticationToken(authorities, null, principal);
    }

    public static VerificationCodeAuthenticationToken unauthenticated(Object principal, String verificationCode) {
        return new VerificationCodeAuthenticationToken(verificationCode, principal);
    }
}