package com.devStudy.chatapp.auth.security;

import com.devStudy.chatapp.auth.service.Implementation.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;

import com.devStudy.chatapp.auth.dto.DTOMapper;
import com.devStudy.chatapp.auth.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.devStudy.chatapp.auth.utils.ConstantValues.TOKEN_FLAG_LOGIN;

public class LoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    private final Logger logger = LoggerFactory.getLogger(LoginAuthenticationSuccessHandler.class);
	
    private static final ObjectMapper mapper = new ObjectMapper();

	private final JwtTokenService jwtTokenService;

	public LoginAuthenticationSuccessHandler(JwtTokenService jwtTokenService) {
		this.jwtTokenService = jwtTokenService;
	}

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
    	logger.info("login du user");
        logger.info("user's authorities : {}", ((User) authentication.getPrincipal()).getAuthorities());
    	
		response.setContentType("application/json;charset=UTF-8");
		response.setStatus(HttpServletResponse.SC_OK);
		
		Map<String,Object> result = new HashMap<>();
		result.put("status", "success");
		result.put("message", "Login successful");
		
		User user = (User)authentication.getPrincipal();
		result.put("UserInfo", DTOMapper.toUserDTO(user));
		String jwtToken = jwtTokenService.generateJwtToken(user.getUsername(), TOKEN_FLAG_LOGIN);
		result.put("isAuthenticated", true);

		// Store JWT token in cookie
		Cookie cookie = new Cookie("JWT-Token", jwtToken);
		cookie.setHttpOnly(true);
		cookie.setPath("/");
		cookie.setMaxAge(86400); // 1 day

		response.addCookie(cookie);

		String json = mapper.writeValueAsString(result);
        logger.info("json : {}", json);
		
		response.getWriter().write(json); 
    }
}