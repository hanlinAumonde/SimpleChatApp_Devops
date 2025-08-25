package com.devStudy.chatapp.auth.security.loginPassword;

import com.devStudy.chatapp.auth.model.User;
import com.devStudy.chatapp.auth.service.BlackListService;
import com.devStudy.chatapp.auth.service.JwtTokenService;
import com.devStudy.chatapp.auth.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtTokenService jwtTokenService;
    private final UserService userService;
    private final BlackListService blackListService;

    @Autowired
    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserService userService, BlackListService blackListService) {
        this.jwtTokenService = jwtTokenService;
        this.userService = userService;
        this.blackListService = blackListService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String jwtToken = jwtTokenService.getTokenFromCookie(request);

        if(jwtToken == null || blackListService.isTokenInBlackList(jwtToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String email = jwtTokenService.validateTokenAndGetEmail(jwtToken);
        if(email != null && SecurityContextHolder.getContext().getAuthentication() == null){
            try {
                UserDetails userDetails = userService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                usernamePasswordAuthenticationToken.setDetails(new WebAuthenticationDetails(request));
                SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                request.setAttribute("userId", ((User) userDetails).getId());
            }catch(UsernameNotFoundException e){
                LOGGER.warn("JWT token valid but user not found: {}. Passing to controller for handling.", email);
            }
        }
        filterChain.doFilter(request, response);
    }
}