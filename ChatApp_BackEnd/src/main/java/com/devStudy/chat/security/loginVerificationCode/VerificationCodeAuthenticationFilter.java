package com.devStudy.chat.security.loginVerificationCode;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import static com.devStudy.chat.service.utils.ConstantValues.CREDENTIALS_PARAMETER;
import static com.devStudy.chat.service.utils.ConstantValues.PRINCIPAL_PARAMETER;

public class VerificationCodeAuthenticationFilter extends AbstractAuthenticationProcessingFilter {

    private boolean postOnly = true;

    public VerificationCodeAuthenticationFilter(String VERIFICATION_CODE_LOGIN_ENDPOINT) {
        super(new AntPathRequestMatcher(VERIFICATION_CODE_LOGIN_ENDPOINT, "POST"));
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, ServletException {
        if(postOnly && !request.getMethod().equals("POST")) {
            throw new ServletException("Authentication method not supported: " + request.getMethod());
        }

        String principal = obtainPrincipal(request);
        String credentials = obtainCredentials(request);

        VerificationCodeAuthenticationToken authRequest =
                VerificationCodeAuthenticationToken.unauthenticated(principal, credentials);
        setDetails(request, authRequest);

        return this.getAuthenticationManager().authenticate(authRequest);

    }

    protected String obtainPrincipal(HttpServletRequest request) {
        String principal = request.getParameter(PRINCIPAL_PARAMETER);
        return principal != null ? principal.trim() : "";
    }

    protected String obtainCredentials(HttpServletRequest request) {
        String credentials = request.getParameter(CREDENTIALS_PARAMETER);
        return credentials != null ? credentials : "";
    }

    protected void setDetails(HttpServletRequest request, VerificationCodeAuthenticationToken authRequest) {
        authRequest.setDetails(authenticationDetailsSource.buildDetails(request));
    }

    public void setPostOnly(boolean postOnly) {
        this.postOnly = postOnly;
    }
}
