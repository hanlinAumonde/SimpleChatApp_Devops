package com.devStudy.chatapp.auth.config;

import java.util.Map;
import java.util.function.Supplier;

import javax.sql.DataSource;

import com.devStudy.chatapp.auth.security.LoginAuthenticationFailureHandler;
import com.devStudy.chatapp.auth.security.loginPassword.AccountAuthenticationProvider;
import com.devStudy.chatapp.auth.security.LoginAuthenticationSuccessHandler;
import com.devStudy.chatapp.auth.security.loginPassword.JwtAuthenticationFilter;
import com.devStudy.chatapp.auth.security.loginVerificationCode.VerificationCodeAuthenticationFilter;
import com.devStudy.chatapp.auth.security.loginVerificationCode.VerificationCodeAuthenticationProvider;
import com.devStudy.chatapp.auth.service.BlackListService;
import com.devStudy.chatapp.auth.service.JwtTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import com.devStudy.chatapp.auth.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class SecurityConfig {
	
	@Value("${chatroomApp.rememberMe.key}")
	private String rememberMeKey;
	
	@Value("${chatroomApp.rememberMe.expirationTime}")
	private int rememberMeExpirationTime;

    @Value("${chatroomApp.VERIFICATION_CODE_LOGIN_ENDPOINT}")
    private String VERIFICATION_CODE_LOGIN_ENDPOINT;

    @Value("${chatroomApp.MAX_FAILED_ATTEMPTS}")
    private int maxFailedAttempts;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AccountAuthenticationProvider authProvider(
            PasswordEncoder passwordEncoder,
            UserService userService) {
		return new AccountAuthenticationProvider(passwordEncoder, userService, maxFailedAttempts);
	}

    @Bean
    AuthenticationManager authManager(HttpSecurity http,
                                      AccountAuthenticationProvider authProvider,
                                      VerificationCodeAuthenticationProvider verificationCodeAuthenticationProvider) throws Exception{
        AuthenticationManagerBuilder authenticationManagerBuilder =
                http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder.authenticationProvider(authProvider);
        authenticationManagerBuilder.authenticationProvider(verificationCodeAuthenticationProvider);
        return authenticationManagerBuilder.build();
    }

    @Bean
    VerificationCodeAuthenticationFilter verificationCodeAuthenticationFilter(AuthenticationManager authManager, JwtTokenService jwtTokenService) {
        VerificationCodeAuthenticationFilter filter = new VerificationCodeAuthenticationFilter(VERIFICATION_CODE_LOGIN_ENDPOINT);
        filter.setAuthenticationManager(authManager);
        filter.setAuthenticationSuccessHandler(new LoginAuthenticationSuccessHandler(jwtTokenService));
        filter.setAuthenticationFailureHandler(new LoginAuthenticationFailureHandler());
        return filter;
    }

    @Bean
    PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        return tokenRepository;
	}

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    PersistentTokenRepository persistentTokenRepository,
                                    UserService userDetailService,
                                    BlackListService blackListService,
                                    JwtAuthenticationFilter jwtAuthenticationFilter,
                                    VerificationCodeAuthenticationFilter verificationCodeAuthenticationFilter,
                                    JwtTokenService jwtTokenService) throws Exception {
        return http
                .cors(cors ->
        			cors.configurationSource(corsConfigurationSource())
        		)
          
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                
                .rememberMe(rememberMeConfig -> 
                	rememberMeConfig
	                	.tokenRepository(persistentTokenRepository)
	                    .key(rememberMeKey) 
	                    .tokenValiditySeconds(rememberMeExpirationTime)
	                    .userDetailsService(userDetailService)  
	                    .authenticationSuccessHandler(new LoginAuthenticationSuccessHandler(jwtTokenService))
                )
                
                .authorizeHttpRequests(auth -> 
                	auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .anyRequest().authenticated()
                )

                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(verificationCodeAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .formLogin(formLogin -> 
                	formLogin
						.loginProcessingUrl("/api/auth/login-process")
						.successHandler(new LoginAuthenticationSuccessHandler(jwtTokenService))
						.failureHandler(new LoginAuthenticationFailureHandler())
                )

                .exceptionHandling(exception -> 
                    exception.authenticationEntryPoint((request, response, authException) -> {
                        String jwtToken = jwtTokenService.getTokenFromCookie(request);
                        if(jwtToken != null && !blackListService.isTokenInBlackList(jwtToken)) {
                            blackListService.addTokenToBlackList(jwtToken, jwtTokenService.getExpirationDate(jwtToken).getTime());
                        }
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write(new ObjectMapper().writeValueAsString(
                                Map.ofEntries(
                                        Map.entry("status", "error"),
                                        Map.entry("message", "Unauthorized"),
                                        Map.entry("isAuthenticated", false)
                                )
                        ));
                    })
                )
                .build();
    }

    //Ref: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html#csrf-integration-javascript
    static final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {
        private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
        private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
            this.xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
            String headerValue = request.getHeader(csrfToken.getHeaderName());
            return (StringUtils.hasText(headerValue) ? this.plain : this.xor).resolveCsrfTokenValue(request, csrfToken);
        }
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
    	CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOriginPattern("*");
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfiguration);
        return source;
	}
}